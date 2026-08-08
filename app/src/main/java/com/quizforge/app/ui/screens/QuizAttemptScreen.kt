package com.quizforge.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.Question
import com.quizforge.app.data.Quiz
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.DifficultyBadge
import com.quizforge.app.ui.components.XpPopup
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { INFO, PLAYING, SUBMITTING }

private val Orange = Color(0xFFF57C2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizAttemptScreen(vm: AppViewModel, nav: NavHostController, quizId: String, mode: String, only: String = "") {
    val quiz = remember(quizId) { vm.getQuiz(quizId) }
    val allQuestions = remember(quizId) { vm.getQuestions(quizId) }
    // Every attempt gets a fresh random order.
    val questions = remember(allQuestions, only) {
        val base = if (only.isNotBlank()) {
            val ids = only.split(",").toSet()
            allQuestions.filter { it.id in ids }
        } else allQuestions
        base.shuffled()
    }

    var phase by remember { mutableStateOf(Phase.INFO) }
    var current by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var flagged by remember { mutableStateOf<Set<String>>(emptySet()) }
    var revealed by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showNavigator by remember { mutableStateOf(false) }
    var xpPopup by remember { mutableStateOf(false) }
    var timed by remember { mutableStateOf(false) }
    var timeSeconds by remember { mutableStateOf(quiz?.timeLimitSeconds ?: 60) }
    var timeLeft by remember { mutableStateOf(quiz?.timeLimitSeconds ?: 60) }
    var runScore by remember { mutableStateOf(0) }
    var lastCorrect by remember { mutableStateOf(true) }
    val startTime = remember { System.currentTimeMillis() }
    var settings by remember { mutableStateOf(AppSettings("system", true, true, "pause")) }
    LaunchedEffect(Unit) { vm.settings.collect { settings = it } }

    val lifecycleOwner = LocalLifecycleOwner.current
    var autoSubmitted by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun submit() {
        if (phase == Phase.SUBMITTING) return
        phase = Phase.SUBMITTING
        val timeTaken = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(1)
        scope.launch {
            val result = vm.recordAttempt(quiz!!, questions, answers, timeTaken)
            nav.navigate(Routes.results(result.attempt.id)) {
                popUpTo(Routes.QUIZZES) { inclusive = false }
            }
        }
    }

    // countdown timer for the whole quiz (only when timed)
    if (phase == Phase.PLAYING && timed) {
        LaunchedEffect(current) {
            timeLeft = timeSeconds
            while (timeLeft > 0) {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    delay(1000)
                    timeLeft--
                } else {
                    delay(500)
                    if (settings.timerOnBackground == "submit") { autoSubmitted = true; submit(); break }
                }
            }
            if (!autoSubmitted && timeLeft <= 0) submit()
        }
    }

    fun selectOption(opt: String) {
        if (revealed) return
        selected = opt
        val q = questions[current]
        answers[q.id] = opt
        val correct = opt == q.correctOption
        if (settings.soundEnabled) vm.playSound(correct)
        if (settings.hapticsEnabled) vm.vibrate(correct)
        lastCorrect = correct
        runScore = (runScore + (if (correct) 10 else -5)).coerceIn(0, questions.size * 10)
        revealed = true
        if (correct) {
            xpPopup = true
            scope.launch { delay(900); xpPopup = false }
        }
    }

    fun next() {
        if (current < questions.size - 1) {
            current++
            revealed = false
            selected = null
        } else {
            submit()
        }
    }

    when (phase) {
        Phase.INFO -> InfoPhase(
            vm, nav, quiz, questions.size,
            timed = timed, timeSeconds = timeSeconds,
            onTimedChange = { timed = it },
            onSecondsChange = { timeSeconds = it },
            onStart = { phase = Phase.PLAYING }
        )

        Phase.SUBMITTING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Saving your result...", modifier = Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Phase.PLAYING -> {
            val q = questions[current]
            Column(modifier = Modifier.fillMaxSize()) {
                // top bar — Figma: surface + bottom border, back tile, centered title, timer chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    Text(
                        quiz?.title ?: "",
                        fontFamily = com.quizforge.app.ui.theme.SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    if (timed) {
                        TimerChip("$timeLeft", Red)
                    } else {
                        Spacer(Modifier.width(34.dp))
                    }
                }
                // Figma progress: 5px rounded gradient
                com.quizforge.app.ui.components.ForgeProgressBar(
                    progress = (current + 1f) / questions.size,
                    modifier = Modifier.fillMaxWidth(),
                    height = 5.dp
                )

                // live score bar — green up on correct, red down on wrong
                PointsBar(runScore, questions.size * 10, lastCorrect)

                // utility row — flag, navigator, submit (kept functional, compact)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    TextButton(onClick = {
                        val id = q.id
                        flagged = if (id in flagged) flagged - id else flagged + id
                    }) {
                        Icon(
                            if (q.id in flagged) Icons.Filled.Flag else Icons.Filled.BookmarkBorder,
                            contentDescription = "Flag",
                            tint = if (q.id in flagged) Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(if (q.id in flagged) "  Flagged" else "  Flag", fontSize = 12.sp, color = if (q.id in flagged) Red else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showNavigator = true }) {
                        Icon(Icons.Filled.Apps, contentDescription = "Navigator", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text("  Navigator", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { submit() }) {
                        Text("Submit", color = com.quizforge.app.ui.theme.Violet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.quizforge.app.ui.components.ForgeKicker(
                                    "Question ${current + 1} of ${questions.size}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Text(
                                q.questionText,
                                fontFamily = com.quizforge.app.ui.theme.SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                lineHeight = 25.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(q.options()) { (letter, text) ->
                            val isSelected = selected == letter
                            val isCorrect = letter == q.correctOption
                            val bg = when {
                                !revealed && isSelected -> Indigo.copy(alpha = 0.15f)
                                revealed && isCorrect -> Green.copy(alpha = 0.18f)
                                revealed && isSelected && !isCorrect -> Red.copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val borderColor = when {
                                revealed && isCorrect -> Green
                                revealed && isSelected && !isCorrect -> Red
                                isSelected -> Indigo
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            // animated touch: card springs in on press
                            val interactionSource = remember { MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 0.97f else 1f,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
                                label = "optionScale"
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = bg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = androidx.compose.foundation.LocalIndication.current,
                                        enabled = !revealed
                                    ) { selectOption(letter) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(30.dp).background(borderColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(letter.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                                    if (revealed && isCorrect) Icon(Icons.Filled.Check, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                                    if (revealed && isSelected && !isCorrect) Icon(Icons.Filled.Close, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                                XpPopup(show = xpPopup && revealed, amount = 10, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        if (revealed) {
                            item {
                                Button(
                                    onClick = { next() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Text(if (current < questions.size - 1) "Next" else "Finish", modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            if (showNavigator) {
                ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Question Navigator", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Green: correct • Red: wrong • Grey: not answered • Orange: flagged",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        val grid = questions.chunked(6)
                        grid.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                                row.forEach { itemQ ->
                                    val answered = answers.containsKey(itemQ.id)
                                    val isCorrect = answered && answers[itemQ.id] == itemQ.correctOption
                                    val isFlagged = itemQ.id in flagged
                                    val bg = when {
                                        isFlagged -> Orange
                                        answered && isCorrect -> Green
                                        answered && !isCorrect -> Red
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val idx = questions.indexOf(itemQ)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(bg, RoundedCornerShape(10.dp))
                                            .clickable {
                                                current = idx
                                                revealed = answers.containsKey(itemQ.id)
                                                selected = answers[itemQ.id]
                                                showNavigator = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${idx + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/** Live points bar: grows green on a correct answer, drops red on a wrong one. */
@Composable
private fun PointsBar(runScore: Int, maxScore: Int, lastCorrect: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (maxScore > 0) (runScore.toFloat() / maxScore).coerceIn(0f, 1f) else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "pointsBar"
    )
    val barColor by animateColorAsState(if (lastCorrect) Green else Red, label = "pointsBarColor")
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text("Score", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(8.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text("$runScore", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = barColor)
        }
    }
}

@Composable
/** Figma timer chip — red pill with clock icon. */
@Composable
private fun TimerChip(text: String, color: Color) {
    Surface(
        color = com.quizforge.app.ui.theme.RedBg,
        shape = RoundedCornerShape(99.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.quizforge.app.ui.theme.RedBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Icon(
                Icons.Filled.Timer,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(" $text", color = color, fontFamily = com.quizforge.app.ui.theme.SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoPhase(
    vm: AppViewModel,
    nav: NavHostController,
    quiz: Quiz?,
    questionCount: Int,
    timed: Boolean,
    timeSeconds: Int,
    onTimedChange: (Boolean) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    if (quiz == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Quiz not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Quiz Info", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(72.dp).background(Indigo.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Text("Q", color = Indigo, fontWeight = FontWeight.Black, fontSize = 30.sp)
        }
        Text(quiz.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(quiz.category, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
            DifficultyBadge(quiz.difficulty)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                Text("$questionCount questions", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
            if (quiz.timeLimitSeconds != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                    Text("${quiz.timeLimitSeconds}s default", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }

        // quiz description (replaces the old "how it works")
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About this quiz", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (quiz.description.isNotBlank()) quiz.description
                    else "Answer each question and get instant feedback. Use the flag icon to review questions later. Your score counts toward levels and badges.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // timed quiz option
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Timed quiz", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Auto-submits when time runs out", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = timed, onCheckedChange = onTimedChange)
                }
                if (timed) {
                    OutlinedTextField(
                        value = if (timeSeconds > 0) timeSeconds.toString() else "",
                        onValueChange = { text ->
                            val v = text.filter { it.isDigit() }.take(4).toIntOrNull()
                            onSecondsChange((v ?: 0).coerceIn(5, 3600))
                        },
                        label = { Text("Time limit (seconds)") },
                        placeholder = { Text("e.g. 60") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text("  Start Quiz", modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
