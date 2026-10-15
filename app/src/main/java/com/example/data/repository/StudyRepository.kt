package com.example.data.repository

import com.example.ai.*
import com.example.core.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.FlashcardGrade
import com.example.data.model.QuizQuestion
import com.example.data.model.SpacedRepetitionScheduler
import com.example.data.model.TutorSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StudyRepository(
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val subjectDao = database.subjectDao()
    private val documentDao = database.documentDao()
    private val chatDao = database.chatDao()
    private val quizDao = database.quizDao()
    private val flashcardDao = database.flashcardDao()
    private val plannerDao = database.plannerDao()
    private val studySessionDao = database.studySessionDao()

    private val mockAiService = MockAiService()

    private var cachedCustomKey: String = ""

    private val geminiAiService = GeminiAiService(
        customApiKeyProvider = { cachedCustomKey }
    )

    suspend fun getActiveAiService(): AiService {
        val profile = preferencesRepository.userProfileFlow.first()
        cachedCustomKey = profile.customApiKey
        return if (profile.isDemoMode) {
            mockAiService
        } else {
            geminiAiService
        }
    }

    suspend fun testApiConnection(modelId: String): Result<String> {
        val profile = preferencesRepository.userProfileFlow.first()
        cachedCustomKey = profile.customApiKey
        return geminiAiService.testConnection(modelId)
    }

    // --- Subjects ---
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    suspend fun getSubject(id: String) = subjectDao.getSubjectById(id)
    suspend fun saveSubject(subject: SubjectEntity) = subjectDao.insertSubject(subject)
    suspend fun deleteSubject(id: String) = subjectDao.deleteSubjectById(id)

    // --- Documents ---
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    fun getDocumentsBySubject(subjectId: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsBySubject(subjectId)
    suspend fun getDocument(id: String) = documentDao.getDocumentById(id)
    suspend fun saveDocument(doc: DocumentEntity) = documentDao.insertDocument(doc)
    suspend fun deleteDocument(id: String) = documentDao.deleteDocumentById(id)

    // --- Chat & Tutor ---
    val allConversations: Flow<List<ConversationEntity>> = chatDao.getAllConversations()
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForConversation(convId)
    suspend fun createConversation(title: String, subjectId: String? = null): ConversationEntity {
        val conv = ConversationEntity(title = title, subjectId = subjectId)
        chatDao.insertConversation(conv)
        return conv
    }
    suspend fun saveChatMessage(message: ChatMessageEntity) = chatDao.insertMessage(message)
    suspend fun deleteConversation(convId: String) {
        chatDao.deleteMessagesForConversation(convId)
        chatDao.deleteConversationById(convId)
    }

    suspend fun sendTutorMessage(
        convId: String,
        userText: String,
        history: List<ChatMessageEntity>,
        settings: TutorSettings,
        documentContext: String? = null
    ): Result<String> {
        val userMsg = ChatMessageEntity(
            conversationId = convId,
            role = "user",
            content = userText
        )
        chatDao.insertMessage(userMsg)

        val service = getActiveAiService()
        val aiResult = service.tutorChat(history, userText, settings, documentContext)

        if (aiResult.isSuccess) {
            val replyText = aiResult.getOrNull() ?: ""
            val assistantMsg = ChatMessageEntity(
                conversationId = convId,
                role = "assistant",
                content = replyText
            )
            chatDao.insertMessage(assistantMsg)
            // Update conversation time
            chatDao.getConversationById(convId)?.let {
                chatDao.updateConversation(it.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        return aiResult
    }

    // --- Quizzes ---
    val allQuizzes: Flow<List<QuizEntity>> = quizDao.getAllQuizzes()
    val completedQuizzes: Flow<List<QuizEntity>> = quizDao.getCompletedQuizzes()
    suspend fun getQuiz(id: String) = quizDao.getQuizById(id)
    suspend fun saveQuiz(quiz: QuizEntity) = quizDao.insertQuiz(quiz)
    suspend fun updateQuiz(quiz: QuizEntity) = quizDao.updateQuiz(quiz)
    suspend fun deleteQuiz(id: String) = quizDao.deleteQuizById(id)

    suspend fun completeQuizSubmission(
        quizId: String,
        questions: List<QuizQuestion>,
        timeTakenSeconds: Int
    ): QuizEntity? {
        val quiz = quizDao.getQuizById(quizId) ?: return null
        val correctCount = questions.count { it.isCorrect() }
        val scorePercent = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0

        val updated = quiz.copy(
            questionsJson = QuizQuestion.toJsonString(questions),
            correctAnswers = correctCount,
            scorePercentage = scorePercent,
            timeTakenSeconds = timeTakenSeconds,
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        quizDao.updateQuiz(updated)

        // Log study session for quiz
        val mins = (timeTakenSeconds / 60).coerceAtLeast(1)
        studySessionDao.insertSession(
            StudySessionEntity(
                subjectId = quiz.subjectId,
                subjectName = quiz.subjectName,
                durationMinutes = mins,
                sessionType = "Quiz",
                notes = "Completed '${quiz.title}' with $scorePercent% score"
            )
        )
        return updated
    }

    // --- Flashcards ---
    val allDecks: Flow<List<FlashcardDeckEntity>> = flashcardDao.getAllDecks()
    suspend fun getDeck(id: String) = flashcardDao.getDeckById(id)
    suspend fun saveDeck(deck: FlashcardDeckEntity) = flashcardDao.insertDeck(deck)
    suspend fun deleteDeck(id: String) {
        flashcardDao.deleteCardsForDeck(id)
        flashcardDao.deleteDeckById(id)
    }

    fun getCardsForDeck(deckId: String): Flow<List<FlashcardEntity>> = flashcardDao.getCardsForDeck(deckId)
    fun getDueCards(deckId: String): Flow<List<FlashcardEntity>> =
        flashcardDao.getDueCardsForDeck(deckId, System.currentTimeMillis())
    val totalDueCardsCount: Flow<Int> = flashcardDao.getTotalDueCardsCount(System.currentTimeMillis())
    val totalCardsCount: Flow<Int> = flashcardDao.getTotalCardsCount()

    suspend fun saveFlashcard(card: FlashcardEntity) {
        flashcardDao.insertCard(card)
        // update deck count
        flashcardDao.getDeckById(card.deckId)?.let { deck ->
            flashcardDao.updateDeck(deck.copy(cardCount = deck.cardCount + 1))
        }
    }

    suspend fun reviewFlashcard(card: FlashcardEntity, grade: FlashcardGrade): FlashcardEntity {
        val updated = SpacedRepetitionScheduler.review(card, grade)
        flashcardDao.updateCard(updated)
        return updated
    }

    // --- Planner Tasks ---
    val allTasks: Flow<List<PlannerTaskEntity>> = plannerDao.getAllTasks()
    val pendingTasks: Flow<List<PlannerTaskEntity>> = plannerDao.getPendingTasks()
    suspend fun saveTask(task: PlannerTaskEntity) = plannerDao.insertTask(task)
    suspend fun updateTask(task: PlannerTaskEntity) = plannerDao.updateTask(task)
    suspend fun deleteTask(id: String) = plannerDao.deleteTaskById(id)

    // --- Study Sessions & Analytics ---
    val allSessions: Flow<List<StudySessionEntity>> = studySessionDao.getAllSessions()
    fun getSessionsSince(since: Long) = studySessionDao.getSessionsSince(since)
    fun getTotalMinutesSince(since: Long) = studySessionDao.getTotalMinutesSince(since)
    suspend fun logSession(session: StudySessionEntity) = studySessionDao.insertSession(session)

    // --- Summarizer, Quiz, Flashcard AI actions ---
    suspend fun generateSummary(title: String, content: String, type: String, length: String): Result<String> {
        val service = getActiveAiService()
        return service.summarizeDocument(title, content, type, length)
    }

    suspend fun generateQuizFromTopic(
        subject: SubjectEntity?,
        topic: String,
        count: Int,
        difficulty: String,
        type: String
    ): Result<QuizEntity> {
        val service = getActiveAiService()
        val questionsResult = service.generateQuiz(subject?.name ?: "General", topic, count, difficulty, type)

        return questionsResult.mapCatching { questions ->
            val quiz = QuizEntity(
                title = "$topic Quiz",
                subjectId = subject?.id,
                subjectName = subject?.name,
                topicOrDocTitle = topic,
                difficulty = difficulty,
                questionType = type,
                questionsJson = QuizQuestion.toJsonString(questions),
                totalQuestions = questions.size,
                isCompleted = false
            )
            quizDao.insertQuiz(quiz)
            quiz
        }
    }

    suspend fun generateFlashcardsForDeck(
        deckTitle: String,
        subject: SubjectEntity?,
        topicOrContent: String,
        count: Int
    ): Result<FlashcardDeckEntity> {
        val service = getActiveAiService()
        val cardsResult = service.generateFlashcards(topicOrContent, count)

        return cardsResult.mapCatching { pairs ->
            val deck = FlashcardDeckEntity(
                title = deckTitle,
                subjectId = subject?.id,
                subjectName = subject?.name,
                description = "Generated flashcards for $topicOrContent",
                cardCount = pairs.size
            )
            flashcardDao.insertDeck(deck)

            pairs.forEach { (front, back) ->
                flashcardDao.insertCard(
                    FlashcardEntity(
                        deckId = deck.id,
                        front = front,
                        back = back
                    )
                )
            }
            deck
        }
    }

    suspend fun generateStudyPlan(
        subjects: String,
        examDateText: String,
        hoursPerDay: Float,
        syllabus: String
    ): Result<String> {
        val service = getActiveAiService()
        return service.generateStudyPlan(subjects, examDateText, hoursPerDay, syllabus)
    }
}
