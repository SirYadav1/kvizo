package com.kvizo.app.ui.screens

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.kvizo.app.Routes
import com.kvizo.app.ui.AppViewModel
import kotlinx.coroutines.delay

/**
 * Splash — plays the transparent Kvizo intro animation (animated WebP, assets/splash).
 * The animation has no background of its own: it floats on the current theme
 * background, so it follows the system dark/light theme (no white flash, no
 * hardcoded color). Light theme uses the dark-ink variant, dark theme the
 * light-ink variant.
 *
 * The animation runs once (~4.5s); when it finishes the app navigates to
 * SETUP (first run) or HOME. Devices below API 28 (no animated WebP) get the
 * static first frame + a timed navigation. A safety net never traps the user
 * on the splash. Only shows on a fresh app start (cold start); resuming from
 * background never recreates this screen.
 */
@Composable
fun SplashScreen(vm: AppViewModel, nav: NavHostController) {
    var navigated by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        vm.checkForUpdatesAtLaunch()
    }

    fun goNext() {
        if (navigated) return
        navigated = true
        if (vm.profile == null) nav.navigate(Routes.SETUP) { popUpTo(Routes.SPLASH) { inclusive = true } }
        else nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }

    // safety net — never trap the user on the splash
    LaunchedEffect(Unit) {
        delay(8_000)
        goNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    val asset = if (dark) "splash/kvizo_splash_dark.webp" else "splash/kvizo_splash_light.webp"
                    val handler = Handler(Looper.getMainLooper())
                    fun done() {
                        handler.removeCallbacksAndMessages(null)
                        goNext()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ctx.assets, asset))
                            setImageDrawable(d)
                            val anim = d as? AnimatedImageDrawable
                            if (anim != null) {
                                vm.playSplashJingle()
                                anim.start()
                                handler.post(object : Runnable {
                                    override fun run() {
                                        if (anim.isRunning) handler.postDelayed(this, 120) else done()
                                    }
                                })
                            } else {
                                handler.postDelayed({ done() }, 2_000)
                            }
                        } catch (e: Exception) {
                            handler.postDelayed({ done() }, 500)
                        }
                    } else {
                        vm.playSplashJingle()
                        try {
                            setImageBitmap(BitmapFactory.decodeStream(ctx.assets.open(asset)))
                        } catch (e: Exception) {
                            // nothing to show — safety net will navigate
                        }
                        handler.postDelayed({ done() }, 3_000)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
