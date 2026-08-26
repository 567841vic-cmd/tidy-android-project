package tw.vic.tidy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Palette: a cool ink base read as "the volume", brass for anything the
 * user can reclaim, jade for headroom that already exists, clay for
 * anything worth a second look before deleting.
 */
object T {
    val Ink900 = Color(0xFF10151C)
    val Ink800 = Color(0xFF171E28)
    val Ink700 = Color(0xFF1F2836)
    val Line = Color(0xFF2E3A4A)

    val Mist = Color(0xFFE4E8EE)
    val MistDim = Color(0xFF8C97A8)
    val MistFaint = Color(0xFF5B6779)

    val Brass = Color(0xFFC8A24A)
    val BrassDeep = Color(0xFF6B5526)
    val Jade = Color(0xFF4FB59A)
    val Clay = Color(0xFFC4634A)
    val Violet = Color(0xFF7C6BA8)
    val Slate = Color(0xFF44566E)
    val Steel = Color(0xFF5E7690)
}

object Ty {
    val Display = TextStyle(
        fontSize = 42.sp, fontWeight = FontWeight.Light,
        letterSpacing = (-1.5).sp, color = T.Mist
    )
    val Number = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, color = T.Mist)
    val Title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = T.Mist)
    val Body = TextStyle(fontSize = 14.sp, color = T.Mist)
    val BodyDim = TextStyle(fontSize = 14.sp, color = T.MistDim)
    val Caption = TextStyle(fontSize = 12.sp, color = T.MistDim)
    val Mono = TextStyle(fontSize = 11.sp, color = T.MistFaint)

    /** Eyebrow labels. Wide tracking so short CJK labels read as structure. */
    val Label = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 2.2.sp, color = T.MistDim
    )
}

private val TidyColors = darkColorScheme(
    primary = T.Brass,
    onPrimary = T.Ink900,
    secondary = T.Jade,
    onSecondary = T.Ink900,
    error = T.Clay,
    background = T.Ink900,
    onBackground = T.Mist,
    surface = T.Ink800,
    onSurface = T.Mist,
    surfaceVariant = T.Ink700,
    onSurfaceVariant = T.MistDim,
    outline = T.Line
)

@Composable
fun TidyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TidyColors, content = content)
}
