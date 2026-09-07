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
import com.kvizo.app.ui.components.ForgeCard
import com.kvizo.app.ui.theme.*
import com.kvizo.app.util.StringProvider

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsState(com.kvizo.app.data.AppSettings("system", true, true, false, true, true, true, "en"))
    val profile = vm.profile
    val quizzes: List<Quiz> = vm.quizzes
    val profileData = vm.profileData

    LaunchedEffect(settings.language) { StringProvider.setLanguage(settings.language) }

    val totalQuizzes = quizzes.size
    val attemptsList = profileData?.attempts ?: emptyList()
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
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text("P$level", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(playerName, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text(StringProvider.t("level") + " $level", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("Total: $xp XP", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(end = 12.dp))
                            Text("Accuracy: $accuracy%", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(end = 12.dp))
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

        item { Text(StringProvider.t("badges"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 4.dp)) }
        item {
            if (badgeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(StringProvider.t("complete_quizzes_unlock"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    badgeList.take(4).forEach { badge ->
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(VioletPale), contentAlignment = Alignment.Center) {
                                    Text(badge.badgeName.take(2), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Violet)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(badge.badgeName, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
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
                ForgeCard(modifier = Modifier.fillMaxWidth().clickable { nav.navigate("attempt/${quiz.id}") }) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(VioletPale), contentAlignment = Alignment.Center) {
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
            Surface(shape = RoundedCornerShape(14.dp), color = VioletPale, modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.SETUP) }) {
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
