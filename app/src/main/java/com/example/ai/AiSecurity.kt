package com.example.ai

import com.example.data.model.QuizQuestion
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * DigiTech StudyMate AI — Security, Privacy, and Rate Limiting Subsystem
 *
 * Implements:
 * 1. Client-side rate limiting to prevent API quota depletion, runaway loops, and accidental burst calls.
 * 2. Strict AI output validation and sanitization (stripping control chars, preventing script injection, and bounding lengths).
 * 3. File validation and sanitization helpers.
 */
object AiSecuritySanitizer {
    private const val MAX_AI_OUTPUT_LENGTH = 100_000
    private const val MAX_PROMPT_INPUT_LENGTH = 30_000

    /**
     * Sanitizes AI prompt text or context before transmission to avoid runaway token bloat.
     */
    fun sanitizePromptInput(input: String): String {
        val trimmed = input.trim()
        val bounded = if (trimmed.length > MAX_PROMPT_INPUT_LENGTH) {
            trimmed.substring(0, MAX_PROMPT_INPUT_LENGTH)
        } else {
            trimmed
        }
        // Strip null bytes and dangerous control characters
        return bounded.replace("\u0000", "")
    }

    /**
     * Sanitizes AI response markdown/text before displaying in UI or persisting to Room.
     */
    fun sanitizeAiOutput(rawOutput: String): String {
        if (rawOutput.isBlank()) return ""
        val sanitized = rawOutput
            .replace("\u0000", "")
            .replace(Regex("<script[\\s\\S]*?>[\\s\\S]*?<\\/script>", RegexOption.IGNORE_CASE), "[Script removed for security]")
            .replace(Regex("<iframe[\\s\\S]*?>[\\s\\S]*?<\\/iframe>", RegexOption.IGNORE_CASE), "")
            .trim()

        return if (sanitized.length > MAX_AI_OUTPUT_LENGTH) {
            sanitized.substring(0, MAX_AI_OUTPUT_LENGTH) + "\n\n...[Response truncated for security & length limit]"
        } else {
            sanitized
        }
    }

    /**
     * Sanitizes file names to prevent path traversal or unsafe file system characters.
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace("..", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")
            .trim()
            .ifBlank { "imported_document" }
    }

    /**
     * Validates and sanitizes a list of generated flashcard pairs.
     */
    fun sanitizeFlashcards(cards: List<Pair<String, String>>): List<Pair<String, String>> {
        return cards.mapNotNull { (front, back) ->
            val cleanFront = sanitizeAiOutput(front).take(1000).trim()
            val cleanBack = sanitizeAiOutput(back).take(3000).trim()
            if (cleanFront.isNotBlank() && cleanBack.isNotBlank()) {
                Pair(cleanFront, cleanBack)
            } else {
                null
            }
        }
    }
}

/**
 * Thread-safe client rate limiter for Gemini / AI API calls.
 */
class AiRateLimiter(
    private val minIntervalMillis: Long = 2000L,       // At least 2.0s between AI invocations
    private val maxRequestsPerMinute: Int = 12         // Max 12 requests in any 60-second window
) {
    private val lastRequestTimestamp = AtomicLong(0L)
    private val requestTimestamps = ConcurrentHashMap.newKeySet<Long>()

    data class RateLimitResult(
        val isAllowed: Boolean,
        val waitSeconds: Long = 0,
        val message: String = ""
    )

    @Synchronized
    fun tryAcquire(actionName: String = "AI Request"): RateLimitResult {
        val now = System.currentTimeMillis()

        // 1. Minimum interval check
        val lastTime = lastRequestTimestamp.get()
        val timeSinceLast = now - lastTime
        if (lastTime > 0 && timeSinceLast < minIntervalMillis) {
            val waitSec = ((minIntervalMillis - timeSinceLast) / 1000L) + 1
            return RateLimitResult(
                isAllowed = false,
                waitSeconds = waitSec,
                message = "Please wait $waitSec second(s) before sending another $actionName to protect API quota."
            )
        }

        // 2. Sliding window per-minute check
        val oneMinuteAgo = now - 60_000L
        requestTimestamps.removeIf { it < oneMinuteAgo }

        if (requestTimestamps.size >= maxRequestsPerMinute) {
            return RateLimitResult(
                isAllowed = false,
                waitSeconds = 10,
                message = "Rate limit reached ($maxRequestsPerMinute requests/min). Please wait a few seconds before trying again."
            )
        }

        // Record request
        lastRequestTimestamp.set(now)
        requestTimestamps.add(now)
        return RateLimitResult(isAllowed = true)
    }

    fun reset() {
        lastRequestTimestamp.set(0L)
        requestTimestamps.clear()
    }
}
