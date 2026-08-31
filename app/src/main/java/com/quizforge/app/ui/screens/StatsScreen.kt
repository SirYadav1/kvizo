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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.BarChart
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
    var attempts by remember { mutableStateOf(listOf<Attempt>()) }
    var range by remember { mutableStateOf("All Time") }
    var exportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(profile?.id) {
        profile?.let { attempts = vm.repo.getAttempts(it.id) }
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
            val cat = vm.repo.getQuizById(a.quizId)?.category ?: "Other"
            val e = map[cat] ?: (0 to 0)
            map[cat] = (e.first + a.correctAnswers) to (e.second + a.totalQuestions)
        }
        map.map { (k, v) -> k to (v.first.toFloat() / v.second.coerceAtLeast(1)) }.sortedByDescending { it.second }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.weight(1f))
            var rangeMenu by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { rangeMenu = true }, shape = RoundedCornerShape(10.dp)) {
                Text(range, fontSize = 12.sp)
            }
            DropdownMenu(expanded = rangeMenu, onDismissRequest = { rangeMenu = false }) {
                listOf("All Time", "This Week", "This Month").forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = { range = r; rangeMenu = false })
                }
            }
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
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Activity Heatmap")
                        HeatmapCalendar(heatmap, modifier = Modifier.padding(top = 8.dp))
                        Text("Last 15 weeks • 365 days", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // accuracy donut
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
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
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Accuracy Trend (last ${trend.size} attempts)")
                        LineChart(trend, modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp))
                    }
                }
            }

            // category bars
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
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
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Weak Areas")
                        val weak = categoryStats.filter { it.second < 0.6f }
                        if (weak.isEmpty()) {
                            Text("Nothing to improve — great job! 🎯", fontSize = 12.sp, color = Green)
                        } else {
                            weak.forEach { (cat, acc) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("${(acc * 100).toInt()}%", fontSize = 13.sp, color = Red)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, tint = Indigo, modifier = Modifier.size(20.dp))
                        Text("  Best score: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filtered.maxOfOrNull { it.score } ?: 0}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
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
