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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
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
import com.quizforge.app.ui.components.ForgeCard
import com.quizforge.app.ui.components.ForgeProgressBar
import com.quizforge.app.ui.components.ForgeSectionLabel
import com.quizforge.app.ui.components.GradientText
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.InkSub
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.ui.theme.SpaceGrotesk
import com.quizforge.app.ui.theme.Violet
import com.quizforge.app.ui.theme.VioletLight
import com.quizforge.app.ui.theme.VioletPale
import com.quizforge.app.ui.theme.amberBg
import com.quizforge.app.ui.theme.greenBg
import com.quizforge.app.ui.theme.redBg
import com.quizforge.app.ui.theme.violetPale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
        vm.ensureHomeLoaded()
        androidx.compose.runtime.snapshotFlow { vm.homeData }.collect { d ->
            if (d != null) {
                todayCount = d.todayCount
                totalAttempts = d.totalAttempts
                quizzes = d.quizzes
                questionCounts = d.questionCounts
                totalTime = d.totalTime
                streak = d.streak
                recentBadges = d.recentBadges
                weeklyAccuracy = d.weeklyAccuracy
                weakAreas = d.weakAreas
            }
        }
    }

    val nextLevelXp = if (profile.level < 6) XpEngine.xpForLevel(profile.level + 1) else 0
    val pctToNext = (XpEngine.levelProgress(profile.xp) * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // header — "Forge" + two icon buttons (Figma)
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    "Forge",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButtonBox(Icons.Filled.BarChart) { nav.navigate(Routes.STATS) }
                IconButtonBox(Icons.Filled.Settings) { nav.navigate(Routes.SETTINGS) }
            }
        }

        // profile card — Figma: avatar tile + name + LVL gradient text + XP
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = VioletPale,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFFC4B5FD))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(vm.avatarEmoji(profile.avatarId), fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(profile.username, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (profile.status.isNotBlank()) profile.status else XpEngine.levelTitle(profile.level), fontSize = 12.sp, color = InkSub, modifier = Modifier.padding(top = 1.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            GradientText("LVL ${profile.level}", fontSize = 13.sp)
                            Text("${profile.xp} XP", fontSize = 12.sp, color = Violet, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 1.dp))
                        }
                    }
                    // XP bar — Figma: 6px rounded, gradient, "64% to Level 5" / "2,000 XP"
                    ForgeProgressBar(
                        progress = XpEngine.levelProgress(profile.xp),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        height = 6.dp
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(
                            if (profile.level < 6) "$pctToNext% to Level ${profile.level + 1}" else "Max level reached",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        if (profile.level < 6) Text("$nextLevelXp XP", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // stats — Figma 2-col grid: 🔥 Streak / 📊 This week
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FigmaStatCard(Icons.Filled.LocalFireDepartment, "Streak", "$streak", "days in a row", Violet, Modifier.weight(1f))
                FigmaStatCard(Icons.Filled.BarChart, "This week", "${(weeklyAccuracy * 100).toInt()}%", "accuracy", Green, Modifier.weight(1f))
            }
        }

        // secondary mini stats — every card explains its own data
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip("$todayCount", "quizzes\ntoday", Modifier.weight(1f))
                MiniChip("${quizzes.size}", "quizzes\ncreated", Modifier.weight(1f))
                MiniChip("$totalAttempts", "attempts\ntaken", Modifier.weight(1f))
                MiniChip(formatTime(totalTime), "time\nplayed", Modifier.weight(1f))
            }
        }

        // weak areas — Figma "Focus area" warning card
        if (weakAreas.isNotEmpty()) {
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(36.dp).background(redBg(), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Focus area", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Red, letterSpacing = 0.08.sp)
                            Text(
                                "${weakAreas.joinToString()} is below 60% — keep practicing",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // quick actions — Figma 3-tile grid, bigger icons in tinted tiles
        item {
            ForgeSectionLabel("Quick actions")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FigmaQuickAction(Icons.Filled.Edit, "Create", violetPale(), Violet, Modifier.weight(1f)) { nav.navigate(Routes.BUILDER) }
                FigmaQuickAction(Icons.Filled.PlayArrow, "Play", greenBg(), Green, Modifier.weight(1f)) { nav.navigate(Routes.QUIZZES) }
                FigmaQuickAction(Icons.Filled.TrendingUp, "Stats", amberBg(), Amber, Modifier.weight(1f)) { nav.navigate(Routes.STATS) }
            }
        }

        // popular quizzes
        item {
            ForgeSectionLabel("Popular quizzes")
        }
        val popular = quizzes.sortedByDescending { it.attemptsCount }
        if (popular.isEmpty()) {
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_empty_quiz), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(72.dp))
                        Text("No quizzes yet", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
                        Text("Create one or import a .txt file", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        } else {
            items(popular.take(5), key = { it.id }) { quiz ->
                PopularQuizRow(quiz, questionCounts[quiz.id] ?: 0) { nav.navigate(Routes.attempt(quiz.id)) }
            }
        }

        // recent quizzes
        item { ForgeSectionLabel("Recent quizzes") }
        if (quizzes.isEmpty()) {
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.attempt(quiz.id)) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(VioletPale, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text("Q", color = Violet, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(quiz.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${questionCounts[quiz.id] ?: 0} questions | ${quiz.attemptsCount} attempts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
                        }
                        Text("  ${quiz.averageScore.toInt()}%", fontSize = 11.sp, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, color = if (quiz.averageScore >= 60) Green else Amber)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/* ---- Figma sub-components ---- */

@Composable
private fun IconButtonBox(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .padding(start = 8.dp)
            .size(34.dp)
            .clickable(onClick = onClick, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = VioletLight,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun FigmaStatCard(icon: ImageVector, label: String, value: String, sub: String, valueColor: Color, modifier: Modifier = Modifier) {
    ForgeCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(15.dp))
                Text("  $label", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = valueColor, modifier = Modifier.padding(top = 6.dp))
            Text(sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun MiniChip(value: String, label: String, modifier: Modifier = Modifier) {
    ForgeCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                label,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.15.sp,
                lineHeight = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun FigmaQuickAction(
    icon: ImageVector,
    label: String,
    bg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.clickable(onClick = onClick, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(tint.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PopularQuizRow(quiz: Quiz, questionCount: Int, onClick: () -> Unit) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(VioletPale, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(formatCount(quiz.attemptsCount), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Violet)
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(quiz.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${quiz.category} · $questionCount questions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
            }
            val score = quiz.averageScore.toInt()
            Surface(
                color = if (score >= 60) greenBg() else amberBg(),
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (score >= 60) Color(0xFFA7F3D0) else Color(0xFFFDE68A))
            ) {
                Text("$score%", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (score >= 60) Green else Amber, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1000 -> "%.1fk".format(n / 1000f)
    else -> "$n"
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
