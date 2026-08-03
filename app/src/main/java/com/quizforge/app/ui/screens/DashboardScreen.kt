package com.quizforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.R
import com.quizforge.app.Routes
import com.quizforge.app.data.Quiz
import com.quizforge.app.logic.XpEngine
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.DifficultyBadge
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.components.StatCard
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo

@Composable
fun DashboardScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile ?: run {
        LaunchedEffect(Unit) { nav.navigate(Routes.SETUP) { popUpTo(Routes.HOME) { inclusive = true } } }
        return@DashboardScreen Box(Modifier.fillMaxSize())
    }
    var todayCount by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(0) }
    var weeklyAccuracy by remember { mutableStateOf(0f) }
    var streak by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(0L) }
    var recentBadges by remember { mutableStateOf(listOf<String>()) }
    var quizzes by remember { mutableStateOf(listOf<Quiz>()) }
    var weakAreas by remember { mutableStateOf(listOf<String>()) }
    var questionCounts by remember { mutableStateOf(mapOf<String, Int>()) }

    LaunchedEffect(profile.id) {
        val attempts = vm.repo.getAttempts(profile.id)
        todayCount = vm.repo.getAttemptCountToday(profile.id)
        totalAttempts = attempts.size
        quizzes = vm.repo.getQuizzes(profile.id, "published") + vm.repo.getQuizzes(profile.id, "draft")
        quizzes = quizzes.sortedByDescending { it.updatedAt }
        questionCounts = quizzes.associate { it.id to vm.repo.getQuestions(it.id).size }
        totalTime = vm.repo.totalTimeSpent(profile.id)
        val daySet = attempts.map { XpEngine.dateStr(it.attemptedAt) }.toSet()
        streak = XpEngine.currentStreak(daySet)
        recentBadges = vm.repo.getBadges(profile.id).takeLast(3).map { it.badgeName }
        // weekly accuracy: last 7 days
        val weekAgo = System.currentTimeMillis() - 7L * 86400000
        val weekAttempts = attempts.filter { it.attemptedAt >= weekAgo }
        weeklyAccuracy = if (weekAttempts.isEmpty()) 0f
        else weekAttempts.sumOf { it.correctAnswers }.toFloat() / weekAttempts.sumOf { it.totalQuestions }.coerceAtLeast(1)
        // weak areas: categories below 60%
        weakAreas = vm.repo.categoryAccuracy(profile.id)
            .filter { (c, p) -> p.second >= 3 && p.first * 100 / p.second < 60 }
            .map { it.key }
            .take(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("QuizForge", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Profile")
                }
            }
        }

        // profile card
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(52.dp).background(Indigo.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(vm.avatarEmoji(profile.avatarId), fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(profile.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (profile.status.isNotBlank()) Text(profile.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(color = Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("LVL ${profile.level}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Amber)
                                Text("${profile.xp} XP", fontSize = 10.sp, color = Amber)
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { XpEngine.levelProgress(profile.xp) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp),
                        color = Indigo,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        XpEngine.levelTitle(profile.level) + if (profile.level < 6) "  •  ${(XpEngine.levelProgress(profile.xp) * 100).toInt()}% to Level ${profile.level + 1}" else "  •  Max level",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // stat cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("$todayCount", "Today", modifier = Modifier.weight(1f), tint = Indigo)
                StatCard("${quizzes.size}", "Created", modifier = Modifier.weight(1f), tint = Green)
                StatCard("$totalAttempts", "Taken", modifier = Modifier.weight(1f), tint = Amber)
            }
        }

        // weekly performance
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Weekly Performance")
                    LinearProgressIndicator(
                        progress = { weeklyAccuracy },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Green,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("${(weeklyAccuracy * 100).toInt()}% accuracy (7 days)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        // streak + time
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("$streak-day", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, tint = Indigo, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(formatTime(totalTime), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Total time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // recent badges
        if (recentBadges.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Recent Badges")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            recentBadges.forEach { name ->
                                Surface(color = Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp))
                                        Text(" $name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // weak areas hint
        if (weakAreas.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Study insight: focus on ${weakAreas.joinToString()} (below 60% accuracy)",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // quick actions
        item {
            SectionTitle("Quick Actions")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Filled.Add, "Create Quiz", Indigo, Modifier.weight(1f)) { nav.navigate(Routes.BUILDER) }
                QuickAction(Icons.Filled.PlayArrow, "Take Quiz", Green, Modifier.weight(1f)) { nav.navigate(Routes.QUIZZES) }
                QuickAction(Icons.Filled.BarChart, "Statistics", Amber, Modifier.weight(1f)) { nav.navigate(Routes.STATS) }
            }
        }

        // recent quizzes
        item { SectionTitle("Recent Quizzes") }
        if (quizzes.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_empty_quiz), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(72.dp))
                        Text("No quizzes yet", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                        Text("Create one or import a .txt file", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        } else {
            items(quizzes.take(5)) { quiz ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable {
                        nav.navigate(Routes.attempt(quiz.id))
                    }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).background(Indigo.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                            Text("Q", color = Indigo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(quiz.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${questionCounts[quiz.id] ?: 0} questions | ${quiz.attemptsCount} attempts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DifficultyBadge(quiz.difficulty)
                        Text("  ${quiz.averageScore.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (quiz.averageScore >= 60) Green else Amber)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
