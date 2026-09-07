package com.kvizo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.R
import com.kvizo.app.data.CommunityQuiz
import com.kvizo.app.ui.AppViewModel
import com.kvizo.app.ui.components.StatusPill
import com.kvizo.app.ui.theme.Green
import com.kvizo.app.ui.theme.Indigo
import com.kvizo.app.ui.theme.Orange
import com.kvizo.app.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityQuizScreen(
    vm: AppViewModel,
    nav: NavHostController
) {
    var quizzes by remember { mutableStateOf<List<CommunityQuiz>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        val result = vm.fetchCommunityQuizzes()
        result.onSuccess { quizzes = it }
        result.onFailure { error = it.message ?: "Failed to load" }
        isLoading = false
    }

    val categories = quizzes.map { it.category }.distinct()
    val filtered = quizzes.filter { quiz ->
        (searchQuery.isEmpty() || quiz.title.contains(searchQuery, ignoreCase = true)) &&
        (selectedCategory == null || quiz.category == selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Quizzes") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search quizzes...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Category chips
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        androidx.compose.foundation.layout.Spacer(Modifier.height(120.dp))
                        androidx.compose.material3.CircularProgressIndicator(color = Indigo)
                        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                        Text("Loading community quizzes\u2026", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Couldn't load community quizzes", color = Red, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    val result = vm.fetchCommunityQuizzes()
                                    result.onSuccess { quizzes = it }
                                    result.onFailure { error = it.message ?: "Failed to load" }
                                    isLoading = false
                                }
                            }) { Text("Retry", color = Indigo) }
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No community quizzes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { quiz ->
                            var msg by remember(quiz.id) { mutableStateOf<String?>(null) }
                            val imported = remember(quiz.id) { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            quiz.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        StatusPill(quiz.category, Green, com.kvizo.app.ui.theme.greenBg())
                                    }
                                    Text(
                                        "${quiz.difficulty} • ${quiz.questions.size} questions • by ${quiz.author}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    if (msg != null) {
                                        Text(msg!!, fontSize = 11.sp, color = if (msg!!.startsWith("Imported")) Green else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                    }
                                    androidx.compose.material3.TextButton(
                                        enabled = !imported.value,
                                        onClick = {
                                            imported.value = true
                                            vm.importCommunityQuiz(quiz) { m -> msg = m; if (!m.startsWith("Imported")) imported.value = false }
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = Indigo, modifier = Modifier.size(15.dp))
                                        Text(if (imported.value) "Imported" else "  Import", fontSize = 12.sp, color = Indigo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
