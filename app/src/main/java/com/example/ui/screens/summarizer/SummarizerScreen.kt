package com.example.ui.screens.summarizer

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar
import com.example.ui.screens.tutor.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizerScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val userProfile by viewModel.userProfile.collectAsState()
    val summaryResult by viewModel.summaryResult.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()

    var docTitle by remember { mutableStateOf("Physics Mechanics Notes") }
    var inputContent by remember {
        mutableStateOf(
            "Kinematics equations describe the motion of objects without considering the forces causing the motion. " +
            "The 4 fundamental kinematic equations require constant acceleration: v = u + at, s = ut + 0.5at^2, v^2 = u^2 + 2as, s = 0.5(u+v)t. " +
            "Newton's laws of motion form the basis of classical mechanics. First law states inertia: bodies stay at rest or constant velocity unless acted on by external net force. " +
            "Second law states F = ma. Third law states action and reaction forces are equal and opposite."
        )
    }

    var selectedFormat by remember { mutableStateOf("Exam Revision Notes") }
    var selectedLength by remember { mutableStateOf("Medium") }

    val formats = listOf(
        "Quick Summary",
        "Detailed Summary",
        "Exam Revision Notes",
        "Key Points",
        "Important Definitions",
        "Formula Sheet",
        "Chapter Outline"
    )
    val lengths = listOf("Short", "Medium", "Comprehensive")

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "AI Summarizer",
                subtitle = "Turn dense notes into crystal-clear summaries",
                isDemoMode = userProfile.isDemoMode,
                showBackButton = true,
                onBackClick = { onNavigate(NavigationDestination.Home) }
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
            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Topic or Document Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summarizer_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputContent,
                        onValueChange = { inputContent = it },
                        label = { Text("Study Text to Summarize") },
                        placeholder = { Text("Paste lecture notes, textbook chapters, or definitions...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("summarizer_content_input"),
                        maxLines = 6
                    )

                    Text("Summary Format", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        formats.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { format ->
                                    FilterChip(
                                        selected = selectedFormat == format,
                                        onClick = { selectedFormat = format },
                                        label = { Text(format, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Text("Length", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lengths.forEach { len ->
                            FilterChip(
                                selected = selectedLength == len,
                                onClick = { selectedLength = len },
                                label = { Text(len) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.generateSummary(docTitle, inputContent, selectedFormat, selectedLength)
                        },
                        enabled = inputContent.isNotBlank() && !isSummarizing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("summarizer_generate_button")
                    ) {
                        if (isSummarizing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Summary...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate $selectedFormat")
                        }
                    }
                }
            }

            // Summary Result Card
            if (summaryResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Summary",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(summaryResult ?: ""))
                                        viewModel.showMessage("Summary copied to clipboard.")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(
                                    onClick = {
                                        shareText(context, summaryResult ?: "")
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.addNoteDocument(
                                            title = "$docTitle Summary",
                                            subject = null,
                                            content = summaryResult ?: ""
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to Library")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = summaryResult ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions from summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.generateNewQuiz(
                                        subject = null,
                                        topic = docTitle,
                                        count = 4,
                                        difficulty = "Medium",
                                        type = "MCQ"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Quiz from this", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.generateNewFlashcardDeck(
                                        title = "$docTitle Flashcards",
                                        subject = null,
                                        topic = summaryResult ?: docTitle,
                                        count = 4
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Flashcards from this", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
