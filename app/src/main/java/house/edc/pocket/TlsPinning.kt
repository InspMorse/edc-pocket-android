package house.edc.pocket

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class TlsPinConfig(
    val hostPattern: String = "",
    val pinSha256: String = "",
) {
    val active: Boolean get() = hostPattern.isNotBlank() && pinSha256.isNotBlank()
}

object TlsPinning {
    fun buildClient(config: TlsPinConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (config.active) {
            builder.certificatePinner(
                CertificatePinner.Builder()
                    .add(config.hostPattern, "sha256/${config.pinSha256}")
                    .build(),
            )
        }
        return builder.build()
    }

    fun hostPattern(baseUrl: String): String {
        val uri = runCatching { java.net.URI(baseUrl) }.getOrNull() ?: return ""
        val host = uri.host ?: return ""
        return host
    }

    internal fun normalizePin(raw: String): String =
        raw.trim().removePrefix("sha256/").removePrefix("SHA256:")
}

internal fun parseTlsPinFromJson(root: org.json.JSONObject): String {
    val tls = root.optJSONObject("tls") ?: root.optJSONObject("security") ?: org.json.JSONObject()
    for (key in listOf("pin_sha256", "cert_pin", "sha256", "pin")) {
        val value = tls.optString(key).ifBlank { root.optString(key) }
        if (value.isNotBlank()) return TlsPinning.normalizePin(value)
    }
    return ""
}

internal fun parseLogoUrlFromJson(root: org.json.JSONObject): String {
    val branding = root.optJSONObject("branding") ?: org.json.JSONObject()
    for (obj in listOf(branding, root)) {
        for (key in listOf("logo", "logo_url", "icon", "icon_url")) {
            val value = obj.optString(key)
            if (value.isNotBlank() && value != "null") return value
        }
    }
    return ""
}
