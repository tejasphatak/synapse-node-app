package sh.webmind.synapse.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val Mono = FontFamily.Monospace

val BgPrimary = Color(0xFF060814)
val BgSurface = Color(0xFF0A0D1C)
val BgElevated = Color(0xFF141828)

val Accent = Color(0xFF22C55E)            // Node green
val AccentLight = Color(0xFF7FEFB9)
val Warning = Color(0xFFFBBF24)
val Danger = Color(0xFFEF4444)
val Cyan = Color(0xFF22D3EE)
val Violet = Color(0xFFA78BFA)

val TextPrimary = Color(0xFFE8E8E8)
val TextSecondary = Color(0xFF9CA3AF)
val TextSubtle = Color(0xFF6B7280)
val BorderDim = Color(0xFF1F2937)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = BgPrimary,
    primaryContainer = Color(0xFF0F2919),
    secondary = Cyan,
    tertiary = Violet,
    background = BgPrimary,
    surface = BgSurface,
    surfaceVariant = BgElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDim,
    error = Danger,
)

@Composable
fun SynapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = Typography(), content = content)
}
