package com.example.core.ai

import com.example.core.security.SecretVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAIProvider(
    private val secretVault: SecretVault,
    private val baseClient: OkHttpClient = OkHttpClient()
) : AIProvider {

    override val providerId: String = "openai"
    override val displayName: String = "OpenAI & OpenRouter"

    companion object {
        private const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

        val DEFAULT_MODELS = listOf(
            // OpenRouter Active Free Models (Free Tier)
            AIModel(
                id = "openrouter/free",
                displayName = "OpenRouter Auto (Free)",
                providerId = "openai",
                contextWindow = 200_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "google/gemma-4-31b-it:free",
                displayName = "Gemma 4 31B (Free)",
                providerId = "openai",
                contextWindow = 262_144,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "google/gemma-4-26b-a4b-it:free",
                displayName = "Gemma 4 26B (Free)",
                providerId = "openai",
                contextWindow = 262_144,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "nvidia/nemotron-3.5-lightning:free",
                displayName = "Nemotron 3.5 Lightning (Free)",
                providerId = "openai",
                contextWindow = 1_000_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "nvidia/nemotron-3-ultra-550b-a55b:free",
                displayName = "Nemotron 3 Ultra 550B (Free)",
                providerId = "openai",
                contextWindow = 1_000_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "nvidia/nemotron-3.5-super-vl:free",
                displayName = "Nemotron 3.5 Super VL (Free)",
                providerId = "openai",
                contextWindow = 1_000_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "liquid/lfm-2.5-2.6b:free",
                displayName = "Liquid LFM 2.5 (Free)",
                providerId = "openai",
                contextWindow = 65_536,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "cohere/north-mini-code:free",
                displayName = "Cohere Code (Free)",
                providerId = "openai",
                contextWindow = 256_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "recursive/flint-v1.0:free",
                displayName = "Flint v1.0 (Free)",
                providerId = "openai",
                contextWindow = 262_144,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "xingchen-agi/xing4.0-29b-a4b:free",
                displayName = "XingChen 4.0 (Free)",
                providerId = "openai",
                contextWindow = 262_144,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),
            AIModel(
                id = "dots-studio/dots-3-note-preview:free",
                displayName = "Dots3 Note (Free)",
                providerId = "openai",
                contextWindow = 512_000,
                inputCostPer1M = 0.0,
                outputCostPer1M = 0.0
            ),

            // Anthropic Claude on OpenRouter (Latest Non-Sunset Models)
            AIModel(
                id = "anthropic/claude-sonnet-5.5",
                displayName = "Claude 5.5 Sonnet",
                providerId = "openai",
                contextWindow = 200_000,
                inputCostPer1M = 2.00,
                outputCostPer1M = 10.00
            ),
            AIModel(
                id = "anthropic/claude-haiku-5.5",
                displayName = "Claude 5.5 Haiku",
                providerId = "openai",
                contextWindow = 200_000,
                inputCostPer1M = 0.10,
                outputCostPer1M = 0.50
            ),
            AIModel(
                id = "anthropic/claude-opus-5.5",
                displayName = "Claude 5.5 Opus",
                providerId = "openai",
                contextWindow = 200_000,
                inputCostPer1M = 4.00,
                outputCostPer1M = 20.00
            ),
            AIModel(
                id = "anthropic/claude-sonnet-4.5",
                displayName = "Claude 4.5 Sonnet",
                providerId = "openai",
                contextWindow = 200_000,
                inputCostPer1M = 3.00,
                outputCostPer1M = 15.00
            ),

            // Google Gemini on OpenRouter
            AIModel(
                id = "google/gemini-4-argon",
                displayName = "Gemini 4 Argon (OpenRouter)",
                providerId = "openai",
                contextWindow = 2_097_152,
                inputCostPer1M = 1.25,
                outputCostPer1M = 5.00
            ),
            AIModel(
                id = "google/gemini-3.5-flash-lite",
                displayName = "Gemini 3.5 Flash-Lite (OpenRouter)",
                providerId = "openai",
                contextWindow = 1_048_576,
                inputCostPer1M = 0.075,
                outputCostPer1M = 0.30
            ),
            AIModel(
                id = "google/gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash (OpenRouter)",
                providerId = "openai",
                contextWindow = 1_048_576,
                inputCostPer1M = 0.15,
                outputCostPer1M = 0.60
            ),

            // OpenAI Standard Models (for direct OpenAI keys)
            AIModel(
                id = "gpt-4o-mini",
                displayName = "GPT-4o Mini",
                providerId = "openai",
                contextWindow = 128_000,
                inputCostPer1M = 0.15,
                outputCostPer1M = 0.60
            ),
            AIModel(
                id = "gpt-4o",
                displayName = "GPT-4o",
                providerId = "openai",
                contextWindow = 128_000,
                inputCostPer1M = 2.50,
                outputCostPer1M = 10.00
            )
        )

        fun sanitizeModelId(raw: String?, isOpenRouter: Boolean): String {
            if (raw.isNullOrBlank()) {
                return if (isOpenRouter) "openrouter/free" else "gpt-4o-mini"
            }
            return when (raw) {
                "o3-mini" -> if (isOpenRouter) "openrouter/free" else "gpt-4o-mini"
                "anthropic/claude-3.5-sonnet", "claude-3-5-sonnet" -> "anthropic/claude-sonnet-5.5"
                "anthropic/claude-3.5-haiku", "claude-3-5-haiku" -> "anthropic/claude-haiku-5.5"
                "meta-llama/llama-3.3-70b-instruct:free",
                "meta-llama/llama-3.1-8b-instruct:free",
                "deepseek/deepseek-r1:free",
                "deepseek/deepseek-chat:free",
                "google/gemini-2.0-flash-exp:free",
                "qwen/qwen-2.5-coder-32b-instruct:free",
                "mistralai/mistral-7b-instruct:free" -> "openrouter/free"
                "gpt-4o-mini" -> if (isOpenRouter) "openrouter/free" else "gpt-4o-mini"
                else -> raw
            }
        }
    }

    private fun getBaseUrl(apiKey: String): String {
        return if (apiKey.startsWith("sk-or-", ignoreCase = true)) {
            OPENROUTER_BASE_URL
        } else {
            OPENAI_BASE_URL
        }
    }

    private fun requireKey(): String {
        val key = secretVault.getApiKey(providerId)
        if (key.isNullOrBlank()) {
            throw AIProviderException(
                providerId = providerId,
                category = AIErrorCategory.INVALID_API_KEY,
                message = "OpenAI API key is not configured. Add your personal key in AI Provider Management."
            )
        }
        return key
    }

    override suspend fun validateConfiguration(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireKey()
            val baseUrl = getBaseUrl(apiKey)
            val client = baseClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://nomatic.ai")
                .header("X-Title", "Nomatic AI")
                .get()
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(mapHttpError(response.code, body, response.header("Retry-After"), baseUrl))
                }
            }
        } catch (e: AIProviderException) {
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.TIMEOUT, "Connection timed out.", cause = e))
        } catch (e: IOException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.NETWORK_ERROR, "Network error reaching provider: ${e.localizedMessage}", cause = e))
        } catch (e: Exception) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.UNKNOWN, e.localizedMessage ?: "Validation failed", cause = e))
        }
    }

    override suspend fun getModels(): Result<List<AIModel>> = withContext(Dispatchers.IO) {
        Result.success(DEFAULT_MODELS)
    }

    override suspend fun sendMessage(request: AIRequest): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireKey()
            val baseUrl = getBaseUrl(apiKey)
            val isOpRouter = baseUrl == OPENROUTER_BASE_URL
            val modelId = sanitizeModelId(request.preferredModelId, isOpRouter)
            val payload = buildChatJson(request, modelId, stream = false)

            val client = baseClient.newBuilder()
                .connectTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()

            val httpReq = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://nomatic.ai")
                .header("X-Title", "Nomatic AI")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpReq).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        mapHttpError(response.code, rawBody, response.header("Retry-After"), baseUrl)
                    )
                }
                val json = JSONObject(rawBody)
                val choices = json.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val messageObj = firstChoice?.optJSONObject("message")
                val content = messageObj?.optString("content").orEmpty()

                val usage = json.optJSONObject("usage")
                val promptTokens = if (usage != null && usage.has("prompt_tokens")) usage.optLong("prompt_tokens") else null
                val completionTokens = if (usage != null && usage.has("completion_tokens")) usage.optLong("completion_tokens") else null

                Result.success(
                    AIResponse(
                        content = content,
                        providerId = providerId,
                        modelId = modelId,
                        inputTokens = promptTokens,
                        outputTokens = completionTokens,
                        isTokenReportedByProvider = usage != null
                    )
                )
            }
        } catch (e: AIProviderException) {
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.TIMEOUT, "Request timed out after ${request.timeoutSeconds}s.", cause = e))
        } catch (e: IOException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.NETWORK_ERROR, "Network error communicating with AI provider.", cause = e))
        } catch (e: Exception) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.UNKNOWN, e.localizedMessage ?: "Provider error", cause = e))
        }
    }

    override fun streamMessage(request: AIRequest): Flow<AIStreamEvent> = flow {
        val apiKey = requireKey()
        val baseUrl = getBaseUrl(apiKey)
        val isOpRouter = baseUrl == OPENROUTER_BASE_URL
        val modelId = sanitizeModelId(request.preferredModelId, isOpRouter)
        val payload = buildChatJson(request, modelId, stream = true)

        val client = baseClient.newBuilder()
            .connectTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        val httpReq = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://nomatic.ai")
            .header("X-Title", "Nomatic AI")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        emit(AIStreamEvent.Started(providerId = providerId, modelId = modelId))

        val accumulated = StringBuilder()
        var reportedInputTokens: Long? = null
        var reportedOutputTokens: Long? = null

        client.newCall(httpReq).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                throw mapHttpError(response.code, errBody, response.header("Retry-After"), baseUrl)
            }

            val stream = response.body?.byteStream()
                ?: throw AIProviderException(providerId, AIErrorCategory.SERVER_ERROR, "Empty response stream from OpenAI.")

            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    currentCoroutineContext().ensureActive()
                    val trimmed = line?.trim() ?: continue
                    if (!trimmed.startsWith("data:")) continue
                    val dataStr = trimmed.removePrefix("data:").trim()
                    if (dataStr == "[DONE]") break
                    if (dataStr.isEmpty()) continue

                    try {
                        val chunkJson = JSONObject(dataStr)
                        val usageObj = chunkJson.optJSONObject("usage")
                        if (usageObj != null) {
                            if (usageObj.has("prompt_tokens")) reportedInputTokens = usageObj.optLong("prompt_tokens")
                            if (usageObj.has("completion_tokens")) reportedOutputTokens = usageObj.optLong("completion_tokens")
                        }
                        val choices = chunkJson.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val contentDelta = delta?.optString("content", "") ?: ""
                            if (contentDelta.isNotEmpty()) {
                                accumulated.append(contentDelta)
                                emit(
                                    AIStreamEvent.Token(
                                        delta = contentDelta,
                                        accumulated = accumulated.toString(),
                                        providerId = providerId,
                                        modelId = modelId
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore malformed keep-alive frame
                    }
                }
            }
        }

        emit(
            AIStreamEvent.Completed(
                fullText = accumulated.toString(),
                providerId = providerId,
                modelId = modelId,
                inputTokens = reportedInputTokens,
                outputTokens = reportedOutputTokens,
                isTokenReportedByProvider = reportedInputTokens != null || reportedOutputTokens != null
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun buildChatJson(request: AIRequest, modelId: String, stream: Boolean): JSONObject {
        val messagesArray = JSONArray()
        if (!request.systemPrompt.isNullOrBlank()) {
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", request.systemPrompt)
                }
            )
        }
        request.messages.forEach { msg ->
            val mappedRole = when (msg.role.lowercase()) {
                "user", "assistant", "system" -> msg.role.lowercase()
                else -> "user"
            }
            messagesArray.put(
                JSONObject().apply {
                    put("role", mappedRole)
                    put("content", msg.content)
                }
            )
        }

        return JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArray)
            if (!modelId.startsWith("o1") && !modelId.startsWith("o3")) {
                put("temperature", request.temperature)
            }
            put("stream", stream)
            if (stream) {
                put("stream_options", JSONObject().apply { put("include_usage", true) })
            }
        }
    }

    private fun mapHttpError(code: Int, rawBody: String, retryAfterHeader: String?, baseUrl: String): AIProviderException {
        val providerName = if (baseUrl == OPENROUTER_BASE_URL) "OpenRouter" else "OpenAI"
        val serverMessage = try {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        } ?: "HTTP $code from $providerName"

        val retryAfter = retryAfterHeader?.toLongOrNull()

        return when (code) {
            401 -> AIProviderException(providerId, AIErrorCategory.INVALID_API_KEY, "Invalid $providerName API key: $serverMessage")
            403 -> AIProviderException(providerId, AIErrorCategory.PERMISSION_DENIED, "$providerName permission denied: $serverMessage")
            429 -> AIProviderException(providerId, AIErrorCategory.RATE_LIMIT, "$providerName rate limit reached: $serverMessage", retryAfterSeconds = retryAfter)
            400, 422 -> AIProviderException(providerId, AIErrorCategory.INVALID_REQUEST, "Invalid $providerName request: $serverMessage")
            in 500..599 -> AIProviderException(providerId, AIErrorCategory.SERVER_ERROR, "$providerName server error ($code): $serverMessage")
            else -> AIProviderException(providerId, AIErrorCategory.UNKNOWN, "$providerName HTTP $code: $serverMessage")
        }
    }
}
