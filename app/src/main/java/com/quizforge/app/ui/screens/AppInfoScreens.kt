package com.quizforge.app.ui.screens

import android.os.Build
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.BuildConfig
import com.quizforge.app.Routes
import com.quizforge.app.data.AppSettings
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.ForgeCard
import com.quizforge.app.ui.components.PressGlowButton
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.ui.theme.SpaceGrotesk
import com.quizforge.app.ui.theme.Violet
import com.quizforge.app.ui.theme.VioletPale
import com.quizforge.app.util.BackupManager
import kotlinx.coroutines.launch

/** Shared "row" used by Settings: tinted icon tile + title (+subtitle) + chevron. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Indigo,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (subtitle != null) Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/* ================================================================== */
/*  Backup & Restore                                                  */
/* ================================================================== */

@Composable
fun BackupScreen(vm: AppViewModel, nav: NavHostController) {
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = BackupManager.export(vm.repo)
                vm.getApplication<android.app.Application>().contentResolver.openOutputStream(uri)?.use { os -> os.write(json.toByteArray()) }
                message = "Backup saved — profile, stats, badges & quizzes included"
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
                val profiles = BackupManager.restore(text, vm.repo)
                vm.refreshProfile()
                vm.invalidateData()
                message = "Restore complete — ${profiles.joinToString()} restored"
            } catch (e: Exception) {
                message = "Restore failed: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
            androidx.compose.material3.IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp).rotate(180f))
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text("Backup & Restore", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Your data stays yours", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        message?.let {
            Surface(color = Green.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(it, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }

        // Backup
        SectionTitle("Backup")
        PressGlowButton(
            onClick = { backupSaver.launch("quizforge-backup.json") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("  Back up all data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("  Save a JSON file with everything", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Restore / import
        SectionTitle("Restore")
        PressGlowButton(
            onClick = { restorePicker.launch(arrayOf("application/json", "text/*", "*/*")) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("  Restore from JSON", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("  Import stats, badges & quizzes you made", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("What's included", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "• Every profile (XP, level, streak)\n• Quiz attempts & stats (today's, weekly, all time)\n• All quizzes YOU created, with their questions\n• Badges & daily activity",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Danger zone
        SectionTitle("Storage")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Text("  Storage used", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(formatBytes(vm.repo.storageBytes()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                    Text("  Reset all data", fontSize = 14.sp, color = Red, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmReset = true }) { Text("Reset", color = Red) }
                }
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

/* ================================================================== */
/*  Updater                                                           */
/* ================================================================== */

@Composable
fun UpdaterScreen(vm: AppViewModel, nav: NavHostController) {
    var settings by remember { mutableStateOf(AppSettings("system", true, true)) }
    var state by remember { mutableStateOf("idle") } // idle | checking | current | available | none
    var updateUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.settings.collect { settings = it } }

    fun checkUpdate() {
        scope.launch {
            state = "checking"
            val info = vm.checkForUpdates()
            when {
                info.available -> {
                    state = "available"
                    updateUrl = info.url
                    vm.notifyUpdateAvailable()
                }
                info.url.isEmpty() -> state = "none"
                else -> state = "current"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
            androidx.compose.material3.IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp).rotate(180f))
            }
            Text("Updater", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        // Current version card
        SectionTitle("Current version")
        ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).background(VioletPale, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("QuizForge ${BuildConfig.VERSION_NAME}", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("build ${BuildConfig.VERSION_CODE} — Android ${Build.VERSION.RELEASE}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
                }
                Surface(color = Green.copy(alpha = 0.12f), shape = RoundedCornerShape(99.dp)) {
                    Text("Latest", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }

        // Update settings
        SectionTitle("Update settings")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Automatically check for updates", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("on every app start", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.autoUpdateCheck, onCheckedChange = { vm.setAutoUpdateCheck(it) })
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Update notifications", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("notify me when a new version is out", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.updateNotifications, onCheckedChange = { scope.launch { vm.setUpdateNotifications(it) } })
                }
            }
        }

        // Check for updates
        SectionTitle("Check now")
        PressGlowButton(
            onClick = { checkUpdate() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null)
                Text("  ${when (state) {
                    "checking" -> "Checking..."
                    "current" -> "Up to date"
                    "available" -> "Update available — open GitHub"
                    "none" -> "Check for updates"
                    else -> "Check for updates"
                }}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
        }
        when (state) {
            "current" -> Text(
                "You're on the latest release (${BuildConfig.VERSION_NAME})",
                fontSize = 12.sp,
                color = Green,
                modifier = Modifier.padding(top = 10.dp)
            )
            "available" -> Text(
                "A newer version was found — tap above or use the button below to open the GitHub releases page",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            "none" -> Text(
                "Couldn't check — make sure you're online and try again",
                fontSize = 12.sp,
                color = Red,
                modifier = Modifier.padding(top = 10.dp)
            )
            else -> {}
        }
        if (state == "available") {
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl.ifBlank { "https://github.com/SirYadav1/quizforge/releases" }))
                    androidx.core.content.ContextCompat.startActivity(vm.getApplication<android.app.Application>(), intent, null)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Open GitHub releases") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ================================================================== */
/*  Changelog                                                         */
/* ================================================================== */

private data class ChangelogEntry(val version: String, val date: String, val items: List<String>)

private val changelog = listOf(
    ChangelogEntry("1.4.0", "Aug 8, 2026", listOf(
        "Telegram-style dark mode — buttons stay dark",
        "Backup & Restore with JSON import/export",
        "Updater, changelog & about screens",
        "Quiz pauses automatically when you leave the app",
        "Settings redesign: segmented theme, cleaner controls",
        "Speedometer score sweep — settles exactly on your score",
        "One-shot confetti celebration on finish",
        "Figma violet redesign — new UI everywhere",
        "Space Grotesk display font",
        "Dark mode improvements & SVG icons",
        "Press-glow buttons"
    )),
    ChangelogEntry("1.3.0", "Aug 4, 2026", listOf(
        "Premium UI redesign",
        "Polished theme and components",
    )),
    ChangelogEntry("1.1.0", "Aug 3, 2026", listOf(
        "Core quiz engine improvements",
        "Early leaderboard & stats",
    )),
    ChangelogEntry("1.0.0", "Aug 3, 2026", listOf(
        "The first QuizForge release",
        "Offline quizzes & local profiles",
    ))
)

@Composable
fun ChangelogScreen(vm: AppViewModel, nav: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
            androidx.compose.material3.IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp).rotate(180f))
            }
            Column {
                Text("Changelog", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("What's new in every build", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        changelog.forEach { entry ->
            ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("v${entry.version}", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Violet)
                        Spacer(Modifier.weight(1f))
                        Text(entry.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    entry.items.forEach { item ->
                        Row(modifier = Modifier.padding(top = 7.dp), verticalAlignment = Alignment.Top) {
                            Text("•", color = Violet, fontSize = 12.sp)
                            Text("  $item", fontSize = 12.5.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ================================================================== */
/*  About                                                             */
/* ================================================================== */

@Composable
fun AboutScreen(vm: AppViewModel, nav: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
            androidx.compose.material3.IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp).rotate(180f))
            }
            Text("About", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        // App card
        ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).background(VioletPale, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text("Q", color = Violet, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
                Text("QuizForge", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Text(
                    "App: Universal APK\nSupported CPUs: ${Build.SUPPORTED_ABIS.joinToString()}\nAndroid ${26 /* Android 8.0+ */}+",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Developer
        SectionTitle("Developer")
        ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(VioletPale, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("🙂", fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("SirYadav1", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Creator & developer", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    SocialButton("GitHub", "github.com/SirYadav1", "https://github.com/SirYadav1", Modifier.weight(1f))
                    SocialButton("Telegram", "@SirYadav1", "https://t.me/SirYadav1", Modifier.weight(1f))
                    SocialButton("Instagram", "@SirYadav1", "https://instagram.com/SirYadav1", Modifier.weight(1f))
                }
            }
        }

        // Contributors
        SectionTitle("Contributors")
        ForgeCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ContributorRow("🧑‍💻", "SirYadav1", "Founder & lead developer")
                ContributorRow("🧪", "QuizForge Community", "Testers & feedback")
                ContributorRow("🎨", "Figma community", "Design inspiration")
                ContributorRow("❤️", "You", "Every quiz, every XP — thanks for playing!")
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/SirYadav1/quizforge"))
                        androidx.core.content.ContextCompat.startActivity(vm.getApplication<android.app.Application>(), intent, null)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("  Become a contributor — open source", fontSize = 12.5.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SocialButton(label: String, handle: String, url: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clickable(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    androidx.core.content.ContextCompat.startActivity(context, intent, null)
                },
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .padding(vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Indigo)
            Text(handle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun ContributorRow(emoji: String, name: String, role: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 17.sp)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(role, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> "${b / (1024 * 1024)} MB"
}