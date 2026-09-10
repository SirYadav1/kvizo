package com.kvizo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import com.kvizo.app.ui.theme.Amber
import com.kvizo.app.ui.theme.Green
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Ink
import com.kvizo.app.ui.theme.Red
import com.kvizo.app.ui.theme.SpaceGrotesk
import com.kvizo.app.ui.theme.Violet
import com.kvizo.app.ui.theme.VioletGradient
import com.kvizo.app.ui.theme.amberBg
import com.kvizo.app.ui.theme.greenBg
import com.kvizo.app.ui.theme.redBg
import com.kvizo.app.ui.theme.violetGradient
import com.kvizo.app.ui.theme.violetPale
import kotlinx.coroutines.delay
import kotlin.random.Random

/* ------------------------------------------------------------------ */
/* Forge design-system components ("Forge")                            */
/* ------------------------------------------------------------------ */

/** Forge card: white, 1px border, radius 18 (ee in the Forge prototype). */
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

/** Section label — uppercase, 10–11px, letter-spaced (Forge style). */
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

/** Gradient text (Forge .grad-text) — violet → light-violet sweep (dark-aware). */
@Composable
fun GradientText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight = FontWeight.Bold, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextStyle(
            brush = violetGradient(),
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
        modifier = modifier.clickable(onClick = onClick, indication = androidx.compose.foundation.LocalIndication.current, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
        shape = RoundedCornerShape(99.dp),
        color = if (selected) Violet else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (selected) violetGradient() else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
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

/** Colored status pill (Forge status chips) — tinted bg + colored border. */
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

/** Forge progress bar — 5–7px, rounded 99, gradient fill (dark-aware). */
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
                .background(violetGradient(), RoundedCornerShape(99.dp))
        )
    }
}

/** Uppercase tiny label with letter-spacing (Forge "Question 5 of 12"). */
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

/**
 * Primary action button with a purple press-glow: on touch the whole area
 * around the button blooms with a soft violet radial halo + subtle scale-down.
 */
@Composable
fun PressGlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    glowColor: Color = Violet,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.65f, stiffness = 800f),
        label = "pressGlow"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.65f, stiffness = 900f),
        label = "pressScale"
    )
    Box(
        modifier = modifier
            .drawBehind {
                if (glow > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                glowColor.copy(alpha = 0.42f * glow),
                                glowColor.copy(alpha = 0.14f * glow),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.95f
                        )
                    )
                }
            }
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .scale(scale)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)
            ) { content() }
        }
    }
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

/** Celebration confetti — colorful paper pieces falling from the top, once per burst. */
@Composable
fun ConfettiOverlay(show: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = show, enter = fadeIn(), exit = fadeOut()) {
        val pieces = remember {
            List(120) { i ->
                FallingConfetti(
                    startX = 0.02f + Random.nextFloat() * 0.96f,   // horizontal spawn (fraction of width)
                    delay = Random.nextFloat() * 0.4f,             // stagger the fall start
                    duration = 1.5f + Random.nextFloat() * 1.4f,   // fall duration in seconds
                    sway = 18f + Random.nextFloat() * 44f,         // horizontal sway amplitude (px)
                    size = 7f + Random.nextFloat() * 9f,           // piece size (px) — big enough to see
                    strip = i % 3 != 0,
                    colorIndex = i % 8
                )
            }
        }
        val progress = remember { Animatable(0f) }
        LaunchedEffect(show) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(durationMillis = 3000, easing = LinearEasing))
        }
        Canvas(modifier = modifier.fillMaxSize()) {
            val colors = listOf(
                Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFFF59E0B), Color(0xFF10B981),
                Color(0xFFEC4899), Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFEF4444)
            )
            pieces.forEach { p ->
                val t = ((progress.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (t <= 0f) return@forEach
                // gravity: y accelerates (t^2) from above the top edge
                val y = -60f + t * t * (size.height + 120f)
                // swaying drift as it falls
                val x = p.startX * size.width + kotlin.math.sin(t * 6.28f + p.delay * 9f) * p.sway
                val color = colors[p.colorIndex]
                val alpha = ((1f - t) * 1.2f).coerceIn(0f, 1f)
                if (p.strip) {
                    rotate(p.colorIndex * 45f + t * 720f) {
                        drawRect(
                            color.copy(alpha = alpha),
                            topLeft = Offset(x, y),
                            size = Size(p.size * 2.1f, p.size)
                        )
                    }
                } else {
                    drawCircle(color.copy(alpha = alpha), radius = p.size * 0.55f, center = Offset(x, y))
                }
            }
        }
    }
}

private data class FallingConfetti(
    val startX: Float,
    val delay: Float,
    val duration: Float,
    val sway: Float,
    val size: Float,
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
