package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val topicOrDocTitle: String,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val questionType: String = "MCQ", // MCQ, TRUE_FALSE, SHORT_ANSWER
    val questionsJson: String,
    val totalQuestions: Int,
    val correctAnswers: Int = 0,
    val scorePercentage: Int = 0,
    val timeTakenSeconds: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val description: String? = null,
    val cardCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val nextReviewTimestamp: Long = System.currentTimeMillis(),
    val lastReviewedTimestamp: Long? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "planner_tasks")
data class PlannerTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val dateMillis: Long,
    val durationMinutes: Int = 30,
    val priority: String = "Medium", // Low, Medium, High
    val isCompleted: Boolean = false,
    val isRevision: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val subjectName: String? = null,
    val durationMinutes: Int,
    val sessionType: String = "Pomodoro", // Pomodoro, Quiz, Flashcard, Tutor, Reading
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
