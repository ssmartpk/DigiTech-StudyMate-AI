package com.example.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.QuizEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.model.QuizQuestion
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudyMateTopBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "AI Quizzes",
                subtitle = "${quizzes.size} quizzes created",
                isDemoMode = userProfile.isDemoMode,
                showBackButton = true,
                onBackClick = { onNavigate(NavigationDestination.Home) },
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGenerateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Generate AI Quiz") },
                modifier = Modifier.testTag("quiz_generate_fab")
            )
        }
    ) { innerPadding ->
        if (quizzes.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Quiz,
                title = "No quizzes yet",
                description = "Generate an interactive quiz on any topic, textbook chapter, or revision syllabus.",
                actionLabel = "Generate AI Quiz",
                onActionClick = { showGenerateDialog = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizzes) { quiz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (quiz.isCompleted) viewModel.openQuizResult(quiz.id)
                                else viewModel.startQuiz(quiz.id)
                            }
                            .testTag("quiz_card_${quiz.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quiz.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${quiz.subjectName ?: "General"} • ${quiz.totalQuestions} Questions • ${quiz.difficulty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (quiz.isCompleted) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Completed: ${quiz.scorePercentage}% (${quiz.correctAnswers}/${quiz.totalQuestions} correct)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            if (quiz.isCompleted) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.openQuizResult(quiz.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Review", fontSize = 12.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.startQuiz(quiz.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("quiz_card_start_btn")
                                    ) {
                                        Text("Retake", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { viewModel.startQuiz(quiz.id) },
                                    modifier = Modifier.testTag("quiz_card_start_btn")
                                ) {
                                    Text("Start")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        GenerateQuizDialog(
            subjects = subjects,
            onDismiss = { showGenerateDialog = false },
            onGenerate = { subj, topic, count, difficulty, type ->
                viewModel.generateNewQuiz(subj, topic, count, difficulty, type)
                showGenerateDialog = false
            }
        )
    }
}

@Composable
fun GenerateQuizDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onGenerate: (SubjectEntity?, String, Int, String, String) -> Unit
) {
    var topic by remember { mutableStateOf("Cellular Respiration & Krebs Cycle") }
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var questionCount by remember { mutableStateOf(5) }
    var difficulty by remember { mutableStateOf("Medium") }
    var questionType by remember { mutableStateOf("MCQ") }

    val difficulties = listOf("Easy", "Medium", "Hard")
    val counts = listOf(4, 5, 10, 15)
    val types = listOf("MCQ", "True/False", "Short Answer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate AI Quiz", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic or Study Subject") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_topic_input")
                )

                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subjects.take(3).forEach { subj ->
                            FilterChip(
                                selected = selectedSubject?.id == subj.id,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text("Question Count", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    counts.forEach { c ->
                        FilterChip(
                            selected = questionCount == c,
                            onClick = { questionCount = c },
                            label = { Text("$c Questions") }
                        )
                    }
                }

                Text("Difficulty", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    difficulties.forEach { d ->
                        FilterChip(
                            selected = difficulty == d,
                            onClick = { difficulty = d },
                            label = { Text(d) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topic.isNotBlank()) {
                        onGenerate(selectedSubject, topic, questionCount, difficulty, questionType)
                    }
                },
                enabled = topic.isNotBlank(),
                modifier = Modifier.testTag("quiz_dialog_generate_btn")
            ) {
                Text("Generate Quiz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizActiveScreen(
    quizId: String,
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val quiz by viewModel.activeQuiz.collectAsState()
    val questions by viewModel.activeQuizQuestions.collectAsState()
    val questionIndex by viewModel.activeQuizQuestionIndex.collectAsState()
    val elapsedSeconds by viewModel.quizElapsedTimeSeconds.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }

    val currentQuiz = quiz
    if (currentQuiz == null || questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentQ = questions.getOrNull(currentIndex) ?: questions.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentQuiz.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "Question ${currentIndex + 1} of ${questions.size} • Time: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(NavigationDestination.QuizList) }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Progress Indicator
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentQ.questionText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            lineHeight = 24.sp
                        )
                    }
                }

                // Options List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    currentQ.options.forEachIndexed { optionIndex, optionText ->
                        val isSelected = currentQ.userSelectedIndex == optionIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.selectQuizAnswer(currentIndex, optionIndex)
                                }
                                .testTag("quiz_option_${currentIndex}_$optionIndex"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectQuizAnswer(currentIndex, optionIndex) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons (Prev, Next, Submit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(onClick = { currentIndex -= 1 }) {
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentIndex < questions.size - 1) {
                    Button(
                        onClick = { currentIndex += 1 },
                        modifier = Modifier.testTag("quiz_next_question_button")
                    ) {
                        Text("Next Question")
                    }
                } else {
                    Button(
                        onClick = { viewModel.submitQuiz() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("quiz_submit_button")
                    ) {
                        Text("Submit Quiz")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultsScreen(
    quizId: String,
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val quiz by viewModel.activeQuiz.collectAsState()
    val questions by viewModel.activeQuizQuestions.collectAsState()

    val currentQuiz = quiz
    if (currentQuiz == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(NavigationDestination.QuizList) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Score Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentQuiz.scorePercentage >= 70) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${currentQuiz.scorePercentage}%",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = if (currentQuiz.scorePercentage >= 70) "Great Job! Topic Mastered!" else "Needs Revision",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentQuiz.correctAnswers} of ${currentQuiz.totalQuestions} questions answered correctly in ${currentQuiz.timeTakenSeconds}s",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text("Detailed Question Review & Explanations", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            // Questions Explanations List
            questions.forEachIndexed { index, q ->
                val isCorrect = q.isCorrect()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Question ${index + 1}: ${if (isCorrect) "Correct" else "Incorrect"}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) SuccessGreen else ErrorRed
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = q.questionText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Correct Answer: ${q.correctAnswerText}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explanation: ${q.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Return Button
            Button(
                onClick = { onNavigate(NavigationDestination.QuizList) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Return to Quizzes")
            }
        }
    }
}
