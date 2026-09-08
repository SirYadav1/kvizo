package com.kvizo.app.ui.screens

import com.kvizo.app.ui.components.AvatarView
import com.kvizo.app.ui.components.ForgeCard
import com.kvizo.app.ui.components.ForgeProgressBar
import com.kvizo.app.ui.theme.SpaceGrotesk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.R
import com.kvizo.app.Routes
import com.kvizo.app.data.Quiz
import com.kvizo.app.logic.XpEngine
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.theme.*
import com.kvizo.app.util.StringProvider

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
    var quizzes by remember { mutableStateOf(listOf<Quiz>()) }
    var questionCounts by remember { mutableStateOf(mapOf<String, Int>()) }
    var showNotifications by remember { mutableStateOf(false) }
    val settings by vm.settings.collectAsState(com.kvizo.app.data.AppSettings("system", true, true, false, true, true, true, "en"))

    LaunchedEffect(settings.language) { StringProvider.setLanguage(settings.language) }

    LaunchedEffect(profile.id) {
        vm.ensureHomeLoaded()
        snapshotFlow { vm.homeData }.collect { d ->
            if (d != null) {
                todayCount = d.todayCount
                totalAttempts = d.totalAttempts
                quizzes = d.quizzes
                questionCounts = d.questionCounts
                totalTime = d.totalTime
                streak = d.streak
                weeklyAccuracy = d.weeklyAccuracy
            }
        }
    }

    val nextLevelXp = if (profile.level < 6) XpEngine.xpForLevel(profile.level + 1) else 0
    val pctToNext = (XpEngine.levelProgress(profile.xp) * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_kvizo_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kvizo", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                IconButtonBox(Icons.Filled.Notifications) { showNotifications = true }
                IconButtonBox(Icons.Filled.Settings) { nav.navigate(Routes.SETTINGS) }
            }
        }

        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).border(2.5.dp, Violet, CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
                            AvatarView(profile.avatarId, 50.dp)
                        }
                        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("${StringProvider.t("welcome_to_kvizo").substringBefore(" ")}${profile.username}!", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(XpEngine.levelTitle(profile.level), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Violet, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(StringProvider.t("level") + " ${profile.level} ${StringProvider.t("in")} ${nextLevelXp - profile.xp} XP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${profile.xp} / $nextLevelXp XP", fontSize = 12.sp, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = Violet)
                            Text("$pctToNext%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ForgeProgressBar(progress = XpEngine.levelProgress(profile.xp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp), height = 7.dp)
                }
            }
        }

        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(StringProvider.t("daily_quest").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Violet, letterSpacing = 0.1.sp)
                        Text(StringProvider.t("answer_5_math"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = Violet, modifier = Modifier.clip(RoundedCornerShape(99.dp)).clickable { nav.navigate(Routes.QUIZZES) }) {
                        Text(StringProvider.t("start"), color = Color.White, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(StringProvider.t("daily_streak"), "${streak}d", Icons.Filled.LocalFireDepartment, Red, Modifier.weight(1f), StringProvider.t("days_in_a_row"))
                MetricCard(StringProvider.t("this_week"), "${weeklyAccuracy.toInt()}% acc.", Icons.AutoMirrored.Filled.TrendingUp, Violet, Modifier.weight(1f), StringProvider.t("accuracy"))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(StringProvider.t("today"), "$todayCount", Icons.Filled.CalendarToday, Violet, Modifier.weight(1f), StringProvider.t("quizzes_today"))
                MetricCard(StringProvider.t("authored"), "${quizzes.size}", Icons.Filled.EditNote, Indigo, Modifier.weight(1f), StringProvider.t("quizzes_created"))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(StringProvider.t("activity"), "$totalAttempts", Icons.Filled.TaskAlt, Green, Modifier.weight(1f), StringProvider.t("attempts_taken"))
                MetricCard(StringProvider.t("session"), formatTime(totalTime), Icons.Filled.Timer, Amber, Modifier.weight(1f), StringProvider.t("time_played"))
            }
        }

        item {
            Text(StringProvider.t("quick_actions"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickActionBubble(Icons.Filled.Edit, StringProvider.t("create"), Violet, Modifier.weight(1f)) { nav.navigate(Routes.BUILDER) }
                QuickActionBubble(Icons.Filled.PlayArrow, StringProvider.t("play"), Green, Modifier.weight(1f)) { nav.navigate(Routes.QUIZZES) }
                QuickActionBubble(Icons.Filled.BarChart, StringProvider.t("stats"), Amber, Modifier.weight(1f)) { nav.navigate(Routes.STATS) }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(StringProvider.t("popular_quizzes"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(StringProvider.t("view_all") + " > ", fontSize = 12.sp, color = Violet, modifier = Modifier.clickable { nav.navigate(Routes.QUIZZES) })
            }
        }
        val popular = quizzes.sortedByDescending { it.attemptsCount }
        if (popular.isEmpty()) {
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_empty_quiz), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(64.dp))
                        Text(StringProvider.t("no_quizzes_yet"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        Text(StringProvider.t("create_or_import"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(popular.take(5), key = { it.id }) { quiz ->
                PopularQuizCard(quiz, questionCounts[quiz.id] ?: 0) { nav.navigate(Routes.attempt(quiz.id)) }
            }
        }

        item {
            ForgeCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { nav.navigate(Routes.COMMUNITY) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(StringProvider.t("community_spotlight").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Violet, letterSpacing = 0.1.sp)
                    Text(StringProvider.t("explore_labs"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                    Text(StringProvider.t("explore_labs_desc"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp), lineHeight = 17.sp)
                    Surface(shape = RoundedCornerShape(99.dp), color = Violet, modifier = Modifier.padding(top = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("  " + StringProvider.t("download_quizzes"), color = Color.White, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showNotifications) {
        NotificationsBottomSheet(notifications = vm.announcements, onDismiss = { showNotifications = false })
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier, subtitle: String = "") {
    ForgeCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.08.sp, modifier = Modifier.weight(1f))
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 6.dp))
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun QuickActionBubble(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = modifier.clickable(onClick = onClick, indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { MutableInteractionSource() })) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 18.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun PopularQuizCard(quiz: Quiz, questionCount: Int, onClick: () -> Unit) {
    ForgeCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { MutableInteractionSource() })) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(quiz.category, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Violet, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Text(quiz.title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Text("$questionCount ${StringProvider.t("questions")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val score = quiz.averageScore.toInt()
            Text("$score% avg", fontSize = 11.sp, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = if (score >= 60) Green else Amber)
        }
    }
}

@Composable
private fun NotificationsBottomSheet(notifications: List<com.kvizo.app.data.RemoteNotification>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(StringProvider.t("notifications"), fontWeight = FontWeight.Bold, fontSize = 18.sp) }, text = {
        if (notifications.isEmpty()) Text(StringProvider.t("no_notifications"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            notifications.take(10).forEach { n ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(n.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (n.body.isNotBlank()) Text(n.body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(StringProvider.t("close")) } })
}

@Composable
private fun IconButtonBox(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.padding(start = 6.dp).size(40.dp).clickable(onClick = onClick, indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { MutableInteractionSource() })) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = VioletLight, modifier = Modifier.size(20.dp)) }
    }
}

private fun formatTime(millis: Long): String {
    val mins = millis / 60000
    return if (mins < 60) "${mins}m" else "${mins / 60}h ${mins % 60}m"
}
