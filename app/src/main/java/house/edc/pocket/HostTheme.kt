package house.edc.pocket

import androidx.compose.ui.graphics.Color

internal fun parseThemeAccent(raw: String): Color? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    return when {
        text.startsWith("#") -> parseHexColor(text)
        text.startsWith("0x", ignoreCase = true) -> parseHexColor("#${text.drop(2)}")
        else -> null
    }
}

internal fun parseThemeAccentFromHealth(health: HostHealth?): Color? {
    health ?: return null
    return parseThemeAccent(health.themeAccent)
}

private fun parseHexColor(hex: String): Color? {
    val clean = hex.removePrefix("#")
    val value = runCatching { clean.toLong(16) }.getOrNull() ?: return null
    return when (clean.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> null
    }
}

internal fun parseThemeAccentFromJson(root: org.json.JSONObject): String {
    val theme = root.optJSONObject("theme") ?: org.json.JSONObject()
    for (obj in listOf(root, theme)) {
        for (key in listOf("accent", "accent_color", "primary", "primary_color", "theme_color", "color")) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)?.toString()?.trim().orEmpty()
            if (value.isNotEmpty() && value != "null") return value
        }
    }
    return ""
}
