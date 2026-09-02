package house.edc.pocket

import org.json.JSONArray
import org.json.JSONObject

/**
 * The house host returns either a bare JSON array or an object that wraps the
 * array under a common key (e.g. {"items": [...]}). These helpers stay lenient
 * so a slightly different host shape still populates the UI instead of crashing.
 */
private fun asArray(body: String, vararg keys: String): JSONArray {
    val trimmed = body.trim()
    if (trimmed.isEmpty()) return JSONArray()
    return when (trimmed.first()) {
        '[' -> runCatching { JSONArray(trimmed) }.getOrDefault(JSONArray())
        '{' -> {
            val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return JSONArray()
            for (key in keys) {
                obj.optJSONArray(key)?.let { return it }
            }
            JSONArray()
        }
        else -> JSONArray()
    }
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        optJSONObject(i)?.let { out.add(transform(it)) }
    }
    return out
}

private fun JSONObject.str(vararg keys: String): String {
    for (key in keys) {
        if (has(key) && !isNull(key)) return optString(key)
    }
    return ""
}

fun parseClips(body: String): List<ClipEntry> =
    asArray(body, "items", "clips", "clipboard", "entries").mapObjects { obj ->
        ClipEntry(
            id = obj.str("id", "_id"),
            text = obj.str("text", "value", "content"),
            from = obj.str("from", "by", "author"),
            ts = obj.str("ts", "time", "at", "createdAt"),
        )
    }

fun parseTodos(body: String): List<TodoItem> =
    asArray(body, "items", "todos", "list", "entries").mapObjects { obj ->
        TodoItem(
            id = obj.str("id", "_id"),
            text = obj.str("text", "title", "value"),
            done = obj.optBoolean("done", obj.optBoolean("checked", false)),
            from = obj.str("from", "by", "author"),
            ts = obj.str("ts", "time", "at", "createdAt"),
        )
    }

fun parseDrops(body: String): List<DropItem> =
    asArray(body, "items", "drops", "incoming", "files", "entries").mapObjects { obj ->
        DropItem(
            id = obj.str("id", "_id"),
            name = obj.str("name", "filename", "file"),
            from = obj.str("from", "by", "author"),
            ts = obj.str("ts", "time", "at", "createdAt"),
            size = obj.optLong("size", obj.optLong("bytes", 0L)),
        )
    }
