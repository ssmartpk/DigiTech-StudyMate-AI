package com.example.data.model

import com.example.data.local.entity.FlashcardEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val correctAnswerText: String = "",
    val explanation: String = "",
    val difficulty: String = "Medium",
    var userSelectedIndex: Int? = null,
    var userAnswerText: String? = null
) {
    fun isAnswered(): Boolean = userSelectedIndex != null || !userAnswerText.isNullOrBlank()

    fun isCorrect(): Boolean {
        return if (options.isNotEmpty() && userSelectedIndex != null) {
            userSelectedIndex == correctAnswerIndex
        } else if (!userAnswerText.isNullOrBlank()) {
            userAnswerText!!.trim().equals(correctAnswerText.trim(), ignoreCase = true)
        } else {
            false
        }
    }

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("questionText", questionText)
        val optArray = JSONArray()
        options.forEach { optArray.put(it) }
        obj.put("options", optArray)
        obj.put("correctAnswerIndex", correctAnswerIndex)
        obj.put("correctAnswerText", correctAnswerText)
        obj.put("explanation", explanation)
        obj.put("difficulty", difficulty)
        if (userSelectedIndex != null) obj.put("userSelectedIndex", userSelectedIndex!!)
        if (userAnswerText != null) obj.put("userAnswerText", userAnswerText)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): QuizQuestion {
            val optArray = obj.optJSONArray("options")
            val options = mutableListOf<String>()
            if (optArray != null) {
                for (i in 0 until optArray.length()) {
                    options.add(optArray.getString(i))
                }
            }
            return QuizQuestion(
                id = obj.optString("id", UUID.randomUUID().toString()),
                questionText = obj.optString("questionText", ""),
                options = options,
                correctAnswerIndex = obj.optInt("correctAnswerIndex", 0),
                correctAnswerText = obj.optString("correctAnswerText", ""),
                explanation = obj.optString("explanation", ""),
                difficulty = obj.optString("difficulty", "Medium"),
                userSelectedIndex = if (obj.has("userSelectedIndex")) obj.getInt("userSelectedIndex") else null,
                userAnswerText = if (obj.has("userAnswerText")) obj.getString("userAnswerText") else null
            )
        }

        fun parseQuestionsJson(jsonStr: String): List<QuizQuestion> {
            val list = mutableListOf<QuizQuestion>()
            if (jsonStr.isBlank()) return list
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Return empty if parsing error
            }
            return list
        }

        fun toJsonString(questions: List<QuizQuestion>): String {
            val array = JSONArray()
            questions.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}

enum class FlashcardGrade(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4)
}

object SpacedRepetitionScheduler {
    private const val ONE_DAY_MILLIS = 86400000L

    /**
     * Standard SM-2 inspired calculation:
     * grade: 1 (Again), 2 (Hard), 3 (Good), 4 (Easy)
     */
    fun review(card: FlashcardEntity, grade: FlashcardGrade): FlashcardEntity {
        var reps = card.repetitions
        var interval = card.intervalDays
        var ef = card.easeFactor

        when (grade) {
            FlashcardGrade.AGAIN -> {
                reps = 0
                interval = 1
                ef = (ef - 0.2f).coerceAtLeast(1.3f)
            }
            FlashcardGrade.HARD -> {
                reps += 1
                interval = (interval * 1.2f).toInt().coerceAtLeast(1)
                ef = (ef - 0.15f).coerceAtLeast(1.3f)
            }
            FlashcardGrade.GOOD -> {
                reps += 1
                interval = if (reps == 1) 1 else if (reps == 2) 3 else (interval * ef).toInt()
            }
            FlashcardGrade.EASY -> {
                reps += 1
                interval = if (reps == 1) 2 else if (reps == 2) 5 else (interval * ef * 1.3f).toInt()
                ef = (ef + 0.15f).coerceAtMost(3.0f)
            }
        }

        val now = System.currentTimeMillis()
        val nextReview = now + (interval * ONE_DAY_MILLIS)

        return card.copy(
            repetitions = reps,
            intervalDays = interval,
            easeFactor = ef,
            nextReviewTimestamp = nextReview,
            lastReviewedTimestamp = now
        )
    }
}

data class TutorSettings(
    val explanationLevel: String = "School",
    val responseLength: String = "Balanced",
    val teachingStyle: String = "Step-by-step",
    val preferredLanguage: String = "English",
    val includeExamples: Boolean = true,
    val includeRecap: Boolean = true
)
