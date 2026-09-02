package house.edc.pocket

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import java.net.URI
import java.util.regex.Pattern

private val addressPattern = Pattern.compile(
    """\b\d+\s+[A-Za-z0-9\s.'-]{2,40}\b(?:\s*(?:Street|St|Road|Rd|Avenue|Ave|Lane|Ln|Drive|Dr|Way|Court|Ct|Place|Pl)\.?)\b""",
    Pattern.CASE_INSENSITIVE,
)

internal fun firstPhone(text: String): String? {
    runCatching {
        val matcher = Patterns.PHONE.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group()?.trim().orEmpty()
            val digits = raw.count { it.isDigit() }
            if (digits in 7..15) return raw
        }
    }
    val fallback = Regex("""\+?\d[\d\s().-]{7,}\d""")
    return fallback.find(text)?.value?.trim()?.takeIf { phone ->
        phone.count { it.isDigit() } in 7..15
    }
}

internal fun firstAddress(text: String): String? {
    val matcher = addressPattern.matcher(text)
    return if (matcher.find()) matcher.group()?.trim() else null
}

internal fun linkPreviewLabel(url: String): String {
    return runCatching {
        val host = URI(url).host?.removePrefix("www.") ?: return url
        host
    }.getOrDefault(url)
}

internal fun dialPhone(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
    context.startActivity(intent)
}

internal fun openMaps(context: Context, address: String) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:0,0?q=${Uri.encode(address)}"),
    )
    context.startActivity(intent)
}
