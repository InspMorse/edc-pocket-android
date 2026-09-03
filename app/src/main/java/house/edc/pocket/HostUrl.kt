package house.edc.pocket

import java.net.URI

internal fun normalizeHostUrl(raw: String): String = raw.trim().trimEnd('/')

internal fun validateHostUrl(raw: String): String? {
    val url = normalizeHostUrl(raw)
    if (url.isEmpty()) return "URL cannot be empty"
    if (!url.contains("://")) return "Use http:// or https://"
    val uri = runCatching { URI(url) }.getOrNull() ?: return "Invalid URL"
    val scheme = uri.scheme?.lowercase()
    if (scheme !in setOf("http", "https")) return "Use http:// or https://"
    if (uri.host.isNullOrBlank()) return "Missing host name"
    val port = uri.port
    if (port != -1 && port !in 1..65535) return "Invalid port"
    return null
}
