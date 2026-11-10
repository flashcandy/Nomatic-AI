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
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val secretVault: SecretVault,
    private val baseClient: OkHttpClient = OkHttpClient()
) : AIProvider {

    override val providerId: String = "gemini"
    override val displayName: String = "Google Gemini"

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        val DEFAULT_MODELS = listOf(
            AIModel(
                id = "gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash (Latest)",
                providerId = "gemini",
                contextWindow = 1_048_576,
                inputCostPer1M = 0.15,
                outputCostPer1M = 0.60
            ),
            AIModel(
                id = "gemini-3.5-flash-lite",
                displayName = "Gemini 3.5 Flash-Lite (Fast)",
                providerId = "gemini",
                contextWindow = 1_048_576,
                inputCostPer1M = 0.075,
                outputCostPer1M = 0.30
            ),
            AIModel(
                id = "gemini-4-argon",
                displayName = "Gemini 4 Argon (Reasoning)",
                providerId = "gemini",
                contextWindow = 2_097_152,
                inputCostPer1M = 1.25,
                outputCostPer1M = 5.00
            ),
            AIModel(
                id = "gemini-3.1-pro-preview",
                displayName = "Gemini 3.1 Pro (Preview)",
                providerId = "gemini",
                contextWindow = 2_097_152,
                inputCostPer1M = 1.25,
                outputCostPer1M = 5.00
            )
        )

        fun sanitizeGeminiModelId(raw: String?): String {
            if (raw.isNullOrBlank() ||
                raw == "gemini-2.5-flash" ||
                raw == "gemini-2.0-flash" ||
                raw.contains("1.5")
            ) {
                return "gemini-3.8-flash"
            }
            return raw
        }
    }

    private fun requireKey(): String {
        val key = secretVault.getApiKey(providerId)
        if (key.isNullOrBlank()) {
            throw AIProviderException(
                providerId = providerId,
                category = AIErrorCategory.INVALID_API_KEY,
                message = "Google Gemini API key is not configured. Add your personal key in AI Provider Management."
            )
        }
        return key
    }

    override suspend fun validateConfiguration(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireKey()
            val client = baseClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder()
                .url("$BASE_URL/models")
                .header("x-goog-api-key", apiKey)
                .get()
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(mapHttpError(response.code, body, response.header("Retry-After")))
                }
            }
        } catch (e: AIProviderException) {
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.TIMEOUT, "Gemini connection timed out.", cause = e))
        } catch (e: IOException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.NETWORK_ERROR, "Network error reaching Gemini: ${e.localizedMessage}", cause = e))
        } catch (e: Exception) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.UNKNOWN, e.localizedMessage ?: "Gemini validation failed", cause = e))
        }
    }

    override suspend fun getModels(): Result<List<AIModel>> = withContext(Dispatchers.IO) {
        Result.success(DEFAULT_MODELS)
    }

    override suspend fun sendMessage(request: AIRequest): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireKey()
            val modelId = sanitizeGeminiModelId(request.preferredModelId)
            val payload = buildGeminiPayload(request)

            val client = baseClient.newBuilder()
                .connectTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()

            val httpReq = Request.Builder()
                .url("$BASE_URL/models/$modelId:generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpReq).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        mapHttpError(response.code, rawBody, response.header("Retry-After"))
                    )
                }
                val json = JSONObject(rawBody)
                val text = extractCandidateText(json)
                val usage = json.optJSONObject("usageMetadata")
                val inputTokens = if (usage != null && usage.has("promptTokenCount")) usage.optLong("promptTokenCount") else null
                val outputTokens = if (usage != null && usage.has("candidatesTokenCount")) usage.optLong("candidatesTokenCount") else null

                Result.success(
                    AIResponse(
                        content = text,
                        providerId = providerId,
                        modelId = modelId,
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        isTokenReportedByProvider = usage != null
                    )
                )
            }
        } catch (e: AIProviderException) {
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.TIMEOUT, "Gemini request timed out after ${request.timeoutSeconds}s.", cause = e))
        } catch (e: IOException) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.NETWORK_ERROR, "Network error communicating with Gemini.", cause = e))
        } catch (e: Exception) {
            Result.failure(AIProviderException(providerId, AIErrorCategory.UNKNOWN, e.localizedMessage ?: "Gemini error", cause = e))
        }
    }

    override fun streamMessage(request: AIRequest): Flow<AIStreamEvent> = flow {
        val apiKey = requireKey()
        val modelId = sanitizeGeminiModelId(request.preferredModelId)
        val payload = buildGeminiPayload(request)

        val client = baseClient.newBuilder()
            .connectTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(request.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        val httpReq = Request.Builder()
            .url("$BASE_URL/models/$modelId:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
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
                throw mapHttpError(response.code, errBody, response.header("Retry-After"))
            }

            val stream = response.body?.byteStream()
                ?: throw AIProviderException(providerId, AIErrorCategory.SERVER_ERROR, "Empty response stream from Gemini.")

            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    currentCoroutineContext().ensureActive()
                    val trimmed = line?.trim() ?: continue
                    if (!trimmed.startsWith("data:")) continue
                    val dataStr = trimmed.removePrefix("data:").trim()
                    if (dataStr.isEmpty()) continue

                    try {
                        val chunkJson = JSONObject(dataStr)
                        val usage = chunkJson.optJSONObject("usageMetadata")
                        if (usage != null) {
                            if (usage.has("promptTokenCount")) reportedInputTokens = usage.optLong("promptTokenCount")
                            if (usage.has("candidatesTokenCount")) reportedOutputTokens = usage.optLong("candidatesTokenCount")
                        }
                        val deltaText = extractCandidateText(chunkJson)
                        if (deltaText.isNotEmpty()) {
                            accumulated.append(deltaText)
                            emit(
                                AIStreamEvent.Token(
                                    delta = deltaText,
                                    accumulated = accumulated.toString(),
                                    providerId = providerId,
                                    modelId = modelId
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Ignore non-JSON SSE comments
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

    private fun buildGeminiPayload(request: AIRequest): JSONObject {
        val contentsArray = JSONArray()
        request.messages.forEach { msg ->
            if (msg.role.equals("system", ignoreCase = true)) return@forEach
            val geminiRole = if (msg.role.equals("assistant", ignoreCase = true) || msg.role.equals("model", ignoreCase = true)) {
                "model"
            } else {
                "user"
            }
            contentsArray.put(
                JSONObject().apply {
                    put("role", geminiRole)
                    put(
                        "parts",
                        JSONArray().put(
                            JSONObject().apply { put("text", msg.content) }
                        )
                    )
                }
            )
        }

        return JSONObject().apply {
            if (!request.systemPrompt.isNullOrBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().apply { put("text", request.systemPrompt) }
                            )
                        )
                    }
                )
            }
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", request.temperature)
                }
            )
        }
    }

    private fun extractCandidateText(json: JSONObject): String {
        val candidates = json.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.optJSONObject(i)?.optString("text", "") ?: "")
        }
        return sb.toString()
    }

    private fun mapHttpError(code: Int, rawBody: String, retryAfterHeader: String?): AIProviderException {
        val serverMessage = try {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        } ?: "HTTP $code from Gemini"

        val retryAfter = retryAfterHeader?.toLongOrNull()

        return when (code) {
            400 -> {
                if (serverMessage.contains("API_KEY_INVALID", ignoreCase = true) || serverMessage.contains("API key not valid", ignoreCase = true)) {
                    AIProviderException(providerId, AIErrorCategory.INVALID_API_KEY, "Invalid Gemini API key: $serverMessage")
                } else {
                    AIProviderException(providerId, AIErrorCategory.INVALID_REQUEST, "Invalid Gemini request: $serverMessage")
                }
            }
            401 -> AIProviderException(providerId, AIErrorCategory.INVALID_API_KEY, "Invalid Gemini API key: $serverMessage")
            403 -> AIProviderException(providerId, AIErrorCategory.PERMISSION_DENIED, "Gemini API key lacks permission: $serverMessage")
            429 -> AIProviderException(providerId, AIErrorCategory.RATE_LIMIT, "Gemini rate limit reached: $serverMessage", retryAfterSeconds = retryAfter)
            in 500..599 -> AIProviderException(providerId, AIErrorCategory.SERVER_ERROR, "Gemini server error ($code): $serverMessage")
            else -> AIProviderException(providerId, AIErrorCategory.UNKNOWN, "Gemini HTTP $code: $serverMessage")
        }
    }
}
