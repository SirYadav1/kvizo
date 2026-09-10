package com.kvizo.app.ui.screens

import com.kvizo.app.ui.components.AvatarPickerGrid
import com.kvizo.app.ui.theme.SpaceGrotesk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.PressGlowButton
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Violet

@Composable
fun EditProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile
    var username by remember { mutableStateOf(profile?.username ?: "") }
    var status by remember { mutableStateOf(profile?.status ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var avatarId by remember { mutableIntStateOf(profile?.avatarId ?: 1) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
            Text("Edit Profile", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Big avatar preview
        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(60.dp)).background(Indigo.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            com.kvizo.app.ui.components.AvatarView(avatarId, 100.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text("Tap to change", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))
        Text("Pick an avatar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp).align(Alignment.Start))
        AvatarPickerGrid(selected = avatarId, onSelect = { avatarId = it }, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        )

        val presets = listOf("Quiz Master", "Casual Player", "Knowledge Seeker", "Legend")
        presets.chunked(2).forEach { rowP ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                rowP.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (status == p) Indigo.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (status == p) androidx.compose.foundation.BorderStroke(1.5.dp, Indigo.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.weight(1f),
                        onClick = { status = p }
                    ) {
                        Text(
                            p, fontSize = 12.sp,
                            fontWeight = if (status == p) FontWeight.Bold else FontWeight.Medium,
                            color = if (status == p) Indigo else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = status,
            onValueChange = { status = it.take(40) },
            label = { Text("Status (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(150) },
            label = { Text("Bio (optional)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minLines = 2
        )

        if (saved) {
            Text("Profile saved!", color = com.kvizo.app.ui.theme.Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        }

        PressGlowButton(
            onClick = {
                if (username.isNotBlank()) {
                    vm.updateProfile(username.trim(), status.trim(), bio.trim(), avatarId)
                    nav.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 34.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Save Changes", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
