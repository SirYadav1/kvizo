package com.kvizo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kvizo.app.ui.theme.LocalThemeDark
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kvizo.app.ui.theme.Amber
import com.kvizo.app.ui.theme.Green
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Red
import java.util.Calendar

/** Donut chart showing a percentage in the center. */
@Composable
fun DonutChart(percent: Float, modifier: Modifier = Modifier, color: Color = Indigo, size: Dp = 140.dp, label: String? = null) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.toPx() - stroke, size.toPx() - stroke)
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0f, 1f)),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(percent * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            if (label != null) Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Horizontal bar chart: label -> value 0..1. */
@Composable
fun HBarChart(items: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((label, value) in items) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontSize = 12.sp,
                    modifier = Modifier.width(110.dp),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value.coerceIn(0f, 1f))
                            .height(14.dp)
                            .background(Indigo, RoundedCornerShape(7.dp))
                    )
                }
                Text("${(value * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Simple line chart for accuracy trend. */
@Composable
fun LineChart(points: List<Float>, modifier: Modifier = Modifier, color: Color = Green) {
    if (points.size < 2) {
        Box(modifier = modifier.height(120.dp), contentAlignment = Alignment.Center) {
            Text("Not enough data yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Canvas(modifier = modifier.height(120.dp).fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val maxV = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val stepX = w / (points.size - 1)
        val pts = points.mapIndexed { i, v ->
            Offset(i * stepX, h - (v / maxV) * (h - 16f) - 8f)
        }
        // grid lines
        for (g in 1..3) {
            val y = h * g / 4f
            drawLine(Color.Gray.copy(alpha = 0.2f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        // area fill
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(pts.first().x, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, h)
            close()
        }
        drawPath(path, color.copy(alpha = 0.15f))
        // line
        for (i in 1 until pts.size) {
            drawLine(color, pts[i - 1], pts[i], strokeWidth = 3f, cap = StrokeCap.Round)
        }
        // dots
        pts.forEach { drawCircle(color, radius = 4f, center = it) }
    }
}

/** Activity calendar — last `weeks` weeks, Monday-starting, with weekday
 * headers, month labels on the left and a gentle violet scale. */
@Composable
fun HeatmapCalendar(dailyCounts: Map<String, Int>, modifier: Modifier = Modifier, weeks: Int = 14) {
    val cal = Calendar.getInstance()
    val end = cal.clone() as Calendar
    // Start from Monday of the week (weeks-1) weeks ago
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) cal.add(Calendar.DAY_OF_YEAR, -1)
    cal.add(Calendar.DAY_OF_YEAR, -(weeks - 1) * 7)
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val monthFmt = java.text.SimpleDateFormat("MMM", java.util.Locale.US)
    val days = mutableListOf<Pair<String, Int>>()
    var c = cal.clone() as Calendar
    while (c.before(end) || c.timeInMillis <= end.timeInMillis) {
        days.add(fmt.format(c.time) to (dailyCounts[fmt.format(c.time)] ?: 0))
        c.add(Calendar.DAY_OF_YEAR, 1)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // weekday header (Mon..Sun)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(start = 30.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        var lastMonth = ""
        for (week in 0 until weeks) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                // month label — only when the month changes
                val weekStart = week * 7
                val monthKey = if (weekStart < days.size) days[weekStart].first.take(7) else ""
                val month = if (monthKey.isNotEmpty() && monthKey != lastMonth) monthFmt.format(
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(monthKey + "-01")!!
                ) else ""
                if (month.isNotEmpty()) {
                    lastMonth = monthKey
                }
                Text(
                    month,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp)
                )
                for (day in 0 until 7) {
                    val idx = weekStart + day
                    if (idx < days.size) {
                        val (_, count) = days[idx]
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(heatColor(count), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

/** Intensity color for a heatmap cell, theme-aware. */
@Composable
private fun heatColor(count: Int): Color {
    val dark = LocalThemeDark.current
    val steps = listOf("#CDCFE0", "#DEB9F5", "#B98AF5", "#9363E8", "#6D3BD3", "#4B22B0")
    val level = when {
        count <= 0 -> 0
        count <= 3 -> 1
        count <= 6 -> 2
        count <= 10 -> 3
        count <= 15 -> 4
        else -> 5
    }
    val c = steps[level]
    return if (dark) {
        val base = listOf("#1E1B2E", "#3A2D6B", "#5B3EA8", "#7D55C9", "#A27AE8", "#C4A8F5")
        Color(android.graphics.Color.parseColor(base[level]))
    } else Color(android.graphics.Color.parseColor(c))
}

/** Stacked bar chart for difficulty breakdown. */
@Composable
fun StackedBars(items: List<Pair<String, Float>>, modifier: Modifier = Modifier, colors: List<Color> = listOf(Green, Amber, Red)) {
    Row(modifier = modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
        items.forEachIndexed { i, (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text("${(value * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height((140f * value.coerceIn(0f, 1f)).dp)
                        .background(colors[i % colors.size], RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
