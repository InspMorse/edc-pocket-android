package house.edc.pocket

import org.json.JSONObject
import java.net.URLDecoder

data class PairQrPayload(
    val url: String,
    val name: String = "",
    val pinSha256: String = "",
)

internal fun parsePairQr(raw: String): PairQrPayload? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("{")) {
        val obj = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val url = obj.optString("url").ifBlank { obj.optString("host") }
        if (url.isBlank()) return null
        return PairQrPayload(
            url = url,
            name = obj.optString("name", obj.optString("profile", "")),
            pinSha256 = obj.optString("pin_sha256", obj.optString("tls_pin", "")),
        )
    }
    if (text.startsWith("edc://pair", ignoreCase = true)) {
        val query = text.substringAfter('?', "")
        if (query.isEmpty() && !text.contains('?')) return null
        val params = parsePairQuery(query)
        val url = params["url"].orEmpty()
        if (url.isBlank()) return null
        return PairQrPayload(
            url = url,
            name = params["name"].orEmpty(),
            pinSha256 = params["pin"].orEmpty(),
        )
    }
    if (text.contains("://")) {
        return PairQrPayload(url = text)
    }
    return null
}

private fun parsePairQuery(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    return buildMap {
        query.split("&").forEach { part ->
            if (part.isBlank()) return@forEach
            val idx = part.indexOf('=')
            if (idx <= 0) return@forEach
            val key = part.substring(0, idx)
            val value = URLDecoder.decode(part.substring(idx + 1), Charsets.UTF_8.name())
            put(key, value)
        }
    }
}
