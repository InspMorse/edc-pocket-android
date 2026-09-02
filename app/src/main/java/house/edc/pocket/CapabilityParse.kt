package house.edc.pocket

internal fun parseCapabilities(root: org.json.JSONObject): HostCapabilities {
    val capsObj = root.optJSONObject("capabilities")
        ?: root.optJSONObject("features")
        ?: root
    val featureNames = root.optJSONArray("features")?.let { arr ->
        buildSet {
            for (i in 0 until arr.length()) {
                when (val value = arr.opt(i)) {
                    is String -> add(value.trim().lowercase())
                }
            }
        }
    } ?: emptySet()

    if (featureNames.isNotEmpty()) {
        fun listed(vararg names: String): Boolean =
            names.any { featureNames.contains(it.lowercase()) }
        return HostCapabilities(
            clipboard = listed("clipboard", "clip"),
            todo = listed("todo", "todos", "list"),
            todoDelete = listed("todo_delete", "delete_todo", "todo_delete"),
            todoText = listed("todo_text", "todo-text", "list_text"),
            incoming = listed("incoming", "drop", "upload"),
            upload = listed("upload", "incoming", "drop", "photo"),
            sessionUpload = listed("session", "session_upload", "sessions"),
            dashboard = listed("dashboard", "web", "links"),
        )
    }

    return HostCapabilities(
        clipboard = capsObj.flag("clipboard", default = true),
        todo = capsObj.flag("todo", "todos", default = true),
        todoDelete = capsObj.flag("todo_delete", "delete_todo", "todoDelete", default = true),
        todoText = capsObj.flag("todo_text", "todoText", default = true),
        incoming = capsObj.flag("incoming", "drop", default = true),
        upload = capsObj.flag("upload", "photo_upload", default = true),
        sessionUpload = capsObj.flag("session", "session_upload", default = true),
        dashboard = capsObj.flag("dashboard", "dashboard_links", default = true),
    )
}

internal fun parseKnownUsers(root: org.json.JSONObject): List<String> {
    for (key in listOf("users", "identities", "people", "allowed_users")) {
        val arr = root.optJSONArray(key) ?: continue
        val names = buildList {
            for (i in 0 until arr.length()) {
                when (val value = arr.opt(i)) {
                    is String -> if (value.isNotBlank()) add(value.trim())
                    is org.json.JSONObject -> {
                        val name = value.str("name", "id", "identity", "user")
                        if (name.isNotBlank()) add(name)
                    }
                }
            }
        }
        if (names.isNotEmpty()) return names.distinct()
    }
    return emptyList()
}

internal fun parseLinkTemplates(root: org.json.JSONObject): HostLinkTemplates {
    val links = root.optJSONObject("links")
        ?: root.optJSONObject("dashboard_links")
        ?: org.json.JSONObject()
    return HostLinkTemplates(
        dashboardBase = root.str("dashboard_url", "url"),
        clipboardItem = links.str("clipboard", "clipboard_item", "clip"),
        todoItem = links.str("todo", "todo_item", "list"),
    )
}

data class HostLinkTemplates(
    val dashboardBase: String = "",
    val clipboardItem: String = "",
    val todoItem: String = "",
)

private fun org.json.JSONObject.flag(vararg keys: String, default: Boolean): Boolean {
    for (key in keys) {
        if (!has(key) || isNull(key)) continue
        when (val value = opt(key)) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
            is String -> {
                val t = value.trim().lowercase()
                if (t in setOf("false", "0", "no", "off")) return false
                if (t in setOf("true", "1", "yes", "on")) return true
            }
        }
    }
    return default
}

private fun org.json.JSONObject.str(vararg names: String, fallback: String = ""): String {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        val value = opt(name) ?: continue
        if (value is org.json.JSONObject || value is org.json.JSONArray) continue
        val text = value.toString().trim()
        if (text.isNotEmpty() && text != "null") return text
    }
    return fallback
}
