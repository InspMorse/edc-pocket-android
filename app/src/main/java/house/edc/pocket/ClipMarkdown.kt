package house.edc.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal data class MarkdownSegment(
    val text: String,
    val isCodeBlock: Boolean = false,
)

internal fun splitMarkdownSegments(raw: String): List<MarkdownSegment> {
    if (!raw.contains("```")) {
        return listOf(MarkdownSegment(raw))
    }
    val segments = mutableListOf<MarkdownSegment>()
    var rest = raw
    while (rest.contains("```")) {
        val start = rest.indexOf("```")
        if (start > 0) segments += MarkdownSegment(rest.substring(0, start))
        val afterFence = rest.substring(start + 3)
        val langEnd = afterFence.indexOf('\n')
        val codeStart = if (langEnd >= 0) langEnd + 1 else 0
        val end = afterFence.indexOf("```", codeStart)
        if (end < 0) {
            segments += MarkdownSegment(rest.substring(start))
            return segments
        }
        val code = afterFence.substring(codeStart, end).trimEnd()
        segments += MarkdownSegment(code, isCodeBlock = true)
        rest = afterFence.substring(end + 3)
    }
    if (rest.isNotEmpty()) segments += MarkdownSegment(rest)
    return segments.ifEmpty { listOf(MarkdownSegment(raw)) }
}

internal fun annotateInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000))) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
internal fun ClipMarkdownBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onOpenUrl: (String) -> Unit,
) {
    val segments = splitMarkdownSegments(text)
    Column(modifier = modifier) {
        segments.forEach { segment ->
            if (segment.isCodeBlock) {
                Text(
                    text = segment.text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2830))
                        .padding(8.dp),
                )
            } else {
                LinkifiedText(
                    text = segment.text,
                    color = color,
                    linkColor = EdcAccent,
                    onOpenUrl = onOpenUrl,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
