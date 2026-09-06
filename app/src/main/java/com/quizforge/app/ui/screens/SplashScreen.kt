package com.quizforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.quizforge.app.R
import com.quizforge.app.Routes
import com.quizforge.app.ui.AppViewModel
import kotlinx.coroutines.delay

/**
 * Splash = official animated WebP intro (95 frames, light/dark variants),
 * followed by navigation. Mirrors the released v1.5.0 build.
 * painterResource uses the platform decoder — safe for animated WebP
 * (full animation on API 31+, first frame below that).
 */
@Composable
fun SplashScreen(vm: AppViewModel, nav: NavHostController) {
    val dark = isSystemInDarkTheme()
    val animRes = if (dark) R.drawable.splash_anim_dark else R.drawable.splash_anim_light

    LaunchedEffect(Unit) {
        try { vm.playSplashJingle() } catch (_: Exception) {}
        delay(2600) // intro length (~95 frames) + a beat
        try {
            if (vm.profile == null) nav.navigate(Routes.SETUP) { popUpTo(Routes.SPLASH) { inclusive = true } }
            else nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
        } catch (_: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF14121F) else Color(0xFFEDE9FE)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(animRes),
            contentDescription = "Kvizo intro",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(360.dp)
        )
    }
}
