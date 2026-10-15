package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.BackendApiContract
import com.example.ai.GeminiConfig
import com.example.core.UserProfile
import com.example.core.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface NavigationDestination {
    object Home : NavigationDestination
    object Tutor : NavigationDestination
    object Library : NavigationDestination
    object Planner : NavigationDestination
    object Profile : NavigationDestination
    data class DocumentDetail(val documentId: String) : NavigationDestination
    object Summarizer : NavigationDestination
    object QuizList : NavigationDestination
    data class QuizActive(val quizId: String) : NavigationDestination
    data class QuizResult(val quizId: String) : NavigationDestination
    object FlashcardsList : NavigationDestination
    data class FlashcardStudy(val deckId: String) : NavigationDestination
    object BackendContract : NavigationDestination
    object Progress : NavigationDestination
}

data class PomodoroState(
    val isRunning: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isBreak: Boolean = false,
    val completedSessionsCount: Int = 0,
    val selectedSubjectName: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val studyRepository = StudyRepository(database, preferencesRepository)

    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val tutorSettings: StateFlow<TutorSettings> = preferencesRepository.tutorSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TutorSettings())

    private val _currentScreen = MutableStateFlow<NavigationDestination>(NavigationDestination.Home)
    val currentScreen: StateFlow<NavigationDestination> = _currentScreen.asStateFlow()

    // --- Database Flows ---
    val subjects: StateFlow<List<SubjectEntity>> = studyRepository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentEntity>> = studyRepository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<ConversationEntity>> = studyRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizzes: StateFlow<List<QuizEntity>> = studyRepository.allQuizzes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decks: StateFlow<List<FlashcardDeckEntity>> = studyRepository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<PlannerTaskEntity>> = studyRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySessionEntity>> = studyRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat / Tutor State ---
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val currentChatMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) studyRepository.getMessagesForConversation(convId)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTutorThinking = MutableStateFlow(false)
    val isTutorThinking: StateFlow<Boolean> = _isTutorThinking.asStateFlow()

    private val _tutorError = MutableStateFlow<String?>(null)
    val tutorError: StateFlow<String?> = _tutorError.asStateFlow()

    // --- Document Detail State ---
    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    // --- Active Quiz State ---
    private val _activeQuiz = MutableStateFlow<QuizEntity?>(null)
    val activeQuiz: StateFlow<QuizEntity?> = _activeQuiz.asStateFlow()

    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _activeQuizQuestionIndex = MutableStateFlow(0)
    val activeQuizQuestionIndex: StateFlow<Int> = _activeQuizQuestionIndex.asStateFlow()

    private val _quizElapsedTimeSeconds = MutableStateFlow(0)
    val quizElapsedTimeSeconds: StateFlow<Int> = _quizElapsedTimeSeconds.asStateFlow()
    private var quizTimerJob: Job? = null

    // --- Active Flashcard Study State ---
    private val _activeDeck = MutableStateFlow<FlashcardDeckEntity?>(null)
    val activeDeck: StateFlow<FlashcardDeckEntity?> = _activeDeck.asStateFlow()

    private val _activeDeckCards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val activeDeckCards: StateFlow<List<FlashcardEntity>> = _activeDeckCards.asStateFlow()

    private val _activeCardIndex = MutableStateFlow(0)
    val activeCardIndex: StateFlow<Int> = _activeCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    // --- Pomodoro State ---
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()
    private var pomodoroJob: Job? = null

    // --- Summarizer State ---
    private val _summaryResult = MutableStateFlow<String?>(null)
    val summaryResult: StateFlow<String?> = _summaryResult.asStateFlow()
    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    // Status snackbar / toast message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed database with realistic demo data on initial launch
            DatabaseSeeder.seedDemoDataIfEmpty(database)
        }
    }

    private val backStack = mutableListOf<NavigationDestination>()

    fun navigateTo(destination: NavigationDestination) {
        if (_currentScreen.value != destination) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = destination
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != NavigationDestination.Home) {
            _currentScreen.value = NavigationDestination.Home
            return true
        }
        return false
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun importDocument(
        title: String,
        subject: SubjectEntity?,
        fileType: String,
        fileSizeBytes: Long,
        extractedContent: String,
        fileUriOrPath: String? = null
    ) {
        val maxSizeBytes = 15 * 1024 * 1024L // 15MB limit
        if (fileSizeBytes > maxSizeBytes) {
            showMessage("File exceeds maximum allowed limit of 15 MB.")
            return
        }
        viewModelScope.launch {
            val doc = DocumentEntity(
                title = title,
                subjectId = subject?.id,
                subjectName = subject?.name,
                fileType = fileType,
                fileUriOrPath = fileUriOrPath,
                fileSizeBytes = fileSizeBytes,
                extractedText = extractedContent,
                status = "Ready"
            )
            studyRepository.saveDocument(doc)
            showMessage("Document '$title' imported successfully.")
        }
    }

    // --- Onboarding & Preferences ---
    fun completeOnboarding(
        name: String,
        level: String,
        goal: String,
        language: String,
        dailyTarget: Int,
        examDate: Long?
    ) {
        viewModelScope.launch {
            preferencesRepository.updateOnboarding(name, level, goal, language, dailyTarget, examDate)
            _currentScreen.value = NavigationDestination.Home
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDemoMode(enabled)
            showMessage(if (enabled) "Switched to Demo Mode (offline sample responses)" else "Switched to Live AI Mode (Gemini 4 Argon)")
        }
    }

    fun updateProfile(name: String, level: String, goal: String, language: String, target: Int) {
        viewModelScope.launch {
            preferencesRepository.updateProfile(name, level, goal, language, target)
            showMessage("Profile updated successfully.")
        }
    }

    fun updateCustomApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.updateCustomApiKey(apiKey)
            showMessage("API Key saved.")
        }
    }

    fun updateModelId(modelId: String) {
        GeminiConfig.activeModelId = modelId.trim()
        showMessage("Active model set to: ${GeminiConfig.activeModelId}")
    }

    private val _connectionTestStatus = MutableStateFlow<String?>(null)
    val connectionTestStatus: StateFlow<String?> = _connectionTestStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun testApiConnection(modelId: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestStatus.value = "Testing live connection to '$modelId'..."
            val result = studyRepository.testApiConnection(modelId)
            _isTestingConnection.value = false
            if (result.isSuccess) {
                _connectionTestStatus.value = result.getOrNull()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                _connectionTestStatus.value = "Verification Result: $err"
            }
        }
    }

    fun updateTutorSettings(settings: TutorSettings) {
        viewModelScope.launch {
            preferencesRepository.updateTutorSettings(settings)
            showMessage("Tutor preferences updated.")
        }
    }

    // --- Tutor Actions ---
    fun selectConversation(convId: String) {
        _activeConversationId.value = convId
        _tutorError.value = null
    }

    fun startNewConversation(title: String = "Study Session", subjectId: String? = null) {
        viewModelScope.launch {
            val conv = studyRepository.createConversation(title, subjectId)
            _activeConversationId.value = conv.id
            _tutorError.value = null
        }
    }

    fun sendTutorMessage(text: String, contextDoc: String? = null) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _isTutorThinking.value = true
            _tutorError.value = null

            val history = currentChatMessages.value
            val settings = tutorSettings.value
            val result = studyRepository.sendTutorMessage(convId, text, history, settings, contextDoc)

            _isTutorThinking.value = false
            if (result.isFailure) {
                _tutorError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to get AI response"
            }
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            studyRepository.deleteConversation(convId)
            if (_activeConversationId.value == convId) {
                val next = conversations.value.firstOrNull { it.id != convId }
                _activeConversationId.value = next?.id
            }
            showMessage("Conversation deleted.")
        }
    }

    // --- Library & Documents ---
    fun selectDocument(docId: String) {
        viewModelScope.launch {
            val doc = studyRepository.getDocument(docId)
            _selectedDocument.value = doc
            _currentScreen.value = NavigationDestination.DocumentDetail(docId)
        }
    }

    fun addNoteDocument(title: String, subject: SubjectEntity?, content: String) {
        viewModelScope.launch {
            val doc = DocumentEntity(
                title = title,
                subjectId = subject?.id,
                subjectName = subject?.name,
                fileType = "NOTE",
                extractedText = content,
                status = "Ready"
            )
            studyRepository.saveDocument(doc)
            showMessage("Note '$title' saved to Library.")
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            studyRepository.deleteDocument(docId)
            showMessage("Document deleted.")
            _currentScreen.value = NavigationDestination.Library
        }
    }

    fun addSubject(name: String, description: String, colorHex: String, targetMins: Int) {
        viewModelScope.launch {
            val subject = SubjectEntity(
                name = name,
                description = description,
                colorHex = colorHex,
                studyTargetMinutes = targetMins
            )
            studyRepository.saveSubject(subject)
            showMessage("Subject '$name' created.")
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            studyRepository.deleteSubject(subjectId)
            showMessage("Subject removed.")
        }
    }

    // --- Summarizer ---
    fun generateSummary(title: String, content: String, type: String, length: String) {
        if (content.isBlank()) {
            showMessage("Please provide study content to summarize.")
            return
        }
        viewModelScope.launch {
            _isSummarizing.value = true
            _summaryResult.value = null
            val result = studyRepository.generateSummary(title, content, type, length)
            _isSummarizing.value = false
            if (result.isSuccess) {
                _summaryResult.value = result.getOrNull()
            } else {
                showMessage("Summary error: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- Quizzes ---
    fun startQuiz(quizId: String) {
        viewModelScope.launch {
            val quiz = studyRepository.getQuiz(quizId) ?: return@launch
            val parsedQuestions = QuizQuestion.parseQuestionsJson(quiz.questionsJson)
            _activeQuiz.value = quiz
            _activeQuizQuestions.value = parsedQuestions
            _activeQuizQuestionIndex.value = 0
            _quizElapsedTimeSeconds.value = 0
            _currentScreen.value = NavigationDestination.QuizActive(quizId)

            quizTimerJob?.cancel()
            quizTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _quizElapsedTimeSeconds.value += 1
                }
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, selectedOptionIndex: Int) {
        val list = _activeQuizQuestions.value.toMutableList()
        if (questionIndex in list.indices) {
            val q = list[questionIndex]
            list[questionIndex] = q.copy(userSelectedIndex = selectedOptionIndex)
            _activeQuizQuestions.value = list
        }
    }

    fun submitQuiz() {
        quizTimerJob?.cancel()
        val quiz = _activeQuiz.value ?: return
        val questions = _activeQuizQuestions.value
        val timeTaken = _quizElapsedTimeSeconds.value

        viewModelScope.launch {
            val updated = studyRepository.completeQuizSubmission(quiz.id, questions, timeTaken)
            _activeQuiz.value = updated
            _currentScreen.value = NavigationDestination.QuizResult(quiz.id)
            showMessage("Quiz completed! Score: ${updated?.scorePercentage}%")
        }
    }

    fun openQuizResult(quizId: String) {
        viewModelScope.launch {
            val quiz = studyRepository.getQuiz(quizId) ?: return@launch
            val questions = QuizQuestion.parseQuestionsJson(quiz.questionsJson)
            _activeQuiz.value = quiz
            _activeQuizQuestions.value = questions
            _quizElapsedTimeSeconds.value = quiz.timeTakenSeconds
            navigateTo(NavigationDestination.QuizResult(quizId))
        }
    }

    fun generateNewQuiz(
        subject: SubjectEntity?,
        topic: String,
        count: Int,
        difficulty: String,
        type: String
    ) {
        viewModelScope.launch {
            showMessage("Generating quiz for '$topic'...")
            val result = studyRepository.generateQuizFromTopic(subject, topic, count, difficulty, type)
            if (result.isSuccess) {
                val created = result.getOrNull()!!
                showMessage("Quiz '${created.title}' created with ${created.totalQuestions} questions!")
                startQuiz(created.id)
            } else {
                showMessage("Quiz creation error: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- Flashcards ---
    fun startFlashcardStudy(deckId: String) {
        viewModelScope.launch {
            val deck = studyRepository.getDeck(deckId) ?: return@launch
            val cards = studyRepository.getCardsForDeck(deckId).first()
            if (cards.isEmpty()) {
                showMessage("This deck has no cards yet. Add or generate cards first.")
                return@launch
            }
            _activeDeck.value = deck
            _activeDeckCards.value = cards
            _activeCardIndex.value = 0
            _isCardFlipped.value = false
            _currentScreen.value = NavigationDestination.FlashcardStudy(deckId)
        }
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun previousCard() {
        if (_activeCardIndex.value > 0) {
            _activeCardIndex.value -= 1
            _isCardFlipped.value = false
        }
    }

    fun shuffleCards() {
        _activeDeckCards.value = _activeDeckCards.value.shuffled()
        _activeCardIndex.value = 0
        _isCardFlipped.value = false
        showMessage("Cards shuffled.")
    }

    fun gradeActiveCard(grade: FlashcardGrade) {
        val cards = _activeDeckCards.value
        val index = _activeCardIndex.value
        if (index !in cards.indices) return

        val currentCard = cards[index]
        viewModelScope.launch {
            studyRepository.reviewFlashcard(currentCard, grade)

            if (index + 1 < cards.size) {
                _activeCardIndex.value = index + 1
                _isCardFlipped.value = false
            } else {
                showMessage("Completed study session for '${_activeDeck.value?.title}'!")
                studyRepository.logSession(
                    StudySessionEntity(
                        subjectId = _activeDeck.value?.subjectId,
                        subjectName = _activeDeck.value?.subjectName,
                        durationMinutes = 15,
                        sessionType = "Flashcard",
                        notes = "Reviewed ${cards.size} flashcards"
                    )
                )
                _currentScreen.value = NavigationDestination.FlashcardsList
            }
        }
    }

    fun generateNewFlashcardDeck(title: String, subject: SubjectEntity?, topic: String, count: Int) {
        viewModelScope.launch {
            showMessage("Generating flashcards for '$topic'...")
            val result = studyRepository.generateFlashcardsForDeck(title, subject, topic, count)
            if (result.isSuccess) {
                val deck = result.getOrNull()!!
                showMessage("Flashcard Deck '${deck.title}' created with ${deck.cardCount} cards!")
                startFlashcardStudy(deck.id)
            } else {
                showMessage("Flashcard error: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- Planner & Pomodoro ---
    fun addTask(title: String, subject: SubjectEntity?, dateMillis: Long, duration: Int, priority: String, isRevision: Boolean) {
        viewModelScope.launch {
            studyRepository.saveTask(
                PlannerTaskEntity(
                    title = title,
                    subjectId = subject?.id,
                    subjectName = subject?.name,
                    dateMillis = dateMillis,
                    durationMinutes = duration,
                    priority = priority,
                    isRevision = isRevision
                )
            )
            showMessage("Task '$title' added to planner.")
        }
    }

    fun toggleTaskComplete(task: PlannerTaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            studyRepository.updateTask(updated)
            if (updated.isCompleted) {
                studyRepository.logSession(
                    StudySessionEntity(
                        subjectId = task.subjectId,
                        subjectName = task.subjectName,
                        durationMinutes = task.durationMinutes,
                        sessionType = if (task.isRevision) "Reading" else "Pomodoro",
                        notes = "Completed task: ${task.title}"
                    )
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            studyRepository.deleteTask(taskId)
            showMessage("Task removed.")
        }
    }

    fun startPomodoro(subject: SubjectEntity? = null) {
        _pomodoroState.update { it.copy(isRunning = true, selectedSubjectName = subject?.name) }
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (_pomodoroState.value.isRunning && _pomodoroState.value.secondsRemaining > 0) {
                delay(1000)
                _pomodoroState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
            }
            if (_pomodoroState.value.secondsRemaining == 0) {
                val current = _pomodoroState.value
                if (!current.isBreak) {
                    studyRepository.logSession(
                        StudySessionEntity(
                            subjectName = current.selectedSubjectName,
                            durationMinutes = 25,
                            sessionType = "Pomodoro",
                            notes = "Completed 25-minute focus session"
                        )
                    )
                    showMessage("Focus session complete! Time for a 5-minute break.")
                    _pomodoroState.value = PomodoroState(
                        isRunning = false,
                        secondsRemaining = 5 * 60,
                        totalSeconds = 5 * 60,
                        isBreak = true,
                        completedSessionsCount = current.completedSessionsCount + 1,
                        selectedSubjectName = current.selectedSubjectName
                    )
                } else {
                    showMessage("Break finished! Ready for next focus round?")
                    _pomodoroState.value = PomodoroState(
                        isRunning = false,
                        secondsRemaining = 25 * 60,
                        totalSeconds = 25 * 60,
                        isBreak = false,
                        completedSessionsCount = current.completedSessionsCount,
                        selectedSubjectName = current.selectedSubjectName
                    )
                }
            }
        }
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        _pomodoroState.update { it.copy(isRunning = false) }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        _pomodoroState.value = PomodoroState()
    }
}
