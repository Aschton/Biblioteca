package cl.duoc.museo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF0E2A47)
val NavyLight = Color(0xFF1D4269)
val Gold = Color(0xFFB8945A)
val GoldLight = Color(0xFFEFE3C8)
val Cream = Color(0xFFF6F3EE)
val Teal = Color(0xFF2A8C82)
val TealLight = Color(0xFFD5ECE9)
val Success = Color(0xFF2E7D4F)
val Danger = Color(0xFFC0392B)
val Muted = Color(0xFF6B7280)

private fun colors(highContrast: Boolean) = lightColorScheme(
    primary = if (highContrast) Color(0xFF041627) else Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F2),
    onPrimaryContainer = Navy,
    secondary = if (highContrast) Color(0xFF7A5A1E) else Gold,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Color(0xFF4A3712),
    tertiary = Teal,
    tertiaryContainer = TealLight,
    background = if (highContrast) Color.White else Cream,
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF0ECE4),
    onSurfaceVariant = if (highContrast) Color(0xFF222222) else Muted,
    error = Danger
)

private fun typography(scale: Float): Typography {
    fun s(size: Int): TextUnit = (size * scale).sp
    val serif = FontFamily.Serif
    return Typography(
        displaySmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = s(34)),
        headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = s(26)),
        headlineSmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = s(22)),
        titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = s(20)),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = s(16)),
        titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = s(14)),
        bodyLarge = TextStyle(fontSize = s(16), lineHeight = s(22)),
        bodyMedium = TextStyle(fontSize = s(14), lineHeight = s(20)),
        bodySmall = TextStyle(fontSize = s(12), lineHeight = s(16)),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = s(14)),
        labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = s(12)),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = s(11))
    )
}

@Composable
fun MuseoTheme(largeText: Boolean = false, highContrast: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors(highContrast),
        typography = typography(if (largeText) 1.18f else 1f),
        content = content
    )
}
