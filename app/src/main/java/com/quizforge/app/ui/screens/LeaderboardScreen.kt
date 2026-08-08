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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import com.quizforge.app.ui.components.ForgeCard
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.AmberBg
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.InkSub
import com.quizforge.app.ui.theme.SpaceGrotesk
import com.quizforge.app.ui.theme.Violet
import com.quizforge.app.ui.theme.VioletGradient
import com.quizforge.app.ui.theme.VioletPale
import kotlinx.coroutines.flow.first

@Composable
fun LeaderboardScreen(vm: AppViewModel, nav: NavHostController) {
    var metric by remember { mutableStateOf("XP") }
    var entries by remember { mutableStateOf(listOf<LeaderboardEntry>()) }
    var onlineMode by remember { mutableStateOf(false) }

    LaunchedEffect(metric) {
        entries = vm.leaderboardEntries(metric)
        onlineMode = vm.settings.first().leaderboardOnline
    }

    val total = entries.size
    val selfEntry = entries.firstOrNull { it.isSelf }
    val top3 = entries.take(3)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Leaderboard",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
        )
        Text(
            if (onlineMode) "Global rankings • live with login" else "${total.coerceAtLeast(1)} quiz masters on this device",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Figma segmented control — XP / Accuracy
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf("XP", "Accuracy").forEach { m ->
                    val selected = metric == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) VioletGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(onClick = { metric = m }, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            m,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(56.dp))
                    Text(if (onlineMode) "Global leaderboard coming soon" else "No profiles yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        if (onlineMode) "Activates once the online login system is enabled" else "Create a profile to start ranking",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column {
                // Figma podium — order [2nd, 1st, 3rd]
                if (top3.isNotEmpty()) {
                    Podium(top3, vm, metric, Modifier.fillMaxWidth().padding(bottom = 6.dp))
                }
                // compact: top 5 + highlight "You"
                val visible = when {
                    selfEntry == null -> entries.take(5)
                    selfEntry.rank <= 5 -> entries.take(5)
                    else -> entries.take(5) + selfEntry
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(visible, key = { "${it.rank}-${it.username}" }) { e ->
                        LeaderRow(vm, e)
                    }
                    item {
                        if (selfEntry != null && selfEntry.rank > 5) {
                            Text(
                                "…and ${entries.size - 5} more — you're #${selfEntry.rank} of $total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (selfEntry != null) {
                            Text(
                                "You're ranked #${selfEntry.rank} of $total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/** Figma podium: 2nd | 1st | 3rd with colored avatar tiles + rank bubbles + bars. */
@Composable
private fun Podium(top3: List<LeaderboardEntry>, vm: AppViewModel, metric: String, modifier: Modifier = Modifier) {
    val order = top3.sortedBy { if (it.rank == 1) 0 else if (it.rank == 2) 1 else 2 }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        // left spacer to center the winner
        Spacer(Modifier.weight(0.5f))
        order.forEachIndexed { i, e ->
            val color = when (e.rank) {
                1 -> Amber
                2 -> InkSub
                else -> Violet
            }
            val bg = when (e.rank) {
                1 -> AmberBg
                2 -> Color(0xFFF3F4F6)
                else -> VioletPale
            }
            val displayValue = if (metric == "XP") "${e.xp}" else "${e.accuracy}%"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(if (e.rank == 1) 1.2f else 1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(e.username, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = InkSub, maxLines = 1)
                Text(
                    displayValue,
                    fontSize = 11.sp,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Box(modifier = Modifier.padding(top = 6.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.50f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(vm.avatarEmoji(e.avatarId), fontSize = 22.sp)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${e.rank}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(60.dp)
                        .height(if (e.rank == 1) 56.dp else if (e.rank == 2) 42.dp else 30.dp)
                        .background(if (e.rank == 1) VioletPale else Color(0xFFF3F4F6), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
            }
        }
        Spacer(Modifier.weight(0.5f))
    }
}

@Composable
private fun LeaderRow(vm: AppViewModel, e: LeaderboardEntry) {
    val isMedal = e.rank <= 3
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (e.isSelf) Indigo.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (e.isSelf) Indigo.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rankLabel(e.rank), fontWeight = FontWeight.Black, fontSize = if (isMedal) 20.sp else 15.sp, color = rankColor(e.rank), modifier = Modifier.size(36.dp))
            Box(modifier = Modifier.size(38.dp).background(VioletPale, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(vm.avatarEmoji(e.avatarId), fontSize = 18.sp)
            }
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.username, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    if (e.isSelf) {
                        Surface(color = Green.copy(alpha = 0.13f), shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(start = 6.dp)) {
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
            Text("${e.accuracy}%", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (e.accuracy >= 60) Green else Amber)
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
