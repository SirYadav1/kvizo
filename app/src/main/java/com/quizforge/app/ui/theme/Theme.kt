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
/* Soft-Pastel palette — lavender accents, off-white bg, charcoal text  */
/* ------------------------------------------------------------------ */

val Indigo = Color(0xFF8F7BF7)          // soft pastel lavender (primary)
val IndigoDark = Color(0xFF6F5AE6)      // deeper lavender for gradients/filled
val IndigoSoft = Color(0xFFEAE3FF)      // light lavender container
val Amber = Color(0xFFF2B24C)
val Green = Color(0xFF4CC38A)
val Red = Color(0xFFE5586B)
val Orange = Color(0xFFF08A5A)
val Grey = Color(0xFF9C97AC)
val Night = Color(0xFF14121D)
val NightSurface = Color(0xFF1D1A29)

/* Charcoal — text & primary buttons */
val Charcoal = Color(0xFF2F2B3A)
val CharcoalLight = Color(0xFF6E6879)

/* Surfaces — light (off-white w/ purple tint + pure white + lavender) */
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF2EEFC)   // light pastel lavender
val BackgroundLight = Color(0xFFF8F6FD)       // off-white w/ light purple tint
val OutlineLight = Color(0xFFEAE5F8)

/* Surfaces — dark (soft lavender-tinted night) */
val SurfaceDark = Color(0xFF1D1A29)
val SurfaceVariantDark = Color(0xFF282436)
val BackgroundDark = Color(0xFF14121D)
val OutlineDark = Color(0xFF35304A)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = Color(0xFF322A63),
    secondary = Charcoal,
    onSecondary = Color.White,
    tertiary = Green,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Charcoal,
    surface = SurfaceLight,
    onSurface = Charcoal,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = CharcoalLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFEFEBFA),
    error = Red,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3B6FF),
    onPrimary = Color(0xFF322A63),
    primaryContainer = Color(0xFF4F43A8),
    onPrimaryContainer = Color(0xFFEAE3FF),
    secondary = Color(0xFFE4E1EE),
    onSecondary = Color(0xFF2F2B3A),
    tertiary = Color(0xFF85D9AE),
    onTertiary = Color(0xFF0B3A26),
    background = BackgroundDark,
    onBackground = Color(0xFFE8E5F2),
    surface = SurfaceDark,
    onSurface = Color(0xFFE8E5F2),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB4AECE),
    outline = OutlineDark,
    outlineVariant = Color(0xFF322E46),
    error = Color(0xFFFF8FA0),
    onError = Color(0xFF3F0712)
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
/* Shapes — generous, rounded, soft                                     */
/* ------------------------------------------------------------------ */

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
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