package com.quizforge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizforge.app.R

/* ------------------------------------------------------------------ */
/* "Forge" design system — Figma violet: #7C3AED → #A78BFA             */
/* Light surfaces tinted #F5F3FF, Space Grotesk display type           */
/* ------------------------------------------------------------------ */

/* Primary violet family */
val Violet = Color(0xFF7C3AED)          // Figma primary
val VioletDeep = Color(0xFF6D28D9)      // deeper violet for dark elements
val VioletGrad = Color(0xFFA78BFA)      // Figma gradient end
val VioletLight = Color(0xFF9B72F8)     // icon / mid tints
val VioletPale = Color(0xFFEDE9FE)      // soft container (Figma primaryPale)
val VioletPale2 = Color(0xFFF0EBFF)     // lighter container
val VioletBorderStrong = Color(0xFFC4B5FD)
val VioletBorder = Color(0xFFE4DAFF)

/* Text */
val Ink = Color(0xFF1E1333)             // Figma text (deep purple-black)
val InkSub = Color(0xFF6B5B8A)          // secondary text
val InkMuted = Color(0xFFA094B8)        // muted text

/* Semantic colors (Figma) */
val Green = Color(0xFF059669)
val GreenBg = Color(0xFFECFDF5)
val GreenBorder = Color(0xFFA7F3D0)
val Red = Color(0xFFDC2626)
val RedBg = Color(0xFFFEF2F2)
val RedBorder = Color(0xFFFECACA)
val Amber = Color(0xFFB45309)
val AmberBg = Color(0xFFFFFBEB)
val AmberBorder = Color(0xFFFDE68A)
val Orange = Color(0xFFF57C2F)

/* Surfaces — light (Figma: bg #F5F3FF, surface white, alt #EDE9FE) */
val BackgroundLight = Color(0xFFF5F3FF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceAltLight = Color(0xFFEDE9FE)
val OutlineLight = Color(0xFFE4DAFF)
val OutlineStrongLight = Color(0xFFC4B5FD)

/* Surfaces — dark (deep violet-tinted night) */
val SurfaceDark = Color(0xFF1D1828)
val SurfaceAltDark = Color(0xFF251E36)
val BackgroundDark = Color(0xFF14101E)
val OutlineDark = Color(0xFF2F2747)
val OutlineStrongDark = Color(0xFF3E3260)

/* Legacy aliases — kept so existing screens keep compiling */
val Indigo = Violet
val IndigoDark = VioletDeep
val IndigoSoft = VioletPale

/* Gradient — Figma linear-gradient(135deg, #7C3AED, #A78BFA) */
val VioletGradient: Brush = Brush.linearGradient(listOf(Violet, VioletGrad))

/** Dark-aware violet gradient (brighter in dark mode for contrast). */
@Composable
fun violetGradient(): Brush =
    if (isSystemInDarkTheme()) Brush.linearGradient(listOf(Color(0xFFB3A0FF), Color(0xFFC4B5FD)))
    else VioletGradient

/* Dark-aware semantic fills — light pastels are blinding on dark surfaces */
@Composable
fun greenBg(): Color = if (isSystemInDarkTheme()) Color(0xFF0D2B1F) else GreenBg
@Composable
fun redBg(): Color = if (isSystemInDarkTheme()) Color(0xFF3A1216) else RedBg
@Composable
fun amberBg(): Color = if (isSystemInDarkTheme()) Color(0xFF2E2410) else AmberBg
@Composable
fun violetPale(): Color = if (isSystemInDarkTheme()) Color(0xFF2A2350) else VioletPale

/** Figma glow shadow: 0 2px 20px rgba(124,58,237,0.1) */
val FigmaGlow = Color(0x1A7C3AED)

/* Space Grotesk (variable font, wght 300–700) */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.space_grotesk, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.space_grotesk, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.space_grotesk, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.space_grotesk, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletPale,
    onPrimaryContainer = Color(0xFF3B1E8F),
    secondary = VioletDeep,
    onSecondary = Color.White,
    tertiary = Green,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceAltLight,
    onSurfaceVariant = InkSub,
    outline = OutlineLight,
    outlineVariant = Color(0xFFF0EBFF),
    error = Red,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3A0FF),
    onPrimary = Color(0xFF2A135F),
    primaryContainer = Color(0xFF4C3A8F),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFE9E4FA),
    onSecondary = Color(0xFF2A135F),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    background = BackgroundDark,
    onBackground = Color(0xFFF1EDFB),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1EDFB),
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = Color(0xFFB6ACD6),
    outline = OutlineDark,
    outlineVariant = Color(0xFF2A2340),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

/* ------------------------------------------------------------------ */
/* Typography — Space Grotesk for display/headings, system for body    */
/* ------------------------------------------------------------------ */

private val Base = androidx.compose.material3.Typography()

val QuizForgeTypography = androidx.compose.material3.Typography(
    displaySmall = Base.displaySmall.copy(fontFamily = SpaceGrotesk, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = Base.headlineLarge.copy(fontFamily = SpaceGrotesk, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = Base.headlineMedium.copy(fontFamily = SpaceGrotesk, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineSmall = Base.headlineSmall.copy(fontFamily = SpaceGrotesk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = SpaceGrotesk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontFamily = SpaceGrotesk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

/* ------------------------------------------------------------------ */
/* Shapes — Figma radii: 10, 12, 14, 16, 18, 26, 99                    */
/* ------------------------------------------------------------------ */

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
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
