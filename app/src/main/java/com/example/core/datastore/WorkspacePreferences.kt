package com.example.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nomatic_workspace_preferences")

data class WorkspacePrefsState(
    val hasCompletedOnboarding: Boolean = false,
    val darkMode: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val defaultSearchEngine: String = "DuckDuckGo",
    val browserJavaScriptEnabled: Boolean = true,
    val browserHttpsOnly: Boolean = true,
    val browserSafeBrowsing: Boolean = true,
    val automationTelegramEnabled: Boolean = false,
    val automationTelegramChatId: String = "",
    val automationReminderNotifications: Boolean = true,
    val automationScheduledSummaries: Boolean = false,
    val automationShareToAssistant: Boolean = true,
    val automationClipboardToAssistant: Boolean = false,
    val automationBackgroundDocProcessing: Boolean = false,
    val userDisplayName: String = "Operator",
    val lastActiveConversationId: String? = null
)

class WorkspacePreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("has_completed_onboarding")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        val SEARCH_ENGINE = stringPreferencesKey("default_search_engine")
        val BROWSER_JS = booleanPreferencesKey("browser_js_enabled")
        val BROWSER_HTTPS = booleanPreferencesKey("browser_https_only")
        val BROWSER_SAFE = booleanPreferencesKey("browser_safe_browsing")
        val AUTO_TELEGRAM = booleanPreferencesKey("auto_telegram_enabled")
        val AUTO_TELEGRAM_CHAT_ID = stringPreferencesKey("auto_telegram_chat_id")
        val AUTO_REMINDERS = booleanPreferencesKey("auto_reminder_notifications")
        val AUTO_SUMMARIES = booleanPreferencesKey("auto_scheduled_summaries")
        val AUTO_SHARE = booleanPreferencesKey("auto_share_to_assistant")
        val AUTO_CLIPBOARD = booleanPreferencesKey("auto_clipboard_to_assistant")
        val AUTO_BG_DOC = booleanPreferencesKey("auto_bg_doc_processing")
        val USER_NAME = stringPreferencesKey("user_display_name")
        val LAST_ACTIVE_CONV = stringPreferencesKey("last_active_conversation_id")
    }

    val prefsFlow: Flow<WorkspacePrefsState> = context.dataStore.data.map { prefs ->
        WorkspacePrefsState(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            defaultSearchEngine = prefs[Keys.SEARCH_ENGINE] ?: "DuckDuckGo",
            browserJavaScriptEnabled = prefs[Keys.BROWSER_JS] ?: true,
            browserHttpsOnly = prefs[Keys.BROWSER_HTTPS] ?: true,
            browserSafeBrowsing = prefs[Keys.BROWSER_SAFE] ?: true,
            automationTelegramEnabled = prefs[Keys.AUTO_TELEGRAM] ?: false,
            automationTelegramChatId = prefs[Keys.AUTO_TELEGRAM_CHAT_ID] ?: "",
            automationReminderNotifications = prefs[Keys.AUTO_REMINDERS] ?: true,
            automationScheduledSummaries = prefs[Keys.AUTO_SUMMARIES] ?: false,
            automationShareToAssistant = prefs[Keys.AUTO_SHARE] ?: true,
            automationClipboardToAssistant = prefs[Keys.AUTO_CLIPBOARD] ?: false,
            automationBackgroundDocProcessing = prefs[Keys.AUTO_BG_DOC] ?: false,
            userDisplayName = prefs[Keys.USER_NAME] ?: "Operator",
            lastActiveConversationId = prefs[Keys.LAST_ACTIVE_CONV]
        )
    }

    suspend fun setLastActiveConversationId(id: String?) {
        context.dataStore.edit {
            if (id == null) {
                it.remove(Keys.LAST_ACTIVE_CONV)
            } else {
                it[Keys.LAST_ACTIVE_CONV] = id
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setDefaultSearchEngine(engine: String) {
        context.dataStore.edit { it[Keys.SEARCH_ENGINE] = engine }
    }

    suspend fun setBrowserJavaScript(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BROWSER_JS] = enabled }
    }

    suspend fun setBrowserHttpsOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BROWSER_HTTPS] = enabled }
    }

    suspend fun setAutomationToggle(key: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (key) {
                "telegram" -> prefs[Keys.AUTO_TELEGRAM] = enabled
                "reminders" -> prefs[Keys.AUTO_REMINDERS] = enabled
                "summaries" -> prefs[Keys.AUTO_SUMMARIES] = enabled
                "share" -> prefs[Keys.AUTO_SHARE] = enabled
                "clipboard" -> prefs[Keys.AUTO_CLIPBOARD] = enabled
                "bg_doc" -> prefs[Keys.AUTO_BG_DOC] = enabled
            }
        }
    }

    suspend fun setTelegramChatId(chatId: String) {
        context.dataStore.edit { it[Keys.AUTO_TELEGRAM_CHAT_ID] = chatId.trim() }
    }

    suspend fun setUserDisplayName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifBlank { "Operator" } }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
