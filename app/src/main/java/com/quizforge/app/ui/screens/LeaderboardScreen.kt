package com.quizforge.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.data.LeaderboardEntry
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.quizforge.app.ui.components.L

@Composable
fun LeaderboardScreen(vm: AppViewModel, nav: NavHostController) {
    var metric by remember { mutableStateOf("XP") }
    var entries by remember { mutableStateOf(listOf<LeaderboardEntry>()) }
    LaunchedEffect(metric) {
        entries = vm.leaderboardEntries(metric)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Box(modifier = Modifier.size(34.dp).background(Amber.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            }
            Text("  Leaderboard", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.weight(1f))
        }
        Text(
            "Top quiz masters on this device",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            FilterChip(selected = metric == "XP", onClick = { metric = "XP" }, label = { Text("XP") })
            FilterChip(selected = metric == "Accuracy", onClick = { metric = "Accuracy" }, label = { Text(L.s("accuracy")) })
        }

        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(56.dp))
                    Text("No profiles yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        "Create a profile to start ranking",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { "${it.rank}-${it.username}" }) { e ->
                    LeaderRow(vm, e)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun LeaderRow(vm: AppViewModel, e: LeaderboardEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (e.isSelf) Indigo.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (e.isSelf) 2.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rankLabel(e.rank), fontWeight = FontWeight.Black, fontSize = 15.sp, color = rankColor(e.rank), modifier = Modifier.size(34.dp))
            Box(modifier = Modifier.size(36.dp).background(Indigo.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = vm.avatarUrl(e.avatarId),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.username, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    if (e.isSelf) {
                        Surface(color = Green.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(start = 6.dp)) {
                            Text(" YOU", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(
                    "LVL ${e.level} • ${e.xp} XP • ${e.attempts} attempts",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Text("${e.accuracy}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (e.accuracy >= 60) Green else Amber)
        }
    }
}

private fun rankLabel(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "#$rank"
}

@Composable
private fun rankColor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFC107)
    2 -> Color(0xFF9E9E9E)
    3 -> Color(0xFFCD7F32)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
