package com.example

import com.example.ai.GeminiConfig
import com.example.ai.MockAiService
import com.example.data.local.entity.FlashcardEntity
import com.example.data.model.FlashcardGrade
import com.example.data.model.QuizQuestion
import com.example.data.model.SpacedRepetitionScheduler
import com.example.data.model.TutorSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyMateUnitTest {

    @Test
    fun testQuizQuestionScoring_correctAndIncorrect() {
        val q1 = QuizQuestion(
            questionText = "What is the powerhouse of the cell?",
            options = listOf("Ribosome", "Mitochondria", "Nucleus", "Vacuole"),
            correctAnswerIndex = 1,
            correctAnswerText = "Mitochondria",
            explanation = "Mitochondria produce ATP."
        )

        // Unanswered
        assertFalse(q1.isAnswered())
        assertFalse(q1.isCorrect())

        // User selects correct index
        q1.userSelectedIndex = 1
        assertTrue(q1.isAnswered())
        assertTrue(q1.isCorrect())

        // User selects incorrect index
        q1.userSelectedIndex = 0
        assertFalse(q1.isCorrect())
    }

    @Test
    fun testQuizJsonSerializationAndParsing() {
        val questions = listOf(
            QuizQuestion(
                questionText = "Solve x: 2x = 8",
                options = listOf("2", "4", "6", "8"),
                correctAnswerIndex = 1,
                correctAnswerText = "4",
                explanation = "8 divided by 2 is 4."
            ),
            QuizQuestion(
                questionText = "True or False: Water is composed of hydrogen and oxygen.",
                options = listOf("True", "False"),
                correctAnswerIndex = 0,
                correctAnswerText = "True",
                explanation = "H2O has two hydrogen and one oxygen atoms."
            )
        )

        val jsonString = QuizQuestion.toJsonString(questions)
        assertTrue(jsonString.isNotBlank())

        val parsed = QuizQuestion.parseQuestionsJson(jsonString)
        assertEquals(2, parsed.size)
        assertEquals("Solve x: 2x = 8", parsed[0].questionText)
        assertEquals(1, parsed[0].correctAnswerIndex)
        assertEquals("True or False: Water is composed of hydrogen and oxygen.", parsed[1].questionText)
    }

    @Test
    fun testSpacedRepetitionScheduler_againResetsInterval() {
        val initialCard = FlashcardEntity(
            deckId = "deck-1",
            front = "Newton's 2nd Law",
            back = "F = ma",
            intervalDays = 6,
            easeFactor = 2.5f,
            repetitions = 3
        )

        val reviewed = SpacedRepetitionScheduler.review(initialCard, FlashcardGrade.AGAIN)

        // AGAIN resets repetitions to 0 and interval to 1
        assertEquals(0, reviewed.repetitions)
        assertEquals(1, reviewed.intervalDays)
        assertTrue(reviewed.easeFactor < 2.5f)
        assertTrue(reviewed.nextReviewTimestamp > System.currentTimeMillis())
    }

    @Test
    fun testSpacedRepetitionScheduler_easyIncreasesIntervalAndEase() {
        val initialCard = FlashcardEntity(
            deckId = "deck-1",
            front = "Speed of light",
            back = "3 x 10^8 m/s",
            intervalDays = 3,
            easeFactor = 2.5f,
            repetitions = 2
        )

        val reviewed = SpacedRepetitionScheduler.review(initialCard, FlashcardGrade.EASY)

        assertEquals(3, reviewed.repetitions)
        assertTrue(reviewed.intervalDays > 3)
        assertTrue(reviewed.easeFactor > 2.5f)
    }

    @Test
    fun testMockAiService_tutorChatAndQuiz() = runBlocking {
        val aiService = MockAiService()
        val settings = TutorSettings(
            explanationLevel = "School",
            responseLength = "Balanced",
            teachingStyle = "Step-by-step",
            preferredLanguage = "English"
        )

        val tutorResult = aiService.tutorChat(
            history = emptyList(),
            userMessage = "Explain photosynthesis in simple words",
            settings = settings
        )
        assertTrue(tutorResult.isSuccess)
        val text = tutorResult.getOrNull() ?: ""
        assertTrue(text.contains("Demo Mode"))
        assertTrue(text.contains("Photosynthesis"))

        val quizResult = aiService.generateQuiz(
            subjectName = "Biology",
            topicOrContent = "Photosynthesis",
            questionCount = 4,
            difficulty = "Medium",
            questionType = "MCQ"
        )
        assertTrue(quizResult.isSuccess)
        val questions = quizResult.getOrNull() ?: emptyList()
        assertEquals(4, questions.size)
        assertTrue(questions[0].options.isNotEmpty())
    }

    @Test
    fun testDocumentSizeValidation() {
        val maxAllowed = 15 * 1024 * 1024L
        val oversizedFile = 16 * 1024 * 1024L
        val validFile = 2 * 1024 * 1024L

        assertTrue(validFile <= maxAllowed)
        assertTrue(oversizedFile > maxAllowed)
    }

    @Test
    fun testSpacedRepetitionScheduler_hardAndGoodGrades() {
        val card = FlashcardEntity(
            deckId = "deck-1",
            front = "DNA Base pairs",
            back = "Adenine-Thymine, Guanine-Cytosine",
            intervalDays = 2,
            easeFactor = 2.5f,
            repetitions = 1
        )

        val hardReview = SpacedRepetitionScheduler.review(card, FlashcardGrade.HARD)
        assertEquals(2, hardReview.repetitions)
        assertTrue(hardReview.easeFactor < 2.5f)

        val goodReview = SpacedRepetitionScheduler.review(card, FlashcardGrade.GOOD)
        assertEquals(2, goodReview.repetitions)
        assertEquals(3, goodReview.intervalDays) // 2nd rep for GOOD sets interval to 3
    }

    @Test
    fun testMockAiService_summarizeDocument() = runBlocking {
        val aiService = MockAiService()
        val summaryResult = aiService.summarizeDocument(
            title = "Kinematics",
            content = "v = u + at, s = ut + 0.5at^2",
            summaryType = "Exam Revision Notes",
            length = "Medium"
        )
        assertTrue(summaryResult.isSuccess)
        val summaryText = summaryResult.getOrNull() ?: ""
        assertTrue(summaryText.contains("Kinematics"))
        assertTrue(summaryText.contains("Core Concepts") || summaryText.contains("Executive Overview"))
    }

    @Test
    fun testGeminiConfig_defaultModelIsGemini4Argon() {
        assertEquals("gemini-4-argon", GeminiConfig.activeModelId)
        assertTrue(GeminiConfig.VERIFIED_MODELS.contains("gemini-4-argon"))
    }

    @Test
    fun testMockAiService_testConnection() = runBlocking {
        val aiService = MockAiService()
        val result = aiService.testConnection("gemini-4-argon")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("Demo Mode") == true)
    }
}
