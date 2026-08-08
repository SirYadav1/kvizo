package com.quizforge.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.quizforge.app.R
import com.quizforge.app.Routes
import com.quizforge.app.data.Attempt
import com.quizforge.app.data.AttemptAnswer
import com.quizforge.app.data.Quiz
import com.quizforge.app.data.Question
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.AnimatedCounter
import com.quizforge.app.ui.components.ConfettiOverlay
import com.quizforge.app.ui.components.ForgeCard
import com.quizforge.app.ui.components.PressGlowButton
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import com.quizforge.app.ui.theme.SpaceGrotesk
import com.quizforge.app.ui.theme.VioletGrad
import com.quizforge.app.ui.theme.VioletLight
import com.quizforge.app.ui.theme.amberBg
import com.quizforge.app.ui.theme.violetGradient
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
        ConfettiOverlay(show = a.score >= 60, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Figma header: "QUIZ COMPLETE" kicker + title
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

            ScoreRing(score = a.score, modifier = Modifier.padding(top = 20.dp))

            Text(
                "${a.correctAnswers}/${a.totalQuestions} correct · ${a.timeTakenSeconds}s avg",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            // XP card — Figma: white card, gradient icon tile, +XP, level total
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
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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

            // new badges — Figma "Badge Unlocked" pill card
            if (newBadges.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = amberBg(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                        Text(
                            "  Badge Unlocked: ${newBadges.joinToString()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber
                        )
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

            // actions — Figma: Try Again (gradient) + Continue (purple press glow)
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Figma score ring — glow halo, gradient stroke, white center with gradient number. */
@Composable
private fun ScoreRing(score: Int, modifier: Modifier = Modifier) {
    val pct = (score / 100f).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.outline
    Box(modifier = modifier.size(158.dp), contentAlignment = Alignment.Center) {
        // halo glow (Figma: conic-gradient blurred at 30% opacity)
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
            // gradient progress
            drawArc(
                brush = Brush.sweepGradient(listOf(com.quizforge.app.ui.theme.Violet, com.quizforge.app.ui.theme.VioletGrad, com.quizforge.app.ui.theme.Violet)),
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        // center circle (Figma: 108dp white, glow shadow)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.size(108.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    "$score",
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
