package com.example.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.components.StudyMateTopBar
import com.example.ui.screens.home.MetricTile
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StreakGold
import com.example.ui.theme.SuccessGreen

@Composable
fun ProgressScreen(
    viewModel: StudyViewModel,
    onNavigate: (NavigationDestination) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    // Calculated Statistics from real records
    val totalMinutes = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val completedQuizzes = remember(quizzes) { quizzes.filter { it.isCompleted } }
    val avgScore = remember(completedQuizzes) {
        if (completedQuizzes.isNotEmpty()) completedQuizzes.map { it.scorePercentage }.average().toInt() else 0
    }
    val completedTasks = remember(tasks) { tasks.count { it.isCompleted } }

    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = "Progress & Analytics",
                subtitle = "Active learning telemetry",
                isDemoMode = userProfile.isDemoMode,
                showBackButton = true,
                onBackClick = { onNavigate(NavigationDestination.Home) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Overview
            item {
                Text("Overall Study Telemetry", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Total Time",
                        value = "${totalMinutes}m",
                        icon = Icons.Default.Schedule,
                        iconTint = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Avg Score",
                        value = "$avgScore%",
                        icon = Icons.Default.Grade,
                        iconTint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Tasks Done",
                        value = "$completedTasks",
                        icon = Icons.Default.DoneAll,
                        iconTint = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Sessions",
                        value = "${sessions.size}",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = StreakGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Subject-wise Breakdown
            item {
                Text("Subject Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            if (subjects.isEmpty()) {
                item {
                    Text(
                        text = "No subjects added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(subjects) { subj ->
                    val subjectMinutes = sessions.filter { it.subjectId == subj.id || it.subjectName == subj.name }.sumOf { it.durationMinutes }
                    val progressFraction = (subjectMinutes.toFloat() / subj.studyTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subj.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$subjectMinutes / ${subj.studyTargetMinutes} mins",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Learning Recommendations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = SecondaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tutor Recommendation", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (avgScore < 75 && completedQuizzes.isNotEmpty())
                                "Review your recent quiz answers. Spend 15 minutes reviewing the explanations for incorrect questions to cement core formulas."
                            else
                                "Solid consistency! Try active recall by taking a 10-question practice test and testing your flashcard deck before bed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
