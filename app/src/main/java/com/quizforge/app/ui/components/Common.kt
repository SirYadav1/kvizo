package com.quizforge.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Ink
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.ui.theme.SpaceGrotesk
import com.quizforge.app.ui.theme.Violet
import com.quizforge.app.ui.theme.VioletGradient
import kotlinx.coroutines.delay
import kotlin.random.Random

/* ------------------------------------------------------------------ */
/* Figma design-system components ("Forge")                            */
/* ------------------------------------------------------------------ */

/** Figma card: white, 1px border, radius 18 (ee in the Figma prototype). */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) { content() }
}

/** Section label — uppercase, 10–11px, letter-spaced (Figma style). */
@Composable
fun ForgeSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = modifier
    )
}

/** Gradient text (Figma .grad-text) — violet → light-violet sweep. */
@Composable
fun GradientText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight = FontWeight.Bold, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextStyle(
            brush = VioletGradient,
            fontFamily = SpaceGrotesk,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = fontSize * 1.1
        ),
        modifier = modifier
    )
}

/** Pill chip — selected: violet gradient + white text; unselected: surface + border. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
        shape = RoundedCornerShape(99.dp),
        color = if (selected) Violet else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (selected) VioletGradient else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                    RoundedCornerShape(99.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Colored status pill (Figma status chips) — tinted bg + colored border. */
@Composable
fun StatusPill(
    label: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(99.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

/** Figma progress bar — 5–7px, rounded 99, gradient fill. */
@Composable
fun ForgeProgressBar(progress: Float, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 6.dp) {
    Box(
        modifier = modifier
            .height(height)
            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(99.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .background(VioletGradient, RoundedCornerShape(99.dp))
        )
    }
}

/** Uppercase tiny label with letter-spacing (Figma "Question 5 of 12"). */
@Composable
fun ForgeKicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em,
        color = Indigo,
        modifier = modifier
    )
}

/** Difficulty badge with color coding. */
@Composable
fun DifficultyBadge(difficulty: String, modifier: Modifier = Modifier) {
    val color = when (difficulty) {
        "Easy" -> Green
        "Medium" -> Amber
        else -> Red
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            difficulty,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = when (status) {
        "published" -> Green
        "draft" -> Amber
        else -> Color.Gray
    }
    Surface(modifier = modifier, color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            status.replaceFirstChar { it.uppercase() },
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Stat card used on the dashboard — pastel tinted tile with soft border. */
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, tint: Color = Indigo) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("•", color = tint, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                label,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

/** Celebration confetti — multi-burst sprinkle with falling, rotating pieces. */
@Composable
fun ConfettiOverlay(show: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = show, enter = fadeIn(), exit = fadeOut()) {
        val pieces = remember {
            List(120) { i ->
                ConfettiPiece(
                    angle = Random.nextInt(360),
                    speed = 0.5f + Random.nextFloat() * 0.9f,
                    strip = i % 3 != 0,
                    colorIndex = i % 6
                )
            }
        }
        val progress = remember { Animatable(0f) }
        LaunchedEffect(show) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(durationMillis = 2600, easing = LinearEasing))
        }
        Canvas(modifier = modifier.fillMaxSize()) {
            val colors = listOf(Indigo, Amber, Green, Red, Color(0xFFFF6EC7), Color(0xFF00BCD4))
            pieces.forEach { p ->
                val t = progress.value
                // three staggered bursts: showers rain from all edges
                val burstStart = listOf(0f, 0.3f, 0.6f)[p.colorIndex % 3]
                val local = ((t - burstStart) / (1f - burstStart)).coerceIn(0f, 1f)
                val rad = Math.toRadians(p.angle.toDouble())
                val dist = local * size.width * (0.4f + 0.6f * p.speed)
                val x = size.width / 2f + (Math.cos(rad) * dist).toFloat()
                // gravity: y accelerates as pieces fall; start from every row so "You" is covered
                val y = size.height * 0.08f + (Math.sin(rad).toFloat() * dist).coerceAtLeast(0f) + local * local * size.height * 0.85f
                val color = colors[p.colorIndex]
                val alpha = (1f - local).coerceIn(0.15f, 1f)
                if (p.strip) {
                    val s = 2.5f + (p.colorIndex % 3) * 0.9f
                    rotate(p.angle + t * 540f) {
                        drawRect(color.copy(alpha = alpha), topLeft = Offset(x, y), size = Size(s * 2.4f, s))
                    }
                } else {
                    drawCircle(color.copy(alpha = alpha), radius = 2.8f + (p.colorIndex % 3), center = Offset(x, y))
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val angle: Int,
    val speed: Float,
    val strip: Boolean,
    val colorIndex: Int
)

/** Animated count-up number. */
@Composable
fun AnimatedCounter(target: Int, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(target) {
        val steps = 30
        for (i in 1..steps) {
            shown = target * i / steps
            delay(24)
        }
        shown = target
    }
    Text("$shown", modifier = modifier, fontWeight = FontWeight.Bold, fontSize = 28.sp)
}

/** Correct/wrong answer feedback icons. */
@Composable
fun AnswerFeedbackIcon(correct: Boolean, modifier: Modifier = Modifier) {
    if (correct) {
        Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = Green, modifier = modifier.size(48.dp))
    } else {
        Icon(Icons.Filled.Close, contentDescription = "Wrong", tint = Red, modifier = modifier.size(48.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

/** XP popup shown after answering. */
@Composable
fun XpPopup(show: Boolean, amount: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = show, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = modifier
                .background(Indigo.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
            Text(" +$amount XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
