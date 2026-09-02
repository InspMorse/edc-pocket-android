package house.edc.pocket

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OutboxKind {
    CLIP,
    LIST,
    PHOTO,
}

data class OutboxItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: OutboxKind,
    val text: String = "",
    val filename: String = "",
    val session: String = "",
    val imagePath: String = "",
    val mime: String = "image/jpeg",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun label(): String = when (kind) {
        OutboxKind.CLIP -> "Clipboard · ${text.preview()}"
        OutboxKind.LIST -> "List · ${text.preview()}"
        OutboxKind.PHOTO -> "Photo · ${filename.ifBlank { "image" }}"
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("kind", kind.name)
        put("text", text)
        put("filename", filename)
        put("session", session)
        put("imagePath", imagePath)
        put("mime", mime)
        put("createdAt", createdAt)
    }

    companion object {
        internal fun fromJson(obj: JSONObject): OutboxItem = OutboxItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            kind = runCatching { OutboxKind.valueOf(obj.getString("kind")) }
                .getOrDefault(OutboxKind.CLIP),
            text = obj.optString("text"),
            filename = obj.optString("filename"),
            session = obj.optString("session"),
            imagePath = obj.optString("imagePath"),
            mime = obj.optString("mime", "image/jpeg"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        )

        internal fun listFromJson(raw: String): List<OutboxItem> {
            if (raw.isBlank()) return emptyList()
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(fromJson(obj))
                }
            }
        }

        internal fun listToJson(items: List<OutboxItem>): String {
            val arr = JSONArray()
            items.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}

private fun String.preview(max: Int = 48): String {
    val t = trim()
    if (t.length <= max) return t.ifBlank { "…" }
    return t.take(max) + "…"
}
