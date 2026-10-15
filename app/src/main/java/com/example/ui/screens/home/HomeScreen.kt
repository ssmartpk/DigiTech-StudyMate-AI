package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar
import com.example.ui.theme.*

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val destination: NavigationDestination,
    val tag: String
)

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()

    // Calculate actual metrics from records (No invented data)
    val now = remember { System.currentTimeMillis() }
    val startOfToday = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.timeInMillis
    }

    val todayMinutes = remember(sessions) {
        sessions.filter { it.timestamp >= startOfToday }.sumOf { it.durationMinutes }
    }
    val completedTasksToday = remember(tasks) {
        tasks.count { it.isCompleted }
    }
    val quizzesAttempted = remember(quizzes) {
        quizzes.count { it.isCompleted }
    }
    // Calculate streak days
    val streakDays = remember(sessions) {
        if (sessions.isEmpty()) 0
        else {
            val daysSet = sessions.map {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
            }.toSet()
            daysSet.size.coerceAtLeast(1)
        }
    }

    val quickActions = remember {
        listOf(
            QuickActionItem(
                title = "Ask AI Tutor",
                subtitle = "Step-by-step help",
                icon = Icons.Default.ChatBubble,
                containerColor = PrimaryContainerLight,
                contentColor = PrimaryIndigo,
                destination = NavigationDestination.Tutor,
                tag = "quick_action_tutor"
            ),
            QuickActionItem(
                title = "Summarize a PDF",
                subtitle = "Extract key formulas",
                icon = Icons.Default.AutoAwesome,
                containerColor = SecondaryContainerLight,
                contentColor = SecondaryTeal,
                destination = NavigationDestination.Summarizer,
                tag = "quick_action_summarizer"
            ),
            QuickActionItem(
                title = "Scan Study Notes",
                subtitle = "Import & index docs",
                icon = Icons.Default.DocumentScanner,
                containerColor = Color(0xFFF1F8E9),
                contentColor = Color(0xFF33691E),
                destination = NavigationDestination.Library,
                tag = "quick_action_scan"
            ),
            QuickActionItem(
                title = "Generate a Quiz",
                subtitle = "Test your recall",
                icon = Icons.Default.Quiz,
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100),
                destination = NavigationDestination.QuizList,
                tag = "quick_action_quiz"
            ),
            QuickActionItem(
                title = "Create Flashcards",
                subtitle = "Spaced repetition",
                icon = Icons.Default.Style,
                containerColor = Color(0xFFEDE7F6),
                contentColor = Color(0xFF512DA8),
                destination = NavigationDestination.FlashcardsList,
                tag = "quick_action_flashcards"
            ),
            QuickActionItem(
                title = "Build a Study Plan",
                subtitle = "Timetable & Focus",
                icon = Icons.Default.Schedule,
                containerColor = Color(0xFFE1F5FE),
                contentColor = Color(0xFF0277BD),
                destination = NavigationDestination.Planner,
                tag = "quick_action_planner"
            )
        )
    }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "DigiTech StudyMate",
                subtitle = "Your Personal AI Tutor",
                isDemoMode = userProfile.isDemoMode,
                onSettingsClick = { onNavigate(NavigationDestination.Profile) },
                onModeClick = { viewModel.toggleDemoMode(!userProfile.isDemoMode) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Welcome Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryIndigo, PrimaryIndigoLight)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hi, ${userProfile.studentName}!",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "What would you like to learn today?",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { onNavigate(NavigationDestination.Profile) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile.studentName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Daily Target Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Target: $todayMinutes / ${userProfile.dailyTargetMinutes} mins completed",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Today's Study Summary (Metrics)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Study Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(
                        onClick = { onNavigate(NavigationDestination.Progress) },
                        modifier = Modifier.testTag("home_view_analytics_btn")
                    ) {
                        Text("View Analytics")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(NavigationDestination.Progress) },
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Study Time",
                        value = "${todayMinutes}m",
                        icon = Icons.Default.Timer,
                        iconTint = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Tasks Done",
                        value = "$completedTasksToday",
                        icon = Icons.Default.CheckCircle,
                        iconTint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Quizzes",
                        value = "$quizzesAttempted",
                        icon = Icons.Default.Quiz,
                        iconTint = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Streak",
                        value = "${streakDays}d",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = StreakGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Quick Action Cards (Grid)
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quickActions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowActions.forEach { action ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(action.tag)
                                        .clickable { onNavigate(action.destination) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = action.containerColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(action.contentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = action.icon,
                                                contentDescription = null,
                                                tint = action.contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = action.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = action.contentColor
                                            )
                                        )
                                        Text(
                                            text = action.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Continue Learning (Recent items)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue Learning",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { onNavigate(NavigationDestination.Library) }) {
                        Text("View All")
                    }
                }

                if (documents.isEmpty() && conversations.isEmpty()) {
                    Text(
                        text = "No recent study items. Import a note or ask the AI tutor!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(documents.take(3)) { doc ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { viewModel.selectDocument(doc.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(doc.fileType, modifier = Modifier.padding(2.dp))
                                        }
                                        Text(
                                            text = doc.subjectName ?: "General",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Upcoming Tasks
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Study Tasks",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { onNavigate(NavigationDestination.Planner) }) {
                        Text("Open Planner")
                    }
                }

                val pendingTasks = tasks.filter { !it.isCompleted }.take(3)
                if (pendingTasks.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "All tasks completed! Plan your next revision in the Planner tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingTasks.forEach { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        modifier = Modifier.testTag("home_task_checkbox_${task.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = "${task.subjectName ?: "Study"} • ${task.durationMinutes} mins • ${task.priority} Priority",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Daily Learning Tip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Curated Study Tip of the Day",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active recall (testing yourself with flashcards and practice quizzes) boosts long-term retention up to 50% more than passively re-reading textbook notes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
