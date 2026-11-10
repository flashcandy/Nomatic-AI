package com.example.core.ai

import com.example.core.security.SecretVault
import com.example.data.local.AIUsageEntity
import com.example.data.local.NomaticDao
import com.example.data.local.ProviderSettingsEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID

enum class ProviderHealthStatus {
    HEALTHY,
    UNCONFIGURED,
    DEGRADED,
    INVALID_KEY
}

class AIProviderRegistry(
    secretVault: SecretVault
) {
    private val openAIProvider = OpenAIProvider(secretVault)
    private val geminiProvider = GeminiProvider(secretVault)

    private val providers = mapOf(
        "openai" to openAIProvider,
        "gemini" to geminiProvider
    )

    fun getProvider(providerId: String): AIProvider? = providers[providerId.lowercase()]

    fun getAllProviders(): List<AIProvider> = providers.values.toList()

    fun getAllModels(): List<AIModel> = OpenAIProvider.DEFAULT_MODELS + GeminiProvider.DEFAULT_MODELS
}

class AIFallbackManager {
    fun shouldRetryPrimary(error: AIProviderException, attempt: Int, maxRetries: Int): Boolean {
        if (attempt >= maxRetries) return false
        return when (error.category) {
            AIErrorCategory.TIMEOUT,
            AIErrorCategory.SERVER_ERROR,
            AIErrorCategory.NETWORK_ERROR -> true
            AIErrorCategory.RATE_LIMIT -> attempt == 0
            AIErrorCategory.INVALID_API_KEY,
            AIErrorCategory.PERMISSION_DENIED,
            AIErrorCategory.INVALID_REQUEST,
            AIErrorCategory.UNKNOWN -> false
        }
    }

    fun shouldFallbackToSecondary(error: AIProviderException, autoFallbackEnabled: Boolean): Boolean {
        if (!autoFallbackEnabled) return false
        return when (error.category) {
            AIErrorCategory.TIMEOUT,
            AIErrorCategory.RATE_LIMIT,
            AIErrorCategory.SERVER_ERROR,
            AIErrorCategory.NETWORK_ERROR,
            AIErrorCategory.INVALID_API_KEY,
            AIErrorCategory.PERMISSION_DENIED -> true
            AIErrorCategory.INVALID_REQUEST -> false // Never blindly fallback on invalid request
            AIErrorCategory.UNKNOWN -> true
        }
    }
}

