package com.quizforge.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.quizforge.app.BuildConfig
import com.quizforge.app.Routes
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.Profile
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.util.BackupManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    var settings by remember { mutableStateOf(AppSettings("system", true, true, "pause")) }
    var profiles by remember { mutableStateOf(listOf<Profile>()) }
    var confirmReset by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.settings.collect { settings = it }
        profiles = vm.repo.getAllProfiles()
    }

    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = BackupManager.export(vm.repo)
                vm.getApplication<android.app.Application>().contentResolver.openOutputStream(uri)?.use { os -> os.write(json.toByteArray()) }
                message = "Backup saved"
            } catch (e: Exception) {
                message = "Backup failed: ${e.message}"
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = vm.getApplication<android.app.Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { br -> br.readText() } ?: ""
                BackupManager.restore(text, vm.repo)
                message = "Restore complete"
            } catch (e: Exception) {
                message = "Restore failed: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))

        message?.let {
            Surface(color = Green.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(it, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }

        // Appearance
        SectionTitle("Appearance")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DarkMode, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text("  Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                var themeMenu by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { themeMenu = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(settings.themeMode, fontSize = 12.sp)
                }
                DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                    listOf("system", "light", "dark").forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = {
                            themeMenu = false
                            scope.launch { vm.setThemeMode(t) }
                        })
                    }
                }
            }
        }

        // Sound & haptics
        SectionTitle("Sound & Feedback")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Hearing, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Sound effects", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.soundEnabled, onCheckedChange = { scope.launch { vm.setSoundEnabled(it) } })
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Vibration, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Haptic feedback", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = { scope.launch { vm.setHapticsEnabled(it) } })
                }
            }
        }

        // Timer behavior
        SectionTitle("Quiz Timer")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Smartphone, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text("  When app goes to background", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                var timerMenu by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { timerMenu = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(settings.timerOnBackground, fontSize = 12.sp)
                }
                DropdownMenu(expanded = timerMenu, onDismissRequest = { timerMenu = false }) {
                    DropdownMenuItem(text = { Text("pause") }, onClick = { timerMenu = false; scope.launch { vm.setTimerBehavior("pause") } })
                    DropdownMenuItem(text = { Text("submit") }, onClick = { timerMenu = false; scope.launch { vm.setTimerBehavior("submit") } })
                }
            }
        }

        // Profiles
        SectionTitle("Profiles")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("${profiles.size} profile(s) — tap one to switch", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                profiles.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    vm.switchProfile(p.id)
                                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true }; launchSingleTop = true }
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = vm.avatarUrl(p.avatarId),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text("  ${p.username}  ", fontWeight = if (p.isActive) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, color = if (p.isActive) Indigo else MaterialTheme.colorScheme.onSurface)
                        if (p.isActive) Text("(active)", fontSize = 11.sp, color = Green)
                        Spacer(Modifier.weight(1f))
                        Text("LVL ${p.level} • ${p.xp} XP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = { nav.navigate(Routes.SETUP) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Create new profile")
                }
            }
        }

        // Backup
        SectionTitle("Data")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Backup, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Backup all data (JSON)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { backupSaver.launch("kvizo-backup.json") }) { Text("Save", color = Indigo) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Restore from backup", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { restorePicker.launch(arrayOf("application/json", "text/*", "*/*")) }) { Text("Open", color = Indigo) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Storage used", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(formatBytes(vm.repo.storageBytes()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                    Text("  Reset all data", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Red, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmReset = true }) { Text("Reset", color = Red) }
                }
            }
        }

        // About
        SectionTitle("About")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).background(Indigo.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("Kvizo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Made by SirYadav1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Quiz app with XP levels, badges, stats and community quizzes. Built with Kotlin + Compose.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all data?") },
            text = { Text("This permanently deletes ALL profiles, quizzes, attempts and badges. A backup is recommended first.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetAll()
                    nav.navigate(Routes.SETUP) { popUpTo(Routes.SETUP) { inclusive = true } }
                }) { Text("Delete everything", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> "${b / (1024 * 1024)} MB"
}
