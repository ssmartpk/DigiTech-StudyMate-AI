package com.example.ai

import com.example.data.local.entity.ChatMessageEntity
import com.example.data.model.QuizQuestion
import com.example.data.model.TutorSettings
import kotlinx.coroutines.delay
import java.util.UUID

class MockAiService : AiService {

    override suspend fun tutorChat(
        history: List<ChatMessageEntity>,
        userMessage: String,
        settings: TutorSettings,
        documentContext: String?
    ): Result<String> {
        delay(600) // realistic response feel
        val lower = userMessage.lowercase()

        val response = when {
            lower.contains("photosynthesis") -> """[Demo Mode / Offline Sample]
Hello! Let's understand **Photosynthesis** step by step.

### 1. The Core Concept
Photosynthesis is how green plants turn light into chemical energy (food).
- **Formula:** 6CO₂ + 6H₂O + Sunlight ➔ C₆H₁₂O₆ (Glucose) + 6O₂

### 2. The Two Stages
1. **Light Reactions (in Thylakoid membranes):** Sunlight splits water molecules into hydrogen ions and oxygen gas. ATP and NADPH energy packets are formed.
2. **Dark Reactions / Calvin Cycle (in Stroma):** The plant takes in CO₂ from the air and uses ATP/NADPH to build glucose sugar.

### 3. Analogy
Think of a solar-powered bakery: sunlight powers the mixer, water provides the moisture, air provides carbon flour, and glucose is the baked loaf!

${if (settings.includeRecap) "\n**Quick Check:** Can you name the gas released as a byproduct during the light reaction?" else ""}"""

            lower.contains("quadratic") || lower.contains("equation") -> """[Demo Mode / Offline Sample]
Let's break down **Quadratic Equations** simply!

### 1. Standard Form
A quadratic equation always looks like:
ax² + bx + c = 0 (where a ≠ 0).

### 2. The Master Formula
When factoring is difficult, use the quadratic formula:
x = (-b ± √(b² - 4ac)) / (2a)

### 3. Step-by-Step Example
Solve: x² - 5x + 6 = 0
- Coefficients: a = 1, b = -5, c = 6.
- Discriminant: b² - 4ac = (-5)² - 4(1)(6) = 25 - 24 = 1.
- Plug in: x = (-(-5) ± √1) / (2 * 1) = (5 ± 1) / 2.
- Therefore: x = 3 or x = 2.

${if (settings.includeRecap) "\n**Recap:** What does the discriminant (b² - 4ac) tell us about the roots?" else ""}"""

            documentContext != null -> """[Demo Mode / Document-Grounded Answer]
Based on your uploaded study material:

### Key Points from Document
1. **Core Insight:** The document highlights foundational principles and key structural formulas.
2. **Application:** Practical examples emphasize applying these concepts to solve exam questions.
3. **Important Note:** Pay special attention to the definitions and recurring terminology highlighted in your notes.

Would you like me to generate a 5-question practice quiz directly from this document?"""

            else -> """[Demo Mode / Offline Sample]
Hello! I'm DigiTech StudyMate AI, your personal tutor.

### Step-by-Step Explanation
You asked about: **$userMessage**

1. **Fundamental Principle:** In ${settings.explanationLevel} level study, we always start by defining our terms clearly.
2. **Detailed Breakdown:** Every complex topic is composed of smaller, simpler building blocks. By understanding each component individually, the whole system becomes intuitive.
3. **Practical Application:** Always test your understanding by explaining the concept in your own words or applying it to a sample problem.

${if (settings.includeExamples) "### Example\nConsider how this applies in everyday scenarios or typical exam problems to reinforce your intuition.\n" else ""}
${if (settings.includeRecap) "### Short Recap\nMaster the core rules first, verify with a worked example, and test yourself with flashcards!" else ""}"""
        }

        return Result.success(response)
    }

    override suspend fun summarizeDocument(
        title: String,
        content: String,
        summaryType: String,
        length: String
    ): Result<String> {
        delay(700)
        val summary = """[Demo Mode Summary: $summaryType]
# $title — Comprehensive Revision Summary

### 1. Executive Overview
This document contains essential study material focused on core definitions, formulas, and structural relationships required for mastery.

### 2. Main Concepts & Key Facts
- **Primary Mechanism:** Sequential processes driven by chemical and physical governing laws.
- **Critical Components:** Distinct stages that interact systematically to achieve equilibrium.
- **Important Rule:** Conservation principles apply across all energy transformations.

### 3. Essential Definitions
- **Core Process:** The primary active mechanism described throughout the text.
- **Catalyst / Driver:** The initiating agent that accelerates reactions without being consumed.
- **Equilibrium State:** The balanced condition achieved when opposing rates equalize.

### 4. Exam Revision Checklist
- [x] Memorize the 3 core principles.
- [x] Practice derivations without looking at the notes.
- [x] Review potential tricky multiple-choice distinctions.

### 5. Short Recap
Focus revision on the transition phases and fundamental equations. Test your knowledge with an interactive quiz!"""
        return Result.success(summary)
    }

