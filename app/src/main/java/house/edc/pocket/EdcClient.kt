package house.edc.pocket

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class EdcClient(
    private val resolver: ContentResolver,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun url(base: String, path: String, identity: String): String {
        val root = base.trimEnd('/')
        val join = if (path.contains("?")) "&" else "?"
        return "$root$path${join}as=$identity"
    }

    private fun get(base: String, path: String, identity: String): Pair<Int, String> {
        val req = Request.Builder()
            .url(url(base, path, identity))
            .addHeader("from", identity)
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            return res.code to (res.body?.string().orEmpty())
        }
    }

    fun probeHealth(base: String, identity: String): HostHealth {
        val (code, body) = get(base, "/api/health", identity)
        if (code !in 200..299) error("Host answered $code")
        return parseHealth(body)
    }

    fun probe(base: String, identity: String): String = probeHealth(base, identity).summary()

    fun findReachableHost(settings: EdcSettings, identity: String): ReachableHost? {
        val candidates = linkedMapOf<String, HostPreset?>()
        if (settings.preset == HostPreset.CUSTOM) {
            if (settings.baseUrl.isNotBlank()) candidates[settings.baseUrl] = HostPreset.CUSTOM
        } else {
            candidates[HostPreset.LAN.url] = HostPreset.LAN
            candidates[HostPreset.TAILSCALE.url] = HostPreset.TAILSCALE
            val current = settings.baseUrl
            if (current.isNotBlank()) candidates[current] = settings.preset
        }
        for ((base, preset) in candidates) {
            val health = runCatching { probeHealth(base, identity) }.getOrNull() ?: continue
            if (health.ok) return ReachableHost(preset, base, health)
        }
        return null
    }

    fun load(base: String, identity: String): HostSnapshot {
        val clip = runCatching { get(base, "/api/clipboard", identity) }.getOrNull()
        val todo = runCatching { get(base, "/api/todo", identity) }.getOrNull()
        val drop = runCatching { get(base, "/api/incoming", identity) }.getOrNull()
        val clipOk = clip != null && clip.first in 200..299
        val todoOk = todo != null && todo.first in 200..299
        if (!clipOk && !todoOk) error("Host unreachable")
        val clips = if (clipOk) parseClips(clip!!.second) else emptyList()
        return HostSnapshot(
            latest = clips.firstOrNull(),
            history = clips,
            todos = if (todoOk) parseTodos(todo!!.second) else emptyList(),
            drops = if (drop != null && drop.first in 200..299) {
                parseDrops(drop.second, base)
            } else {
                emptyList()
            },
        )
    }

    fun todoPlainText(base: String, identity: String): String {
        val (code, body) = get(base, "/api/todo/text", identity)
        if (code in 200..299 && body.isNotBlank()) return body.trim()
        val snapshot = load(base, identity)
        return snapshot.todos.joinToString("\n") { item ->
            val mark = if (item.done) "☑" else "☐"
            "$mark ${item.text}"
        }
    }

    fun sendText(base: String, identity: String, text: String) {
        val body = """{"text":${text.json()},"from":${identity.json()}}""".toRequestBody(jsonType)
        post(base, "/api/clipboard", identity, body)
    }

    fun addTodo(base: String, identity: String, text: String) {
        val body =
            """{"text":${text.json()},"from":${identity.json()},"done":false}""".toRequestBody(jsonType)
        post(base, "/api/todo", identity, body)
    }

    fun toggleTodo(base: String, identity: String, id: String, done: Boolean) {
        val payload = """{"id":${id.json()},"done":$done,"from":${identity.json()}}"""
        val body = payload.toRequestBody(jsonType)
        val req = Request.Builder()
            .url(url(base, "/api/todo/${id.encode()}", identity))
            .addHeader("from", identity)
            .addHeader("content-type", "application/json")
            .patch(body)
            .build()
        http.newCall(req).execute().use { res ->
            if (res.isSuccessful) return
            if (res.code == 404 || res.code == 405) {
                val fallback =
                    """{"id":${id.json()},"done":$done,"from":${identity.json()},"action":"toggle"}"""
                        .toRequestBody(jsonType)
                post(base, "/api/todo", identity, fallback)
                return
            }
            error("Update failed (${res.code})")
        }
    }

    fun deleteTodo(base: String, identity: String, id: String) {
        val body = """{"from":${identity.json()}}""".toRequestBody(jsonType)
        val req = Request.Builder()
            .url(url(base, "/api/todo/${id.encode()}", identity))
            .addHeader("from", identity)
            .delete(body)
            .build()
        http.newCall(req).execute().use { res ->
            if (res.isSuccessful) return
            if (res.code == 404 || res.code == 405) {
                val fallback =
                    """{"id":${id.json()},"from":${identity.json()},"action":"delete"}"""
                        .toRequestBody(jsonType)
                post(base, "/api/todo", identity, fallback)
                return
            }
            error("Delete failed (${res.code})")
        }
    }

    fun downloadIncoming(base: String, identity: String, drop: DropItem): Pair<ByteArray, String> {
        val openUrl = incomingOpenUrl(base, drop) ?: error("No download link")
        val req = Request.Builder()
            .url(openUrl)
            .addHeader("from", identity)
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("Download failed (${res.code})")
            val bytes = res.body?.bytes() ?: error("Empty file")
            val mime = res.header("Content-Type")?.substringBefore(";")?.trim()
                ?: guessMime(drop.name)
            return bytes to mime
        }
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".webp", true) -> "image/webp"
        name.endsWith(".gif", true) -> "image/gif"
        name.endsWith(".heic", true) -> "image/heic"
        else -> "image/jpeg"
    }

    fun uploadImage(
        base: String,
        identity: String,
        uri: Uri,
        filename: String,
        session: String = "",
    ) {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image")
        val sessionClean = session.trim().trim('/', '\\')
        val baseName = filename.ifBlank { "photo.jpg" }
        val name = if (sessionClean.isEmpty()) {
            baseName
        } else {
            "$sessionClean/${baseName.substringAfterLast('/')}"
        }
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val paths = listOf("/api/drop", "/api/incoming", "/api/upload", "/drop", "/incoming")
        var last = "Upload failed"
        for (path in paths) {
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("from", identity)
                .addFormDataPart("filename", name)
                .addFormDataPart("file", name.substringAfterLast('/'), bytes.toRequestBody(mime.toMediaType()))
            if (sessionClean.isNotEmpty()) builder.addFormDataPart("session", sessionClean)
            val part = builder.build()
            val req = Request.Builder()
                .url(url(base, path, identity))
                .addHeader("from", identity)
                .post(part)
                .build()
            val (ok, code) = http.newCall(req).execute().use { res ->
                res.isSuccessful to res.code
            }
            if (ok) return
            last = "Upload failed ($code)"
            if (code != 404 && code != 405) break
        }
        error(last)
    }

    fun incomingOpenUrl(base: String, drop: DropItem): String? {
        if (drop.path.startsWith("http")) return drop.path
        val root = base.trimEnd('/')
        val rel = drop.path.trim().trimStart('/')
        if (rel.isNotEmpty()) {
            return "$root/$rel"
        }
        if (drop.id.isNotBlank()) {
            return "$root/api/incoming/${drop.id.encode()}"
        }
        return null
    }

    private fun post(base: String, path: String, identity: String, body: okhttp3.RequestBody) {
        val req = Request.Builder()
            .url(url(base, path, identity))
            .addHeader("from", identity)
            .post(body)
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("Request failed (${res.code})")
        }
    }

    private fun String.json(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun String.encode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
