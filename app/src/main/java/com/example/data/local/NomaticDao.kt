package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NomaticDao {

    // Conversations & Messages
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY updatedAt DESC")
    suspend fun getConversationsOnce(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' OR id IN (SELECT conversationId FROM messages WHERE content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchConversation(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesOnce(conversationId: String): List<MessageEntity>

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Attachments
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    // Prompts
    @Query("SELECT * FROM prompts ORDER BY isFavorite DESC, updatedAt DESC")
    fun observePrompts(): Flow<List<PromptEntity>>

    @Query("SELECT COUNT(*) FROM prompts")
    suspend fun getPromptCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrompt(prompt: PromptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrompts(prompts: List<PromptEntity>)

    @Query("DELETE FROM prompts WHERE id = :promptId")
    suspend fun deletePrompt(promptId: String)

    // Notes / Knowledge Vault
    @Query("SELECT * FROM notes ORDER BY CASE WHEN tags LIKE '%PINNED%' THEN 0 ELSE 1 END, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    // Bookmarks & Browser History
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 100")
    fun observeBrowserHistory(): Flow<List<BrowserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowserHistory(entry: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history")
    suspend fun clearBrowserHistory()

    // Tasks & Productivity
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)

    // AI Usage
    @Query("SELECT * FROM ai_usage ORDER BY createdAt DESC")
    fun observeAIUsage(): Flow<List<AIUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAIUsage(usage: AIUsageEntity)

    // Provider Settings
    @Query("SELECT * FROM provider_settings WHERE id = 'default' LIMIT 1")
    fun observeProviderSettings(): Flow<ProviderSettingsEntity?>

    @Query("SELECT * FROM provider_settings WHERE id = 'default' LIMIT 1")
    suspend fun getProviderSettingsOnce(): ProviderSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProviderSettings(settings: ProviderSettingsEntity)

    // Full database wipe
    @Query("DELETE FROM ai_usage")
    suspend fun clearAIUsage()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM prompts")
    suspend fun clearPrompts()

    @Query("DELETE FROM bookmarks")
    suspend fun clearBookmarks()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()
}
