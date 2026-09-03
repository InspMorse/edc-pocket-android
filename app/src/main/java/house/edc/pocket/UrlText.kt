package house.edc.pocket

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

private val urlPattern = Pattern.compile("(https?://[^\\s]+)", Pattern.CASE_INSENSITIVE)

internal fun firstUrl(text: String): String? {
    val m = urlPattern.matcher(text)
    return if (m.find()) m.group(1)?.trimEnd('.', ',', ';', ')') else null
}

internal fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@Composable
internal fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = EdcCyan,
    maxLines: Int = Int.MAX_VALUE,
    onOpenUrl: (String) -> Unit,
) {
    val annotated = buildAnnotatedString {
        val matcher = urlPattern.matcher(text)
        var last = 0
        while (matcher.find()) {
            if (matcher.start() > last) append(text.substring(last, matcher.start()))
            val url = matcher.group(1) ?: continue
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            ) {
                append(url)
            }
            pop()
            last = matcher.end()
        }
        if (last < text.length) append(text.substring(last))
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = color),
        maxLines = maxLines,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.item
                ?.let { url -> onOpenUrl(url) }
        },
    )
}
