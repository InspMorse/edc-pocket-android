package house.edc.pocket

import org.json.JSONObject

internal fun parseFeatureFlags(root: JSONObject): Map<String, Boolean> {
    val flagsObj = root.optJSONObject("feature_flags")
        ?: root.optJSONObject("flags")
        ?: return emptyMap()
    return buildMap {
        flagsObj.keys().forEach { key ->
            when (val value = flagsObj.opt(key)) {
                is Boolean -> put(key, value)
                is Number -> put(key, value.toInt() != 0)
                is String -> {
                    val t = value.trim().lowercase()
                    when (t) {
                        "true", "1", "yes", "on" -> put(key, true)
                        "false", "0", "no", "off" -> put(key, false)
                    }
                }
            }
        }
    }
}

object FeatureFlags {
    fun effectiveCapabilities(health: HostHealth?): HostCapabilities {
        val base = health?.capabilities ?: HostCapabilities.ALL
        val flags = health?.featureFlags.orEmpty()
        if (flags.isEmpty()) return base
        return HostCapabilities(
            clipboard = base.clipboard && flagEnabled(flags, "clipboard", default = true),
            todo = base.todo && flagEnabled(flags, "todo", "list", default = true),
            todoDelete = base.todoDelete && flagEnabled(flags, "todo_delete", default = true),
            todoText = base.todoText && flagEnabled(flags, "todo_text", default = true),
            incoming = base.incoming && flagEnabled(flags, "incoming", "drop", default = true),
            upload = base.upload && flagEnabled(flags, "upload", default = true),
            sessionUpload = base.sessionUpload && flagEnabled(flags, "session", "session_upload", default = true),
            dashboard = base.dashboard && flagEnabled(flags, "dashboard", default = true),
            conditionalFetch = base.conditionalFetch && flagEnabled(flags, "conditional_fetch", "etag", default = true),
            sse = base.sse && flagEnabled(flags, "sse", "events", default = true),
            websocket = base.websocket && flagEnabled(flags, "websocket", "ws", default = true),
            push = base.push && flagEnabled(flags, "push", "fcm", default = true),
        )
    }

    fun disabledSummary(health: HostHealth?): List<String> {
        val caps = effectiveCapabilities(health)
        val base = health?.capabilities ?: HostCapabilities.ALL
        return buildList {
            if (base.clipboard && !caps.clipboard) add("Clipboard disabled by host flag")
            if (base.todo && !caps.todo) add("List disabled by host flag")
            if (base.incoming && !caps.incoming) add("Incoming disabled by host flag")
            if (base.upload && !caps.upload) add("Upload disabled by host flag")
            if (base.dashboard && !caps.dashboard) add("Dashboard disabled by host flag")
        }
    }

    private fun flagEnabled(flags: Map<String, Boolean>, vararg keys: String, default: Boolean): Boolean {
        keys.forEach { key ->
            flags[key]?.let { return it }
            flags[key.lowercase()]?.let { return it }
        }
        return default
    }
}
