package com.quizforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.Profile
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.theme.violetGradient
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Orange
import com.quizforge.app.ui.theme.SpaceGrotesk
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    var settings by remember { mutableStateOf(AppSettings("system", true, true)) }
    var profiles by remember { mutableStateOf(listOf<Profile>()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.settings.collect { settings = it }
        profiles = vm.repo.getAllProfiles()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Settings", fontFamily = com.quizforge.app.ui.theme.SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))

        // ---------- Appearance ----------
        SectionTitle("Appearance")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                    Text("  Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
                // segmented control — System / Light / Dark
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                            val selected = settings.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = { scope.launch { vm.setThemeMode(mode) } }, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- Sound & Feedback ----------
        SectionTitle("Sound & Feedback")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Hearing, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                    Text("  Sound effects", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.soundEnabled, onCheckedChange = { scope.launch { vm.setSoundEnabled(it) } })
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Vibration, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                    Text("  Haptic feedback", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = { scope.launch { vm.setHapticsEnabled(it) } })
                }
            }
        }

        // ---------- Backup & Restore (top of the info stack) ----------
        SectionTitle("Data")
        SettingsRow(Icons.Filled.Storage, "Backup & Restore", "Save or import your JSON backup", Indigo) { nav.navigate(Routes.BACKUP) }

        // ---------- App info ----------
        SectionTitle("App")
        SettingsRow(Icons.Filled.SystemUpdate, "Updater", "Check for updates & notifications", com.quizforge.app.ui.theme.Violet) { nav.navigate(Routes.UPDATER) }
        SettingsRow(Icons.Filled.History, "Changelog", "What's new in every build", Orange) { nav.navigate(Routes.CHANGELOG) }
        SettingsRow(Icons.Filled.Info, "About", "Version, developer & contributors", com.quizforge.app.ui.theme.Green) { nav.navigate(Routes.ABOUT) }

        // ---------- Profiles ----------
        SectionTitle("Profiles")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
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
                        Text(vm.avatarEmoji(p.avatarId), fontSize = 20.sp)
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

        Spacer(Modifier.height(24.dp))
    }
}