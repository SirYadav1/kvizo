package com.quizforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.theme.Indigo
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun ProfileSetupScreen(vm: AppViewModel, nav: NavHostController) {
    var username by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarId by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to QuizForge", fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.padding(top = 24.dp))
        Text("Create your local profile — no login needed", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Text("Pick an avatar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp).align(Alignment.Start))

        // Avatar grid
        val avatars = AppViewModel.AVATAR_RESOURCES
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avatars.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { _, resId ->
                        val idx = avatars.indexOf(resId)
                        val selected = avatarId == idx
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (selected) Indigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(if (selected) 2.dp else 0.dp, if (selected) Indigo else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .clickable { avatarId = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = vm.avatarUrl(idx),
                                contentDescription = "Avatar $idx",
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
        OutlinedTextField(
            value = status,
            onValueChange = { status = it.take(40) },
            label = { Text("Status (optional)") },
            placeholder = { Text("e.g. Quiz Master") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(150) },
            label = { Text("Bio (optional, max 150)") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minLines = 2
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Button(
            onClick = {
                if (username.isBlank()) error = "Username is required"
                else {
                    vm.createProfile(username.trim(), avatarId, status.trim(), bio.trim())
                    nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 32.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Start Learning", modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
