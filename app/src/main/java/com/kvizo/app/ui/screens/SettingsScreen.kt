package com.kvizo.app.ui.screens

import com.kvizo.app.ui.components.AvatarView

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.kvizo.app.Routes
import com.kvizo.app.data.AppSettings
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.SectionTitle
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Orange
import com.kvizo.app.ui.theme.Red
import com.kvizo.app.ui.theme.SpaceGrotesk
import com.kvizo.app.util.StringProvider
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    var settings by remember { mutableStateOf(AppSettings("system", true, true)) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.settings.collect { settings = it }
        StringProvider.setLanguage(settings.language)
    }

    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी",
        "zh" to "中文",
        "es" to "Español",
        "fr" to "Français"
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(StringProvider.t("settings"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))

        // ---------- Appearance ----------
        SectionTitle(StringProvider.t("theme"))
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                    Text("  ${StringProvider.t("theme")}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        listOf("system" to StringProvider.t("system"), "light" to StringProvider.t("light"), "dark" to StringProvider.t("dark")).forEach { (mode, label) ->
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
                    Text("  ${StringProvider.t("sound_effects")}", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.soundEnabled, onCheckedChange = { scope.launch { vm.setSoundEnabled(it) } })
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Vibration, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                    Text("  ${StringProvider.t("haptic_feedback")}", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = settings.hapticsEnabled, onCheckedChange = { scope.launch { vm.setHapticsEnabled(it) } })
                }
            }
        }

        // ---------- Language ----------
        SectionTitle(StringProvider.t("language"))
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).clickable { showLanguageDialog = true }) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Language, contentDescription = null, tint = Indigo, modifier = Modifier.size(24.dp))
                Text("  ${StringProvider.t("language")}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(languages.find { it.first == settings.language }?.second ?: "English", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- Backup & Restore ----------
        SectionTitle(StringProvider.t("backup_restore"))
        com.kvizo.app.ui.screens.SettingsRow(Icons.Filled.Storage, StringProvider.t("backup_restore"), StringProvider.t("your_data_stays_yours"), Indigo) { nav.navigate(Routes.BACKUP) }

        // ---------- App info ----------
        SectionTitle("App")
        com.kvizo.app.ui.screens.SettingsRow(Icons.Filled.SystemUpdate, StringProvider.t("updater"), "Check for updates", com.kvizo.app.ui.theme.Violet) { nav.navigate(Routes.UPDATER) }
        com.kvizo.app.ui.screens.SettingsRow(Icons.Filled.History, StringProvider.t("changelog"), "What's new", Orange) { nav.navigate(Routes.CHANGELOG) }
        com.kvizo.app.ui.screens.SettingsRow(Icons.Filled.Info, StringProvider.t("about"), "Version & contributors", com.kvizo.app.ui.theme.Green) { nav.navigate(Routes.ABOUT) }

        // ---------- Sign Out ----------
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable { showSignOutDialog = true }) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = Red, modifier = Modifier.size(24.dp))
                Text("  ${StringProvider.t("sign_out")}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Red)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(StringProvider.t("sign_out")) },
            text = { Text(StringProvider.t("sign_out_confirm")) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    nav.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }) { Text(StringProvider.t("sign_out"), color = Red) }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text(StringProvider.t("cancel")) } }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(StringProvider.t("language")) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        val isSelected = settings.language == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { vm.setLanguage(code) }
                                    StringProvider.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Text("✓", fontSize = 16.sp, color = Indigo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(StringProvider.t("close")) }
            }
        )
    }
}
