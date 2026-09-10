package com.kvizo.app.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import com.kvizo.app.ui.theme.Violet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.kvizo.app.R
import com.kvizo.app.Routes
import com.kvizo.app.data.Attempt
import com.kvizo.app.data.AttemptAnswer
import com.kvizo.app.data.Quiz
import com.kvizo.app.data.Question
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.AnimatedCounter
import com.kvizo.app.ui.components.ConfettiOverlay
import com.kvizo.app.ui.components.ForgeCard
import com.kvizo.app.ui.components.PressGlowButton
import com.kvizo.app.ui.theme.Amber
import com.kvizo.app.ui.theme.Green
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Red
import com.kvizo.app.ui.theme.SpaceGrotesk
import com.kvizo.app.ui.theme.VioletGrad
import com.kvizo.app.ui.theme.VioletGradient
import com.kvizo.app.ui.theme.VioletLight
import com.kvizo.app.ui.theme.amberBg
import com.kvizo.app.ui.theme.amberBorder
import com.kvizo.app.ui.theme.violetGradient
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun ResultsScreen(vm: AppViewModel, nav: NavHostController, attemptId: String) {
    var attempt by remember { mutableStateOf<Attempt?>(null) }
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var wrongIds by remember { mutableStateOf(listOf<String>()) }
    var newBadges by remember { mutableStateOf(listOf<String>()) }
    var xpGained by remember { mutableStateOf(0) }
    var perfect by remember { mutableStateOf(false) }
    var reviewQuestions by remember { mutableStateOf(listOf<Question>()) }
    var answerMap by remember { mutableStateOf(mapOf<String, AttemptAnswer>()) }
    // confetti fires exactly once — after the score ring finishes its speed-meter sweep
    var celebrate by remember(attemptId) { mutableStateOf(false) }
    val context = LocalContext.current

    // hide the confetti layer again once the burst has finished falling
    LaunchedEffect(celebrate) {
        if (celebrate) {
            delay(3400)
            celebrate = false
        }
    }

    LaunchedEffect(attemptId) {
        attempt = vm.repo.getAttemptById(attemptId)
        attempt?.let { a ->
            quiz = vm.repo.getQuizById(a.quizId)
            val answers = vm.repo.getAttemptAnswers(a.id)
            answerMap = answers.associateBy { it.questionId }
            wrongIds = answers.filter { !it.isCorrect }.map { it.questionId }
            quiz?.let { reviewQuestions = vm.repo.getQuestions(it.id) }
            newBadges = vm.repo.getBadges(a.profileId)
                .filter { kotlin.math.abs(it.unlockedAt - a.attemptedAt) < 2000 }
                .map { it.badgeName }
            xpGained = a.xpEarned
            perfect = a.totalQuestions > 0 && a.correctAnswers == a.totalQuestions
        }
    }

    // Back always lands on Home — never a stuck screen
    BackHandler {
        nav.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Celebration: trophy fanfare + confetti + epic vibration when the quiz wraps up.
    LaunchedEffect(attempt?.id) {
        val a = attempt ?: return@LaunchedEffect
        delay(350)
        if (a.score >= 60) {
            vm.playSuccessJingle()
            vm.epicVibrate()
        } else {
            vm.playBell()
        }
    }

    val a = attempt
    if (a == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val q = quiz

    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiOverlay(show = celebrate, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Forge header: "QUIZ COMPLETE" kicker + title
            Text(
                "QUIZ COMPLETE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (q != null) {
                Text(
                    q.title,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            ScoreRing(score = a.score, modifier = Modifier.padding(top = 20.dp), onSettled = { celebrate = true })

            Text(
                "${a.correctAnswers}/${a.totalQuestions} correct · ${a.timeTakenSeconds}s avg",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            // XP card — Forge: white card, gradient icon tile, +XP, level total
            ForgeCard(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(46.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.background(violetGradient(), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.OfflineBolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCounter(xpGained, modifier = Modifier)
                            Text(" XP earned", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 5.dp, start = 6.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Level ${vm.profile?.level ?: 1}", fontFamily = SpaceGrotesk, color = Indigo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${vm.profile?.xp ?: 0} XP total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }

            // new badges — animated pill cards
            newBadges.forEachIndexed { idx, name ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn()
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = amberBg(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, amberBorder()),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                            Text(
                                "  Badge Unlocked: $name",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Amber
                            )
                        }
                    }
                }
            }

            // answer review summary
            Text("Review", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewStat("${a.correctAnswers}", "Correct", Green, Modifier.weight(1f))
                ReviewStat("${a.totalQuestions - a.correctAnswers}", "Wrong", Red, Modifier.weight(1f))
                ReviewStat("${a.timeTakenSeconds}s", "Time", Indigo, Modifier.weight(1f))
            }

            // full answer review — what the user answered for every question
            if (reviewQuestions.isNotEmpty()) {
                Text("Your Answers", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp))
                reviewQuestions.forEach { rq ->
                    val ans = answerMap[rq.id]
                    val userSel = ans?.selectedOption
                    val correct = ans?.isCorrect == true
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (correct) Green.copy(alpha = 0.10f) else Red.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (correct) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (correct) Green else Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "  ${rq.questionText}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            val opts = rq.options()
                            val userLabel = opts.firstOrNull { it.first == userSel }?.second ?: "Not answered"
                            Text(
                                "Your answer: ${if (userSel.isNullOrBlank()) "Not answered" else "${userSel}) $userLabel"}",
                                fontSize = 12.sp,
                                color = if (correct) Green else Red,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            if (!correct) {
                                val correctLabel = opts.firstOrNull { it.first == rq.correctOption }?.second ?: ""
                                Text(
                                    "Correct: ${rq.correctOption}) $correctLabel",
                                    fontSize = 12.sp,
                                    color = Green,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // actions — Forge: Try Again (gradient) + Continue (purple press glow)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                PressGlowButton(
                    onClick = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true }; launchSingleTop = true } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(99.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    glowColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Home", fontSize = 13.sp)
                }
                if (wrongIds.isNotEmpty()) {
                    PressGlowButton(
                        onClick = {
                            nav.navigate(Routes.attempt(a.quizId, "normal", wrongIds.joinToString(","))) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(99.dp),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Retry ${wrongIds.size} wrong", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            PressGlowButton(
                onClick = {
                    val msg = buildString {
                        append("🔥 I scored ${a.score}% on \"${q?.title ?: "a quiz"}\" in Kvizo!\n")
                        append("✅ ${a.correctAnswers}/${a.totalQuestions} correct · ${a.timeTakenSeconds}s avg · +$xpGained XP\n")
                        append("Think you can beat me? 🏆")
                    }
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            },
                            "Share my score"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(99.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                glowColor = Violet
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  Share my score", fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Forge score ring — glow halo, gradient stroke, white center with gradient number. */
@Composable
private fun ScoreRing(score: Int, modifier: Modifier = Modifier, onSettled: () -> Unit = {}) {
    val pct = (score / 100f).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.outline
    // speed-meter sweep: fast accelerating run that lands EXACTLY on the real score
    val animated = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animated.snapTo(0f)
        animated.animateTo(pct, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
        onSettled()
    }
    val shownScore = (score * animated.value).roundToInt().coerceIn(0, 100)
    Box(modifier = modifier.size(158.dp), contentAlignment = Alignment.Center) {
        // halo glow (Forge: conic-gradient blurred at 30% opacity)
        Box(
            modifier = Modifier
                .size(158.dp + 28.dp)
                .background(
                    Brush.radialGradient(
                        listOf(VioletLight.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // ring
        Canvas(modifier = Modifier.size(158.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            // track
            drawArc(
                color = trackColor,
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // gradient progress — sweeps like a speedometer needle
            drawArc(
                brush = Brush.sweepGradient(listOf(com.kvizo.app.ui.theme.Violet, com.kvizo.app.ui.theme.VioletGrad, com.kvizo.app.ui.theme.Violet)),
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        // center circle (Forge: 108dp white, glow shadow)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.size(108.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    "$shownScore",
                    style = TextStyle(brush = VioletGradient, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 40.sp)
                )
                Text("out of 100", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ReviewStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    ForgeCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
