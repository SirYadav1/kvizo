package com.quizforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.screens.DashboardScreen
import com.quizforge.app.ui.screens.LeaderboardScreen
import com.quizforge.app.ui.screens.ProfileSetupScreen
import com.quizforge.app.ui.screens.ProfileScreen
import com.quizforge.app.ui.screens.QuizAttemptScreen
import com.quizforge.app.ui.screens.QuizBuilderScreen
import com.quizforge.app.ui.screens.QuizListScreen
import com.quizforge.app.ui.screens.ResultsScreen
import com.quizforge.app.ui.screens.SettingsScreen
import com.quizforge.app.ui.screens.SplashScreen
import com.quizforge.app.ui.screens.StatsScreen
import com.quizforge.app.ui.screens.CommunityQuizScreen
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.KvizoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState(initial = null)
            val dark = when (settings?.themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            KvizoTheme(darkTheme = dark) {
                KvizoRoot(vm)
            }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val HOME = "home"
    const val QUIZZES = "quizzes"
    const val BUILDER = "builder"
    const val ATTEMPT = "attempt/{quizId}?mode={mode}&only={only}"
    const val RESULTS = "results/{attemptId}"
    const val STATS = "stats"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val COMMUNITY = "community"

    fun attempt(quizId: String, mode: String = "normal", only: String = "") = "attempt/$quizId?mode=$mode&only=$only"
    fun results(attemptId: String) = "results/$attemptId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun KvizoRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val tabs = listOf(
        Tab(Routes.HOME, "Home", Icons.Filled.Home),
        Tab(Routes.QUIZZES, "Quizzes", Icons.Filled.List),
        Tab(Routes.BUILDER, "Create", Icons.Filled.AddCircle),
        Tab(Routes.LEADERBOARD, "Leaderboard", Icons.Filled.EmojiEvents),
        Tab(Routes.STATS, "Stats", Icons.Filled.BarChart),
    )
    val showBottomBar = route in tabs.map { it.route } || route?.startsWith(Routes.BUILDER) == true

    Scaffold(
        topBar = {},
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(120)) },
            exitTransition = { fadeOut(tween(90)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { fadeOut(tween(90)) }
        ) {
            composable(Routes.SPLASH) { SplashScreen(vm, nav) }
            composable(Routes.SETUP) { ProfileSetupScreen(vm, nav) }
            composable(Routes.HOME) { DashboardScreen(vm, nav) }
            composable(Routes.QUIZZES) { QuizListScreen(vm, nav) }
            composable(Routes.BUILDER) { QuizBuilderScreen(vm, nav, quizId = null) }
            composable(
                "builder?quizId={quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                QuizBuilderScreen(vm, nav, quizId = entry.arguments?.getString("quizId"))
            }
            composable(
                Routes.ATTEMPT,
                arguments = listOf(
                    navArgument("quizId") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "normal" },
                    navArgument("only") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val quizId = entry.arguments?.getString("quizId") ?: ""
                val mode = entry.arguments?.getString("mode") ?: "normal"
                val only = entry.arguments?.getString("only") ?: ""
                QuizAttemptScreen(vm, nav, quizId, mode, only)
            }
            composable(
                Routes.RESULTS,
                arguments = listOf(navArgument("attemptId") { type = NavType.StringType })
            ) { entry ->
                ResultsScreen(vm, nav, entry.arguments?.getString("attemptId") ?: "")
            }
            composable(Routes.STATS) { StatsScreen(vm, nav) }
            composable(Routes.LEADERBOARD) { LeaderboardScreen(vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            composable(Routes.PROFILE) { ProfileScreen(vm, nav) }
            composable(Routes.COMMUNITY) { CommunityQuizScreen(vm, nav) }
        }
    }
}
