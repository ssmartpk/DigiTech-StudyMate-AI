package com.example.ui.screens.tutor

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.model.TutorSettings
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val userProfile by viewModel.userProfile.collectAsState()
    val tutorSettings by viewModel.tutorSettings.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val messages by viewModel.currentChatMessages.collectAsState()
    val isThinking by viewModel.isTutorThinking.collectAsState()
    val tutorError by viewModel.tutorError.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showConversationsMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Ensure there is an active conversation
    LaunchedEffect(conversations, activeConversationId) {
        if (activeConversationId == null && conversations.isNotEmpty()) {
            viewModel.selectConversation(conversations.first().id)
        } else if (activeConversationId == null && conversations.isEmpty()) {
            viewModel.startNewConversation("Study Session")
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedPrompts = remember {
        listOf(
            "Explain photosynthesis in simple words.",
            "Help me understand quadratic equations.",
            "Explain this topic with an example.",
            "Test my knowledge of biology.",
            "Teach me this concept step by step."
        )
    }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "AI Tutor",
                subtitle = "DigiTech StudyMate AI",
                isDemoMode = userProfile.isDemoMode,
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("tutor_screen_root")
        ) {
            // Header / Conversation Switcher Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showConversationsMenu = true }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = conversations.firstOrNull { it.id == activeConversationId }?.title ?: "Current Session",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.startNewConversation("Session ${conversations.size + 1}") },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tutor_new_conversation_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Conversation", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tutor_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Tutor Settings")
                    }
                }
            }

            // Dropdown Menu for conversations
            DropdownMenu(
                expanded = showConversationsMenu,
                onDismissRequest = { showConversationsMenu = false }
            ) {
                conversations.forEach { conv ->
                    DropdownMenuItem(
                        text = { Text(conv.title) },
                        onClick = {
                            viewModel.selectConversation(conv.id)
                            showConversationsMenu = false
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.deleteConversation(conv.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // Error Banner if any
            if (tutorError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tutorError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.toggleDemoMode(true) }) {
                            Text("Switch to Demo Mode", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Hi, ${userProfile.studentName}!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "I am your personal AI Tutor. Ask me any concept, formula, or problem to explain step by step.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            viewModel.showMessage("Copied to clipboard.")
                        },
                        onShare = {
                            shareText(context, message.content)
                        },
                        onSaveToLibrary = {
                            viewModel.addNoteDocument(
                                title = "Tutor Note: ${message.content.take(30)}...",
                                subject = null,
                                content = message.content
                            )
                        }
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DigiTech StudyMate is thinking step by step...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Suggested Prompts Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedPrompts) { prompt ->
                    SuggestionChip(
                        onClick = {
                            inputText = prompt
                            viewModel.sendTutorMessage(prompt)
                        },
                        label = { Text(prompt, fontSize = 12.sp) },
                        modifier = Modifier.testTag("tutor_suggested_prompt")
                    )
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask your tutor a question...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tutor_message_input"),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val text = inputText.trim()
                                inputText = ""
                                viewModel.sendTutorMessage(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("tutor_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        TutorSettingsDialog(
            currentSettings = tutorSettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                viewModel.updateTutorSettings(updated)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveToLibrary: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isUser) 0.dp else 1.dp
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }

            if (!isUser) {
                // Action row for AI answers: Copy, Share, Save to Library
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSaveToLibrary, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Save to Library",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorSettingsDialog(
    currentSettings: TutorSettings,
    onDismiss: () -> Unit,
    onSave: (TutorSettings) -> Unit
) {
    var level by remember { mutableStateOf(currentSettings.explanationLevel) }
    var length by remember { mutableStateOf(currentSettings.responseLength) }
    var style by remember { mutableStateOf(currentSettings.teachingStyle) }
    var language by remember { mutableStateOf(currentSettings.preferredLanguage) }
    var includeExamples by remember { mutableStateOf(currentSettings.includeExamples) }
    var includeRecap by remember { mutableStateOf(currentSettings.includeRecap) }

    val levels = listOf("Very Simple", "School", "College", "University")
    val lengths = listOf("Short", "Balanced", "Detailed")
    val styles = listOf("Step-by-step", "Examples", "Socratic questions", "Exam-focused")
    val languages = listOf("English", "Urdu", "Roman Urdu", "Hindi")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutor Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Explanation Level", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.take(2).forEach {
                        FilterChip(selected = level == it, onClick = { level = it }, label = { Text(it, fontSize = 11.sp) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.drop(2).forEach {
                        FilterChip(selected = level == it, onClick = { level = it }, label = { Text(it, fontSize = 11.sp) })
                    }
                }

                Text("Response Length", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    lengths.forEach {
                        FilterChip(selected = length == it, onClick = { length = it }, label = { Text(it, fontSize = 11.sp) })
                    }
                }

                Text("Teaching Style", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    styles.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach {
                                FilterChip(selected = style == it, onClick = { style = it }, label = { Text(it, fontSize = 11.sp) })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Everyday Examples", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeExamples, onCheckedChange = { includeExamples = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Quick Recap / Question", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeRecap, onCheckedChange = { includeRecap = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        TutorSettings(
                            explanationLevel = level,
                            responseLength = length,
                            teachingStyle = style,
                            preferredLanguage = language,
                            includeExamples = includeExamples,
                            includeRecap = includeRecap
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Tutor Explanation")
    context.startActivity(shareIntent)
}
