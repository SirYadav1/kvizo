package com.kvizo.app.ui.screens

import com.kvizo.app.ui.components.AvatarCatalog
import com.kvizo.app.ui.components.AvatarPickerGrid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.Routes
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.PressGlowButton
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.SpaceGrotesk
import com.kvizo.app.ui.theme.Violet
import com.kvizo.app.ui.theme.violetGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileSetupScreen(vm: AppViewModel, nav: NavHostController) {
    var username by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarId by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // entrance animation — fade + slide-up + scale
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e
                translationY = (1f - e) * 42f
                scaleX = 0.96f + 0.04f * e
                scaleY = 0.96f + 0.04f * e
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // hero card — gradient welcome
        Surface(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(violetGradient(), RoundedCornerShape(22.dp)).fillMaxWidth().padding(vertical = 26.dp, horizontal = 20.dp)
            ) {
                Box(modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    "Welcome to Kvizo",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Create your local profile — no login needed. Forge knowledge. Earn XP.",
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        // pick an avatar
        Text(
            "Pick an avatar",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp).align(Alignment.Start)
        )

        AvatarPickerGrid(
            selected = avatarId,
            onSelect = { avatarId = it },
            modifier = Modifier.fillMaxWidth()
        )

        // username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; error = null },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        )

        // quick status presets
        val presets = listOf("Quiz Master", "Casual Player", "Knowledge Seeker", "Legend")
        presets.chunked(2).forEach { rowP ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                rowP.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (status == p) Violet.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (status == p) androidx.compose.foundation.BorderStroke(1.5.dp, Violet.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.weight(1f),
                        onClick = { status = p }
                    ) {
                        Text(
                            p,
                            fontSize = 12.sp,
                            fontWeight = if (status == p) FontWeight.Bold else FontWeight.Medium,
                            color = if (status == p) Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // status + bio
        OutlinedTextField(
            value = status,
            onValueChange = { status = it.take(40) },
            label = { Text("Status (optional)") },
            placeholder = { Text("e.g. Quiz Master") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(150) },
            label = { Text("Bio (optional, max 150)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minLines = 2
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }

        PressGlowButton(
            onClick = {
                if (username.isBlank()) error = "Username is required"
                else {
                    creating = true
                    vm.createProfile(username.trim(), avatarId, status.trim(), bio.trim())
                    scope.launch {
                        delay(500)
                        nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 34.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                if (creating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                    Text("  Creating profile...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Start Learning", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(17.dp).padding(start = 8.dp))
                }
            }
        }
    }
}
