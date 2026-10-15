package com.example.ai

/**
 * DigiTech StudyMate AI — Production Backend Integration Contract
 * Target Architecture: PHP 8.3+ with PDO, MySQL 8.0+ / MariaDB 10.11+, and HTTPS (cURL / OpenSSL).
 */
object BackendApiContract {
    const val SPEC_VERSION = "1.0.0"

    data class EndpointSpec(
        val method: String,
        val path: String,
        val description: String,
        val authRequired: Boolean = true,
        val rateLimit: String = "60 req/min",
        val requestSample: String,
        val responseSample: String
    )

    val endpoints = listOf(
        EndpointSpec(
            method = "POST",
            path = "/api/v1/ai/chat",
            description = "Send message to AI Tutor with conversation history and optional document context.",
            rateLimit = "20 req/min",
            requestSample = """{
  "conversation_id": "conv-123",
  "message": "Explain photosynthesis simply",
  "document_id": "doc-456", // optional
  "settings": {
    "level": "School",
    "length": "Balanced",
    "language": "English",
    "style": "Step-by-step",
    "include_examples": true,
    "include_recap": true
  }
}""",
            responseSample = """{
  "status": "success",
  "data": {
    "message_id": "msg-789",
    "role": "assistant",
    "content": "Photosynthesis is the process by which green plants...",
    "model": "gemini-4-argon",
    "created_at": "2026-10-15T08:00:00Z"
  }
}"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/ai/summarize",
            description = "Generate structured summary from document text or pasted study notes.",
            rateLimit = "10 req/min",
            requestSample = """{
  "document_id": "doc-456",
  "summary_type": "Exam Revision Notes", // Quick, Detailed, Exam Notes, Formulas
  "length": "Medium"
}""",
            responseSample = """{
  "status": "success",
  "data": {
    "summary": "### Core Concepts\n1. Light reactions...\n2. Calvin cycle...",
    "key_definitions": ["RuBisCO", "Thylakoid"],
    "formula_sheet": ["6CO2 + 6H2O -> C6H12O6 + 6O2"]
  }
}"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/ai/quiz",
            description = "Generate verifiable multiple choice, true/false, or short-answer questions.",
            rateLimit = "15 req/min",
            requestSample = """{
  "subject_id": "sub-bio-1",
  "topic": "Cellular Respiration",
  "question_count": 5,
  "difficulty": "Medium",
  "question_type": "MCQ"
}""",
            responseSample = """{
  "status": "success",
  "data": {
    "quiz_id": "quiz-999",
    "questions": [
      {
        "id": "q-1",
        "questionText": "Where does glycolysis occur?",
        "options": ["Cytoplasm", "Mitochondria", "Nucleus", "Ribosome"],
        "correctAnswerIndex": 0,
        "correctAnswerText": "Cytoplasm",
        "explanation": "Glycolysis takes place in the cell cytoplasm."
      }
    ]
  }
}"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/ai/flashcards",
            description = "Generate spaced repetition flashcards from topic or notes.",
            rateLimit = "15 req/min",
            requestSample = """{
  "subject_id": "sub-phy-2",
  "deck_title": "Kinematics Drill",
  "card_count": 5
}""",
            responseSample = """{
  "status": "success",
  "data": {
    "deck_id": "deck-111",
    "cards": [
      {
        "front": "State Newton's First Law",
        "back": "Law of Inertia: velocity remains constant unless net force acts."
      }
    ]
  }
}"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/ai/study-plan",
            description = "Generate realistic study timetable with focus blocks and rest intervals.",
            rateLimit = "10 req/min",
            requestSample = """{
  "subjects": ["Biology", "Physics"],
  "exam_date": "2026-11-20",
  "daily_hours": 3.5,
  "syllabus": "Chapters 1 to 5"
}""",
            responseSample = """{
  "status": "success",
  "data": {
    "plan_markdown": "# Personalized Revision Plan\n- Session 1: 08:00 - 08:50...",
    "recommended_tasks": [
      { "title": "Review Chapter 1", "duration": 50 }
    ]
  }
}"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/conversations",
            description = "List all student chat conversations with the AI Tutor.",
            requestSample = "GET /api/v1/conversations?limit=20&page=1",
            responseSample = """{
  "status": "success",
  "data": [
    { "id": "conv-1", "title": "Biology Questions", "updated_at": "2026-10-15T08:00:00Z" }
  ]
}"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/conversations",
            description = "Create a new conversation session.",
            requestSample = """{ "title": "Physics Mechanics Revision", "subject_id": "sub-phy-2" }""",
            responseSample = """{ "status": "success", "data": { "id": "conv-new-1" } }"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/documents",
            description = "Retrieve all indexed study materials and documents.",
            requestSample = "GET /api/v1/documents",
            responseSample = """{ "status": "success", "data": [{ "id": "doc-1", "title": "Bio Summary" }] }"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/documents",
            description = "Upload and index new study document (multipart/form-data or JSON text).",
            requestSample = """{ "title": "Chapter 4 Notes", "subject_id": "sub-1", "text_content": "..." }""",
            responseSample = """{ "status": "success", "data": { "id": "doc-2", "status": "Ready" } }"""
        ),
        EndpointSpec(
            method = "DELETE",
            path = "/api/v1/documents/{id}",
            description = "Delete a study document and related extracted embeddings.",
            requestSample = "DELETE /api/v1/documents/doc-1",
            responseSample = """{ "status": "success", "message": "Document removed" }"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/subjects",
            description = "List all enrolled subjects with progress metrics.",
            requestSample = "GET /api/v1/subjects",
            responseSample = """{ "status": "success", "data": [{ "id": "sub-1", "name": "Biology" }] }"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/subjects",
            description = "Add or update a study subject.",
            requestSample = """{ "name": "Chemistry", "color": "#00796B", "target_minutes": 60 }""",
            responseSample = """{ "status": "success", "data": { "id": "sub-chem" } }"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/planner/tasks",
            description = "Retrieve planner schedule for a specific date or week.",
            requestSample = "GET /api/v1/planner/tasks?from=2026-10-15&to=2026-10-22",
            responseSample = """{ "status": "success", "data": [{ "id": "task-1", "title": "Solve 5 problems" }] }"""
        ),
        EndpointSpec(
            method = "POST",
            path = "/api/v1/planner/tasks",
            description = "Create or update a study task.",
            requestSample = """{ "title": "Complete quiz", "subject_id": "sub-1", "due_date": "2026-10-16" }""",
            responseSample = """{ "status": "success", "data": { "id": "task-2" } }"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/progress",
            description = "Retrieve consolidated student statistics, study streak, and mastery analytics.",
            requestSample = "GET /api/v1/progress?period=weekly",
            responseSample = """{
  "status": "success",
  "data": {
    "total_minutes": 185,
    "completed_sessions": 6,
    "quizzes_attempted": 4,
    "avg_score_percent": 88,
    "streak_days": 5
  }
}"""
        ),
        EndpointSpec(
            method = "GET",
            path = "/api/v1/profile",
            description = "Fetch student profile, educational level, and study preferences.",
            requestSample = "GET /api/v1/profile",
            responseSample = """{ "status": "success", "data": { "name": "Alex", "level": "College" } }"""
        ),
        EndpointSpec(
            method = "PUT",
            path = "/api/v1/profile",
            description = "Update student profile and learning targets.",
            requestSample = """{ "name": "Alex", "daily_target_minutes": 60 }""",
            responseSample = """{ "status": "success", "message": "Profile updated" }"""
        )
    )

    const val PHP_DEPLOYMENT_GUIDE = """
=== PHP 8.3 & MySQL BACKEND SETUP INSTRUCTIONS ===
1. Environment: Apache/Nginx with PHP 8.3+, PDO_MySQL, OpenSSL, and JSON extensions.
2. Database: Create database 'studymate_db' in utf8mb4 collation.
3. Tables:
   - users (id, email, password_hash, name, education_level, created_at)
   - subjects (id, user_id, name, description, color, target_mins)
   - documents (id, user_id, subject_id, title, content, summary, status)
   - conversations (id, user_id, title, updated_at)
   - messages (id, conversation_id, role, content, created_at)
   - quizzes (id, user_id, subject_id, questions_json, score, completed_at)
   - flashcard_decks (id, user_id, subject_id, title)
   - flashcards (id, deck_id, front, back, interval_days, ease_factor, next_review)
   - planner_tasks (id, user_id, subject_id, title, date_time, is_completed)
   - study_sessions (id, user_id, subject_id, duration_minutes, session_type, logged_at)
4. AI Secret: Keep GEMINI_API_KEY inside your server's .env / config.php outside the public web root.
5. All endpoints must enforce Authorization: Bearer <JWT_TOKEN> headers.
"""
}
