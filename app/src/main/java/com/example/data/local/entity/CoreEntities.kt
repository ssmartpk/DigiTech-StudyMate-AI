package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val colorHex: String = "#1A237E",
    val iconName: String = "School",
    val studyTargetMinutes: Int = 60,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val fileType: String = "NOTE", // PDF, IMAGE, NOTE, TEXT
    val fileUriOrPath: String? = null,
    val fileSizeBytes: Long = 0L,
    val extractedText: String = "",
    val summaryText: String? = null,
    val status: String = "Ready", // Ready, Processing, Failed
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // user, assistant, system
    val content: String,
    val isSavedToLibrary: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
