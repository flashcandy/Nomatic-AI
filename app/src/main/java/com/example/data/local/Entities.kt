package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updatedAt"]), Index(value = ["isArchived"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val defaultProvider: String? = "openai",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["createdAt"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val createdAt: Long,
    val status: String = "COMPLETED", // "STREAMING", "COMPLETED", "INCOMPLETE", "FAILED", "FALLBACK"
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val errorMessage: String? = null
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String?,
    val localUri: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long
)

@Entity(
    tableName = "prompts",
    indices = [Index(value = ["category"]), Index(value = ["isFavorite"])]
)
data class PromptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String,
    val category: String,
    val tags: String,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"]), Index(value = ["category"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val description: String? = null,
    val category: String? = "General",
    val createdAt: Long
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["category"]), Index(value = ["updatedAt"])]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String? = "Notes", // Saved links, Notes, AI responses, Prompts, Linux commands, Scripts, Technical documentation, Bookmarks, Documents
    val tags: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "browser_history",
    indices = [Index(value = ["visitedAt"])]
)
data class BrowserHistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val visitedAt: Long
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["isCompleted"]), Index(value = ["dueAt"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val dueAt: Long? = null,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW" or category tags like "PLANNER", "LEARNING", "SHOPPING"
    val isCompleted: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["scheduledAt"]), Index(value = ["isEnabled"])]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val scheduledAt: Long,
    val recurrence: String? = "NONE", // "NONE", "DAILY", "WEEKLY"
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "ai_usage",
    indices = [Index(value = ["providerId"]), Index(value = ["createdAt"])]
)
data class AIUsageEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelId: String,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val estimatedCost: Double? = null,
    val requestStatus: String, // "SUCCESS", "FALLBACK_SUCCESS", "FAILED"
    val createdAt: Long
)

@Entity(tableName = "provider_settings")
data class ProviderSettingsEntity(
    @PrimaryKey val id: String = "default",
    val primaryProvider: String = "openai",
    val secondaryProvider: String = "gemini",
    val autoFallbackEnabled: Boolean = true,
    val maxRetries: Int = 2,
    val timeoutSeconds: Int = 45,
    val openAiEnabled: Boolean = true,
    val openAiDefaultModel: String = "openrouter/free",
    val geminiEnabled: Boolean = true,
    val geminiDefaultModel: String = "gemini-3.8-flash",
    val updatedAt: Long = System.currentTimeMillis()
)
