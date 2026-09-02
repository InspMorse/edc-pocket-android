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

    fun probe(base: String, identity: String): String {
        val (code, _) = get(base, "/api/clipboard", identity)
        if (code in 200..299) return "Host reached."
        error("Host answered $code")
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
            drops = if (drop != null && drop.first in 200..299) parseDrops(drop.second) else emptyList(),
        )
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

    fun uploadImage(base: String, identity: String, uri: Uri, filename: String) {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image")
        val name = filename.ifBlank { "photo.jpg" }
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val paths = listOf("/api/drop", "/api/incoming", "/api/upload", "/drop", "/incoming")
        var last = "Upload failed"
        for (path in paths) {
            val part = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("from", identity)
                .addFormDataPart("filename", name)
                .addFormDataPart("file", name, bytes.toRequestBody(mime.toMediaType()))
                .build()
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
