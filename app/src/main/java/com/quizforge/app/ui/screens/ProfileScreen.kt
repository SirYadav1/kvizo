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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.data.Badge
import com.quizforge.app.logic.XpEngine
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile ?: return
    var badges by remember { mutableStateOf(listOf<Badge>()) }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.username) }
    var editStatus by remember { mutableStateOf(profile.status) }
    var editBio by remember { mutableStateOf(profile.bio) }
    var selectedAvatar by remember { mutableStateOf(profile.avatarId) }

    LaunchedEffect(profile.id) {
        badges = vm.repo.getBadges(profile.id)
    }

    val avatarCount = vm.avatarCount()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("My Profile", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showEdit = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Indigo)
            }
        }

        // profile header
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Box(modifier = Modifier.size(80.dp).background(Indigo.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = vm.avatarUrl(profile.avatarId),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(profile.username, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                if (profile.status.isNotBlank()) Text(profile.status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.bio.isNotBlank()) {
                    Text(profile.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Surface(color = Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("LVL ${profile.level}", fontWeight = FontWeight.Bold, color = Amber, fontSize = 14.sp)
                        Text("  ${profile.xp} XP", color = Amber, fontSize = 12.sp)
                    }
                }
                Text(XpEngine.levelTitle(profile.level), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }

        // stats row
        val attempts = remember(profile.id) { vm.repo.getAttempts(profile.id) }
        val quizzesCreated = remember(profile.id) { vm.repo.getQuizzes(profile.id).size }
        val avg = if (attempts.isEmpty()) 0f else attempts.sumOf { it.correctAnswers }.toFloat() / attempts.sumOf { it.totalQuestions }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            MiniStat("${attempts.size}", "Attempts", Modifier.weight(1f))
            MiniStat("$quizzesCreated", "Created", Modifier.weight(1f))
            MiniStat("${(avg * 100).toInt()}%", "Accuracy", Modifier.weight(1f))
        }

        // settings entry — Settings live under Profile
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { nav.navigate(Routes.SETTINGS) }
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text("  Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(com.quizforge.app.logic.XpEngine.dateStr(b.unlockedAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (selectedAvatar == i) Indigo.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedAvatar = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = vm.avatarRes(i)),
                                    contentDescription = "Avatar $i",
                                    modifier = Modifier.size(38.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
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
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Indigo)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
