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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.data.DIFF_EASY
import com.quizforge.app.data.DIFF_HARD
import com.quizforge.app.data.DIFF_MEDIUM
import com.quizforge.app.data.Quiz
import com.quizforge.app.logic.XpEngine
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile ?: return
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.username) }
    var editStatus by remember { mutableStateOf(profile.status) }
    var editBio by remember { mutableStateOf(profile.bio) }
    var selectedAvatar by remember { mutableStateOf(profile.avatarId) }

    val attempts = vm.profileData?.attempts ?: emptyList()
    val quizzesCreated = vm.profileData?.quizzes ?: emptyList()
    val totalTime = vm.profileData?.totalTime ?: 0L
    val diffStats = vm.profileData?.diffStats ?: emptyMap()
    val badges = vm.profileData?.badges ?: emptyList()

    LaunchedEffect(profile.id) {
        vm.ensureProfileLoaded()
    }

    val avatarCount = vm.avatarCount()
    val answeredTotal = attempts.sumOf { it.totalQuestions }.coerceAtLeast(1)
    val correctTotal = attempts.sumOf { it.correctAnswers }
    val avg = correctTotal * 100 / answeredTotal
    val daySet = attempts.map { XpEngine.dateStr(it.attemptedAt) }.toSet()
    val streak = XpEngine.currentStreak(daySet)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("My Profile", fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showEdit = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Indigo)
            }
        }

        // profile header
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Box(modifier = Modifier.size(80.dp).background(Indigo.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(vm.avatarEmoji(profile.avatarId), fontSize = 38.sp)
                }
                Text(profile.username, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                if (profile.status.isNotBlank()) Text(profile.status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.bio.isNotBlank()) {
                    Text(profile.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                }
                Surface(color = Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("LVL ${profile.level}", fontWeight = FontWeight.Bold, color = Amber, fontSize = 14.sp)
                        Text("  ${profile.xp} XP", color = Amber, fontSize = 12.sp)
                    }
                }
                LinearProgressIndicator(
                    progress = { XpEngine.levelProgress(profile.xp) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp),
                    color = Indigo,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    XpEngine.levelTitle(profile.level) + if (profile.level < 6) "  •  ${(XpEngine.levelProgress(profile.xp) * 100).toInt()}% to Level ${profile.level + 1}" else "  •  Max level",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // stats row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            MiniStat("${attempts.size}", "Attempts", Modifier.weight(1f), Indigo)
            MiniStat("${quizzesCreated.size}", "Created", Modifier.weight(1f), Green)
            MiniStat("$avg%", "Accuracy", Modifier.weight(1f), Amber)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            MiniStat("$streak-day", "Streak", Modifier.weight(1f), Red)
            MiniStat(formatTime(totalTime), "Time", Modifier.weight(1f), Indigo)
            MiniStat("${badges.size}", "Badges", Modifier.weight(1f), Amber)
        }

        // difficulty performance
        if (attempts.isNotEmpty()) {
            SectionTitle("Performance by Difficulty")
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    listOf(DIFF_EASY, DIFF_MEDIUM, DIFF_HARD).forEach { d ->
                        val (c, t) = diffStats[d] ?: (0 to 0)
                        val pct = if (t > 0) c * 100 / t else 0
                        val color = when (d) {
                            DIFF_EASY -> Green
                            DIFF_HARD -> Red
                            else -> Amber
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                            Text("  $d", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.width(90.dp).height(6.dp),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text("  $pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }

        // my quizzes
        SectionTitle("My Quizzes (${quizzesCreated.size})")
        if (quizzesCreated.isEmpty()) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Text("No quizzes yet — create one from the Create tab!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
        } else {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    quizzesCreated.sortedByDescending { it.updatedAt }.take(6).forEach { q ->
                        QuizRow(q, modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.attempt(q.id)) })
                    }
                }
            }
        }

        // badges
        SectionTitle("Badges (${badges.size}/${com.quizforge.app.data.BADGE_DEFS.size})")
        if (badges.isEmpty()) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Text("No badges yet — take quizzes to earn them!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
        } else {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    badges.forEach { b ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                            Text("  ${b.badgeName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(XpEngine.dateStr(b.unlockedAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // settings entry — at the very bottom
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.SETTINGS) }
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text("  Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    Text("Avatar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                        (0 until avatarCount).forEach { i ->
                            val emoji = vm.avatarEmoji(i)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (selectedAvatar == i) Indigo.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedAvatar = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editStatus,
                        onValueChange = { editStatus = it },
                        label = { Text("Status (e.g. Quiz Master)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEdit = false
                    if (editName.isNotBlank()) {
                        vm.updateProfile(editName.trim(), editStatus.trim(), editBio.trim(), selectedAvatar)
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuizRow(q: Quiz, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(Indigo.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text("Q", color = Indigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(q.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${q.attemptsCount} attempts  •  ${q.averageScore.toInt()}% avg", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            q.status.capitalize(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (q.status == "draft") Amber else Green
        )
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = Indigo) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = tint)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }