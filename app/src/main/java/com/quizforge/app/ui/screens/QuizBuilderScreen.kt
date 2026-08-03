package com.quizforge.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.data.DIFF_EASY
import com.quizforge.app.data.DIFF_HARD
import com.quizforge.app.data.DIFF_MEDIUM
import com.quizforge.app.data.PRESET_CATEGORIES
import com.quizforge.app.data.Question
import com.quizforge.app.data.STATUS_DRAFT
import com.quizforge.app.data.STATUS_PUBLISHED
import com.quizforge.app.parsing.ParseException
import com.quizforge.app.parsing.TxtParser
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red

private class EQ(
    var text: String = "",
    var opts: MutableList<String> = mutableListOf("", "", "", ""),
    var correct: Int = 0,
    var isTF: Boolean = false
) {
    fun toQuestion(quizId: String, position: Int): Question = Question(
        id = "",
        quizId = quizId,
        questionText = text,
        optionA = opts.getOrElse(0) { "" },
        optionB = opts.getOrElse(1) { "" },
        optionC = opts.getOrElse(2) { "" },
        optionD = opts.getOrElse(3) { "" },
        correctOption = "abcd"[correct.coerceIn(0, 3)].toString(),
        position = position,
        isBookmarked = false
    )

    companion object {
        fun from(q: Question): EQ {
            val opts = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
            return EQ(
                text = q.questionText,
                opts = opts.toMutableList(),
                correct = "abcd".indexOf(q.correctOption).coerceAtLeast(0),
                isTF = opts[2].isBlank() && opts[3].isBlank()
            )
        }
    }
}

