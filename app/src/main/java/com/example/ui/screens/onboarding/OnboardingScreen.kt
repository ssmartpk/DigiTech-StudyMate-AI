package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (name: String, level: String, goal: String, language: String, dailyTarget: Int, examDate: Long?) -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Learn Anything",
                subtitle = "DigiTech StudyMate AI",
                description = "Your personal tutor that explains difficult topics step by step with clear everyday examples and analogies tailored to your level.",
                icon = Icons.Default.School
            ),
            OnboardingSlide(
                title = "Turn Notes into Knowledge",
                subtitle = "Deep Study Library",
                description = "Import lecture PDFs, scanned textbook photos, and study notes. Instantly generate exam summaries, flashcard decks, and practice tests.",
                icon = Icons.Default.Book
            ),
            OnboardingSlide(
                title = "Prepare with Confidence",
                subtitle = "Mastery & Focus",
                description = "Build realistic revision schedules, master concepts with spaced-repetition drills, and track your study streak and quiz mastery.",
                icon = Icons.Default.AutoAwesome
            )
        )
    }

    var currentSlideIndex by remember { mutableStateOf(0) }
    var isConfiguringProfile by remember { mutableStateOf(false) }

    // User preference inputs
    var name by remember { mutableStateOf("Alex") }
    var selectedLevel by remember { mutableStateOf("College") }
    var studyGoal by remember { mutableStateOf("Master exam syllabus with step-by-step guidance") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var dailyTargetMinutes by remember { mutableStateOf(45) }

    val levels = listOf("School", "College", "University", "Exam Candidate")
    val languages = listOf("English", "Urdu", "Roman Urdu", "Hindi", "Follow My Question")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isConfiguringProfile) {
            val slide = slides[currentSlideIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { isConfiguringProfile = true },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip Setup")
                    }
                }

                // Slide Graphic & Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryIndigo, SecondaryTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Page Indicator Dots & Navigation Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        slides.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (index == currentSlideIndex) 20.dp else 8.dp, 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index == currentSlideIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentSlideIndex > 0) {
                            OutlinedButton(
                                onClick = { currentSlideIndex -= 1 },
                                modifier = Modifier.testTag("onboarding_prev_button")
                            ) {
                                Text("Previous")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (currentSlideIndex < slides.size - 1) {
                                    currentSlideIndex += 1
                                } else {
                                    isConfiguringProfile = true
                                }
                            },
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text(if (currentSlideIndex == slides.size - 1) "Configure Profile" else "Next")
                        }
                    }
                }
            }
        } else {
            // Profile configuration step
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .systemBarsPadding()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome to DigiTech StudyMate!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tell us about your learning goals so your AI tutor can personalize explanations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Preferred Name or Nickname") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Education Level
                Text(
                    text = "Education Level",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.take(2).forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.drop(2).forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language
                Text(
                    text = "Preferred Explanation Language",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    languages.chunked(3).forEach { chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunk.forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily target
                Text(
                    text = "Daily Study Target: $dailyTargetMinutes minutes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = dailyTargetMinutes.toFloat(),
                    onValueChange = { dailyTargetMinutes = it.toInt() },
                    valueRange = 15f..180f,
                    steps = 10,
                    modifier = Modifier.testTag("onboarding_target_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Study Goal
                OutlinedTextField(
                    value = studyGoal,
                    onValueChange = { studyGoal = it },
                    label = { Text("Main Study Goal or Target Exam") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_goal_input")
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        onComplete(
                            name.ifBlank { "Student" },
                            selectedLevel,
                            studyGoal,
                            selectedLanguage,
                            dailyTargetMinutes,
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_get_started_button")
                ) {
                    Text("Get Started as Student", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
