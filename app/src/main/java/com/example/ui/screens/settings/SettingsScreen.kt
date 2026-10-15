package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiConfig
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val tutorSettings by viewModel.tutorSettings.collectAsState()
    val testStatus by viewModel.connectionTestStatus.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var studentName by remember(userProfile.studentName) { mutableStateOf(userProfile.studentName) }
    var apiKeyInput by remember(userProfile.customApiKey) { mutableStateOf(userProfile.customApiKey) }
    var activeModel by remember { mutableStateOf(GeminiConfig.activeModelId) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "Profile & Settings",
                subtitle = "DigiTech StudyMate AI",
                isDemoMode = userProfile.isDemoMode,
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) }
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
            // Student Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Student Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Education Level: ${userProfile.educationLevel}", style = MaterialTheme.typography.bodyMedium)
                        Text("Target: ${userProfile.dailyTargetMinutes}m/day", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                studentName,
                                userProfile.educationLevel,
                                userProfile.studyGoal,
                                userProfile.preferredLanguage,
                                userProfile.dailyTargetMinutes
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Update Profile")
                    }
                }
            }

            // AI Service Configuration & Model Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AI Mode & Model Configuration", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    // Demo Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Demo Mode (Offline Samples)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = if (userProfile.isDemoMode) "Using verified educational samples without sending API calls" else "Live AI active (Connecting to Google Gemini)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userProfile.isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(it) },
                            modifier = Modifier.testTag("demo_mode_toggle")
                        )
                    }

                    HorizontalDivider()

                    // Gemini Model ID
                    Text("Gemini Model ID", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = activeModel,
                        onValueChange = {
                            activeModel = it
                            viewModel.updateModelId(it)
                        },
                        label = { Text("Model ID (Default: gemini-4-argon)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Verified Gemini Models", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GeminiConfig.VERIFIED_MODELS.take(2).forEach { m ->
                            FilterChip(
                                selected = activeModel == m,
                                onClick = {
                                    activeModel = m
                                    viewModel.updateModelId(m)
                                },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GeminiConfig.VERIFIED_MODELS.drop(2).forEach { m ->
                            FilterChip(
                                selected = activeModel == m,
                                onClick = {
                                    activeModel = m
                                    viewModel.updateModelId(m)
                                },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Test API Button
                    Button(
                        onClick = { viewModel.testApiConnection(activeModel) },
                        enabled = !isTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_api_connection_btn")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing API...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Model Availability ($activeModel)")
                        }
                    }

                    if (!testStatus.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (testStatus!!.contains("Successful", ignoreCase = true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_test_result_surface")
                        ) {
                            Text(
                                text = testStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testStatus!!.contains("Successful", ignoreCase = true)) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Optional Custom API Key (for testing without BuildConfig)
                    Text("Custom Gemini API Key (Optional)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Enter API Key or use AI Studio Secrets panel") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.updateCustomApiKey(apiKeyInput) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Key")
                    }

                    Text(
                        text = "Production Security: In production, credentials should be kept on the secure PHP/MySQL backend rather than embedded in the mobile APK. The app routes AI queries through typed repository interfaces compliant with backend proxying.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Learning Progress & Analytics Shortcut
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(NavigationDestination.Progress) }
                    .testTag("settings_progress_analytics_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
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
                            text = "Learning Progress & Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "View study minutes, session history, quiz accuracy, and topic mastery.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Backend Integration Contract Shortcut
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(NavigationDestination.BackendContract) }
                    .testTag("settings_backend_contract_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                            text = "Production Backend Contract",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "View all 17 documented API endpoints, schemas, and PHP 8.3/MySQL setup guide.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Data Management Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Data Management", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedButton(
                        onClick = {
                            val export = "DigiTech StudyMate Student: ${userProfile.studentName}\n" +
                                    "Education Level: ${userProfile.educationLevel}\n" +
                                    "Goal: ${userProfile.studyGoal}\n" +
                                    "Daily Target: ${userProfile.dailyTargetMinutes} mins\n" +
                                    "Backend: ${userProfile.backendBaseUrl}"
                            clipboardManager.setText(AnnotatedString(export))
                            viewModel.showMessage("Study profile exported to clipboard.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Personal Study Profile")
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Study Data")
                    }
                }
            }

            // Brand & About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DigiTech StudyMate AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Your Personal AI Tutor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0 (Native Android with Jetpack Compose & Room)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Publisher: DigiTech",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Reset Study Data?") },
            text = { Text("Are you sure you want to clear all local study data, notes, and quiz history?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.showMessage("Local study data reset.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
