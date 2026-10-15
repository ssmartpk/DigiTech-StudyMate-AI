package com.example.data.local

import com.example.data.local.entity.*
import com.example.data.model.FlashcardGrade
import com.example.data.model.QuizQuestion
import com.example.data.model.SpacedRepetitionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSeeder {
    suspend fun seedDemoDataIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        val subjectDao = db.subjectDao()
        val documentDao = db.documentDao()
        val chatDao = db.chatDao()
        val quizDao = db.quizDao()
        val flashcardDao = db.flashcardDao()
        val plannerDao = db.plannerDao()
        val sessionDao = db.studySessionDao()

        // Check if database already has subjects
        val existingSubject = subjectDao.getSubjectById("sub-bio-1")
        if (existingSubject != null) return@withContext

        // 1. Subjects
        val bioSubject = SubjectEntity(
            id = "sub-bio-1",
            name = "Cellular Biology",
            description = "Cell structures, photosynthesis, cellular respiration, and DNA replication.",
            colorHex = "#2E7D32",
            iconName = "Biotech",
            studyTargetMinutes = 90
        )
        val physicsSubject = SubjectEntity(
            id = "sub-phy-2",
            name = "Physics: Mechanics",
            description = "Newton's laws of motion, kinematics, work, energy, and momentum.",
            colorHex = "#1565C0",
            iconName = "Speed",
            studyTargetMinutes = 120
        )
        val csSubject = SubjectEntity(
            id = "sub-cs-3",
            name = "Computer Science",
            description = "Data structures, algorithms, sorting algorithms, and complexity analysis.",
            colorHex = "#6A1B9A",
            iconName = "Code",
            studyTargetMinutes = 60
        )

        subjectDao.insertSubject(bioSubject)
        subjectDao.insertSubject(physicsSubject)
        subjectDao.insertSubject(csSubject)

        // 2. Study Documents & Notes
        val doc1 = DocumentEntity(
            id = "doc-bio-summary",
            title = "Photosynthesis & Light Reactions Overview",
            subjectId = bioSubject.id,
            subjectName = bioSubject.name,
            fileType = "NOTE",
            extractedText = """Photosynthesis occurs in plant chloroplasts and consists of two main stages:
1. Light-Dependent Reactions: Take place in thylakoid membranes. Chlorophyll absorbs solar photon energy, exciting electrons. Water molecules (H2O) are photolysed into electrons, protons, and oxygen gas (O2). ATP and NADPH are synthesized to store chemical energy.
2. Light-Independent Reactions (Calvin Cycle): Take place in the stroma. Ribulose 1,5-bisphosphate carboxylase-oxygenase (RuBisCO) fixes atmospheric carbon dioxide (CO2) into 3-PGA, which is reduced using ATP and NADPH into Glyceraldehyde 3-phosphate (G3P) for glucose synthesis.""",
            summaryText = """Key Concepts:
- Location: Chloroplast (Thylakoids & Stroma).
- Reactants: 6CO2 + 6H2O + Light Energy.
- Products: C6H12O6 (Glucose) + 6O2.
- Crucial Enzyme: RuBisCO in the Calvin Cycle.
- Energy Carriers: ATP and NADPH produced in light reactions are consumed in dark reactions.""",
            status = "Ready"
        )

        val doc2 = DocumentEntity(
            id = "doc-physics-formula",
            title = "Kinematics & Newton's Laws Formula Guide",
            subjectId = physicsSubject.id,
            subjectName = physicsSubject.name,
            fileType = "NOTE",
            extractedText = """Kinematic Equations (Constant Acceleration a):
1. v = u + at
2. s = ut + (1/2)at^2
3. v^2 = u^2 + 2as
4. s = ((u + v) / 2) * t

Newton's Three Laws:
1. First Law (Inertia): An object remains at rest or continues at constant velocity unless acted upon by a net external force.
2. Second Law (Momentum & Force): F_net = m * a (Force equals mass times acceleration).
3. Third Law (Action-Reaction): For every action force, there is an equal and opposite reaction force.""",
            summaryText = """Essential Takeaways:
- Linear Kinematics: 4 equations relating displacement (s), initial velocity (u), final velocity (v), acceleration (a), and time (t).
- F = ma is fundamental in dynamics.
- Always establish a coordinate direction (+/-) before solving vector problems.""",
            status = "Ready"
        )

        documentDao.insertDocument(doc1)
        documentDao.insertDocument(doc2)

        // 3. Conversations
        val convId = "conv-demo-1"
        val conv = ConversationEntity(
            id = convId,
            title = "Explaining Photosynthesis Simply",
            subjectId = bioSubject.id
        )
        chatDao.insertConversation(conv)

        val msg1 = ChatMessageEntity(
            conversationId = convId,
            role = "user",
            content = "Can you explain photosynthesis in very simple words with an everyday analogy?"
        )
        val msg2 = ChatMessageEntity(
            conversationId = convId,
            role = "assistant",
            content = """[Demo Mode Example Response]
Hello! Let's think of photosynthesis as **nature's solar-powered bakery**:

### 1. The Solar Kitchen (Chloroplasts)
The leaves of green plants are like little solar-powered bakeries. Inside their cells are tiny green kitchens called **chloroplasts**.

### 2. The Ingredients
To bake its food (glucose sugar), the plant needs three simple ingredients:
1. **Water (H₂O)**: Absorbed by the roots from the soil.
2. **Carbon Dioxide (CO₂)**: Taken in from the air through tiny leaf pores called *stomata*.
3. **Sunlight**: The solar energy that powers the kitchen oven!

### 3. The Baking Process
- **Step A (Catching the Light)**: Chlorophyll absorbs sunlight and uses its energy to crack water molecules open, releasing fresh **Oxygen** into the air for us to breathe!
- **Step B (Making the Treat)**: In the Calvin Cycle, the plant combines the trapped hydrogen with carbon dioxide to bake **Glucose** (plant food and energy).

### Quick Recap
**Sunlight + Water + Carbon Dioxide ➔ Glucose + Oxygen**

Would you like a quick practice question to test what you just learned?"""
        )
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)

        // 4. Sample Quizzes
        val questions = listOf(
            QuizQuestion(
                questionText = "Where do the light-dependent reactions of photosynthesis take place?",
                options = listOf("Thylakoid membranes", "Stroma", "Mitochondrial matrix", "Cytoplasm"),
                correctAnswerIndex = 0,
                correctAnswerText = "Thylakoid membranes",
                explanation = "Light-dependent reactions occur in the thylakoid membranes of chloroplasts where chlorophyll pigments absorb sunlight."
            ),
            QuizQuestion(
                questionText = "Which gas is released into the atmosphere as a byproduct of water photolysis in photosynthesis?",
                options = listOf("Carbon Dioxide (CO2)", "Nitrogen (N2)", "Oxygen (O2)", "Methane (CH4)"),
                correctAnswerIndex = 2,
                correctAnswerText = "Oxygen (O2)",
                explanation = "When water molecules are split by light energy, electrons and protons are extracted, and O2 gas is released."
            ),
            QuizQuestion(
                questionText = "What is the primary enzyme responsible for carbon fixation in the Calvin Cycle?",
                options = listOf("DNA Polymerase", "RuBisCO", "Amylase", "ATP Synthase"),
                correctAnswerIndex = 1,
                correctAnswerText = "RuBisCO",
                explanation = "RuBisCO (Ribulose-1,5-bisphosphate carboxylase-oxygenase) catalyzes the attachment of CO2 to RuBP."
            ),
            QuizQuestion(
                questionText = "True or False: The Calvin Cycle can only occur when direct sunlight is hitting the leaf.",
                options = listOf("True", "False"),
                correctAnswerIndex = 1,
                correctAnswerText = "False",
                explanation = "The Calvin Cycle is light-independent; it uses stored chemical energy (ATP & NADPH) and can proceed in the dark as long as those carriers remain available."
            )
        )

        val sampleQuiz = QuizEntity(
            id = "quiz-bio-1",
            title = "Photosynthesis Mastery Quiz",
            subjectId = bioSubject.id,
            subjectName = bioSubject.name,
            topicOrDocTitle = "Photosynthesis & Light Reactions",
            difficulty = "Medium",
            questionType = "MCQ",
            questionsJson = QuizQuestion.toJsonString(questions),
            totalQuestions = questions.size,
            isCompleted = false
        )
        quizDao.insertQuiz(sampleQuiz)

        // 5. Flashcards Deck
        val deck1 = FlashcardDeckEntity(
            id = "deck-physics-1",
            title = "Kinematics & Dynamics Fundamentals",
            subjectId = physicsSubject.id,
            subjectName = physicsSubject.name,
            description = "Key formulas, definitions, and Newton's laws.",
            cardCount = 4
        )
        flashcardDao.insertDeck(deck1)

        val cards = listOf(
            FlashcardEntity(
                deckId = deck1.id,
                front = "State Newton's First Law of Motion",
                back = "An object remains at rest or in uniform motion along a straight line unless acted upon by a net external force (Law of Inertia)."
            ),
            FlashcardEntity(
                deckId = deck1.id,
                front = "What is the formula for final velocity given u, a, and s?",
                back = "v² = u² + 2as\n(where v = final velocity, u = initial velocity, a = acceleration, s = displacement)."
            ),
            FlashcardEntity(
                deckId = deck1.id,
                front = "Define Acceleration and its SI unit",
                back = "Acceleration is the rate of change of velocity with respect to time: a = Δv / Δt. Its SI unit is meters per second squared (m/s²)."
            ),
            FlashcardEntity(
                deckId = deck1.id,
                front = "What does Newton's Third Law state?",
                back = "When one body exerts a force on a second body, the second body simultaneously exerts a force equal in magnitude and opposite in direction on the first body."
            )
        )
        cards.forEach { flashcardDao.insertCard(it) }

        // 6. Planner Tasks
        val now = System.currentTimeMillis()
        val oneHour = 3600000L
        plannerDao.insertTask(
            PlannerTaskEntity(
                title = "Review Photosynthesis Calvin Cycle Steps",
                subjectId = bioSubject.id,
                subjectName = bioSubject.name,
                dateMillis = now + 2 * oneHour,
                durationMinutes = 45,
                priority = "High",
                isCompleted = false,
                isRevision = true
            )
        )
        plannerDao.insertTask(
            PlannerTaskEntity(
                title = "Solve 5 Newton's 2nd Law Kinematics Problems",
                subjectId = physicsSubject.id,
                subjectName = physicsSubject.name,
                dateMillis = now + 5 * oneHour,
                durationMinutes = 60,
                priority = "Medium",
                isCompleted = false
            )
        )

        // 7. Study Sessions History (for statistics)
        sessionDao.insertSession(
            StudySessionEntity(
                subjectId = bioSubject.id,
                subjectName = bioSubject.name,
                durationMinutes = 25,
                sessionType = "Pomodoro",
                notes = "Light reactions review",
                timestamp = now - 86400000L // yesterday
            )
        )
        sessionDao.insertSession(
            StudySessionEntity(
                subjectId = physicsSubject.id,
                subjectName = physicsSubject.name,
                durationMinutes = 30,
                sessionType = "Flashcard",
                notes = "Mechanics formulas drill",
                timestamp = now - 1800000L // 30 mins ago today
            )
        )
    }
}
