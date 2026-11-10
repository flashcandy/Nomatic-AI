package com.example

import com.example.core.ai.AIErrorCategory
import com.example.core.ai.AIFallbackManager
import com.example.core.ai.AIProviderException
import com.example.core.security.AndroidKeystoreVault
import com.example.core.utilities.DeveloperSafetyAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun apiKeyMasking_hidesMiddleCharacters() {
        val masked = AndroidKeystoreVault.maskKey("sk-proj-1234567890abcdef")
        assertEquals("sk-••••••••••••cdef", masked)
        assertNull(AndroidKeystoreVault.maskKey(""))
    }

    @Test
    fun developerSafetyAnalyzer_flagsDestructiveCommands() {
        val safeReport = DeveloperSafetyAnalyzer.inspectContent("ls -la /var/log && df -h")
        assertFalse(safeReport.isDestructive)

        val destructiveLinux = DeveloperSafetyAnalyzer.inspectContent("sudo rm -rf /var/lib/docker")
        assertTrue(destructiveLinux.isDestructive)

        val destructiveAbInitio = DeveloperSafetyAnalyzer.inspectContent("air object rm /Projects/prod/mp/load.mp")
        assertTrue(destructiveAbInitio.isDestructive)
    }

    @Test
    fun aiFallbackManager_neverBlindlyFallsBackOnInvalidRequest() {
        val manager = AIFallbackManager()
        val invalidReqError = AIProviderException("openai", AIErrorCategory.INVALID_REQUEST, "400 Bad Request")
        assertFalse(manager.shouldFallbackToSecondary(invalidReqError, autoFallbackEnabled = true))

        val timeoutError = AIProviderException("openai", AIErrorCategory.TIMEOUT, "Timed out")
        assertTrue(manager.shouldFallbackToSecondary(timeoutError, autoFallbackEnabled = true))
        assertTrue(manager.shouldRetryPrimary(timeoutError, attempt = 0, maxRetries = 2))
    }

    @Test
    fun geminiModelSanitizer_remapsSunsetModels() {
        assertEquals("gemini-3.8-flash", com.example.core.ai.GeminiProvider.sanitizeGeminiModelId("gemini-2.5-flash"))
        assertEquals("gemini-3.8-flash", com.example.core.ai.GeminiProvider.sanitizeGeminiModelId("gemini-1.5-pro"))
        assertEquals("gemini-3.8-flash", com.example.core.ai.GeminiProvider.sanitizeGeminiModelId(null))
        assertEquals("gemini-4-argon", com.example.core.ai.GeminiProvider.sanitizeGeminiModelId("gemini-4-argon"))
    }

    @Test
    fun openRouterModelSanitizer_remapsSunsetModelsToFreeAutoOrLatest() {
        assertEquals("openrouter/free", com.example.core.ai.OpenAIProvider.sanitizeModelId("meta-llama/llama-3.3-70b-instruct:free", isOpenRouter = true))
        assertEquals("openrouter/free", com.example.core.ai.OpenAIProvider.sanitizeModelId("o3-mini", isOpenRouter = true))
        assertEquals("gpt-4o-mini", com.example.core.ai.OpenAIProvider.sanitizeModelId("o3-mini", isOpenRouter = false))
        assertEquals("anthropic/claude-sonnet-5.5", com.example.core.ai.OpenAIProvider.sanitizeModelId("anthropic/claude-3.5-sonnet", isOpenRouter = true))
        assertEquals("openrouter/free", com.example.core.ai.OpenAIProvider.sanitizeModelId(null, isOpenRouter = true))
        assertEquals("gpt-4o-mini", com.example.core.ai.OpenAIProvider.sanitizeModelId(null, isOpenRouter = false))
    }
}
