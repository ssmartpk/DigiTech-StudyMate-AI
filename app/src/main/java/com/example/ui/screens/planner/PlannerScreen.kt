package com.example.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
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
import com.example.data.local.entity.PlannerTaskEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudyMateTopBar
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Tasks & Schedule, 1 = Pomodoro Focus Timer
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAiPlanDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "Study Planner",
                subtitle = "${tasks.count { !it.isCompleted }} pending tasks",
                isDemoMode = userProfile.isDemoMode,
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) },
                onSettingsClick = { onNavigate(NavigationDestination.Profile) }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Task") },
                    modifier = Modifier.testTag("planner_add_task_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tasks & Schedule") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pomodoro Timer") }
                )
            }

            if (selectedTab == 0) {
                // Task List Section
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Plan Generator banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
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
                                        text = "AI Study Timetable Generator",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Generate a balanced revision plan based on your exam date and syllabus.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = { showAiPlanDialog = true },
                                    modifier = Modifier.testTag("planner_ai_plan_button")
                                ) {
                                    Text("Generate Plan", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (tasks.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.EventNote,
                                title = "No study tasks scheduled",
                                description = "Add a task or generate an AI timetable to stay organized.",
                                actionLabel = "Add First Task",
                                onActionClick = { showAddTaskDialog = true }
                            )
                        }
                    } else {
                        items(tasks) { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_item_${task.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleTaskComplete(task) },
                                        modifier = Modifier.testTag("task_checkbox_${task.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "${task.subjectName ?: "General"} • ${task.durationMinutes} mins • ${task.priority} Priority${if (task.isRevision) " • Revision" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Pomodoro Focus Timer Section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (pomodoroState.isBreak) "Break Time (Rest & Hydrate)" else "Pomodoro Focus Session",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (pomodoroState.selectedSubjectName != null) "Focusing on ${pomodoroState.selectedSubjectName}" else "25 minutes deep focus, 5 minutes break",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Circular Timer Countdown
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                if (pomodoroState.isBreak) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val minutes = pomodoroState.secondsRemaining / 60
                        val seconds = pomodoroState.secondsRemaining % 60
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pomodoroState.isBreak) SecondaryTeal else PrimaryIndigo
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${pomodoroState.completedSessionsCount} sessions completed",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Timer Controls (Start/Pause, Reset)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (pomodoroState.isRunning) {
                                    viewModel.pausePomodoro()
                                } else {
                                    viewModel.startPomodoro(subjects.firstOrNull())
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("pomodoro_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (pomodoroState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (pomodoroState.isRunning) "Pause" else "Start Focus")
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetPomodoro() },
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            subjects = subjects,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, subj, duration, priority, isRevision ->
                viewModel.addTask(title, subj, System.currentTimeMillis() + 3600000L, duration, priority, isRevision)
                showAddTaskDialog = false
            }
        )
    }

    if (showAiPlanDialog) {
        AiPlanDialog(
            subjects = subjects,
            onDismiss = { showAiPlanDialog = false },
            onGenerate = { subjNames, examDate, hours, syllabus ->
                viewModel.showMessage("Generating AI study plan for $subjNames...")
                viewModel.addTask("Study Plan Session: $subjNames", subjects.firstOrNull(), System.currentTimeMillis() + 7200000L, 45, "High", true)
                showAiPlanDialog = false
            }
        )
    }
}

@Composable
fun AddTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: SubjectEntity?, duration: Int, priority: String, isRevision: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var durationMinutes by remember { mutableStateOf(30) }
    var priority by remember { mutableStateOf("Medium") }
    var isRevision by remember { mutableStateOf(false) }

    val priorities = listOf("Low", "Medium", "High")
    val durations = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description") },
                    placeholder = { Text("e.g. Read chapter 3 formulas and derive...") },
                    modifier = Modifier.fillMaxWidth()
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

                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durations.forEach { d ->
                        FilterChip(
                            selected = durationMinutes == d,
                            onClick = { durationMinutes = d },
                            label = { Text("${d}m") }
                        )
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Revision Session")
                    Switch(checked = isRevision, onCheckedChange = { isRevision = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, selectedSubject, durationMinutes, priority, isRevision)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiPlanDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onGenerate: (subjNames: String, examDate: String, hours: Float, syllabus: String) -> Unit
) {
    var examDate by remember { mutableStateOf("Nov 25, 2026") }
    var hoursPerDay by remember { mutableStateOf(3f) }
    var syllabus by remember { mutableStateOf("Photosynthesis, Kinematics, Newton's Laws") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Study Plan Generator", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Exam Date or Target Deadline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = syllabus,
                    onValueChange = { syllabus = it },
                    label = { Text("Syllabus / Topics to Cover") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Text("Available Study Time: ${hoursPerDay.toInt()} hours / day", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hoursPerDay,
                    onValueChange = { hoursPerDay = it },
                    valueRange = 1f..8f,
                    steps = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(subjects.joinToString { it.name }.ifBlank { "All Subjects" }, examDate, hoursPerDay, syllabus)
                }
            ) {
                Text("Build Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
