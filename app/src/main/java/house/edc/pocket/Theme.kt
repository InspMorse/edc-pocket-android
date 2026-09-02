package house.edc.pocket

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val EdcCyan = Color(0xFF22D3EE)
internal val EdcBg = Color(0xFF07080A)
internal val EdcSurface = Color(0xFF101216)
internal val EdcSurfaceHi = Color(0xFF181C22)
internal val EdcInk = Color(0xFFE2E8F0)
internal val EdcMuted = Color(0xFF94A3B8)

private val EdcColors = darkColorScheme(
    primary = EdcCyan,
    onPrimary = Color(0xFF042026),
    primaryContainer = Color(0xFF0E3A43),
    onPrimaryContainer = EdcCyan,
    secondary = EdcCyan,
    onSecondary = Color(0xFF042026),
    background = EdcBg,
    onBackground = EdcInk,
    surface = EdcSurface,
    onSurface = EdcInk,
    surfaceVariant = EdcSurfaceHi,
    onSurfaceVariant = EdcMuted,
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
)

@Composable
fun EdcPocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EdcColors,
        content = content,
    )
}
