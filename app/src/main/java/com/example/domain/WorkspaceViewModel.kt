package com.example.domain

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.AIFallbackManager
import com.example.core.ai.AIMessageInput
import com.example.core.ai.AIModel
import com.example.core.ai.AIProviderRegistry
import com.example.core.ai.AIRequest
import com.example.core.ai.AIRequestRouter
import com.example.core.ai.AIStreamEvent
import com.example.core.ai.AIUsageTracker
import com.example.core.ai.ProviderHealthStatus
import com.example.core.datastore.WorkspacePreferences
import com.example.core.datastore.WorkspacePrefsState
import com.example.core.security.AndroidKeystoreVault
import com.example.core.utilities.DeveloperSafetyAnalyzer
import com.example.core.utilities.DocumentProcessor
import com.example.core.utilities.ExtractedDocument
import com.example.core.utilities.ReminderScheduler
import com.example.data.local.AIUsageEntity
import com.example.data.local.AttachmentEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserHistoryEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.NomaticDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.PromptEntity
import com.example.data.local.ProviderSettingsEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://duckduckgo.com",
    val title: String = "New Tab",
    val readableContent: String = "",
    val isLoading: Boolean = false
)

data class SharedIncomingPayload(
    val rawText: String? = null,
    val uri: Uri? = null,
    val mimeType: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = NomaticDatabase.getInstance(context)
    private val dao = database.dao()
    val preferences = WorkspacePreferences(context)
    val secretVault = AndroidKeystoreVault(context)

    val providerRegistry = AIProviderRegistry(secretVault)
    private val fallbackManager = AIFallbackManager()
    private val usageTracker = AIUsageTracker(dao, providerRegistry)
    val requestRouter = AIRequestRouter(
        registry = providerRegistry,
        fallbackManager = fallbackManager,
        usageTracker = usageTracker,
        secretVault = secretVault,
        dao = dao
    )

    // Reactive Flows from Room & DataStore
    val prefsState: StateFlow<WorkspacePrefsState> = preferences.prefsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        WorkspacePrefsState()
    )

    val providerSettings: StateFlow<ProviderSettingsEntity> = dao.observeProviderSettings()
        .flatMapLatest { flowOf(it ?: ProviderSettingsEntity()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProviderSettingsEntity())

    val availableModels: List<AIModel> = providerRegistry.getAllModels()

    val providerHealth: StateFlow<Map<String, ProviderHealthStatus>> = requestRouter.healthState

    private val _conversationSearchQuery = MutableStateFlow("")
    val conversationSearchQuery: StateFlow<String> = _conversationSearchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = _conversationSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) dao.observeConversations() else dao.searchConversations(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else dao.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val prompts: StateFlow<List<PromptEntity>> = dao.observePrompts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = dao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = dao.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val browserHistory: StateFlow<List<BrowserHistoryEntity>> = dao.observeBrowserHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = dao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = dao.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usageLogs: StateFlow<List<AIUsageEntity>> = dao.observeAIUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workspaceMemories: StateFlow<List<NoteEntity>> = dao.observeNotes()
        .flatMapLatest { list ->
            flowOf(list.filter { it.category == "Workspace Memory" || it.tags.contains("memory", ignoreCase = true) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Ephemeral States
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Masked API Keys State
    private val _maskedOpenAiKey = MutableStateFlow<String?>(null)
    val maskedOpenAiKey: StateFlow<String?> = _maskedOpenAiKey.asStateFlow()

    private val _maskedGeminiKey = MutableStateFlow<String?>(null)
    val maskedGeminiKey: StateFlow<String?> = _maskedGeminiKey.asStateFlow()

    private val _maskedTelegramToken = MutableStateFlow<String?>(null)
    val maskedTelegramToken: StateFlow<String?> = _maskedTelegramToken.asStateFlow()

    private val _connectionTestStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectionTestStatus: StateFlow<Map<String, String>> = _connectionTestStatus.asStateFlow()

    // Assistant Chat Streaming State
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingPartialText = MutableStateFlow("")
    val streamingPartialText: StateFlow<String> = _streamingPartialText.asStateFlow()

    private val _streamingProviderBadge = MutableStateFlow<String?>(null)
    val streamingProviderBadge: StateFlow<String?> = _streamingProviderBadge.asStateFlow()

    private val _pendingChatDraft = MutableStateFlow("")
    val pendingChatDraft: StateFlow<String> = _pendingChatDraft.asStateFlow()

    private val _selectedChatProvider = MutableStateFlow<String?>(null)
    val selectedChatProvider: StateFlow<String?> = _selectedChatProvider.asStateFlow()

    private val _selectedChatModel = MutableStateFlow<String?>(null)
    val selectedChatModel: StateFlow<String?> = _selectedChatModel.asStateFlow()

    private val _attachedDocument = MutableStateFlow<ExtractedDocument?>(null)
    val attachedDocument: StateFlow<ExtractedDocument?> = _attachedDocument.asStateFlow()

    private val _streamErrorFallbackProvider = MutableStateFlow<String?>(null)
    val streamErrorFallbackProvider: StateFlow<String?> = _streamErrorFallbackProvider.asStateFlow()

    private var activeStreamJob: Job? = null

    // Mini Browser State
    private val _browserTabs = MutableStateFlow(listOf(BrowserTab()))
    val browserTabs: StateFlow<List<BrowserTab>> = _browserTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_browserTabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _browserAiResult = MutableStateFlow<String?>(null)
    val browserAiResult: StateFlow<String?> = _browserAiResult.asStateFlow()

    private val _isBrowserAiLoading = MutableStateFlow(false)
    val isBrowserAiLoading: StateFlow<Boolean> = _isBrowserAiLoading.asStateFlow()

    // Document Intelligence State
    private val _activeDocument = MutableStateFlow<ExtractedDocument?>(null)
    val activeDocument: StateFlow<ExtractedDocument?> = _activeDocument.asStateFlow()

    private val _documentAnalysisOutput = MutableStateFlow<String?>(null)
    val documentAnalysisOutput: StateFlow<String?> = _documentAnalysisOutput.asStateFlow()

    private val _isDocumentAnalyzing = MutableStateFlow(false)
    val isDocumentAnalyzing: StateFlow<Boolean> = _isDocumentAnalyzing.asStateFlow()

    // Developer Assistant State
    private val _developerOutput = MutableStateFlow<String?>(null)
    val developerOutput: StateFlow<String?> = _developerOutput.asStateFlow()

    private val _isDeveloperLoading = MutableStateFlow(false)
    val isDeveloperLoading: StateFlow<Boolean> = _isDeveloperLoading.asStateFlow()

    // Incoming Sharesheet Payload
    private val _sharedPayload = MutableStateFlow<SharedIncomingPayload?>(null)
    val sharedPayload: StateFlow<SharedIncomingPayload?> = _sharedPayload.asStateFlow()

    init {
        viewModelScope.launch {
            NomaticDatabase.seedDefaultsIfEmpty(dao)
            refreshMaskedKeys()
            requestRouter.refreshKeyHealth()

            // Remove any orphan empty "New Workspace Session" records with 0 messages
            val allConvs = dao.getConversationsOnce()
            for (conv in allConvs) {
                if (conv.title == "New Workspace Session" && dao.getMessageCount(conv.id) == 0) {
                    dao.deleteConversation(conv.id)
                }
            }

            // Restore previous AI chat session from persistent storage
            val existingConvs = dao.getConversationsOnce()
            val savedLastId = try {
                preferences.prefsFlow.first().lastActiveConversationId
            } catch (_: Exception) {
                prefsState.value.lastActiveConversationId
            }
            if (_activeConversationId.value == null) {
                if (savedLastId != null && existingConvs.any { it.id == savedLastId }) {
                    _activeConversationId.value = savedLastId
                } else if (existingConvs.isNotEmpty()) {
                    _activeConversationId.value = existingConvs.first().id
                }
            }

            _isInitializing.value = false
        }
    }

    fun showStatus(message: String?) {
        _statusBannerMessage.value = message
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun handleIncomingShare(rawText: String?, uri: Uri?, mimeType: String?) {
        if (rawText.isNullOrBlank() && uri == null) return
        _sharedPayload.value = SharedIncomingPayload(rawText = rawText, uri = uri, mimeType = mimeType)
    }

    fun clearIncomingShare() {
        _sharedPayload.value = null
    }

    // --- API KEY & PROVIDER MANAGEMENT ---

    fun refreshMaskedKeys() {
        _maskedOpenAiKey.value = secretVault.getMaskedKey("openai")
        _maskedGeminiKey.value = secretVault.getMaskedKey("gemini")
        _maskedTelegramToken.value = secretVault.getMaskedKey("telegram")
        requestRouter.refreshKeyHealth()
    }

    fun saveApiKey(providerId: String, rawKey: String) {
        secretVault.saveApiKey(providerId, rawKey)
        refreshMaskedKeys()
        showStatus("${providerId.uppercase()} key encrypted with Android Keystore AES-256-GCM.")
    }

    fun deleteApiKey(providerId: String) {
        secretVault.deleteApiKey(providerId)
        refreshMaskedKeys()
        showStatus("${providerId.uppercase()} key removed from encrypted storage.")
    }

    fun testProviderConnection(providerId: String) {
        viewModelScope.launch {
            val currentMap = _connectionTestStatus.value.toMutableMap()
            currentMap[providerId] = "TESTING..."
            _connectionTestStatus.value = currentMap

            val provider = providerRegistry.getProvider(providerId) ?: return@launch
            val result = provider.validateConfiguration()

            val updatedMap = _connectionTestStatus.value.toMutableMap()
            if (result.isSuccess) {
                updatedMap[providerId] = "VERIFIED • ONLINE"
                requestRouter.updateProviderHealth(providerId, ProviderHealthStatus.HEALTHY)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                updatedMap[providerId] = "ERROR: $err"
                requestRouter.updateProviderHealth(providerId, ProviderHealthStatus.INVALID_KEY)
            }
            _connectionTestStatus.value = updatedMap
        }
    }

    fun updateProviderSettings(transform: (ProviderSettingsEntity) -> ProviderSettingsEntity) {
        viewModelScope.launch {
            val current = dao.getProviderSettingsOnce() ?: ProviderSettingsEntity()
            val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
            dao.upsertProviderSettings(updated)
        }
    }

    // --- CONVERSATIONAL ASSISTANT ---

    fun setConversationSearchQuery(query: String) {
        _conversationSearchQuery.value = query
    }

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            preferences.setLastActiveConversationId(conversationId)
        }
    }

    fun setPendingChatDraft(draft: String) {
        _pendingChatDraft.value = draft
    }

    fun setSelectedChatProvider(providerId: String?, modelId: String? = null) {
        _selectedChatProvider.value = providerId
        _selectedChatModel.value = modelId
    }

    fun attachFileToChat(uri: Uri) {
        viewModelScope.launch {
            val result = DocumentProcessor.processUri(context, uri)
            if (result.isSuccess) {
                _attachedDocument.value = result.getOrNull()
                showStatus("Attached ${result.getOrNull()?.fileName}")
            } else {
                showStatus(result.exceptionOrNull()?.message ?: "Attachment error")
            }
        }
    }

    fun clearChatAttachment() {
        _attachedDocument.value = null
    }

    fun createNewConversation(initialProvider: String? = null): String {
        activeStreamJob?.cancel()
        _isStreaming.value = false
        _streamingPartialText.value = ""
        _streamingProviderBadge.value = null

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val provider = initialProvider ?: providerSettings.value.primaryProvider
        // Set synchronously on the main thread so any subsequent sendChatMessage uses this exact ID
        _activeConversationId.value = id
        viewModelScope.launch {
            dao.upsertConversation(
                ConversationEntity(
                    id = id,
                    title = "New Workspace Session",
                    createdAt = now,
                    updatedAt = now,
                    defaultProvider = provider
                )
            )
            preferences.setLastActiveConversationId(id)
        }
        return id
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            dao.updateConversationTitle(conversationId, newTitle.trim(), System.currentTimeMillis())
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            dao.deleteMessagesForConversation(conversationId)
            dao.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                val remaining = dao.getConversationsOnce()
                val nextId = remaining.firstOrNull()?.id
                _activeConversationId.value = nextId
                preferences.setLastActiveConversationId(nextId)
            }
            showStatus("Conversation deleted")
        }
    }

    fun saveConversationToVault(conversationId: String) {
        viewModelScope.launch {
            val conv = dao.getConversationById(conversationId) ?: conversations.value.find { it.id == conversationId } ?: return@launch
            val msgs = dao.getMessagesOnce(conversationId)
            if (msgs.isEmpty()) {
                showStatus("No messages in chat to store.")
                return@launch
            }
            val formattedContent = buildString {
                append("# Session: ").append(conv.title).append("\n\n")
                append("**Provider:** ").append(conv.defaultProvider ?: "AI").append("  \n")
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(conv.updatedAt))
                append("**Date:** ").append(dateStr).append("\n\n---\n\n")
                msgs.forEach { msg ->
                    val roleTag = if (msg.role.equals("user", ignoreCase = true)) "USER" else "AI ASSISTANT (${msg.modelId ?: "AI"})"
                    append("### ").append(roleTag).append("\n\n")
                    append(msg.content).append("\n\n")
                }
            }
            saveNoteToVault(
                title = "Chat: ${conv.title}",
                content = formattedContent,
                category = "AI responses",
                tags = "chat,session,ai,${conv.defaultProvider ?: "assistant"}"
            )
            showStatus("Saved entire chat '${conv.title}' to Vault!")
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            dao.clearAllConversations()
            _activeConversationId.value = null
            preferences.setLastActiveConversationId(null)
            showStatus("All previous AI chats cleared from storage.")
        }
    }

    fun sendChatMessage(
        userText: String,
        overrideProviderId: String? = _selectedChatProvider.value,
        overrideModelId: String? = _selectedChatModel.value,
        startNewSession: Boolean = false
    ) {
        val trimmed = userText.trim()
        val attachment = _attachedDocument.value
        if (trimmed.isEmpty() && attachment == null) return

        activeStreamJob?.cancel()
        _streamErrorFallbackProvider.value = null

        val targetConvId = if (startNewSession || _activeConversationId.value == null) {
            val freshId = UUID.randomUUID().toString()
            _activeConversationId.value = freshId
            freshId
        } else {
            _activeConversationId.value!!
        }

        activeStreamJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sessionTitle = trimmed.take(44).ifBlank { attachment?.fileName ?: "Workspace Session" }
            val defaultProv = overrideProviderId ?: providerSettings.value.primaryProvider

            // Ensure ConversationEntity exists in Room before inserting MessageEntity (Foreign Key safety)
            val existingConv = dao.getConversationById(targetConvId)
            if (existingConv == null) {
                dao.upsertConversation(
                    ConversationEntity(
                        id = targetConvId,
                        title = sessionTitle,
                        createdAt = now,
                        updatedAt = now,
                        defaultProvider = defaultProv
                    )
                )
            } else if ((existingConv.title == "New Workspace Session" || existingConv.title == "Workspace Session") && trimmed.isNotEmpty()) {
                dao.updateConversationTitle(targetConvId, sessionTitle, now)
            } else {
                dao.touchConversation(targetConvId, now)
            }

            _activeConversationId.value = targetConvId
            preferences.setLastActiveConversationId(targetConvId)

            val fullUserContent = if (attachment != null) {
                "$trimmed\n\n--- Attached Document (${attachment.fileName}) ---\n${attachment.extractedText.take(12_000)}"
            } else {
                trimmed
            }

            val userMsgId = UUID.randomUUID().toString()
            dao.upsertMessage(
                MessageEntity(
                    id = userMsgId,
                    conversationId = targetConvId,
                    role = "user",
                    content = if (attachment != null) "$trimmed\n[Attached: ${attachment.fileName}]" else trimmed,
                    createdAt = now,
                    status = "COMPLETED"
                )
            )

            if (attachment != null) {
                dao.insertAttachment(
                    AttachmentEntity(
                        id = UUID.randomUUID().toString(),
                        messageId = userMsgId,
                        localUri = attachment.uriString,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType,
                        fileSize = attachment.fileSizeBytes,
                        createdAt = now
                    )
                )
                _attachedDocument.value = null
            }

            _pendingChatDraft.value = ""

            // Check if query matches local Workspace Memory (0 API Tokens Consumed!)
            val cachedMatch = notes.value.find { note ->
                (note.category == "Workspace Memory" || note.category == "Linux commands" || note.tags.contains("memory", ignoreCase = true)) &&
                    (note.title.contains(trimmed, ignoreCase = true) || (trimmed.length >= 3 && note.content.contains(trimmed, ignoreCase = true)))
            }

            if (cachedMatch != null && attachment == null) {
                val cachedResponseText = "⚡ MEMORY CACHE (0 TOKENS CONSUMED)\n\n### ${cachedMatch.title}\n${cachedMatch.content}"
                dao.upsertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = targetConvId,
                        role = "assistant",
                        content = cachedResponseText,
                        providerId = "memory",
                        modelId = "local_cache",
                        createdAt = System.currentTimeMillis(),
                        status = "COMPLETED",
                        inputTokens = 0L,
                        outputTokens = 0L
                    )
                )
                showStatus("Answered from Workspace Memory Cache (0 Tokens Consumed!)")
            } else {
                streamAssistantReply(targetConvId, fullUserContent, overrideProviderId, overrideModelId)
            }
        }
    }

    fun editAndResendUserMessage(message: MessageEntity, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            dao.upsertMessage(message.copy(content = newContent.trim(), createdAt = System.currentTimeMillis()))
            streamAssistantReply(
                conversationId = message.conversationId,
                latestPromptOverride = newContent.trim(),
                overrideProviderId = _selectedChatProvider.value,
                overrideModelId = _selectedChatModel.value
            )
        }
    }

    fun regenerateLastAssistantMessage(conversationId: String, forceProviderId: String? = null) {
        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            val msgs = dao.getMessagesOnce(conversationId)
            val lastMsg = msgs.lastOrNull()
            if (lastMsg != null && lastMsg.role == "assistant") {
                dao.deleteMessage(lastMsg.id)
            }
            val lastUser = dao.getMessagesOnce(conversationId).lastOrNull { it.role == "user" } ?: return@launch
            streamAssistantReply(
                conversationId = conversationId,
                latestPromptOverride = lastUser.content,
                overrideProviderId = forceProviderId ?: _selectedChatProvider.value,
                overrideModelId = _selectedChatModel.value
            )
        }
    }

    fun cancelActiveStream() {
        activeStreamJob?.cancel()
        viewModelScope.launch {
            val convId = _activeConversationId.value
            val partial = _streamingPartialText.value
            if (convId != null && partial.isNotBlank()) {
                dao.upsertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = convId,
                        role = "assistant",
                        content = partial,
                        providerId = _streamingProviderBadge.value ?: providerSettings.value.primaryProvider,
                        modelId = _selectedChatModel.value ?: "cancelled",
                        createdAt = System.currentTimeMillis(),
                        status = "INCOMPLETE",
                        errorMessage = "Cancelled by user (partial response preserved)"
                    )
                )
            }
            _isStreaming.value = false
            _streamingPartialText.value = ""
            _streamingProviderBadge.value = null
        }
    }

    private suspend fun streamAssistantReply(
        conversationId: String,
        latestPromptOverride: String,
        overrideProviderId: String?,
        overrideModelId: String?
    ) {
        _isStreaming.value = true
        _streamingPartialText.value = ""
        _streamErrorFallbackProvider.value = null

        val historyEntities = dao.getMessagesOnce(conversationId)
        val neutralHistory = historyEntities.dropLast(1).takeLast(14).map {
            AIMessageInput(role = it.role, content = it.content)
        } + AIMessageInput(role = "user", content = latestPromptOverride)

        val req = AIRequest(
            conversationId = conversationId,
            messages = neutralHistory,
            preferredProviderId = overrideProviderId,
            preferredModelId = overrideModelId
        )

        try {
            requestRouter.routeStreamingRequest(req).collect { event ->
                when (event) {
                    is AIStreamEvent.Started -> {
                        _streamingProviderBadge.value = "${event.providerId.uppercase()} • ${event.modelId}" +
                            if (event.isFallback) " (FALLBACK)" else ""
                        if (event.fallbackNotice != null) {
                            showStatus(event.fallbackNotice)
                        }
                    }
                    is AIStreamEvent.Token -> {
                        _streamingPartialText.value = event.accumulated
                    }
                    is AIStreamEvent.Completed -> {
                        val fullContent = event.fullText.ifBlank { _streamingPartialText.value }.ifBlank { "Response completed." }
                        val estimatedIn = (latestPromptOverride.length / 4L).coerceAtLeast(1L)
                        val estimatedOut = (fullContent.length / 4L).coerceAtLeast(1L)
                        val finalInTokens = event.inputTokens ?: estimatedIn
                        val finalOutTokens = event.outputTokens ?: estimatedOut
                        val completedAt = System.currentTimeMillis()

                        if (dao.getConversationById(conversationId) == null) {
                            dao.upsertConversation(
                                ConversationEntity(
                                    id = conversationId,
                                    title = latestPromptOverride.take(44).ifBlank { "Workspace Session" },
                                    createdAt = completedAt,
                                    updatedAt = completedAt,
                                    defaultProvider = event.providerId
                                )
                            )
                        }

                        dao.upsertMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = "assistant",
                                content = fullContent,
                                providerId = event.providerId,
                                modelId = event.modelId,
                                createdAt = completedAt,
                                status = if (event.isFallback) "FALLBACK" else "COMPLETED",
                                inputTokens = finalInTokens,
                                outputTokens = finalOutTokens
                            )
                        )
                        dao.touchConversation(conversationId, completedAt)
                        _activeConversationId.value = conversationId
                        preferences.setLastActiveConversationId(conversationId)
                        _isStreaming.value = false
                        _streamingPartialText.value = ""
                        _streamingProviderBadge.value = null
                    }
                    is AIStreamEvent.Error -> {
                        val status = if (event.partialText.isNotBlank()) "INCOMPLETE" else "FAILED"
                        val estimatedIn = (latestPromptOverride.length / 4L).coerceAtLeast(1L)
                        val estimatedOut = (event.partialText.length / 4L).coerceAtLeast(0L)
                        val errAt = System.currentTimeMillis()

                        if (dao.getConversationById(conversationId) == null) {
                            dao.upsertConversation(
                                ConversationEntity(
                                    id = conversationId,
                                    title = latestPromptOverride.take(44).ifBlank { "Workspace Session" },
                                    createdAt = errAt,
                                    updatedAt = errAt,
                                    defaultProvider = event.exception.providerId
                                )
                            )
                        }

                        dao.upsertMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = "assistant",
                                content = event.partialText.ifBlank { "Request could not be completed." },
                                providerId = event.exception.providerId,
                                modelId = overrideModelId ?: "error",
                                createdAt = errAt,
                                status = status,
                                inputTokens = estimatedIn,
                                outputTokens = estimatedOut,
                                errorMessage = event.exception.message
                            )
                        )
                        dao.touchConversation(conversationId, errAt)
                        _activeConversationId.value = conversationId
                        preferences.setLastActiveConversationId(conversationId)
                        _streamErrorFallbackProvider.value = event.fallbackProviderId
                        _isStreaming.value = false
                        _streamingPartialText.value = ""
                        _streamingProviderBadge.value = null
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val partial = _streamingPartialText.value
            val errAt = System.currentTimeMillis()
            if (dao.getConversationById(conversationId) == null) {
                dao.upsertConversation(
                    ConversationEntity(
                        id = conversationId,
                        title = latestPromptOverride.take(44).ifBlank { "Workspace Session" },
                        createdAt = errAt,
                        updatedAt = errAt,
                        defaultProvider = overrideProviderId ?: providerSettings.value.primaryProvider
                    )
                )
            }
            if (partial.isNotBlank()) {
                val estimatedIn = (latestPromptOverride.length / 4L).coerceAtLeast(1L)
                val estimatedOut = (partial.length / 4L).coerceAtLeast(1L)
                dao.upsertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "assistant",
                        content = partial,
                        providerId = overrideProviderId ?: providerSettings.value.primaryProvider,
                        modelId = overrideModelId ?: "default",
                        createdAt = errAt,
                        status = "INCOMPLETE",
                        inputTokens = estimatedIn,
                        outputTokens = estimatedOut,
                        errorMessage = e.localizedMessage
                    )
                )
                dao.touchConversation(conversationId, errAt)
            }
            _activeConversationId.value = conversationId
            preferences.setLastActiveConversationId(conversationId)
            _isStreaming.value = false
            _streamingPartialText.value = ""
            _streamingProviderBadge.value = null
            showStatus(e.localizedMessage ?: "Generation error")
        }
    }

    // --- MINI BROWSER ---

    fun addBrowserTab(initialUrl: String = "https://duckduckgo.com") {
        val newTab = BrowserTab(url = normalizeUrlOrSearch(initialUrl))
        _browserTabs.value = _browserTabs.value + newTab
        _activeTabId.value = newTab.id
    }

    fun selectBrowserTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun closeBrowserTab(tabId: String) {
        val remaining = _browserTabs.value.filterNot { it.id == tabId }
        if (remaining.isEmpty()) {
            val fresh = BrowserTab()
            _browserTabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _browserTabs.value = remaining
            if (_activeTabId.value == tabId) {
                _activeTabId.value = remaining.last().id
            }
        }
    }

    fun updateActiveTabUrl(input: String) {
        val url = normalizeUrlOrSearch(input)
        val currentId = _activeTabId.value
        _browserTabs.value = _browserTabs.value.map { tab ->
            if (tab.id == currentId) tab.copy(url = url) else tab
        }
    }

    fun onWebPageLoaded(tabId: String, url: String, title: String, readableText: String) {
        _browserTabs.value = _browserTabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = url,
                    title = title.ifBlank { url },
                    readableContent = readableText,
                    isLoading = false
                )
            } else {
                tab
            }
        }
        viewModelScope.launch {
            dao.insertBrowserHistory(
                BrowserHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = title.ifBlank { url },
                    visitedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun normalizeUrlOrSearch(rawInput: String): String {
        val input = rawInput.trim()
        if (input.startsWith("https://") || input.startsWith("http://")) {
            return if (prefsState.value.browserHttpsOnly && input.startsWith("http://")) {
                input.replaceFirst("http://", "https://")
            } else {
                input
            }
        }
        if (!input.contains(" ") && input.contains(".")) {
            return "https://$input"
        }
        val encoded = Uri.encode(input)
        return when (prefsState.value.defaultSearchEngine) {
            "Google" -> "https://www.google.com/search?q=$encoded"
            "Brave" -> "https://search.brave.com/search?q=$encoded"
            "Startpage" -> "https://www.startpage.com/do/search?q=$encoded"
            else -> "https://duckduckgo.com/?q=$encoded"
        }
    }

    fun analyzeActiveWebpageWithAI(question: String? = null) {
        val activeTab = _browserTabs.value.find { it.id == _activeTabId.value } ?: return
        viewModelScope.launch {
            _isBrowserAiLoading.value = true
            _browserAiResult.value = null

            val pageText = activeTab.readableContent.ifBlank {
                fetchReadableWebpageFallback(activeTab.url)
            }

            if (pageText.isBlank()) {
                _browserAiResult.value = "Unable to extract readable text from ${activeTab.url}. Ensure the page has finished loading."
                _isBrowserAiLoading.value = false
                return@launch
            }

            val prompt = if (question.isNullOrBlank()) {
                "Summarize the following webpage clearly with: 1) Executive Summary, 2) Key Takeaways, and 3) Technical/Actionable Points.\n\nURL: ${activeTab.url}\nTitle: ${activeTab.title}\n\nContent:\n${pageText.take(14_000)}"
            } else {
                "Answer the following question based on the webpage content.\nQuestion: $question\n\nURL: ${activeTab.url}\nTitle: ${activeTab.title}\n\nContent:\n${pageText.take(14_000)}"
            }

            val result = requestRouter.routeUnaryRequest(
                AIRequest(
                    conversationId = "browser_analysis",
                    messages = listOf(AIMessageInput("user", prompt))
                )
            )
            if (result.isSuccess) {
                val res = result.getOrThrow()
                val header = "[${res.providerId.uppercase()} • ${res.modelId}${if (res.isFallback) " • FALLBACK" else ""}]\n\n"
                _browserAiResult.value = header + res.content
            } else {
                _browserAiResult.value = "AI Analysis Failed: ${result.exceptionOrNull()?.message}"
            }
            _isBrowserAiLoading.value = false
        }
    }

    fun clearBrowserAiResult() {
        _browserAiResult.value = null
    }

    private suspend fun fetchReadableWebpageFallback(url: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                html.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun addBookmark(url: String, title: String, category: String = "Browser") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertBookmark(
                BookmarkEntity(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = title.ifBlank { url },
                    category = category,
                    createdAt = now
                )
            )
            dao.upsertNote(
                NoteEntity(
                    id = UUID.randomUUID().toString(),
                    title = title.ifBlank { url },
                    content = "Saved Bookmark URL: $url",
                    category = "Bookmarks",
                    tags = "bookmark,web",
                    createdAt = now,
                    updatedAt = now
                )
            )
            showStatus("Bookmark saved to Browser & Knowledge Vault")
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            dao.deleteBookmark(bookmarkId)
            showStatus("Bookmark removed")
        }
    }

    // --- KNOWLEDGE VAULT & PROMPTS ---

    fun saveNoteToVault(
        id: String = UUID.randomUUID().toString(),
        title: String,
        content: String,
        category: String = "Notes",
        tags: String = ""
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertNote(
                NoteEntity(
                    id = id,
                    title = title.ifBlank { "Untitled Vault Entry" },
                    content = content,
                    category = category,
                    tags = tags,
                    createdAt = now,
                    updatedAt = now
                )
            )
            showStatus("Saved to Knowledge Vault ($category)")
        }
    }

    fun addWorkspaceMemory(title: String, content: String, tags: String = "memory") {
        if (title.isBlank() || content.isBlank()) return
        saveNoteToVault(
            title = title.trim(),
            content = content.trim(),
            category = "Workspace Memory",
            tags = "memory,$tags"
        )
    }

    fun clearAllWorkspaceMemories() {
        viewModelScope.launch {
            val mems = notes.value.filter { it.category == "Workspace Memory" || it.tags.contains("memory", ignoreCase = true) }
            mems.forEach { dao.deleteNote(it.id) }
            showStatus("Cleared all Workspace Memories from local storage")
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            val tagsList = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (tagsList.contains("PINNED")) {
                tagsList.remove("PINNED")
            } else {
                tagsList.add(0, "PINNED")
            }
            dao.upsertNote(note.copy(tags = tagsList.joinToString(","), updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            dao.deleteNote(noteId)
        }
    }

    fun savePrompt(
        id: String = UUID.randomUUID().toString(),
        title: String,
        description: String,
        content: String,
        category: String,
        tags: String,
        isFavorite: Boolean = false
    ) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertPrompt(
                PromptEntity(
                    id = id,
                    title = title.trim(),
                    description = description.trim(),
                    content = content.trim(),
                    category = category.trim().ifBlank { "General" },
                    tags = tags.trim(),
                    isFavorite = isFavorite,
                    createdAt = now,
                    updatedAt = now
                )
            )
            showStatus("Prompt saved to Prompt Library")
        }
    }

    fun togglePromptFavorite(prompt: PromptEntity) {
        viewModelScope.launch {
            dao.upsertPrompt(prompt.copy(isFavorite = !prompt.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deletePrompt(promptId: String) {
        viewModelScope.launch {
            dao.deletePrompt(promptId)
        }
    }

    // --- DEVELOPER ASSISTANT ---

    fun runDeveloperAssistantQuery(
        domainCategory: String,
        subTopic: String,
        userProblem: String
    ) {
        viewModelScope.launch {
            _isDeveloperLoading.value = true
            // Always generate immediate deterministic offline reference + attempt AI synthesis if key is configured
            val offlineRunbook = buildOfflineDeveloperRunbook(domainCategory, subTopic, userProblem)
            _developerOutput.value = offlineRunbook

            val hasAnyKey = secretVault.hasApiKey("openai") || secretVault.hasApiKey("gemini")
            if (hasAnyKey && userProblem.isNotBlank()) {
                val systemPrompt = """
                    You are Nomatic AI Developer Assistant specializing in $domainCategory ($subTopic).
                    Provide production-grade commands, configuration blocks, and root-cause troubleshooting.
                    IMPORTANT SAFETY REQUIREMENT:
                    - Never instruct blind execution of destructive commands.
                    - Clearly prefix any destructive command (e.g., rm -rf, mkfs, dd, chmod 777, terraform destroy, air object rm, m_rm) with # WARNING: DESTRUCTIVE COMMAND.
                """.trimIndent()

                val aiResult = requestRouter.routeUnaryRequest(
                    AIRequest(
                        conversationId = "dev_assistant",
                        messages = listOf(AIMessageInput("user", "Topic: $domainCategory / $subTopic\nProblem/Request: $userProblem")),
                        systemPrompt = systemPrompt
                    )
                )
                if (aiResult.isSuccess) {
                    val res = aiResult.getOrThrow()
                    val safety = DeveloperSafetyAnalyzer.inspectContent(res.content)
                    val safetyHeader = if (safety.isDestructive) {
                        "⚠️ ${safety.safetyGuidance}\n\n"
                    } else {
                        "✓ ${safety.safetyGuidance}\n\n"
                    }
                    _developerOutput.value = "${safetyHeader}[AI SYNTHESIS • ${res.providerId.uppercase()} • ${res.modelId}]\n\n${res.content}\n\n---\n$offlineRunbook"
                }
            }
            _isDeveloperLoading.value = false
        }
    }

    private fun buildOfflineDeveloperRunbook(category: String, subTopic: String, problem: String): String {
        val safety = DeveloperSafetyAnalyzer.inspectContent(problem)
        val warningBlock = if (safety.isDestructive) {
            "⚠️ ${safety.safetyGuidance}\n\n"
        } else {
            "✓ Review-Only Mode: Commands are displayed for inspection and never executed automatically.\n\n"
        }

        val referenceBody = when (category) {
            "Linux Administration" -> """
                ### Linux Administration Runbook ($subTopic)
                ```bash
                # 1. Process & CPU/Memory Triage
                ps aux --sort=-%mem | head -n 15
                pidstat -u -r -d 1 5

                # 2. Disk & Inode Inspection
                df -hT && df -i
                lsof +L1 # Find deleted files still held open by processes

                # 3. Systemd & Journal Diagnostics
                systemctl status <service-name> --no-pager
                journalctl -xeu <service-name> --since "30 min ago"

                # 4. File Permissions & Cron Verification
                namei -l /path/to/target
                crontab -l && ls -la /etc/cron*
                ```
            """.trimIndent()

            "Ab Initio Administration" -> """
                ### Ab Initio EME & GDE Administration ($subTopic)
                ```bash
                # 1. Inspect EME Project Inheritance & Sandbox Parameters
                air project show /Projects/<eme-project>
                air sandbox info <sandbox-dir>

                # 2. Check Object Versions & Lock Status
                air object versions -verbose /Projects/<eme-project>/mp/<graph>.mp
                air lock show -project /Projects/<eme-project>

                # 3. Create Primary & Exact Release Tags
                air tag create <release_tag_v1> -file /Projects/<eme-project>/pset/<job>.pset -primary
                air tag diff <tag_old> <tag_new>

                # 4. Safe Migration Export / Import (.sav)
                air save /tmp/<release_tag_v1>.sav -from-tag <release_tag_v1>
                air load /tmp/<release_tag_v1>.sav -relocate /Projects/DEV /Projects/QA
                ```
            """.trimIndent()

            "AWS & Cloud" -> """
                ### AWS, Kubernetes, Docker & Terraform ($subTopic)
                ```bash
                # 1. AWS IAM & STS Identity Verification
                aws sts get-caller-identity
                aws iam simulate-principal-policy --policy-source-arn <role-arn> --action-names s3:GetObject

                # 2. EKS & Kubernetes Pod / Network Triage
                kubectl get pods -A -o wide --field-selector=status.phase!=Running
                kubectl describe pod <pod-name> -n <namespace>
                kubectl logs <pod-name> -n <namespace> --previous --tail=100

                # 3. Docker Container & Layer Inspection
                docker stats --no-stream
                docker inspect <container-id> --format='{{json .State.Health}}'

                # 4. Terraform Safe Plan Verification
                terraform validate && terraform plan -out=tfplan.binary
                ```
            """.trimIndent()

            "LDAP" -> """
                ### LDAP & Active Directory Troubleshooting ($subTopic)
                ```bash
                # 1. Verify LDAPS TLS Handshake & Certificate Chain (Port 636)
                openssl s_client -connect ldap.corp.example.com:636 -showcerts </dev/null

                # 2. Authenticated LDAP Search with Nested Group Filter (OID 1.2.840.113556.1.4.1941)
                ldapsearch -x -H ldaps://ldap.corp.example.com:636 \
                  -D "CN=bind_svc,OU=Services,DC=corp,DC=example,DC=com" -W \
                  -b "DC=corp,DC=example,DC=com" \
                  "(&(objectClass=user)(sAMAccountName=${problem.ifBlank { "target_user" }})(memberOf:1.2.840.113556.1.4.1941:=CN=Admins,OU=Groups,DC=corp,DC=example,DC=com))" \
                  dn cn mail pwdLastSet lockoutTime

                # 3. RootDSE Naming Context Discovery
                ldapsearch -x -H ldaps://ldap.corp.example.com:636 -b "" -s base "(objectclass=*)" namingContexts
                ```
            """.trimIndent()

            else -> """
                ### Programming & Code Review Synthesis ($subTopic)
                ```kotlin
                // Structured Concurrency & Safe Error Handling Pattern
                suspend fun executeSafeOperation(): Result<String> = withContext(Dispatchers.IO) {
                    runCatching {
                        // Validate inputs, enforce timeouts, and avoid blocking Main thread
                        "Verified execution"
                    }
                }
                ```
                ```python
                # Python Diagnostic & Structured Logging Pattern
                import logging, subprocess, shlex
                logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
                ```
            """.trimIndent()
        }

        return warningBlock + referenceBody
    }

    // --- DOCUMENT INTELLIGENCE ---

    fun loadDocumentForIntelligence(uri: Uri) {
        viewModelScope.launch {
            _isDocumentAnalyzing.value = true
            _documentAnalysisOutput.value = null
            val res = DocumentProcessor.processUri(context, uri)
            if (res.isSuccess) {
                val doc = res.getOrThrow()
                _activeDocument.value = doc
                _documentAnalysisOutput.value = "Loaded '${doc.fileName}' (${doc.pageCount} page(s), ${DocumentProcessor.formatSize(doc.fileSizeBytes)}).\n\nPreview of Locally Extracted Content:\n${doc.extractedText.take(1500)}"
            } else {
                showStatus(res.exceptionOrNull()?.message ?: "Failed to read document")
            }
            _isDocumentAnalyzing.value = false
        }
    }

    fun analyzeActiveDocument(mode: String, customQuestion: String = "") {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            _isDocumentAnalyzing.value = true
            val instruction = when (mode) {
                "SUMMARIZE" -> "Provide an executive summary, key findings, and action items for this document."
                "EXTRACT_TABLES" -> "Extract all structured data, tables, metrics, and key-value pairs from this document in clean Markdown tables."
                "GENERATE_NOTES" -> "Generate structured study/technical notes with headings and bullet points from this document."
                "EXPLAIN_TECH" -> "Explain the technical architecture, terminology, and workflows described in this document."
                else -> "Answer the following question using only the provided document content: $customQuestion"
            }

            val prompt = "$instruction\n\nDocument Name: ${doc.fileName}\nContent:\n${doc.extractedText.take(18_000)}"
            val result = requestRouter.routeUnaryRequest(
                AIRequest(
                    conversationId = "doc_intelligence",
                    messages = listOf(AIMessageInput("user", prompt))
                )
            )
            if (result.isSuccess) {
                val r = result.getOrThrow()
                _documentAnalysisOutput.value = "[${r.providerId.uppercase()} • ${r.modelId}]\n\n${r.content}"
            } else {
                _documentAnalysisOutput.value = "AI Request Failed: ${result.exceptionOrNull()?.message}\n\nLocal Extracted Content:\n${doc.extractedText.take(3000)}"
            }
            _isDocumentAnalyzing.value = false
        }
    }

    // --- PRODUCTIVITY CENTRE (TASKS & REMINDERS) ---

    fun addTask(title: String, description: String?, priority: String = "MEDIUM", dueAt: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertTask(
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    description = description?.trim(),
                    dueAt = dueAt,
                    priority = priority,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            dao.deleteTask(taskId)
        }
    }

    fun addReminder(
        title: String,
        description: String?,
        scheduledAt: Long,
        recurrence: String = "NONE"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val entity = ReminderEntity(
                id = id,
                title = title.trim(),
                description = description?.trim(),
                scheduledAt = scheduledAt,
                recurrence = recurrence,
                isEnabled = true
            )
            dao.upsertReminder(entity)
            ReminderScheduler.scheduleReminder(context, id, entity.title, entity.description, scheduledAt)
            showStatus("Reminder scheduled")
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, minutes: Int = 15) {
        viewModelScope.launch {
            val newTime = System.currentTimeMillis() + (minutes * 60_000L)
            val updated = reminder.copy(scheduledAt = newTime, isEnabled = true)
            dao.upsertReminder(updated)
            ReminderScheduler.scheduleReminder(context, reminder.id, reminder.title, reminder.description, newTime)
            showStatus("Snoozed for $minutes minutes")
        }
    }

    fun completeOrToggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val newEnabled = !reminder.isEnabled
            dao.upsertReminder(reminder.copy(isEnabled = newEnabled))
            if (!newEnabled) {
                ReminderScheduler.cancelReminder(context, reminder.id)
            } else {
                ReminderScheduler.scheduleReminder(context, reminder.id, reminder.title, reminder.description, reminder.scheduledAt)
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            dao.deleteReminder(reminderId)
            ReminderScheduler.cancelReminder(context, reminderId)
        }
    }

    // --- AUTOMATION CENTRE ---

    fun sendTelegramTestMessage(messageText: String) {
        viewModelScope.launch {
            val token = secretVault.getApiKey("telegram")
            val chatId = prefsState.value.automationTelegramChatId
            if (token.isNullOrBlank() || chatId.isBlank()) {
                showStatus("Configure both Telegram Bot Token and Chat ID first.")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val payload = JSONObject().apply {
                        put("chat_id", chatId)
                        put("text", messageText)
                    }
                    val req = Request.Builder()
                        .url("https://api.telegram.org/bot$token/sendMessage")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) Result.success(Unit)
                        else Result.failure(IllegalStateException("Telegram HTTP ${resp.code}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (result.isSuccess) {
                showStatus("Dispatched message to Telegram Bot successfully.")
            } else {
                showStatus("Telegram dispatch failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun generateLocalDailySummaryNow() {
        viewModelScope.launch {
            val openTasks = tasks.value.count { !it.isCompleted }
            val completedTasks = tasks.value.count { it.isCompleted }
            val activeReminders = reminders.value.count { it.isEnabled }
            val totalNotes = notes.value.size
            val totalConversations = conversations.value.size
            val summaryText = """
                # Daily Local Workspace Digest
                - **Pending Tasks**: $openTasks ($completedTasks completed)
                - **Active Reminders**: $activeReminders
                - **Knowledge Vault Entries**: $totalNotes
                - **AI Conversations**: $totalConversations
                - **Total AI Requests Logged**: ${usageLogs.value.size}
            """.trimIndent()
            saveNoteToVault(
                title = "Daily Workspace Digest",
                content = summaryText,
                category = "Notes",
                tags = "PINNED,digest,automation"
            )
        }
    }

    // --- PRIVACY, EXPORT & DATA MANAGEMENT ---

    fun buildWorkspaceExportJson(): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("application", "Nomatic AI")
        root.put("apiKeysIncluded", false) // Never include raw API keys in exported backups

        val notesArr = JSONArray()
        notes.value.forEach { n ->
            notesArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("category", n.category)
                    put("tags", n.tags)
                }
            )
        }
        root.put("notes", notesArr)

        val promptsArr = JSONArray()
        prompts.value.forEach { p ->
            promptsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("description", p.description)
                    put("content", p.content)
                    put("category", p.category)
                    put("tags", p.tags)
                }
            )
        }
        root.put("prompts", promptsArr)

        val tasksArr = JSONArray()
        tasks.value.forEach { t ->
            tasksArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("priority", t.priority)
                    put("isCompleted", t.isCompleted)
                }
            )
        }
        root.put("tasks", tasksArr)
        return root.toString(2)
    }

    fun importVaultJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)
                val now = System.currentTimeMillis()
                val notesArr = root.optJSONArray("notes")
                if (notesArr != null) {
                    for (i in 0 until notesArr.length()) {
                        val obj = notesArr.getJSONObject(i)
                        dao.upsertNote(
                            NoteEntity(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                title = obj.optString("title", "Imported Note"),
                                content = obj.optString("content", ""),
                                category = obj.optString("category", "Notes"),
                                tags = obj.optString("tags", "imported"),
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                }
                showStatus("Imported Knowledge Vault backup successfully")
            } catch (e: Exception) {
                showStatus("Import failed: Invalid JSON format")
            }
        }
    }

    fun updateUserDisplayName(name: String) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "Operator" }
            preferences.setUserDisplayName(cleanName)
            showStatus("Updated display name to '$cleanName'")
        }
    }

    fun clearAllConversationHistory() {
        viewModelScope.launch {
            dao.clearAllConversations()
            _activeConversationId.value = null
            showStatus("Cleared all local conversation history")
        }
    }

    fun clearAllBrowserHistory() {
        viewModelScope.launch {
            dao.clearBrowserHistory()
            showStatus("Cleared local browser history")
        }
    }

    fun clearCachedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            withContext(Dispatchers.Main) {
                showStatus("Cleared temporary application cache")
            }
        }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            secretVault.clearAllKeys()
            dao.clearAllConversations()
            dao.clearBrowserHistory()
            dao.clearNotes()
            dao.clearPrompts()
            dao.clearBookmarks()
            dao.clearTasks()
            dao.clearReminders()
            dao.clearAIUsage()
            NomaticDatabase.seedDefaultsIfEmpty(dao)
            refreshMaskedKeys()
            showStatus("All personal keys and local workspace data deleted")
        }
    }

    fun runModelArena(
        prompt: String,
        providerA: String,
        modelA: String,
        providerB: String,
        modelB: String,
        onComplete: (ArenaResult, ArenaResult) -> Unit
    ) {
        viewModelScope.launch {
            _isDeveloperLoading.value = true
            val deferredA = async(Dispatchers.IO) {
                val start = System.currentTimeMillis()
                val req = AIRequest(
                    conversationId = "arena_bench",
                    messages = listOf(AIMessageInput(role = "user", content = prompt)),
                    preferredProviderId = providerA,
                    preferredModelId = modelA
                )
                val res = requestRouter.routeUnaryRequest(req)
                val latency = System.currentTimeMillis() - start
                if (res.isSuccess) {
                    val data = res.getOrThrow()
                    ArenaResult(
                        providerId = providerA,
                        modelId = modelA,
                        responseText = data.content,
                        latencyMs = latency,
                        inputTokens = data.inputTokens,
                        outputTokens = data.outputTokens
                    )
                } else {
                    ArenaResult(
                        providerId = providerA,
                        modelId = modelA,
                        responseText = "",
                        latencyMs = latency,
                        inputTokens = null,
                        outputTokens = null,
                        error = res.exceptionOrNull()?.message ?: "Provider query failed"
                    )
                }
            }

            val deferredB = async(Dispatchers.IO) {
                val start = System.currentTimeMillis()
                val req = AIRequest(
                    conversationId = "arena_bench",
                    messages = listOf(AIMessageInput(role = "user", content = prompt)),
                    preferredProviderId = providerB,
                    preferredModelId = modelB
                )
                val res = requestRouter.routeUnaryRequest(req)
                val latency = System.currentTimeMillis() - start
                if (res.isSuccess) {
                    val data = res.getOrThrow()
                    ArenaResult(
                        providerId = providerB,
                        modelId = modelB,
                        responseText = data.content,
                        latencyMs = latency,
                        inputTokens = data.inputTokens,
                        outputTokens = data.outputTokens
                    )
                } else {
                    ArenaResult(
                        providerId = providerB,
                        modelId = modelB,
                        responseText = "",
                        latencyMs = latency,
                        inputTokens = null,
                        outputTokens = null,
                        error = res.exceptionOrNull()?.message ?: "Provider query failed"
                    )
                }
            }

            val resA = deferredA.await()
            val resB = deferredB.await()
            _isDeveloperLoading.value = false
            onComplete(resA, resB)
        }
    }
}

data class ArenaResult(
    val providerId: String,
    val modelId: String,
    val responseText: String,
    val latencyMs: Long,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val error: String? = null
)
