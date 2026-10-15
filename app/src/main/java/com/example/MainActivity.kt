package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NavigationDestination
import com.example.ui.StudyViewModel
import com.example.ui.screens.flashcards.FlashcardDeckListScreen
import com.example.ui.screens.flashcards.FlashcardStudyScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.DocumentDetailScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.planner.PlannerScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.quiz.QuizActiveScreen
import com.example.ui.screens.quiz.QuizListScreen
import com.example.ui.screens.quiz.QuizResultsScreen
import com.example.ui.screens.settings.BackendContractScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.summarizer.SummarizerScreen
import com.example.ui.screens.tutor.TutorScreen
import com.example.ui.theme.StudyMateTheme

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val destination: NavigationDestination,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyMateApp()
        }
    }
}

@Composable
fun StudyMateApp(viewModel: StudyViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        val msg = userMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    val isDarkTheme = when (userProfile.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    StudyMateTheme(darkTheme = isDarkTheme) {
        if (!userProfile.isOnboardingCompleted) {
            OnboardingScreen(
                onComplete = { name, level, goal, language, dailyTarget, examDate ->
                    viewModel.completeOnboarding(name, level, goal, language, dailyTarget, examDate)
                }
            )
        } else {
            val bottomNavItems = remember {
                listOf(
                    BottomNavItem(
                        title = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        destination = NavigationDestination.Home,
                        tag = "nav_home"
                    ),
                    BottomNavItem(
                        title = "AI Tutor",
                        selectedIcon = Icons.Filled.School,
                        unselectedIcon = Icons.Outlined.School,
                        destination = NavigationDestination.Tutor,
                        tag = "nav_tutor"
                    ),
                    BottomNavItem(
                        title = "Library",
                        selectedIcon = Icons.Filled.Folder,
                        unselectedIcon = Icons.Outlined.Folder,
                        destination = NavigationDestination.Library,
                        tag = "nav_library"
                    ),
                    BottomNavItem(
                        title = "Planner",
                        selectedIcon = Icons.Filled.CalendarMonth,
                        unselectedIcon = Icons.Outlined.CalendarMonth,
                        destination = NavigationDestination.Planner,
                        tag = "nav_planner"
                    ),
                    BottomNavItem(
                        title = "Profile",
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person,
                        destination = NavigationDestination.Profile,
                        tag = "nav_profile"
                    )
                )
            }

            val isPrimaryScreen = currentScreen in listOf(
                NavigationDestination.Home,
                NavigationDestination.Tutor,
                NavigationDestination.Library,
                NavigationDestination.Planner,
                NavigationDestination.Profile
            )

            // BackHandler for secondary screens
            if (!isPrimaryScreen) {
                BackHandler {
                    viewModel.navigateBack()
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (isPrimaryScreen) {
                        NavigationBar(
                            modifier = Modifier
                                .testTag("main_bottom_nav")
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentScreen == item.destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    },
                                    label = { Text(item.title) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isPrimaryScreen) paddingValues.calculateBottomPadding() else 0.dp)
                ) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { destination ->
                        when (destination) {
                            is NavigationDestination.Home -> HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Tutor -> TutorScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Library -> LibraryScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Planner -> PlannerScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Profile -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.DocumentDetail -> DocumentDetailScreen(
                                documentId = destination.documentId,
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Summarizer -> SummarizerScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.QuizList -> QuizListScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.QuizActive -> QuizActiveScreen(
                                quizId = destination.quizId,
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.QuizResult -> QuizResultsScreen(
                                quizId = destination.quizId,
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.FlashcardsList -> FlashcardDeckListScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.FlashcardStudy -> FlashcardStudyScreen(
                                deckId = destination.deckId,
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.BackendContract -> BackendContractScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            is NavigationDestination.Progress -> ProgressScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
