package house.edc.pocket

import org.json.JSONObject

data class HostRateHint(
    val message: String,
    val retryAfterSec: Long = 0L,
)

class HostRateLimitedException(val hint: HostRateHint) : Exception(hint.message)

internal fun parseRateHintFromHealth(root: JSONObject): HostRateHint? {
    val direct = root.str("rate_limit_message", "abuse_hint", "rate_limit_hint")
    if (direct.isNotBlank()) {
        return HostRateHint(
            message = direct,
            retryAfterSec = root.num("retry_after", "retry_after_sec"),
        )
    }
    root.optJSONObject("rate_limit")?.let { obj ->
        val msg = obj.str("message", "hint", "detail")
        if (msg.isNotBlank()) {
            return HostRateHint(msg, obj.num("retry_after", "retry_after_sec"))
        }
    }
    return null
}

internal fun rateHintFromHttp(code: Int, retryAfterHeader: String?, body: String): HostRateHint? {
    if (code != 429 && code != 503) return null
    val parsed = runCatching { JSONObject(body) }.getOrNull()?.let { parseRateHintFromHealth(it) }
    if (parsed != null) return parsed
    val retry = retryAfterHeader?.toLongOrNull() ?: 0L
    val msg = when (code) {
        429 -> "Host is rate-limiting requests. Slow down and try again."
        else -> "Host asked the client to wait before retrying."
    }
    return HostRateHint(message = msg, retryAfterSec = retry)
}

private fun JSONObject.str(vararg names: String): String {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        val value = opt(name)?.toString()?.trim().orEmpty()
        if (value.isNotEmpty() && value != "null") return value
    }
    return ""
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
