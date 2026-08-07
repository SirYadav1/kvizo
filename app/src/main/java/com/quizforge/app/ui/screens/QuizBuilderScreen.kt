package com.quizforge.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.quizforge.app.parsing.QuizImporter
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.theme.Amber
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
    var tags by remember { mutableStateOf(mutableStateListOf<String>()) }
    var newTag by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timeLimitSec by remember { mutableStateOf(30) }
    var questions by remember { mutableStateOf(mutableListOf<EQ>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var showImportCode by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    if (isEdit) {
        val existing = remember(quizId) { vm.getQuiz(quizId) }
        LaunchedEffectLoad(quizId, existing?.title) {
            val q = existing ?: return@LaunchedEffectLoad
            title = q.title
            category = q.category
            difficulty = q.difficulty
            tags = q.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList()
            description = q.description
            timeLimitSec = q.timeLimitSeconds ?: 30
            questions = vm.getQuestions(quizId).map { EQ.from(it) }.toMutableList()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val resolver = vm.getApplication<android.app.Application>().contentResolver
            var name: String? = null
            try {
                resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) name = c.getString(0)
                }
            } catch (_: Exception) {
            }
            val text = resolver.openInputStream(uri)?.bufferedReader()?.use { br -> br.readText() }
            if (text.isNullOrBlank()) { error = "File is empty"; return@rememberLauncherForActivityResult }
            val parsed = QuizImporter.detectAndParse(name, text, "temp")
            questions = (questions + parsed.map { EQ.from(it) }).toMutableList()
            val ext = name?.substringAfterLast('.', "").orEmpty().uppercase().ifBlank { "TXT" }
            info = "Imported ${parsed.size} questions ($ext)"
            error = null
        } catch (e: ParseException) {
            error = e.message
            info = null
        } catch (e: Exception) {
            error = "Could not read file: ${e.message}"
        }
    }

    val addTag: (String) -> Unit = { t ->
        val v = t.trim().trimEnd { it == ',' }
        if (v.isNotEmpty() && tags.none { it.equals(v, ignoreCase = true) }) tags.add(v)
        newTag = ""
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isEdit) "Edit Quiz" else "Create Quiz", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("(${questions.size} questions)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { showPreview = true },
                enabled = title.isNotBlank() || questions.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Visibility, contentDescription = "Preview", tint = Indigo)
            }
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("text/plain", "text/csv", "application/json", "*/*")) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                Text("  TXT / JSON", fontSize = 12.sp)
            }
        }

        error?.let {
            Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        info?.let {
            Text(it, color = Green, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TitleCard(title, { title = it }) }

            item {
                DetailsCard(
                    category = category,
                    onCategoryChange = { category = it },
                    timeLimitSec = timeLimitSec,
                    onTimeLimitChange = { timeLimitSec = it }
                )
            }

            item { DifficultyCard(difficulty, { difficulty = it }) }

            item {
                TagsCard(
                    tags = tags,
                    newTag = newTag,
                    onNewTagChange = { newTag = it },
                    onAddTag = { addTag(newTag) },
                    onRemoveTag = { tags.remove(it) }
                )
            }

            item { DescriptionCard(description, { description = it }) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { saveQuiz(vm, nav, isEdit, quizId, title, category, difficulty, tags.joinToString(", "), description, timeLimitSec, questions, STATUS_DRAFT) { error = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.quizforge.app.ui.theme.Charcoal),
                        border = BorderStroke(1.5.dp, com.quizforge.app.ui.theme.Charcoal.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("  Save Draft", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { saveQuiz(vm, nav, isEdit, quizId, title, category, difficulty, tags.joinToString(", "), description, timeLimitSec, questions, STATUS_PUBLISHED) { error = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.quizforge.app.ui.theme.Charcoal)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("  Publish Quiz", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Questions (${questions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { questions = (questions + EQ()).toMutableList() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Add Question")
                    }
                }
            }

            if (questions.isEmpty()) {
                item { EmptyStateCard(onAdd = { questions = (questions + EQ()).toMutableList() }) }
            }

            itemsIndexed(questions, key = { _, eq -> System.identityHashCode(eq) }) { idx, eq ->
                QuestionEditor(
                    index = idx,
                    eq = eq,
                    onDelete = { questions = questions.filterIndexed { i, _ -> i != idx }.toMutableList() },
                    onMoveUp = { if (idx > 0) questions = questions.toMutableList().apply { val t = this[idx]; this[idx] = this[idx - 1]; this[idx - 1] = t } },
                    onMoveDown = { if (idx < questions.size - 1) questions = questions.toMutableList().apply { val t = this[idx]; this[idx] = this[idx + 1]; this[idx + 1] = t } },
                    onChange = { questions = questions.toMutableList() }
                )
            }
        }
    }

    if (showPreview) {
        PreviewDialog(
            title = title,
            category = category,
            difficulty = difficulty,
            tags = tags,
            description = description,
            timeLimitSec = timeLimitSec,
            questions = questions,
            onDismiss = { showPreview = false }
        )
    }

    if (showImportCode) {
        ImportCodeDialog(
            onDismiss = { showImportCode = false },
            onImported = { shared ->
                showImportCode = false
                title = shared.title
                category = shared.category
                difficulty = shared.difficulty
                tags = shared.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList()
                description = shared.description
                timeLimitSec = shared.timeLimitSeconds ?: 30
                questions = shared.questions.map { EQ.from(it) }.toMutableList()
                info = "Quiz loaded from code (${shared.questions.size} questions)"
            },
            onError = { error = it }
        )
    }
}

@Composable
private fun CardFrame(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun TitleCard(title: String, onTitleChange: (String) -> Unit) {
    CardFrame {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Quiz title *") },
            placeholder = { Text("e.g. General Knowledge Master") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailsCard(category: String, onCategoryChange: (String) -> Unit, timeLimitSec: Int, onTimeLimitChange: (Int) -> Unit) {
    CardFrame {
        CardLabel("Details")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box {
                var catMenu by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { catMenu = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(category, maxLines = 1, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                    PRESET_CATEGORIES.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { onCategoryChange(c); catMenu = false })
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CardLabel("Time limit", modifier = Modifier.weight(1f))
            Text(
                if (timeLimitSec <= 0) "None" else "[ $timeLimitSec sec ]",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Indigo
            )
        }
        Slider(
            value = timeLimitSec.toFloat(),
            onValueChange = { onTimeLimitChange(it.toInt()) },
            valueRange = 0f..120f
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("None", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("120 sec", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun difficultyColor(d: String): Color = when (d) {
    DIFF_EASY -> Green
    DIFF_HARD -> Red
    else -> Amber
}

@Composable
private fun DifficultyCard(current: String, onSelect: (String) -> Unit) {
    CardFrame {
        CardLabel("Difficulty")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(DIFF_EASY, DIFF_MEDIUM, DIFF_HARD).forEach { d ->
                val c = difficultyColor(d)
                val selected = current == d
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) c.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selected) c.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f).clickable { onSelect(d) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(d, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) c else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsCard(
    tags: List<String>,
    newTag: String,
    onNewTagChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    CardFrame {
        CardLabel("Tags (optional)")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { t ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Indigo.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Indigo.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t, fontSize = 12.sp, color = Indigo)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove $t",
                            tint = Indigo,
                            modifier = Modifier.size(16.dp).clip(CircleShape).clickable { onRemoveTag(t) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = newTag,
            onValueChange = onNewTagChange,
            label = { Text("Add tag") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddTag() }),
            trailingIcon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add tag",
                    tint = Indigo,
                    modifier = Modifier.clip(CircleShape).clickable { onAddTag() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DescriptionCard(description: String, onChange: (String) -> Unit) {
    CardFrame {
        CardLabel("Description")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onChange,
            placeholder = { Text("Tell players what this quiz is about, how hard it is, what they'll be tested on...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyStateCard(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Indigo.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, tint = Indigo, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("No questions yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add your first question to start building this quiz",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.quizforge.app.ui.theme.Charcoal)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Text("  Add First Question", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PreviewDialog(
    title: String,
    category: String,
    difficulty: String,
    tags: List<String>,
    description: String,
    timeLimitSec: Int,
    questions: List<EQ>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(if (title.isBlank()) "Untitled quiz" else title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "$category  •  $difficulty" + if (timeLimitSec > 0) "  •  $timeLimitSec sec" else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tags.isNotEmpty()) {
                        Text(tags.joinToString(" · "), fontSize = 12.sp, color = difficultyColor(difficulty))
                    }
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(description, fontSize = 13.sp)
                    }
                }
                if (questions.isEmpty()) {
                    item { Text("No questions yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                itemsIndexed(questions) { idx, q ->
                    Column {
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Q${idx + 1}. ${if (q.text.isBlank()) "(empty question)" else q.text}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        val letters = listOf("a", "b", "c", "d")
                        q.opts.forEachIndexed { oi, opt ->
                            Text(
                                "${letters[oi]}. ${if (opt.isBlank()) "(empty)" else opt}",
                                fontSize = 12.sp,
                                color = if (oi == q.correct) Green else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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
    description: String,
    timeLimitSec: Int,
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
    val time = timeLimitSec.takeIf { it > 0 }
    if (isEdit) {
        val existing = vm.getQuiz(quizId!!) ?: return
        vm.updateQuiz(
            existing.copy(
                title = title.trim(),
                category = category,
                difficulty = difficulty,
                tags = tags.trim(),
                description = description.trim(),
                timeLimitSeconds = time,
                status = status,
                updatedAt = System.currentTimeMillis()
            ),
            qs
        )
    } else {
        vm.createQuiz(title.trim(), category, difficulty, tags.trim(), time, status, qs, description.trim())
    }
    nav.navigate(Routes.QUIZZES) {
        popUpTo(Routes.BUILDER) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun QuestionEditor(index: Int, eq: EQ, onDelete: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onChange: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
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