@Composable
fun QuizBuilderScreen(vm: AppViewModel, nav: NavHostController, quizId: String?) {
    val isEdit = quizId != null
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PRESET_CATEGORIES[0]) }
    var difficulty by remember { mutableStateOf(DIFF_EASY) }
    var tags by remember { mutableStateOf("") }
    var timeLimit by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(mutableListOf<EQ>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var showImportCode by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<Question>?>(null) }

    if (isEdit) {
        val existing = remember(quizId) { vm.getQuiz(quizId) }
        LaunchedEffectLoad(quizId, existing?.title) {
            val q = existing ?: return@LaunchedEffectLoad
            title = q.title
            category = q.category
            difficulty = q.difficulty
            tags = q.tags
            timeLimit = q.timeLimitSeconds?.toString() ?: ""
            questions = vm.getQuestions(quizId).map { EQ.from(it) }.toMutableList()
        }
    }

    val txtPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = vm.getApplication<android.app.Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { br -> br.readText() }
            if (text.isNullOrBlank()) { error = "File is empty"; return@rememberLauncherForActivityResult }
            val parsed = TxtParser.parse(text, "temp")
            pendingImport = parsed
            error = null
        } catch (e: ParseException) {
            error = e.message
            info = null
        } catch (e: Exception) {
            error = "Could not read file: ${e.message}"
        }
    }

    // TXT import preview — user approves before questions are added
    pendingImport?.let { pq ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import preview") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${pq.size} questions ready to add:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    pq.take(10).forEachIndexed { i, q ->
                        Text(
                            "${i + 1}. ${q.questionText}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (pq.size > 10) {
                        Text("...and ${pq.size - 10} more", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    questions = (questions + pq.map { EQ.from(it) }).toMutableList()
                    info = "Imported ${pq.size} questions"
                    pendingImport = null
                }) { Text("Add ${pq.size}") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isEdit) "Edit Quiz" else "Create Quiz", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("${questions.size} questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = {
                txtPicker.launch(arrayOf("text/plain", "text/*", "*/*"))
            }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  Import TXT", fontSize = 12.sp)
            }
        }

        error?.let {
            Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
        info?.let {
            Text(it, color = Green, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Quiz title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                var catMenu by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { catMenu = true }) {
                                    Text("Category: $category", maxLines = 1)
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                                    PRESET_CATEGORIES.forEach { c ->
                                        DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catMenu = false })
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = timeLimit,
                                onValueChange = { timeLimit = it.filter { ch -> ch.isDigit() }.take(5) },
                                label = { Text("Time limit (s)") },
                                placeholder = { Text("None") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(130.dp)
                            )
                        }
                        Text("Difficulty", fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(DIFF_EASY, DIFF_MEDIUM, DIFF_HARD).forEach { d ->
                                FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d) })
                            }
                        }
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Questions", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { questions = (questions + EQ()).toMutableList() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Add question")
                    }
                }
            }

            itemsIndexed(questions) { idx, eq ->
                QuestionEditor(
                    index = idx,
                    eq = eq,
                    onDelete = { questions = questions.filterIndexed { i, _ -> i != idx }.toMutableList() },
                    onMoveUp = { if (idx > 0) questions = questions.toMutableList().apply { val t = this[idx]; this[idx] = this[idx - 1]; this[idx - 1] = t } },
                    onMoveDown = { if (idx < questions.size - 1) questions = questions.toMutableList().apply { val t = this[idx]; this[idx] = this[idx + 1]; this[idx + 1] = t } },
                    onChange = { questions = questions.toMutableList() }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)) {
                    OutlinedButton(
                        onClick = { saveQuiz(vm, nav, isEdit, quizId, title, category, difficulty, tags, timeLimit, questions, STATUS_DRAFT) { error = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save Draft") }
                    Button(
                        onClick = { saveQuiz(vm, nav, isEdit, quizId, title, category, difficulty, tags, timeLimit, questions, STATUS_PUBLISHED) { error = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Publish") }
                }
            }
        }
    }

    if (showImportCode) {
        ImportCodeDialog(
            onDismiss = { showImportCode = false },
            onImported = { shared ->
                showImportCode = false
                title = shared.title
                category = shared.category
                difficulty = shared.difficulty
                tags = shared.tags
                timeLimit = shared.timeLimitSeconds?.toString() ?: ""
                questions = shared.questions.map { EQ.from(it) }.toMutableList()
                info = "Quiz loaded from code (${shared.questions.size} questions)"
            },
            onError = { error = it }
        )
    }
}

@Composable
private fun LaunchedEffectLoad(key1: String?, key2: String?, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key1, key2) { block() }
}

private fun saveQuiz(
    vm: AppViewModel,
    nav: NavHostController,
    isEdit: Boolean,
    quizId: String?,
    title: String,
    category: String,
    difficulty: String,
    tags: String,
    timeLimit: String,
    questions: List<EQ>,
    status: String,
    onError: (String) -> Unit
) {
    if (title.isBlank()) { onError("Quiz title is required"); return }
    if (questions.isEmpty()) { onError("Add at least one question"); return }
    val qs = mutableListOf<Question>()
    for ((i, eq) in questions.withIndex()) {
        if (eq.text.isBlank()) { onError("Question ${i + 1}: question text is empty"); return }
        if (eq.opts.any { it.isBlank() }) { onError("Question ${i + 1}: all options must be filled"); return }
        qs.add(eq.toQuestion("", i))
    }
    val time = timeLimit.toIntOrNull()?.takeIf { it > 0 }
    if (isEdit) {
        val existing = vm.getQuiz(quizId!!) ?: return
        vm.updateQuiz(
            existing.copy(
                title = title.trim(),
                category = category,
                difficulty = difficulty,
                tags = tags.trim(),
                timeLimitSeconds = time,
                status = status,
                updatedAt = System.currentTimeMillis()
            ),
            qs
        )
    } else {
        vm.createQuiz(title.trim(), category, difficulty, tags.trim(), time, status, qs)
    }
    nav.navigate(Routes.QUIZZES) {
        popUpTo(Routes.BUILDER) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun QuestionEditor(index: Int, eq: EQ, onDelete: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onChange: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Indigo.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Indigo)
                }
                Spacer(Modifier.width(8.dp))
                Text("Question", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(15.dp)) }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Down", modifier = Modifier.size(15.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Red, modifier = Modifier.size(16.dp)) }
            }
            OutlinedTextField(
                value = eq.text,
                onValueChange = { eq.text = it; onChange() },
                placeholder = { Text("Question text") },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                minLines = 2
            )
            // T/F toggle
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("True/False", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                FilterChip(selected = eq.isTF, onClick = {
                    eq.isTF = !eq.isTF
                    if (eq.isTF) {
                        eq.opts = mutableListOf("True", "False")
                        eq.correct = 0
                    } else {
                        eq.opts = mutableListOf("", "", "", "")
                        eq.correct = 0
                    }
                    onChange()
                }, label = { Text(if (eq.isTF) "Yes" else "No") })
            }
            val letters = listOf("a", "b", "c", "d")
            eq.opts.forEachIndexed { oi, opt ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    RadioButton(
                        selected = eq.correct == oi,
                        onClick = { eq.correct = oi; onChange() }
                    )
                    OutlinedTextField(
                        value = opt,
                        onValueChange = { eq.opts[oi] = it; onChange() },
                        placeholder = { Text("Option ${letters[oi]}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Correct: ${letters[eq.correct.coerceIn(0, eq.opts.lastIndex)]}  (select the right answer)",
                fontSize = 11.sp,
                color = Green,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ImportCodeDialog(onDismiss: () -> Unit, onImported: (com.quizforge.app.util.ShareCodec.SharedQuiz) -> Unit, onError: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import quiz from code") },
        text = {
            Column {
                Text("Paste a share code to fill this builder.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    onImported(com.quizforge.app.util.ShareCodec.decode(code))
                } catch (e: Exception) {
                    onError("Invalid code: ${e.message}")
                    onDismiss()
                }
            }) { Text("Load") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
