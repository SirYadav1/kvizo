package com.quizforge.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.R
import com.quizforge.app.Routes
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.theme.Indigo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(vm: AppViewModel, nav: NavHostController) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(650))
        delay(500)
        if (vm.profile == null) nav.navigate(Routes.SETUP) { popUpTo(Routes.SPLASH) { inclusive = true } }
        else nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }
    val brand = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // soft radial brand glow behind the logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = brand
            drawCircle(color = c.copy(alpha = 0.06f), radius = size.minDimension * 0.42f, center = center)
            drawCircle(color = c.copy(alpha = 0.05f), radius = size.minDimension * 0.30f, center = center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "QuizForge logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = scale.value.coerceAtMost(1f)
                    }
            )
            Text(
                "QuizForge",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp).width(48.dp),
                thickness = 3.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                "Forge knowledge. Earn XP.",
                fontSize = 13.sp,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
