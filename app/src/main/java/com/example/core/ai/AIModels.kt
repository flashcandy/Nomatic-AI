package com.example.core.ai

import kotlinx.coroutines.flow.Flow

data class AIModel(
    val id: String,
    val displayName: String,
    val providerId: String,
    val contextWindow: Int,
    val supportsStreaming: Boolean = true,
    val inputCostPer1M: Double,
    val outputCostPer1M: Double
)

data class AIMessageInput(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class AIRequest(
    val conversationId: String,
    val messages: List<AIMessageInput>,
    val preferredProviderId: String? = null,
    val preferredModelId: String? = null,
    val systemPrompt: String? = "You are Nomatic AI, a concise, technical, privacy-first personal intelligence assistant.",
    val temperature: Double = 0.3,
    val timeoutSeconds: Int = 45
)

data class AIResponse(
    val content: String,
    val providerId: String,
    val modelId: String,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val isTokenReportedByProvider: Boolean,
    val isFallback: Boolean = false,
    val primaryFailureReason: String? = null
)

enum class AIErrorCategory {
    INVALID_API_KEY,
    PERMISSION_DENIED,
    RATE_LIMIT,
    TIMEOUT,
    SERVER_ERROR,
    NETWORK_ERROR,
    INVALID_REQUEST,
    UNKNOWN
}

class AIProviderException(
    val providerId: String,
    val category: AIErrorCategory,
    override val message: String,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null
) : Exception(message, cause)

sealed class AIStreamEvent {
    data class Started(
        val providerId: String,
        val modelId: String,
        val isFallback: Boolean = false,
        val fallbackNotice: String? = null
    ) : AIStreamEvent()

    data class Token(
        val delta: String,
        val accumulated: String,
        val providerId: String,
        val modelId: String
    ) : AIStreamEvent()

    data class Completed(
        val fullText: String,
        val providerId: String,
        val modelId: String,
        val inputTokens: Long?,
        val outputTokens: Long?,
        val isTokenReportedByProvider: Boolean,
        val isFallback: Boolean = false
    ) : AIStreamEvent()

    data class Error(
        val exception: AIProviderException,
        val partialText: String,
        val canRetryWithFallback: Boolean,
        val fallbackProviderId: String?
    ) : AIStreamEvent()
}

interface AIProvider {
    val providerId: String
    val displayName: String

    suspend fun validateConfiguration(): Result<Unit>

    suspend fun getModels(): Result<List<AIModel>>

    suspend fun sendMessage(request: AIRequest): Result<AIResponse>

    fun streamMessage(request: AIRequest): Flow<AIStreamEvent>
}
