package com.kvizo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kvizo.app.R
import com.kvizo.app.data.CommunityQuiz
import com.kvizo.app.ui.AppViewModel
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
    var successMsg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // observe import result for the success toast
    LaunchedEffect(Unit) {
        isLoading = true
        val result = vm.fetchCommunityQuizzes()
        result.onSuccess { quizzes = it }
        result.onFailure { error = it.message }
        isLoading = false
        vm.communityImportResult?.let { successMsg = it }
    }
    LaunchedEffect(vm.communityImportResult) {
        vm.communityImportResult?.let { successMsg = it }
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
                    // Animated loading state — pulsing logo + progress
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val infinite = rememberInfiniteTransition(label = "pulse")
                            val scale by infinite.animateFloat(
                                initialValue = 0.85f, targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "scale"
                            )
                            val alpha by infinite.animateFloat(
                                initialValue = 0.5f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600), repeatMode = RepeatMode.Reverse
                                ), label = "alpha"
                            )
                            Icon(
                                Icons.Filled.CloudDownload, contentDescription = null,
                                tint = Indigo.copy(alpha = alpha),
                                modifier = Modifier.size(56.dp).graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Loading community quizzes…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(color = Indigo, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading quizzes", color = Red, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            val scope = rememberCoroutineScope()
                            Button(onClick = {
                                isLoading = true
                                error = null
                                scope.launch {
                                    val result = vm.fetchCommunityQuizzes()
                                    result.onSuccess { quizzes = it }
                                    result.onFailure { error = it.message }
                                    isLoading = false
                                }
                            }) { Text("Retry") }
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No quizzes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered) { quiz ->
                                CommunityQuizCard(quiz = quiz, onImport = {
                                    vm.importCommunityQuiz(quiz)
                                })
                            }
                        }
                        // Success toast — animated slide-up banner
                        successMsg?.let { msg ->
                            val slide = remember { Animatable(1f) }
                            LaunchedEffect(msg) {
                                slide.animateTo(0f, tween(300))
                                kotlinx.coroutines.delay(2500)
                                slide.animateTo(-1.5f, tween(300))
                                successMsg = null
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Green,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                                    .graphicsLayer { translationY = (slide.value * 120).dp.toPx() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(msg, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityQuizCard(quiz: CommunityQuiz, onImport: () -> Unit) {
    val diffColor = when (quiz.difficulty.lowercase()) {
        "easy" -> Green
        "medium" -> Orange
        "hard" -> Red
        else -> Indigo
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quiz.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = diffColor.copy(alpha = 0.15f)) {
                    Text(quiz.difficulty, color = diffColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("${quiz.questions.size} questions  •  ${quiz.category}  •  by ${quiz.author}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import Quiz")
            }
        }
    }
}