class AIUsageTracker(
    private val dao: NomaticDao,
    private val registry: AIProviderRegistry
) {
    suspend fun recordUsage(
        providerId: String,
        modelId: String,
        inputTokens: Long?,
        outputTokens: Long?,
        status: String
    ) {
        val model = registry.getAllModels().find { it.id == modelId }
        val estimatedCost = if (inputTokens != null && outputTokens != null && model != null) {
            val inCost = (inputTokens.toDouble() / 1_000_000.0) * model.inputCostPer1M
            val outCost = (outputTokens.toDouble() / 1_000_000.0) * model.outputCostPer1M
            inCost + outCost
        } else {
            null
        }

        dao.insertAIUsage(
            AIUsageEntity(
                id = UUID.randomUUID().toString(),
                providerId = providerId,
                modelId = modelId,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                estimatedCost = estimatedCost,
                requestStatus = status,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

class AIRequestRouter(
    private val registry: AIProviderRegistry,
    private val fallbackManager: AIFallbackManager,
    private val usageTracker: AIUsageTracker,
    private val secretVault: SecretVault,
    private val dao: NomaticDao
) {
    private val _healthState = MutableStateFlow<Map<String, ProviderHealthStatus>>(
        mapOf(
            "openai" to if (secretVault.hasApiKey("openai")) ProviderHealthStatus.HEALTHY else ProviderHealthStatus.UNCONFIGURED,
            "gemini" to if (secretVault.hasApiKey("gemini")) ProviderHealthStatus.HEALTHY else ProviderHealthStatus.UNCONFIGURED
        )
    )
    val healthState: StateFlow<Map<String, ProviderHealthStatus>> = _healthState.asStateFlow()

    fun refreshKeyHealth() {
        val current = _healthState.value.toMutableMap()
        listOf("openai", "gemini").forEach { id ->
            if (!secretVault.hasApiKey(id)) {
                current[id] = ProviderHealthStatus.UNCONFIGURED
            } else if (current[id] == ProviderHealthStatus.UNCONFIGURED || current[id] == ProviderHealthStatus.INVALID_KEY) {
                current[id] = ProviderHealthStatus.HEALTHY
            }
        }
        _healthState.value = current
    }

    fun updateProviderHealth(providerId: String, status: ProviderHealthStatus) {
        val current = _healthState.value.toMutableMap()
        current[providerId.lowercase()] = status
        _healthState.value = current
    }

    suspend fun routeUnaryRequest(request: AIRequest): Result<AIResponse> {
        val settings = dao.getProviderSettingsOnce() ?: ProviderSettingsEntity()
        val primaryId = (request.preferredProviderId ?: settings.primaryProvider).lowercase()
        val secondaryId = if (primaryId == "openai") "gemini" else "openai"

        val primaryResult = executeWithRetry(primaryId, request, settings)
        if (primaryResult.isSuccess) {
            val res = primaryResult.getOrThrow()
            updateProviderHealth(primaryId, ProviderHealthStatus.HEALTHY)
            usageTracker.recordUsage(res.providerId, res.modelId, res.inputTokens, res.outputTokens, "SUCCESS")
            return Result.success(res)
        }

        val primaryError = primaryResult.exceptionOrNull() as? AIProviderException
            ?: AIProviderException(primaryId, AIErrorCategory.UNKNOWN, "Primary provider failed")

        markFailureHealth(primaryId, primaryError)
        usageTracker.recordUsage(primaryId, resolveModelForProvider(primaryId, request, settings), null, null, "FAILED")

        val isSecondaryEnabled = if (secondaryId == "openai") settings.openAiEnabled else settings.geminiEnabled
        if (isSecondaryEnabled &&
            secretVault.hasApiKey(secondaryId) &&
            fallbackManager.shouldFallbackToSecondary(primaryError, settings.autoFallbackEnabled)
        ) {
            val fallbackReq = request.copy(
                preferredProviderId = secondaryId,
                preferredModelId = resolveModelForProvider(secondaryId, request.copy(preferredModelId = null), settings)
            )
            val secondaryResult = executeWithRetry(secondaryId, fallbackReq, settings)
            if (secondaryResult.isSuccess) {
                val res = secondaryResult.getOrThrow().copy(
                    isFallback = true,
                    primaryFailureReason = "${primaryId.uppercase()}: ${primaryError.message}"
                )
                updateProviderHealth(secondaryId, ProviderHealthStatus.HEALTHY)
                usageTracker.recordUsage(res.providerId, res.modelId, res.inputTokens, res.outputTokens, "FALLBACK_SUCCESS")
                return Result.success(res)
            } else {
                val secErr = secondaryResult.exceptionOrNull() as? AIProviderException
                if (secErr != null) markFailureHealth(secondaryId, secErr)
                usageTracker.recordUsage(secondaryId, resolveModelForProvider(secondaryId, fallbackReq, settings), null, null, "FAILED")
            }
        }

        return Result.failure(primaryError)
    }

    fun routeStreamingRequest(request: AIRequest): Flow<AIStreamEvent> = flow {
        val settings = dao.getProviderSettingsOnce() ?: ProviderSettingsEntity()
        val primaryId = (request.preferredProviderId ?: settings.primaryProvider).lowercase()
        val secondaryId = if (primaryId == "openai") "gemini" else "openai"
        val primaryProvider = registry.getProvider(primaryId)
            ?: throw AIProviderException(primaryId, AIErrorCategory.INVALID_REQUEST, "Unknown provider $primaryId")

        val primaryModel = resolveModelForProvider(primaryId, request, settings)
        val normalizedPrimaryReq = request.copy(
            preferredProviderId = primaryId,
            preferredModelId = primaryModel,
            timeoutSeconds = settings.timeoutSeconds
        )

        var emittedAnyToken = false
        val partialBuffer = StringBuilder()

        try {
            primaryProvider.streamMessage(normalizedPrimaryReq).collect { event ->
                when (event) {
                    is AIStreamEvent.Started -> emit(event)
                    is AIStreamEvent.Token -> {
                        emittedAnyToken = true
                        partialBuffer.clear()
                        partialBuffer.append(event.accumulated)
                        emit(event)
                    }
                    is AIStreamEvent.Completed -> {
                        updateProviderHealth(primaryId, ProviderHealthStatus.HEALTHY)
                        usageTracker.recordUsage(
                            event.providerId,
                            event.modelId,
                            event.inputTokens,
                            event.outputTokens,
                            "SUCCESS"
                        )
                        emit(event)
                    }
                    is AIStreamEvent.Error -> emit(event)
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val primaryError = (t as? AIProviderException) ?: AIProviderException(
                providerId = primaryId,
                category = AIErrorCategory.NETWORK_ERROR,
                message = t.localizedMessage ?: "Stream interrupted",
                cause = t
            )
            markFailureHealth(primaryId, primaryError)
            usageTracker.recordUsage(primaryId, primaryModel, null, null, "FAILED")

            val isSecondaryReady = (if (secondaryId == "openai") settings.openAiEnabled else settings.geminiEnabled) &&
                secretVault.hasApiKey(secondaryId)

            // CRITICAL STREAMING FALLBACK REQUIREMENT (Section 5):
            // Once response tokens have been displayed, NEVER silently restart the same request
            // with another provider and concatenate outputs. Preserve partial response and offer retry.
            if (emittedAnyToken) {
                emit(
                    AIStreamEvent.Error(
                        exception = primaryError,
                        partialText = partialBuffer.toString(),
                        canRetryWithFallback = isSecondaryReady,
                        fallbackProviderId = if (isSecondaryReady) secondaryId else null
                    )
                )
                return@flow
            }

            // If no tokens were emitted yet and auto-fallback is enabled, transparently switch to secondary
            if (isSecondaryReady && fallbackManager.shouldFallbackToSecondary(primaryError, settings.autoFallbackEnabled)) {
                val secondaryProvider = registry.getProvider(secondaryId)!!
                val secondaryModel = resolveModelForProvider(secondaryId, request.copy(preferredModelId = null), settings)
                val fallbackReq = request.copy(
                    preferredProviderId = secondaryId,
                    preferredModelId = secondaryModel,
                    timeoutSeconds = settings.timeoutSeconds
                )
                try {
                    secondaryProvider.streamMessage(fallbackReq).collect { secEvent ->
                        when (secEvent) {
                            is AIStreamEvent.Started -> emit(
                                secEvent.copy(
                                    isFallback = true,
                                    fallbackNotice = "Switched to ${secondaryProvider.displayName} (${primaryError.category.name})"
                                )
                            )
                            is AIStreamEvent.Token -> {
                                partialBuffer.clear()
                                partialBuffer.append(secEvent.accumulated)
                                emit(secEvent)
                            }
                            is AIStreamEvent.Completed -> {
                                updateProviderHealth(secondaryId, ProviderHealthStatus.HEALTHY)
                                usageTracker.recordUsage(
                                    secEvent.providerId,
                                    secEvent.modelId,
                                    secEvent.inputTokens,
                                    secEvent.outputTokens,
                                    "FALLBACK_SUCCESS"
                                )
                                emit(secEvent.copy(isFallback = true))
                            }
                            is AIStreamEvent.Error -> emit(secEvent)
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (secThrowable: Throwable) {
                    val secError = (secThrowable as? AIProviderException) ?: AIProviderException(
                        providerId = secondaryId,
                        category = AIErrorCategory.NETWORK_ERROR,
                        message = secThrowable.localizedMessage ?: "Fallback stream failed",
                        cause = secThrowable
                    )
                    markFailureHealth(secondaryId, secError)
                    usageTracker.recordUsage(secondaryId, secondaryModel, null, null, "FAILED")
                    emit(
                        AIStreamEvent.Error(
                            exception = secError,
                            partialText = partialBuffer.toString(),
                            canRetryWithFallback = false,
                            fallbackProviderId = null
                        )
                    )
                }
            } else {
                emit(
                    AIStreamEvent.Error(
                        exception = primaryError,
                        partialText = "",
                        canRetryWithFallback = isSecondaryReady,
                        fallbackProviderId = if (isSecondaryReady) secondaryId else null
                    )
                )
            }
        }
    }

    private suspend fun executeWithRetry(
        providerId: String,
        request: AIRequest,
        settings: ProviderSettingsEntity
    ): Result<AIResponse> {
        val isEnabled = if (providerId == "openai") settings.openAiEnabled else settings.geminiEnabled
        if (!isEnabled) {
            return Result.failure(
                AIProviderException(providerId, AIErrorCategory.PERMISSION_DENIED, "${providerId.uppercase()} is disabled in settings.")
            )
        }
        val provider = registry.getProvider(providerId)
            ?: return Result.failure(AIProviderException(providerId, AIErrorCategory.INVALID_REQUEST, "Unknown provider $providerId"))

        val modelId = resolveModelForProvider(providerId, request, settings)
        val normalizedReq = request.copy(
            preferredProviderId = providerId,
            preferredModelId = modelId,
            timeoutSeconds = settings.timeoutSeconds
        )

        var attempt = 0
        var lastError: AIProviderException? = null
        while (attempt <= settings.maxRetries) {
            val result = provider.sendMessage(normalizedReq)
            if (result.isSuccess) return result

            val err = result.exceptionOrNull() as? AIProviderException
                ?: AIProviderException(providerId, AIErrorCategory.UNKNOWN, "Execution failed")
            lastError = err

            if (!fallbackManager.shouldRetryPrimary(err, attempt, settings.maxRetries)) {
                break
            }
            val waitMs = if (err.category == AIErrorCategory.RATE_LIMIT && err.retryAfterSeconds != null) {
                (err.retryAfterSeconds.coerceAtMost(5L)) * 1000L
            } else {
                800L * (attempt + 1)
            }
            delay(waitMs)
            attempt++
        }
        return Result.failure(lastError ?: AIProviderException(providerId, AIErrorCategory.UNKNOWN, "Provider failed"))
    }

    private fun resolveModelForProvider(
        providerId: String,
        request: AIRequest,
        settings: ProviderSettingsEntity
    ): String {
        if (providerId.equals("gemini", ignoreCase = true)) {
            val reqModel = request.preferredModelId
            if (!reqModel.isNullOrBlank()) {
                val sanitized = GeminiProvider.sanitizeGeminiModelId(reqModel)
                val belongsToProvider = registry.getAllModels().any { it.id == sanitized && it.providerId == "gemini" }
                if (belongsToProvider) return sanitized
            }
            return GeminiProvider.sanitizeGeminiModelId(settings.geminiDefaultModel)
        } else {
            val apiKey = secretVault.getApiKey("openai").orEmpty()
            val isOpRouter = apiKey.startsWith("sk-or-", ignoreCase = true)
            val reqModel = request.preferredModelId
            if (!reqModel.isNullOrBlank()) {
                val sanitized = OpenAIProvider.sanitizeModelId(reqModel, isOpRouter)
                val belongsToProvider = registry.getAllModels().any { it.id == sanitized && it.providerId == "openai" }
                if (belongsToProvider) return sanitized
            }
            return OpenAIProvider.sanitizeModelId(settings.openAiDefaultModel, isOpRouter)
        }
    }

    private fun markFailureHealth(providerId: String, error: AIProviderException) {
        val status = when (error.category) {
            AIErrorCategory.INVALID_API_KEY, AIErrorCategory.PERMISSION_DENIED -> ProviderHealthStatus.INVALID_KEY
            else -> ProviderHealthStatus.DEGRADED
        }
        updateProviderHealth(providerId, status)
    }
}
