package com.kvizo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.Routes
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.ForgeCard
import com.kvizo.app.ui.theme.*
import com.kvizo.app.util.StringProvider
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsState(com.kvizo.app.data.AppSettings("system", true, true, false, true, true, true, "en"))
    val coroutineScope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settings.language) { StringProvider.setLanguage(settings.language) }

    val languages = listOf("en" to "English", "hi" to "हिन्दी", "zh" to "中文", "es" to "Español", "fr" to "Français")
    val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                Text(StringProvider.t("settings"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            }
        }

        item { SectionLabel("APPEARANCE") }
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(StringProvider.t("theme"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Customize your visual mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.primaryContainer), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        themes.forEach { (code, label) ->
                            val selected = settings.themeMode == code
                            Surface(shape = RoundedCornerShape(99.dp), color = if (selected) Violet else Color.Transparent, modifier = Modifier.weight(1f).padding(4.dp).clip(RoundedCornerShape(99.dp)).clickable {
                                coroutineScope.launch { vm.setThemeMode(code) }
                            }) {
                                Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("SOUND & FEEDBACK") }
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(StringProvider.t("sound_effects"), "Play celebratory chimes & audio cues", Icons.Filled.VolumeUp, settings.soundEnabled) { vm.setSoundEnabled(it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingToggleRow(StringProvider.t("haptic_feedback"), "Vibrate on correct and wrong answers", Icons.Filled.Vibration, settings.hapticsEnabled) { vm.setHapticsEnabled(it) }
                }
            }
        }

        item { SectionLabel("DATA & STORAGE") }
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                SettingRow(StringProvider.t("backup_restore"), "Save or import your JSON quiz backup", Icons.Filled.Cloud) { nav.navigate(Routes.BACKUP) }
            }
        }

        item { SectionLabel("APPLICATION") }
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingRow(StringProvider.t("changelog"), "What's new in every build", Icons.Filled.Update) { nav.navigate(Routes.CHANGELOG) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingRow(StringProvider.t("about"), "Version 1.6.1, developer & contributors", Icons.Filled.Info) { nav.navigate(Routes.ABOUT) }
                }
            }
        }

        item { SectionLabel("LANGUAGE") }
        item {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                SettingRow(StringProvider.t("language"), languages.find { it.first == settings.language }?.second ?: "English", Icons.Filled.Language) { showLanguageDialog = true }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = RedBg, border = BorderStroke(1.dp, RedBorder), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { showSignOutDialog = true }) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(StringProvider.t("sign_out"), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Red)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showLanguageDialog) {
        AlertDialog(onDismissRequest = { showLanguageDialog = false }, title = { Text(StringProvider.t("language"), fontWeight = FontWeight.Bold) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                languages.forEach { (code, name) ->
                    val isSelected = settings.language == code
                    Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        coroutineScope.launch { vm.setLanguage(code) }; StringProvider.setLanguage(code); showLanguageDialog = false
                    }) {
                        Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Violet else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(StringProvider.t("close")) } })
    }

    if (showSignOutDialog) {
        AlertDialog(onDismissRequest = { showSignOutDialog = false }, title = { Text(StringProvider.t("sign_out"), fontWeight = FontWeight.Bold) }, text = { Text(StringProvider.t("sign_out_confirm")) }, confirmButton = {
            TextButton(onClick = { showSignOutDialog = false; nav.navigate(Routes.SETUP) { popUpTo(0) { inclusive = true } } }) { Text(StringProvider.t("sign_out"), color = Red) }
        }, dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text(StringProvider.t("cancel")) } })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.1.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun SettingToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Violet, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { MutableInteractionSource() }).padding(16.dp)) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
