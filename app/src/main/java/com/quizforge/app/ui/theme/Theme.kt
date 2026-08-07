package com.quizforge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ */
/* Brand palette — a deep, refined violet with clean near-white neutrals */
/* ------------------------------------------------------------------ */

val Indigo = Color(0xFF5A46E0)          // primary violet
val IndigoDark = Color(0xFF4736C0)
val IndigoSoft = Color(0xFFE9E5FF)      // primary container (light)
val Amber = Color(0xFFF2A33C)
val Green = Color(0xFF2FB47C)
val Red = Color(0xFFE5484D)
val Orange = Color(0xFFF57C2F)
val Grey = Color(0xFF9AA0A6)
val Night = Color(0xFF0F0D17)
val NightSurface = Color(0xFF171521)

/* Surfaces — light */
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0EEF9)
val BackgroundLight = Color(0xFFF6F6FB)
val OutlineLight = Color(0xFFE4E1F1)

/* Surfaces — dark */
val SurfaceDark = Color(0xFF171521)
val SurfaceVariantDark = Color(0xFF221F33)
val BackgroundDark = Color(0xFF0F0D17)
val OutlineDark = Color(0xFF2C2940)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = Color(0xFF221554),
    secondary = Amber,
    onSecondary = Color.White,
    tertiary = Green,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF191A22),
    surface = SurfaceLight,
    onSurface = Color(0xFF191A22),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF555565),
    outline = OutlineLight,
    outlineVariant = Color(0xFFECEAf6),
    error = Red,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFACA4FF),
    onPrimary = Color(0xFF241764),
    primaryContainer = Color(0xFF4636A8),
    onPrimaryContainer = Color(0xFFE9E4FF),
    secondary = Color(0xFFFFC566),
    onSecondary = Color(0xFF3E2A00),
    tertiary = Color(0xFF7FD9AC),
    onTertiary = Color(0xFF003825),
    background = BackgroundDark,
    onBackground = Color(0xFFECEBF4),
    surface = SurfaceDark,
    onSurface = Color(0xFFECEBF4),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB9B6CC),
    outline = OutlineDark,
    outlineVariant = Color(0xFF2B2840),
    error = Color(0xFFFF8A90),
    onError = Color(0xFF3A0003)
)

/* ------------------------------------------------------------------ */
/* Typography — clean, slightly tighter leading for a premium feel     */
/* ------------------------------------------------------------------ */

private val Base = androidx.compose.material3.Typography()

val QuizForgeTypography = androidx.compose.material3.Typography(
    displaySmall = Base.displaySmall.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = Base.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = Base.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineSmall = Base.headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

/* ------------------------------------------------------------------ */
/* Shapes — generous, consistent radii                                 */
/* ------------------------------------------------------------------ */

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QuizForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = QuizForgeTypography,
        shapes = AppShapes,
        content = content
    )
}