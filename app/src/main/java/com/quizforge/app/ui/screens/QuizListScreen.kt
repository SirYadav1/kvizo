package com.quizforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizforge.app.Routes
import com.quizforge.app.data.Quiz
import com.quizforge.app.data.STATUS_ARCHIVED
import com.quizforge.app.data.STATUS_DRAFT
import com.quizforge.app.data.STATUS_PUBLISHED
import com.quizforge.app.ui.AppViewModel
import com.quizforge.app.ui.components.DifficultyBadge
import com.quizforge.app.ui.components.StatusChip
import com.quizforge.app.ui.theme.Amber
import com.quizforge.app.ui.theme.Green
import com.quizforge.app.ui.theme.Indigo
import com.quizforge.app.ui.theme.Red

@Composable
fun QuizListScreen(vm: AppViewModel, nav: NavHostController) {
    val profile = vm.profile
    var allQuizzes by remember { mutableStateOf(listOf<Quiz>()) }
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf("Date Modified") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Quiz?>(null) }

    LaunchedEffect(profile?.id) {
        profile?.let { allQuizzes = vm.repo.getQuizzes(it.id) }
    }
    var questionCounts by remember { mutableStateOf(mapOf<String, Int>()) }
    LaunchedEffect(allQuizzes) {
        questionCounts = allQuizzes.associate { it.id to vm.repo.getQuestions(it.id).size }
    }

    val categories = allQuizzes.map { it.category }.distinct().sorted()
    var visible = allQuizzes
    if (categoryFilter != null) visible = visible.filter { it.category == categoryFilter }
    if (statusFilter != null) visible = visible.filter { it.status == statusFilter }
    if (query.isNotBlank()) {
        val q = query.lowercase()
        visible = visible.filter {
            it.title.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tags.lowercase().contains(q)
        }
    }
    visible = when (sortMode) {
        "Date Created" -> visible.sortedByDescending { it.createdAt }
        "Attempts" -> visible.sortedByDescending { it.attemptsCount }
        "Avg Score" -> visible.sortedByDescending { it.averageScore }
        "Alphabetical" -> visible.sortedBy { it.title.lowercase() }
        else -> visible.sortedByDescending { it.updatedAt }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("My Quizzes", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by title, category or tag") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // status tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState())) {
            FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All") })
            FilterChip(selected = statusFilter == STATUS_PUBLISHED, onClick = { statusFilter = STATUS_PUBLISHED }, label = { Text("Published") })
            FilterChip(selected = statusFilter == STATUS_DRAFT, onClick = { statusFilter = STATUS_DRAFT }, label = { Text("Draft") })
            FilterChip(selected = statusFilter == STATUS_ARCHIVED, onClick = { statusFilter = STATUS_ARCHIVED }, label = { Text("Archived") })
        }

        // category filter + sort
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                var showCatMenu by remember { mutableStateOf(false) }
                FilterChip(
                    selected = categoryFilter != null,
                    onClick = { showCatMenu = true },
                    label = { Text(categoryFilter ?: "Category") }
                )
                DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                    DropdownMenuItem(text = { Text("All categories") }, onClick = { categoryFilter = null; showCatMenu = false })
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { categoryFilter = c; showCatMenu = false })
                    }
                }
            }
            var showSortMenu by remember { mutableStateOf(false) }
            TextButton(onClick = { showSortMenu = true }) {
                Text("Sort: $sortMode", fontSize = 12.sp, color = Indigo)
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                listOf("Date Modified", "Date Created", "Attempts", "Avg Score", "Alphabetical").forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { sortMode = s; showSortMenu = false })
                }
            }
            IconButton(onClick = { showImportDialog = true }) {
                Icon(Icons.Filled.FileUpload, contentDescription = "Import quiz code", tint = Indigo)
            }
        }

        Spacer(Modifier.height(6.dp))

        if (visible.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No quizzes found", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Tap + to create, or upload a .txt file", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { quiz ->
                    QuizCard(vm, quiz, questionCounts[quiz.id] ?: 0, nav, onDelete = { deleteTarget = quiz })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete quiz?") },
            text = { Text("\"${deleteTarget!!.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteQuiz(deleteTarget!!.id)
                    allQuizzes = allQuizzes.filter { it.id != deleteTarget!!.id }
                    deleteTarget = null
                }) { Text("Delete", color = Red) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }

    if (showImportDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import quiz") },
            text = {
                Column {
                    Text("Paste the share code received from another user.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("Paste code here") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        minLines = 3
                    )
                    importError?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val shared = com.quizforge.app.util.ShareCodec.decode(code)
                        if (shared.questions.isEmpty()) { importError = "Code has no questions"; return@TextButton }
                        vm.importSharedQuiz(shared)
                        allQuizzes = vm.repo.getQuizzes(profile!!.id)
                        showImportDialog = false
                        importError = null
                    } catch (e: Exception) {
                        importError = "Invalid code: ${e.message}"
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuizCard(vm: AppViewModel, quiz: Quiz, questionCount: Int, nav: NavHostController, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.attempt(quiz.id)) }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(quiz.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.size(6.dp))
                    StatusChip(quiz.status)
                }
                Text("${quiz.category} • $questionCount questions • ${quiz.attemptsCount} attempts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                if (quiz.tags.isNotBlank()) {
                    Text("#${quiz.tags.replace(",", " #")}", fontSize = 10.sp, color = Indigo, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    DifficultyBadge(quiz.difficulty)
                    Text("  ${quiz.averageScore.toInt()}% avg", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (quiz.averageScore >= 60) Green else Amber)
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Take quiz") }, onClick = { menuOpen = false; nav.navigate(Routes.attempt(quiz.id)) })
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; nav.navigate("builder?quizId=${quiz.id}") })
                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuOpen = false; vm.duplicateQuiz(quiz.id) })
                DropdownMenuItem(text = { Text("Share code") }, onClick = {
                    menuOpen = false
                    val code = com.quizforge.app.util.ShareCodec.encode(
                        com.quizforge.app.util.ShareCodec.SharedQuiz(
                            quiz.title, quiz.category, quiz.difficulty, quiz.tags, quiz.timeLimitSeconds,
                            vm.repo.getQuestions(quiz.id)
                        )
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "QuizForge quiz: ${quiz.title}\nShare code:\n$code")
                    }
                    androidx.core.content.ContextCompat.startActivity(
                        vm.getApplication<android.app.Application>(), android.content.Intent.createChooser(intent, "Share quiz"), null
                    )
                })
                DropdownMenuItem(text = { Text(if (quiz.status == STATUS_ARCHIVED) "Unarchive" else "Archive") }, onClick = {
                    menuOpen = false
                    vm.repo.updateQuizMeta(
                        quiz.copy(status = if (quiz.status == STATUS_ARCHIVED) STATUS_PUBLISHED else STATUS_ARCHIVED, updatedAt = System.currentTimeMillis())
                    )
                    vm.refreshQuizzes()
                })
                DropdownMenuItem(text = { Text("Delete", color = Red) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}
