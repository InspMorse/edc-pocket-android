package house.edc.pocket

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun parseClips(raw: String): List<ClipEntry> {
    val root = parseJson(raw) ?: return clipFromPlain(raw)
    val latest = mutableListOf<ClipEntry>()
    val rest = mutableListOf<ClipEntry>()

    fun take(obj: JSONObject, newest: Boolean = false) {
        val entry = obj.toClip() ?: return
        if (newest) latest += entry else rest += entry
    }

    when (root) {
        is JSONArray -> root.objects().forEach { take(it) }
        is JSONObject -> {
            for (key in listOf("latest", "clip", "current")) {
                when (val value = root.opt(key)) {
                    is JSONObject -> take(value, newest = true)
                    is String -> take(JSONObject().put("text", value), newest = true)
                }
            }
            for (key in listOf("history", "clips", "items", "data", "clipboard", "records")) {
                root.optJSONArray(key)?.objects()?.forEach { take(it) }
            }
            if (latest.isEmpty()) take(root, newest = true)
        }
        is String -> clipFromPlain(root).forEach { rest += it }
    }

    val seen = HashSet<String>()
    return (latest + rest).filter { seen.add("${it.id}|${it.text}|${it.ts}") }
}

internal fun parseTodos(raw: String): List<TodoItem> {
    val root = parseJson(raw) ?: return emptyList()
    val items = ArrayList<TodoItem>()
    fun take(obj: JSONObject) {
        obj.toTodo()?.let { items += it }
    }
    when (root) {
        is JSONArray -> root.objects().forEach(::take)
        is JSONObject -> {
            var nested = false
            for (key in listOf("todos", "items", "list", "data", "tasks")) {
                val arr = root.optJSONArray(key)
                if (arr != null) {
                    nested = true
                    arr.objects().forEach(::take)
                }
            }
            if (!nested) take(root)
        }
    }
    return items
}

internal fun parseDrops(raw: String): List<DropItem> {
    val root = parseJson(raw) ?: return emptyList()
    val items = ArrayList<DropItem>()
    fun take(obj: JSONObject) {
        obj.toDrop()?.let { items += it }
    }
    when (root) {
        is JSONArray -> root.objects().forEach(::take)
        is JSONObject -> {
            var nested = false
            for (key in listOf("drops", "incoming", "files", "items", "data")) {
                val arr = root.optJSONArray(key)
                if (arr != null) {
                    nested = true
                    arr.objects().forEach(::take)
                }
            }
            if (!nested) take(root)
        }
    }
    return items
}

private fun parseJson(raw: String): Any? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    return try {
        when {
            text.startsWith("[") -> JSONArray(text)
            text.startsWith("{") -> JSONObject(text)
            text.startsWith("\"") && text.endsWith("\"") -> JSONObject("{\"text\":$text}").optString("text")
            else -> text
        }
    } catch (_: Exception) {
        null
    }
}

private fun clipFromPlain(raw: String): List<ClipEntry> {
    val text = raw.trim().trim('"')
    if (text.isEmpty() || text.startsWith("<")) return emptyList()
    return listOf(
        ClipEntry(
            id = UUID.randomUUID().toString(),
            text = text,
            from = "",
            ts = "",
        ),
    )
}

private fun JSONObject.toClip(): ClipEntry? {
    val text = str("text", "clip", "content", "value", "body", "message")
    if (text.isEmpty()) return null
    return ClipEntry(
        id = str("id", "_id", "uuid", fallback = UUID.randomUUID().toString()),
        text = text,
        from = str("from", "by", "user", "as", "identity", "who"),
        ts = str("ts", "time", "timestamp", "at", "created", "created_at", "date"),
    )
}

private fun JSONObject.toTodo(): TodoItem? {
    val text = str("text", "title", "item", "content", "value", "body")
    if (text.isEmpty()) return null
    return TodoItem(
        id = str("id", "_id", "uuid", fallback = UUID.randomUUID().toString()),
        text = text,
        done = bool("done", "checked", "complete", "completed"),
        from = str("from", "by", "user", "as", "identity", "who"),
        ts = str("ts", "time", "timestamp", "at", "created", "created_at", "date"),
    )
}

private fun JSONObject.toDrop(): DropItem? {
    val name = str("name", "filename", "file", "title", "path")
    if (name.isEmpty()) return null
    return DropItem(
        id = str("id", "_id", "uuid", fallback = name),
        name = name.substringAfterLast('/'),
        from = str("from", "by", "user", "as", "identity", "who"),
        ts = str("ts", "time", "timestamp", "at", "created", "created_at", "date"),
        size = num("size", "bytes", "length"),
    )
}

private fun JSONArray.objects(): List<JSONObject> {
    val out = ArrayList<JSONObject>(length())
    for (i in 0 until length()) {
        when (val value = opt(i)) {
            is JSONObject -> out += value
            is String -> {
                val obj = JSONObject()
                obj.put("text", value)
                obj.put("name", value)
                out += obj
            }
        }
    }
    return out
}

private fun JSONObject.str(vararg names: String, fallback: String = ""): String {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        val value = opt(name) ?: continue
        if (value is JSONObject || value is JSONArray) continue
        val text = value.toString().trim()
        if (text.isNotEmpty() && text != "null") return text
    }
    return fallback
}

private fun JSONObject.bool(vararg names: String): Boolean {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        when (val value = opt(name)) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
            is String -> {
                val t = value.trim().lowercase()
                if (t in setOf("true", "1", "yes", "done", "checked")) return true
                if (t in setOf("false", "0", "no")) return false
            }
        }
    }
    return false
}

private fun JSONObject.num(vararg names: String): Long {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        when (val value = opt(name)) {
            is Number -> return value.toLong()
            is String -> value.toLongOrNull()?.let { return it }
        }
    }
    return 0L
}
