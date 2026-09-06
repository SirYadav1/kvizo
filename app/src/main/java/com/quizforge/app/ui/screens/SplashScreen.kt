package com.quizforge.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizforge.app.R
import com.quizforge.app.Routes
import com.quizforge.app.ui.AppViewModel
import kotlinx.coroutines.delay

/**
 * Splash = official animated WebP intro (95 frames, light/dark variants),
 * followed by navigation. Mirrors the released v1.5.0 build.
 */
@Composable
fun SplashScreen(vm: AppViewModel, nav: NavHostController) {
    val dark = isSystemInDarkTheme()
    val animRes = if (dark) R.drawable.splash_anim_dark else R.drawable.splash_anim_light
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.playSplashJingle()
        delay(2600) // intro length (~95 frames) + a beat
        if (vm.profile == null) nav.navigate(Routes.SETUP) { popUpTo(Routes.SPLASH) { inclusive = true } }
        else nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF14121F) else Color(0xFFEDE9FE)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(animRes).build(),
            contentDescription = "Kvizo intro",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(360.dp)
        )
    }
}
