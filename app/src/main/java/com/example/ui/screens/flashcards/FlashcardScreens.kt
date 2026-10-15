package com.example.ui.screens.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FlashcardDeckEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.model.FlashcardGrade
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudyMateTopBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.StreakGold
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDeckListScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "Study Flashcards",
                subtitle = "Spaced repetition active recall",
                isDemoMode = userProfile.isDemoMode,
                showBackButton = true,
                onBackClick = { onNavigate(NavigationDestination.Home) },
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Generate Flashcards") },
                modifier = Modifier.testTag("flashcard_generate_fab")
            )
        }
    ) { innerPadding ->
        if (decks.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Style,
                title = "No Flashcard Decks",
                description = "Generate flashcards from any topic, formula sheet, or lecture notes.",
                actionLabel = "Create Deck",
                onActionClick = { showCreateDialog = true },
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
                items(decks) { deck ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startFlashcardStudy(deck.id) }
                            .testTag("deck_card_${deck.id}"),
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
                                    text = deck.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${deck.subjectName ?: "General"} • ${deck.cardCount} cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!deck.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = deck.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.startFlashcardStudy(deck.id) },
                                modifier = Modifier.testTag("deck_study_button")
                            ) {
                                Text("Study")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        GenerateFlashcardsDialog(
            subjects = subjects,
            onDismiss = { showCreateDialog = false },
            onGenerate = { title, subj, topic, count ->
                viewModel.generateNewFlashcardDeck(title, subj, topic, count)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun GenerateFlashcardsDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onGenerate: (title: String, subject: SubjectEntity?, topic: String, count: Int) -> Unit
) {
    var title by remember { mutableStateOf("Organic Chemistry Reactions") }
    var topic by remember { mutableStateOf("Functional groups, esterification, and nucleophilic substitution") }
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var count by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate AI Flashcards", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic or Study Content") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.take(3).forEach { subj ->
                            FilterChip(
                                selected = selectedSubject?.id == subj.id,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text("Card Count: $count", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 5, 8, 10).forEach { c ->
                        FilterChip(selected = count == c, onClick = { count = c }, label = { Text("$c Cards") })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && topic.isNotBlank()) {
                        onGenerate(title, selectedSubject, topic, count)
                    }
                },
                enabled = title.isNotBlank() && topic.isNotBlank(),
                modifier = Modifier.testTag("flashcard_dialog_generate_btn")
            ) {
                Text("Generate Deck")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    deckId: String,
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val deck by viewModel.activeDeck.collectAsState()
    val cards by viewModel.activeDeckCards.collectAsState()
    val currentIndex by viewModel.activeCardIndex.collectAsState()
    val isFlipped by viewModel.isCardFlipped.collectAsState()

    val currentDeck = deck
    if (currentDeck == null || cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentCard = cards.getOrNull(currentIndex) ?: cards.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentDeck.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Card ${currentIndex + 1} of ${cards.size}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(NavigationDestination.FlashcardsList) }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Study")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.shuffleCards() },
                        modifier = Modifier.testTag("flashcard_shuffle_btn")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Deck")
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / cards.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            // Flashcard Surface (Tap to Flip)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp)
                    .clickable { viewModel.flipCard() }
                    .testTag("flashcard_flip_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Badge(
                            containerColor = if (isFlipped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (isFlipped) "ANSWER / EXPLANATION" else "QUESTION / TERM",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isFlipped) currentCard.back else currentCard.front,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            ),
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isFlipped) "Tap to see front" else "Tap card to flip answer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Spaced Repetition Grading Actions (Again, Hard, Good, Easy)
            if (isFlipped) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How well did you recall this?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.gradeActiveCard(FlashcardGrade.AGAIN) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_grade_again")
                        ) {
                            Text("Again (1d)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.gradeActiveCard(FlashcardGrade.HARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_grade_hard")
                        ) {
                            Text("Hard (2d)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.gradeActiveCard(FlashcardGrade.GOOD) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_grade_good")
                        ) {
                            Text("Good (3d)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.gradeActiveCard(FlashcardGrade.EASY) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_grade_easy")
                        ) {
                            Text("Easy (5d)", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { viewModel.previousCard() },
                            modifier = Modifier
                                .weight(0.4f)
                                .height(48.dp)
                                .testTag("flashcard_prev_btn")
                        ) {
                            Text("Previous")
                        }
                    }
                    Button(
                        onClick = { viewModel.flipCard() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("flashcard_reveal_button")
                    ) {
                        Text("Show Answer")
                    }
                }
            }
        }
    }
}
