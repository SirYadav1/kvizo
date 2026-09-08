package com.kvizo.app.ui.screens

import com.kvizo.app.ui.theme.SpaceGrotesk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.Routes
import com.kvizo.app.data.Quiz
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.AvatarView
import com.kvizo.app.ui.components.ForgeCard
import com.kvizo.app.ui.theme.*
import com.kvizo.app.util.StringProvider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsState(com.kvizo.app.data.AppSettings("system", true, true, false, true, true, true, "en"))
    val profile = vm.profile
    val quizzes: List<Quiz> = vm.quizzes
    val profileData = vm.profileData

    LaunchedEffect(settings.language) { StringProvider.setLanguage(settings.language) }

    val totalQuizzes = quizzes.size
    val attemptsList = profileData?.attempts ?: emptyList()
    var showAllBadges by remember { mutableStateOf(false) }
    val totalCorrect = attemptsList.sumOf { it.correctAnswers }
    val totalAnswered = attemptsList.sumOf { it.totalQuestions }
    val accuracy = if (totalAnswered > 0) (totalCorrect * 100 / totalAnswered) else 0
    val xp = profile?.xp ?: 0
    val level = profile?.level ?: ((xp / 100) + 1)
    val progressInLevel = xp % 100
    val playerName = profile?.username ?: "Player"
    val badgeList = profileData?.badges ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                Text("PROFILE", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Indigo, Violet)))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            AvatarView(profile?.avatarId ?: 1, 54.dp)
                        }
                        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(playerName, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                            Text(StringProvider.t("level") + " $level • $accuracy% accuracy", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(
                            "$xp" to "Total XP",
                            "$totalQuizzes" to "Quizzes",
                            "${attemptsList.size}" to "Attempts",
                            "${badgeList.size}" to "Badges"
                        ).forEach { (value, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }
        }

        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(StringProvider.t("level_progress"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                        Text("$xp / 100 XP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    val fraction = if (progressInLevel > 0) progressInLevel.toFloat() / 100f else 0f
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = fraction).background(Brush.horizontalGradient(listOf(Violet, Indigo)), RoundedCornerShape(99.dp)))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    StringProvider.t("total_quizzes") to "$totalQuizzes",
                    StringProvider.t("total_questions") to "${attemptsList.sumOf { it.totalQuestions }}"
                ).forEach { (label, value) ->
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(14.dp)) {
                        Column { Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(StringProvider.t("badges"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
                if (badgeList.isNotEmpty()) {
                    TextButton(onClick = { showAllBadges = true }) {
                        Text("View All", fontSize = 12.sp, color = Violet, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            val allBadges = listOf(
                "first_quiz" to "First Quiz", "quiz_5" to "Quiz Regular", "quiz_10" to "Quiz Enthusiast",
                "quiz_25" to "Quiz Master", "bookworm" to "Bookworm", "perfect_score" to "Perfect Score",
                "flawless_3" to "Flawless x3", "streak_3" to "3-Day Streak", "streak_7" to "7-Day Streak",
                "speed_demon" to "Speed Demon", "rapid_fire" to "Rapid Fire", "creator" to "Creator"
            )
            val unlockedCodes = badgeList.map { it.badgeCode }.toSet()
            if (badgeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(StringProvider.t("complete_quizzes_unlock"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allBadges.take(4).forEach { (code, name) ->
                        val unlocked = code in unlockedCodes
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(10.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.then(if (!unlocked) Modifier.graphicsLayer { alpha = 0.4f } else Modifier)) {
                                Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(if (unlocked) Violet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(name.take(2), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (unlocked) Violet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(name, fontSize = 8.sp, maxLines = 1, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
        if (showAllBadges) {
            item {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAllBadges = false },
                    title = { Text("All Badges", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold) },
                    text = {
                        val allBadgesFull = listOf(
                            "first_quiz" to "First Quiz", "quiz_5" to "Quiz Regular", "quiz_10" to "Quiz Enthusiast",
                            "quiz_25" to "Quiz Master", "quiz_50" to "Bookworm", "quiz_100" to "Century of Attempts",
                            "perfect_score" to "Perfect Score", "flawless_3" to "Flawless x3", "flawless_5" to "Flawless x5",
                            "flawless_10" to "Flawless x10", "streak_3" to "3-Day Streak", "streak_7" to "7-Day Streak",
                            "streak_14" to "Fortnight Streak", "streak_30" to "30-Day Streak",
                            "speed_demon" to "Speed Demon", "speed_king" to "Speed King", "rapid_fire" to "Rapid Fire",
                            "sharpshooter" to "Sharpshooter", "category_king" to "Category King",
                            "creator" to "Creator", "quiz_producer" to "Quiz Producer",
                            "community_pioneer" to "Community Pioneer", "night_owl" to "Night Owl",
                            "early_bird" to "Early Bird", "legend" to "Legendary", "centurion" to "Centurion",
                            "comeback" to "Comeback King"
                        )
                        val unlockedCodes = badgeList.map { it.badgeCode }.toSet()
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                            allBadgesFull.chunked(4).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { (code, name) ->
                                        val unlocked = code in unlockedCodes
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).then(if (!unlocked) Modifier.graphicsLayer { alpha = 0.35f } else Modifier)) {
                                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (unlocked) Violet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (unlocked) Amber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(name, fontSize = 9.sp, maxLines = 2, textAlign = TextAlign.Center, color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                        }
                                    }
                                    // Fill remaining space in row
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { showAllBadges = false }) { Text("Close") }
                    }
                )
            }
        }

        item { Text("MY QUIZZES", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 4.dp)) }
        if (quizzes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(StringProvider.t("your_quiz_will_appear"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(quizzes.take(5), key = { it.id }) { quiz: Quiz ->
                ForgeCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { nav.navigate("attempt/${quiz.id}") }) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Quiz, contentDescription = null, tint = Violet)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(quiz.title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(quiz.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { nav.navigate(Routes.EDIT_PROFILE) }) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(StringProvider.t("edit_profile"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Violet)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