    override suspend fun generateQuiz(
        subjectName: String,
        topicOrContent: String,
        questionCount: Int,
        difficulty: String,
        questionType: String
    ): Result<List<QuizQuestion>> {
        delay(800)
        val questions = mutableListOf<QuizQuestion>()

        questions.add(
            QuizQuestion(
                questionText = "In $topicOrContent, what is the primary fundamental principle involved?",
                options = listOf(
                    "Conservation of energy and momentum",
                    "Random non-deterministic variation",
                    "Spontaneous irreversible loss of matter",
                    "Absolute static immobility"
                ),
                correctAnswerIndex = 0,
                correctAnswerText = "Conservation of energy and momentum",
                explanation = "Fundamental laws dictate that total energy remains constant across isolated closed systems.",
                difficulty = difficulty
            )
        )

        questions.add(
            QuizQuestion(
                questionText = "Which of the following is considered an essential requirement for $topicOrContent to occur efficiently?",
                options = listOf(
                    "High activation barrier with no catalyst",
                    "Sufficient reactant concentration and favorable conditions",
                    "Complete absence of all thermal energy",
                    "External constant interference"
                ),
                correctAnswerIndex = 1,
                correctAnswerText = "Sufficient reactant concentration and favorable conditions",
                explanation = "Optimal reactant concentration and conditions allow the reaction or mechanism to proceed effectively.",
                difficulty = difficulty
            )
        )

        questions.add(
            QuizQuestion(
                questionText = "True or False: In $topicOrContent, the overall system entropy always decreases spontaneously without energy input.",
                options = listOf("True", "False"),
                correctAnswerIndex = 1,
                correctAnswerText = "False",
                explanation = "By the Second Law of Thermodynamics, entropy of an isolated system never decreases spontaneously.",
                difficulty = difficulty
            )
        )

        if (questionCount >= 4) {
            questions.add(
                QuizQuestion(
                    questionText = "What is the standard unit of measurement associated with the primary variable in $topicOrContent?",
                    options = listOf("Joules (J)", "Newtons (N)", "Watts (W)", "Pascals (Pa)"),
                    correctAnswerIndex = 0,
                    correctAnswerText = "Joules (J)",
                    explanation = "Energy and work quantities are measured in Joules (SI standard).",
                    difficulty = difficulty
                )
            )
        }

        if (questionCount >= 5) {
            questions.add(
                QuizQuestion(
                    questionText = "When solving a problem regarding $topicOrContent, what is the recommended first step?",
                    options = listOf(
                        "Immediately guess a numerical value",
                        "Identify the known variables and formulate the governing equation",
                        "Disregard standard units",
                        "Skip coordinate reference points"
                    ),
                    correctAnswerIndex = 1,
                    correctAnswerText = "Identify the known variables and formulate the governing equation",
                    explanation = "Establishing knowns, unknowns, and appropriate equations ensures structured and accurate problem solving.",
                    difficulty = difficulty
                )
            )
        }

        return Result.success(questions.take(questionCount))
    }

    override suspend fun generateFlashcards(
        topicOrContent: String,
        cardCount: Int
    ): Result<List<Pair<String, String>>> {
        delay(600)
        val cards = listOf(
            Pair(
                "What is the primary definition of $topicOrContent?",
                "The scientific study or systematic process governing structure, behavior, and quantitative relationships in this domain."
            ),
            Pair(
                "What are the main governing equations in $topicOrContent?",
                "The core formulas relating initial conditions, rate of change, and final observed outcomes."
            ),
            Pair(
                "Give an everyday example illustrating $topicOrContent.",
                "How energy converts from potential to kinetic when water flows down a hydroelectric dam."
            ),
            Pair(
                "What is the most common student pitfall in this topic?",
                "Confusing scalar quantities with directional vectors or failing to convert units into standard SI units."
            ),
            Pair(
                "How do you verify your solution in $topicOrContent?",
                "Perform dimensional analysis (unit check) and ensure the order of magnitude makes physical sense."
            )
        )
        return Result.success(cards.take(cardCount))
    }

    override suspend fun generateStudyPlan(
        subjects: String,
        examDateText: String,
        availableHoursPerDay: Float,
        topicsOrSyllabus: String
    ): Result<String> {
        delay(700)
        val plan = """[Demo Mode Generated Study Plan]
# Personalized Revision Schedule for $subjects

### Target Exam Timeline
- **Exam Date:** $examDateText
- **Available Time:** $availableHoursPerDay hours / day

---

### Daily Schedule Breakdown
- **08:00 - 08:50 (Session 1):** High-priority theory & concept building ($subjects).
- **08:50 - 09:00:** 10-minute active break (hydrate, stretch).
- **09:00 - 09:50 (Session 2):** Problem solving and formula application.
- **09:50 - 10:00:** Short break.
- **10:00 - 10:30 (Session 3):** Spaced-repetition flashcard drill and quiz review.

---

### Weekly Milestones
1. **Days 1 - 2:** Foundation & Definitions. Complete 20 flashcards.
2. **Days 3 - 4:** Intensive problem sets & worked examples.
3. **Day 5:** Timed MCQ and short-answer practice test.
4. **Day 6:** Targeted review of incorrect answers and weak topics.
5. **Day 7:** Active recall and light recap."""
        return Result.success(plan)
    }

    override suspend fun explainTopic(topic: String, settings: TutorSettings): Result<String> {
        return tutorChat(emptyList(), "Explain $topic in detail", settings, null)
    }

    override suspend fun testConnection(modelId: String): Result<String> {
        delay(300)
        return Result.success("[Demo Mode Simulation] Successfully connected to mock educational engine. Switch to Live AI mode in Settings to connect to Google Gemini API.")
    }
}
