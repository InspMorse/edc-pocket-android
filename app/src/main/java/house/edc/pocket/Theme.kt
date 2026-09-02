package house.edc.pocket

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val EdcCyan = Color(0xFF22D3EE)
internal val EdcBg = Color(0xFF07080A)
internal val EdcSurface = Color(0xFF101216)
internal val EdcSurfaceHi = Color(0xFF181C22)
internal val EdcInk = Color(0xFFE2E8F0)
internal val EdcMuted = Color(0xFF94A3B8)

internal val LocalEdcAccent = staticCompositionLocalOf { EdcCyan }

internal val EdcAccent: Color
    @Composable get() = LocalEdcAccent.current

private fun edcColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF042026),
    primaryContainer = accent.copy(alpha = 0.22f),
    onPrimaryContainer = accent,
    secondary = accent,
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
fun EdcPocketTheme(
    hostAccent: Color? = null,
    content: @Composable () -> Unit,
) {
    val accent = hostAccent ?: EdcCyan
    CompositionLocalProvider(LocalEdcAccent provides accent) {
        MaterialTheme(
            colorScheme = edcColorScheme(accent),
            content = content,
        )
    }
}
