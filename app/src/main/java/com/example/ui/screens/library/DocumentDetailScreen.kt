package com.example.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar

@Composable
fun DocumentDetailScreen(
    documentId: String,
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val document by viewModel.selectedDocument.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val doc = document
    if (doc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = doc.title,
                subtitle = doc.subjectName ?: "Study Note",
                isDemoMode = userProfile.isDemoMode,
                showBackButton = true,
                onBackClick = { onNavigate(NavigationDestination.Library) }
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
            // Action Buttons Row (Grid of AI tools)
            Text(
                text = "AI Study Tools for this Document",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.generateSummary(doc.title, doc.extractedText, "Detailed Summary", "Medium")
                        onNavigate(NavigationDestination.Summarizer)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("doc_action_summarize")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Summarize")
                }

                Button(
                    onClick = {
                        viewModel.startNewConversation("Study: ${doc.title}")
                        viewModel.sendTutorMessage("Please explain the key concepts in this study material simply.", doc.extractedText)
                        onNavigate(NavigationDestination.Tutor)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("doc_action_explain")
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Explain")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.generateNewQuiz(
                            subject = null,
                            topic = doc.title,
                            count = 4,
                            difficulty = "Medium",
                            type = "MCQ"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("doc_action_quiz")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Quiz")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.generateNewFlashcardDeck(
                            title = "${doc.title} Cards",
                            subject = null,
                            topic = doc.title,
                            count = 5
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("doc_action_flashcards")
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Make Flashcards")
                }
            }

            // Summary Section if present
            if (!doc.summaryText.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saved Revision Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = doc.summaryText ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Document Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Extracted Document Content",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = doc.extractedText.ifBlank { "No text content available." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Delete Action
            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Document from Library")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete '${doc.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteDocument(doc.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
