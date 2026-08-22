package com.kvizo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Indigo = Color(0xFF6C4DF6)
val IndigoDark = Color(0xFF4B2FC9)
val Amber = Color(0xFFF5A623)
val Green = Color(0xFF2EBB6E)
val Red = Color(0xFFE5484D)
val Orange = Color(0xFFF57C2F)
val Grey = Color(0xFF9E9E9E)
val Night = Color(0xFF14121F)
val NightSurface = Color(0xFF1E1B2E)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF241254),
    secondary = Amber,
    onSecondary = Color.White,
    tertiary = Green,
    background = Color(0xFFF8F7FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EEF8),
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4A4FF),
    onPrimary = Color(0xFF2A1B66),
    primaryContainer = Color(0xFF45308F),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Amber,
    onSecondary = Color(0xFF3A2A00),
    tertiary = Color(0xFF5FDB9A),
    background = Night,
    surface = NightSurface,
    surfaceVariant = Color(0xFF2A2740),
    error = Color(0xFFFF6B70)
)

@Composable
fun QuizForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

/** Rounder, softer corners app-wide — cards 20dp, buttons 14dp, chips 10dp. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val Typography = androidx.compose.material3.Typography()
