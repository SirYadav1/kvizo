package com.kvizo.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import com.kvizo.app.ui.components.AnimatedCounter
import com.kvizo.app.ui.components.ConfettiOverlay
import com.kvizo.app.ui.theme.Amber
import com.kvizo.app.ui.theme.Green
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Red

@Composable
fun ResultsScreen(vm: AppViewModel, nav: NavHostController, attemptId: String) {
    var attempt by remember { mutableStateOf<Attempt?>(null) }
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var wrongIds by remember { mutableStateOf(listOf<String>()) }
    var newBadges by remember { mutableStateOf(listOf<String>()) }
    var levelUp by remember { mutableStateOf(false) }
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
            // Badges: diff = current badges vs badges before this attempt. Simplification:
            // the repository returns newly unlocked ones during record; we re-derive by
            // showing all badges whose unlock time is within 2s of the attempt.
            newBadges = vm.repo.getBadges(a.profileId)
                .filter { kotlin.math.abs(it.unlockedAt - a.attemptedAt) < 2000 }
                .map { it.badgeName }
            levelUp = false
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

    // Celebration: jingle + epic vibration only on a good score
    LaunchedEffect(Unit) {
        val sc = attempt?.score ?: 0
        if (sc >= 60) {
            delay(400)
            vm.playSuccessJingle()
            vm.epicVibrate()
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
        ConfettiOverlay(show = a.score >= 60, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Quiz Complete!", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            if (q != null) Text(q.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            // score ring
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(150.dp)
                    .background(
                        if (a.score >= 60) Green.copy(alpha = 0.15f) else Red.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${a.score}", fontWeight = FontWeight.Black, fontSize = 44.sp, color = if (a.score >= 60) Green else Red)
                    Text("out of 100", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "${a.correctAnswers} / ${a.totalQuestions} correct",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // XP card
            Surface(shape = RoundedCornerShape(16.dp), color = Indigo, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("+", color = Color.White, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCounter(xpGained, modifier = Modifier)
                            Text(" XP earned", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp, start = 6.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Level ${vm.profile?.level ?: 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${vm.profile?.xp ?: 0} XP total", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }

            // new badges
            if (newBadges.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = Amber.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Badge unlocked!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Amber)
                        newBadges.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                                Text("  $b", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // answer review summary
            Text("Review", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewStat("${a.correctAnswers}", "Correct", Green, Modifier.weight(1f))
                ReviewStat("${a.totalQuestions - a.correctAnswers}", "Wrong", Red, Modifier.weight(1f))
                ReviewStat("${a.timeTakenSeconds}s", "Time", Indigo, Modifier.weight(1f))
            }

            // full answer review — what the user answered for every question
            if (reviewQuestions.isNotEmpty()) {
                Text("Your Answers", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp))
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

            // actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                OutlinedButton(
                    onClick = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true }; launchSingleTop = true } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Home", fontSize = 13.sp)
                }
                if (wrongIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            nav.navigate(Routes.attempt(a.quizId, "normal", wrongIds.joinToString(","))) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp)
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

@Composable
private fun ReviewStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
