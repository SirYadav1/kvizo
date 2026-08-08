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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.data.Attempt
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.DonutChart
import com.quizforge.app.ui.components.ForgeCard
import com.quizforge.app.ui.components.HeatmapCalendar
import com.quizforge.app.ui.components.HBarChart
import com.quizforge.app.ui.components.LineChart
import com.quizforge.app.ui.components.SectionTitle
import com.quizforge.app.ui.components.StatCard
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.util.Exporter

@Composable
fun StatsScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile
    var range by remember { mutableStateOf("All Time") }
    var exportMenu by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile?.id) {
        vm.ensureStatsLoaded()
    }

    val attempts = vm.statsData?.attempts ?: emptyList()
    val quizCategories = vm.statsData?.categories ?: emptyMap()
    val quizTitles = vm.statsData?.quizTitles ?: emptyMap()

    // dates that have attempts, newest first
    val availableDates = remember(attempts) {
        attempts.map { com.quizforge.app.logic.XpEngine.dateStr(it.attemptedAt) }.distinct().sortedDescending()
    }
    val effectiveDate = selectedDate ?: availableDates.firstOrNull()

    // history: attempts grouped by quiz for the selected date
    val dayAttempts = remember(attempts, effectiveDate) {
        attempts.filter { com.quizforge.app.logic.XpEngine.dateStr(it.attemptedAt) == effectiveDate }
    }
    val historyByQuiz = remember(dayAttempts) {
        dayAttempts.groupBy { it.quizId }.map { (qid, list) ->
            val sorted = list.sortedBy { it.attemptedAt }
            Triple(
                qid,
                list.size,
                Triple(
                    sorted.maxOf { it.correctAnswers * 100 / it.totalQuestions.coerceAtLeast(1) },
                    sorted.sumOf { it.timeTakenSeconds.toLong() },
                    sorted
                )
            )
        }.sortedByDescending { it.second }
    }

    val filtered = when (range) {
        "This Week" -> attempts.filter { it.attemptedAt >= System.currentTimeMillis() - 7L * 86400000 }
        "This Month" -> attempts.filter { it.attemptedAt >= System.currentTimeMillis() - 30L * 86400000 }
        else -> attempts
    }

    val totalQ = filtered.sumOf { it.totalQuestions }.coerceAtLeast(1)
    val correct = filtered.sumOf { it.correctAnswers }
    val accuracy = correct.toFloat() / totalQ
    val totalTime = filtered.sumOf { it.timeTakenSeconds.toLong() }
    val daySet = filtered.map { com.quizforge.app.logic.XpEngine.dateStr(it.attemptedAt) }.toSet()
    val streak = com.quizforge.app.logic.XpEngine.currentStreak(daySet)
    val heatmap = remember(filtered) { buildHeatmap(filtered) }

    // trend: last 10 attempts accuracy
    val trend = remember(filtered) {
        val sorted = filtered.sortedBy { it.attemptedAt }.takeLast(10)
        if (sorted.isEmpty()) listOf(0f) else sorted.map { it.correctAnswers.toFloat() / it.totalQuestions.coerceAtLeast(1) }
    }

    val categoryStats = remember(filtered) {
        val map = linkedMapOf<String, Pair<Int, Int>>()
        filtered.forEach { a ->
            val cat = quizCategories[a.quizId] ?: "Other"
            val e = map[cat] ?: (0 to 0)
            map[cat] = (e.first + a.correctAnswers) to (e.second + a.totalQuestions)
        }
        map.map { (k, v) -> k to (v.first.toFloat() / v.second.coerceAtLeast(1)) }.sortedByDescending { it.second }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Statistics", fontFamily = com.quizforge.app.ui.theme.SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { exportMenu = true }) {
                Icon(Icons.Filled.Download, contentDescription = "Export")
            }
            DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                DropdownMenuItem(text = { Text("Export CSV") }, onClick = {
                    exportMenu = false
                    val f = Exporter.exportAttemptsCsv(vm.getApplication(), vm.repo, vm.profile?.id ?: 0L)
                    Exporter.share(vm.getApplication(), f, "text/csv")
                })
                DropdownMenuItem(text = { Text("Export PDF report") }, onClick = {
                    exportMenu = false
                    val f = Exporter.exportStatsPdf(vm.getApplication(), vm.repo, vm.profile?.id ?: 0L)
                    Exporter.share(vm.getApplication(), f, "application/pdf")
                })
            }
        }

        // range segmented control — All Time / This Week / This Month
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf("All Time", "This Week", "This Month").forEach { r ->
                    val selected = range == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) com.quizforge.app.ui.theme.violetGradient()
                                else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(onClick = { range = r }, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            r,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("${filtered.size}", "Quizzes", Modifier.weight(1f), Indigo)
                    StatCard("$streak-day", "Streak", Modifier.weight(1f), Amber)
                    StatCard(formatHm(totalTime), "Time", Modifier.weight(1f), Green)
                }
            }

            // heatmap
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Activity Heatmap")
                        HeatmapCalendar(heatmap, modifier = Modifier.padding(top = 8.dp))
                        Text("Last 15 weeks • 365 days", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // accuracy donut
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(accuracy, size = 110.dp)
                        Column(modifier = Modifier.padding(start = 20.dp)) {
                            Text("Accuracy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${(accuracy * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 26.sp, color = if (accuracy >= 0.6f) Green else if (accuracy >= 0.4f) Amber else Red)
                            Text("$correct of $totalQ answers correct", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }

            // trend
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Accuracy Trend (last ${trend.size} attempts)")
                        LineChart(trend, modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp))
                    }
                }
            }

            // category bars
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Accuracy by Category")
                        if (categoryStats.isEmpty()) {
                            Text("No data yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            HBarChart(categoryStats, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
            }

            // weak areas
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Weak Areas")
                        val weak = categoryStats.filter { it.second < 0.6f }
                        if (weak.isEmpty()) {
                            Text("Nothing to improve — great job!", fontSize = 12.sp, color = Green)
                        } else {
                            weak.forEach { (cat, acc) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        Text("${(acc * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Red)
                                    }
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { acc },
                                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(6.dp),
                                        color = Red,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShowChart, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                        Text("  Best score: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filtered.maxOfOrNull { it.score } ?: 0}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                    }
                }
            }

            // history by date
            item {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("History by Date")
                        if (availableDates.isEmpty()) {
                            Text("No attempts yet — take a quiz to see history here!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        } else {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    var dateMenu by remember { mutableStateOf(false) }
                                    OutlinedButton(onClick = { dateMenu = true }, shape = RoundedCornerShape(10.dp)) {
                                        Text(formatDateStr(effectiveDate ?: ""), fontSize = 12.sp)
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                                        availableDates.forEach { d ->
                                            val count = attempts.count { com.quizforge.app.logic.XpEngine.dateStr(it.attemptedAt) == d }
                                            DropdownMenuItem(text = { Text("${formatDateStr(d)}  •  $count attempts") }, onClick = { selectedDate = d; dateMenu = false })
                                        }
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${dayAttempts.size} attempts  •  ${historyByQuiz.size} quizzes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Indigo
                                )
                            }

                            if (historyByQuiz.isEmpty()) {
                                Text("Nothing on this day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                historyByQuiz.forEach { (qid, count, info) ->
                                    val (bestPct, totalSecs, sortedAttempts) = info
                                    val title = quizTitles[qid] ?: "Unknown quiz"
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(34.dp).background(Indigo.copy(alpha = 0.12f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                            Text("$count", fontWeight = FontWeight.Bold, color = Indigo, fontSize = 13.sp)
                                        }
                                        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            Text(
                                                "$count ${if (count == 1) "attempt" else "attempts"}  •  best $bestPct%  •  ${formatHm(totalSecs)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("${sortedAttempts.first().correctAnswers}/${sortedAttempts.first().totalQuestions}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun buildHeatmap(attempts: List<Attempt>): Map<String, Int> {
    val map = linkedMapOf<String, Int>()
    attempts.forEach { a ->
        val day = com.quizforge.app.logic.XpEngine.dateStr(a.attemptedAt)
        map[day] = (map[day] ?: 0) + 1
    }
    return map
}

private fun formatHm(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatDateStr(date: String): String {
    if (date.isBlank()) return ""
    return try {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
            .format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(date)!!)
    } catch (_: Exception) {
        date
    }
}
