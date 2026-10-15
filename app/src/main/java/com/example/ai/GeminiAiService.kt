package com.example.ai

import com.example.BuildConfig
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.model.QuizQuestion
import com.example.data.model.TutorSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiAiService(
    private val customApiKeyProvider: () -> String
) : AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(GeminiConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(GeminiConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(GeminiConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private fun getResolvedApiKey(): String {
        val customKey = customApiKeyProvider()
        if (customKey.isNotBlank()) return customKey.trim()

        // Fallback to BuildConfig if configured
        try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = keyField.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") {
                return key.trim()
            }
        } catch (_: Exception) {}
        return ""
    }

    private suspend fun callGeminiApi(
        contents: JSONArray,
        systemInstructionText: String? = null,
        temperature: Double = 0.7
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings or run in Demo Mode.")
            )
        }

        val modelId = GeminiConfig.activeModelId
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val rootJson = JSONObject()
        rootJson.put("contents", contents)

        if (!systemInstructionText.isNullOrBlank()) {
            val sysInstObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemInstructionText))
            sysInstObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysInstObj)
        }

        val genConfig = JSONObject()
        genConfig.put("temperature", temperature)
        rootJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = rootJson.toString().toRequestBody(mediaType)

        var lastException: Exception? = null
        for (attempt in 0..GeminiConfig.MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(responseBody).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }

                    if (response.code == 429) {
                        return@withContext Result.failure(
                            IllegalStateException("Quota exceeded for $modelId (HTTP 429). Google Gemini API verified that model '$modelId' exists, but free-tier quota is 0 requests/day for this API project. You can switch to gemini-2.5-flash or use Demo Mode in Settings.")
                        )
                    } else if (response.code == 404) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Model '$modelId' was not found or is unavailable for this API project. You can adjust the Model ID in DigiTech StudyMate Settings.")
                        )
                    } else if (response.code == 400 || response.code == 403) {
                        return@withContext Result.failure(IllegalArgumentException(errorMsg))
                    }

                    if (attempt < GeminiConfig.MAX_RETRIES) {
                        delay(1000L * (attempt + 1))
                        continue
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                return@withContext Result.failure(IllegalStateException("No valid response text returned from Gemini API."))
            } catch (e: Exception) {
                lastException = e
                if (attempt < GeminiConfig.MAX_RETRIES) {
                    delay(1000L * (attempt + 1))
                }
            }
        }

        return@withContext Result.failure(lastException ?: IOException("Failed to communicate with Gemini API."))
    }

    override suspend fun tutorChat(
        history: List<ChatMessageEntity>,
        userMessage: String,
        settings: TutorSettings,
        documentContext: String?
    ): Result<String> {
        val contentsArray = JSONArray()

        // Include recent history (up to last 10 turns to preserve tokens)
        val recent = history.takeLast(10)
        for (msg in recent) {
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.role == "assistant") "model" else "user")
            val parts = JSONArray()
            parts.put(JSONObject().put("text", msg.content))
            contentObj.put("parts", parts)
            contentsArray.put(contentObj)
        }

        val promptBuilder = StringBuilder()
        if (!documentContext.isNullOrBlank()) {
            promptBuilder.append("STUDY MATERIAL CONTEXT:\n\"\"\"\n$documentContext\n\"\"\"\n\n")
            promptBuilder.append("STUDENT QUESTION (Answer based primarily on the study material above):\n")
        }
        promptBuilder.append(userMessage)

        val userObj = JSONObject()
        userObj.put("role", "user")
        val parts = JSONArray()
        parts.put(JSONObject().put("text", promptBuilder.toString()))
        userObj.put("parts", parts)
        contentsArray.put(userObj)

        val sysInstruction = "${GeminiConfig.TUTOR_SYSTEM_INSTRUCTION}\n" +
                "Education level: ${settings.explanationLevel}.\n" +
                "Response length: ${settings.responseLength}.\n" +
                "Teaching style: ${settings.teachingStyle}.\n" +
                "Preferred language: ${settings.preferredLanguage}.\n" +
                "Include examples: ${settings.includeExamples}.\n" +
                "Include recap: ${settings.includeRecap}."

        return callGeminiApi(contentsArray, sysInstruction, temperature = 0.6)
    }

    override suspend fun summarizeDocument(
        title: String,
        content: String,
        summaryType: String,
        length: String
    ): Result<String> {
        val prompt = """Create a structured educational summary of the following study material.
Summary format: $summaryType.
Length: $length.
Document Title: $title

DOCUMENT TEXT:
\"\"\"
$content
\"\"\"

Requirements:
1. Document overview.
2. Main concepts and numbered steps.
3. Important definitions & formulas (if applicable).
4. Concrete examples.
5. Potential exam revision points.
6. Short recap."""

        val contents = JSONArray().apply {
            put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        }
        return callGeminiApi(contents, GeminiConfig.TUTOR_SYSTEM_INSTRUCTION, temperature = 0.4)
    }

    override suspend fun generateQuiz(
        subjectName: String,
        topicOrContent: String,
        questionCount: Int,
        difficulty: String,
        questionType: String
    ): Result<List<QuizQuestion>> {
        val prompt = """Generate a quiz with exactly $questionCount questions based on the topic: "$topicOrContent" (Subject: $subjectName).
Difficulty: $difficulty.
Question Type: $questionType.

Respond ONLY with a valid JSON array of objects with no markdown code fences or other text.
Each JSON object must have:
- "questionText": string
- "options": array of 4 string choices (for MCQ or True/False)
- "correctAnswerIndex": integer (0 to 3)
- "correctAnswerText": string matching the correct option
- "explanation": string explaining why it is correct
- "difficulty": "$difficulty""""

        val contents = JSONArray().apply {
            put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        }

        val rawResult = callGeminiApi(contents, "You are a teacher and quiz generator. Output only valid JSON arrays.", temperature = 0.3)
        return rawResult.mapCatching { text ->
            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            QuizQuestion.parseQuestionsJson(cleanJson)
        }
    }

    override suspend fun generateFlashcards(
        topicOrContent: String,
        cardCount: Int
    ): Result<List<Pair<String, String>>> {
        val prompt = """Generate exactly $cardCount high-yield revision flashcards for the following topic:
"$topicOrContent"

Respond ONLY with a valid JSON array of objects.
Each object must have:
- "front": string (Question, term, or formula prompt)
- "back": string (Concise explanation, definition, or answer)"""

        val contents = JSONArray().apply {
            put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        }

        val rawResult = callGeminiApi(contents, "Output only valid JSON arrays for flashcards.", temperature = 0.4)
        return rawResult.mapCatching { text ->
            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = JSONArray(cleanJson)
            val list = mutableListOf<Pair<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Pair(obj.getString("front"), obj.getString("back")))
            }
            list
        }
    }

    override suspend fun generateStudyPlan(
        subjects: String,
        examDateText: String,
        availableHoursPerDay: Float,
        topicsOrSyllabus: String
    ): Result<String> {
        val prompt = """Create a balanced, realistic revision study plan.
Subjects: $subjects
Exam Date / Deadline: $examDateText
Available Study Time: $availableHoursPerDay hours per day
Topics/Syllabus: $topicsOrSyllabus

Include:
- Focus blocks and 10-minute rest intervals.
- Priority subject allocation.
- Spaced repetition and practice test days.
- Weekly milestones."""

        val contents = JSONArray().apply {
            put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        }
        return callGeminiApi(contents, GeminiConfig.TUTOR_SYSTEM_INSTRUCTION, temperature = 0.5)
    }

    override suspend fun explainTopic(topic: String, settings: TutorSettings): Result<String> {
        return tutorChat(emptyList(), "Please explain $topic step by step with an example.", settings, null)
    }

    override suspend fun testConnection(modelId: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No API key configured. Please enter a key in Settings."))
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        val testPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "ping")
                        })
                    })
                })
            })
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(testPayload.toString().toRequestBody(mediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(body)
                val replyText = json.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "OK"
                Result.success("Connection Successful! Model '$modelId' responded: '${replyText.trim()}'")
            } else {
                val errObj = try { JSONObject(body).optJSONObject("error") } catch (_: Exception) { null }
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                if (response.code == 429) {
                    Result.failure(IllegalStateException("HTTP 429 (Quota Exceeded): Model '$modelId' is verified on the Google Gemini API, but quota limit is 0 for this account on the free tier. Switching to gemini-2.5-flash or entering a paid key is recommended."))
                } else if (response.code == 404) {
                    Result.failure(IllegalArgumentException("HTTP 404: Model '$modelId' not found on the Google Gemini API."))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $errMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
