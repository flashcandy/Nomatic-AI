package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.SharedIncomingPayload
import com.example.domain.WorkspaceViewModel
import com.example.ui.screens.AIProviderManagementScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AutomationCentreScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DeveloperAssistantScreen
import com.example.ui.screens.DocumentIntelligenceScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.KnowledgeVaultScreen
import com.example.ui.screens.MiniBrowserScreen
import com.example.ui.screens.ModelArenaScreen
import com.example.ui.screens.CodeDiffInspectorScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProductivityCentreScreen
import com.example.ui.screens.PromptLibraryScreen
import com.example.ui.screens.SettingsAndPrivacyScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsDirectoryScreen
import com.example.ui.screens.LogAnalyzerScreen
import com.example.ui.screens.WorkspaceMemoryManagerScreen
import com.example.ui.screens.PromptLabScreen
import com.example.ui.screens.GitReleaseNotesScreen
import com.example.ui.screens.ProjectScaffoldGeneratorScreen
import com.example.ui.screens.UsageDashboardScreen
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticRed
import com.example.ui.theme.NomaticTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object NomaticRoutes {
    const val HOME = "home"
    const val ASSISTANT = "assistant"
    const val BROWSER = "browser"
    const val VAULT = "vault"
    const val TOOLS = "tools"

    // Specialist Sub-Destinations
    const val PROVIDERS = "providers"
    const val PROMPTS = "prompts"
    const val DEV_ASSISTANT = "dev_assistant"
    const val DOC_INTEL = "doc_intel"
    const val PRODUCTIVITY = "productivity"
    const val AUTOMATION = "automation"
    const val USAGE = "usage"
    const val SETTINGS = "settings"
    const val MODEL_ARENA = "model_arena"
    const val CODE_DIFF = "code_diff"
    const val LOG_ANALYZER = "log_analyzer"
    const val WORKSPACE_MEMORY = "workspace_memory"
    const val PROMPT_LAB = "prompt_lab"
    const val GIT_RELEASE = "git_release"
    const val SCAFFOLD_GEN = "scaffold_gen"
}

data class PrimaryNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PRIMARY_NAV_ITEMS = listOf(
    PrimaryNavItem(NomaticRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    PrimaryNavItem(NomaticRoutes.ASSISTANT, "Assistant", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_assistant"),
    PrimaryNavItem(NomaticRoutes.BROWSER, "Browser", Icons.Filled.Language, Icons.Outlined.Language, "nav_browser"),
    PrimaryNavItem(NomaticRoutes.VAULT, "Vault", Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial, "nav_vault"),
    PrimaryNavItem(NomaticRoutes.TOOLS, "Tools", Icons.Filled.Build, Icons.Outlined.Build, "nav_tools")
)

class MainActivity : ComponentActivity() {

    private val viewModel: WorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractIncomingShareIntent(intent)

        setContent {
            val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
            NomaticTheme(darkTheme = prefs.darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NomaticWorkspaceRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractIncomingShareIntent(intent)
    }

    private fun extractIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            viewModel.handleIncomingShare(sharedText, sharedUri, intent.type)
        }
    }
}

@Composable
fun NomaticWorkspaceRoot(viewModel: WorkspaceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val sharedPayload by viewModel.sharedPayload.collectAsStateWithLifecycle()

    val isOnline = rememberNetworkOnlineState(context)

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.setAppUnlocked(true)
    }

    // Auto-dismiss transient status banner after 3.5s
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.showStatus(null)
        }
    }

    if (isInitializing) {
        SplashScreen()
        return
    }

    if (prefs.biometricLockEnabled && !isAppUnlocked) {
        BiometricLockScreen(
            onRequestBiometricUnlock = {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                @Suppress("DEPRECATION")
                val authIntent = km?.createConfirmDeviceCredentialIntent(
                    "Nomatic AI Workspace",
                    "Authenticate to unlock your encrypted workspace"
                )
                if (authIntent != null) {
                    credentialLauncher.launch(authIntent)
                } else {
                    viewModel.setAppUnlocked(true)
                }
            },
            onBypassInEmulator = { viewModel.setAppUnlocked(true) }
        )
        return
    }

    if (!prefs.hasCompletedOnboarding) {
        OnboardingScreen(
            biometricEnabled = prefs.biometricLockEnabled,
            onToggleBiometric = { enabled ->
                scope.launch { viewModel.preferences.setBiometricLock(enabled) }
            },
            onSaveKeys = { openAiKey, geminiKey ->
                if (openAiKey.isNotBlank()) viewModel.saveApiKey("openai", openAiKey)
                if (geminiKey.isNotBlank()) viewModel.saveApiKey("gemini", geminiKey)
            },
            onCompleteOnboarding = {
                scope.launch {
                    viewModel.preferences.setOnboardingCompleted(true)
                    viewModel.setAppUnlocked(true)
                }
            }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NomaticRoutes.HOME

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        PRIMARY_NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToPrimary(navController, item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NomaticRed,
                                    selectedTextColor = NomaticRed,
                                    indicatorColor = NomaticRed.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        PRIMARY_NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToPrimary(navController, item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Offline Telemetry Banner (Section 8 Requirement)
                    AnimatedVisibility(visible = !isOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NomaticAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline Mode",
                                tint = NomaticAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "OFFLINE MODE • Local Vault, Prompts, Tasks, Reminders & History available. AI generation paused.",
                                style = MaterialTheme.typography.labelSmall,
                                color = NomaticAmber
                            )
                        }
                    }

                    // Status Telemetry Toast Banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NomaticRed)
                                .clickable { viewModel.showStatus(null) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = statusBanner.orEmpty().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    NomaticNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Android Sharesheet Action Dialog (Section 9)
    val incoming = sharedPayload
    if (incoming != null) {
        SharesheetActionModal(
            payload = incoming,
            onDismiss = { viewModel.clearIncomingShare() },
            onAskAi = { text ->
                viewModel.clearIncomingShare()
                viewModel.setPendingChatDraft("Analyze this shared content:\n\n$text")
                navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
            },
            onSummarize = { text, uri ->
                viewModel.clearIncomingShare()
                if (uri != null) {
                    viewModel.loadDocumentForIntelligence(uri)
                    navController.navigate(NomaticRoutes.DOC_INTEL)
                } else {
                    viewModel.sendChatMessage("Summarize the following shared content clearly:\n\n$text", startNewSession = true)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                }
            },
            onSaveToVault = { text ->
                viewModel.clearIncomingShare()
                val category = if (text.startsWith("http")) "Saved links" else "Notes"
                viewModel.saveNoteToVault(
                    title = "Shared Content (${category})",
                    content = text,
                    category = category,
                    tags = "shared"
                )
                navigateToPrimary(navController, NomaticRoutes.VAULT)
            },
            onAnalyzeLink = { url ->
                viewModel.clearIncomingShare()
                viewModel.addBrowserTab(url)
                navigateToPrimary(navController, NomaticRoutes.BROWSER)
            },
            onCreateNote = { text ->
                viewModel.clearIncomingShare()
                viewModel.saveNoteToVault(
                    title = text.take(40).ifBlank { "Shared Note" },
                    content = text,
                    category = "Notes",
                    tags = "shared,note"
                )
                navigateToPrimary(navController, NomaticRoutes.VAULT)
            }
        )
    }
}

@Composable
private fun NomaticNavGraph(
    navController: NavHostController,
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val providerSettings by viewModel.providerSettings.collectAsStateWithLifecycle()
    val providerHealth by viewModel.providerHealth.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val conversationSearch by viewModel.conversationSearchQuery.collectAsStateWithLifecycle()
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val browserHistory by viewModel.browserHistory.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val usageLogs by viewModel.usageLogs.collectAsStateWithLifecycle()

    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingPartial by viewModel.streamingPartialText.collectAsStateWithLifecycle()
    val streamingBadge by viewModel.streamingProviderBadge.collectAsStateWithLifecycle()
    val pendingDraft by viewModel.pendingChatDraft.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedChatProvider.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedChatModel.collectAsStateWithLifecycle()
    val attachedDoc by viewModel.attachedDocument.collectAsStateWithLifecycle()
    val streamFallbackId by viewModel.streamErrorFallbackProvider.collectAsStateWithLifecycle()

    val maskedOpenAiKey by viewModel.maskedOpenAiKey.collectAsStateWithLifecycle()
    val maskedGeminiKey by viewModel.maskedGeminiKey.collectAsStateWithLifecycle()
    val maskedTelegramToken by viewModel.maskedTelegramToken.collectAsStateWithLifecycle()
    val connectionTestStatus by viewModel.connectionTestStatus.collectAsStateWithLifecycle()

    val browserTabs by viewModel.browserTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val browserAiResult by viewModel.browserAiResult.collectAsStateWithLifecycle()
    val isBrowserAiLoading by viewModel.isBrowserAiLoading.collectAsStateWithLifecycle()

    val activeDocument by viewModel.activeDocument.collectAsStateWithLifecycle()
    val docAnalysisOutput by viewModel.documentAnalysisOutput.collectAsStateWithLifecycle()
    val isDocAnalyzing by viewModel.isDocumentAnalyzing.collectAsStateWithLifecycle()

    val devOutput by viewModel.developerOutput.collectAsStateWithLifecycle()
    val isDevLoading by viewModel.isDeveloperLoading.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = NomaticRoutes.HOME,
        modifier = modifier
    ) {
        composable(NomaticRoutes.HOME) {
            HomeDashboardScreen(
                userName = prefs.userDisplayName,
                onUpdateUserName = viewModel::updateUserDisplayName,
                providerSettings = providerSettings,
                providerHealth = providerHealth,
                recentConversations = conversations,
                savedPrompts = prompts,
                recentPages = browserHistory,
                recentNotes = notes,
                tasks = tasks,
                reminders = reminders,
                onQuickAsk = { query ->
                    viewModel.sendChatMessage(query, startNewSession = true)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                },
                onOpenConversation = { id ->
                    viewModel.selectConversation(id)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                },
                onNewConversation = {
                    viewModel.createNewConversation()
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                },
                onOpenBrowser = { urlOrMode ->
                    if (urlOrMode == "SUMMARIZE") {
                        navigateToPrimary(navController, NomaticRoutes.BROWSER)
                        viewModel.analyzeActiveWebpageWithAI()
                    } else if (!urlOrMode.isNullOrBlank()) {
                        viewModel.addBrowserTab(urlOrMode)
                        navigateToPrimary(navController, NomaticRoutes.BROWSER)
                    } else {
                        navigateToPrimary(navController, NomaticRoutes.BROWSER)
                    }
                },
                onOpenProviderSettings = { navController.navigate(NomaticRoutes.PROVIDERS) },
                onOpenDocumentIntelligence = { navController.navigate(NomaticRoutes.DOC_INTEL) },
                onOpenDeveloperTools = { navController.navigate(NomaticRoutes.DEV_ASSISTANT) },
                onOpenVaultEditor = { navigateToPrimary(navController, NomaticRoutes.VAULT) },
                onOpenProductivity = { navController.navigate(NomaticRoutes.PRODUCTIVITY) },
                onOpenPrompts = { navController.navigate(NomaticRoutes.PROMPTS) },
                onOpenSettings = { navController.navigate(NomaticRoutes.SETTINGS) },
                onUsePrompt = { prompt ->
                    viewModel.setPendingChatDraft(prompt.content)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                }
            )
        }

        composable(NomaticRoutes.ASSISTANT) {
            AssistantScreen(
                conversations = conversations,
                activeConversationId = activeConversationId,
                messages = activeMessages,
                conversationSearchQuery = conversationSearch,
                isStreaming = isStreaming,
                streamingPartialText = streamingPartial,
                streamingProviderBadge = streamingBadge,
                pendingDraft = pendingDraft,
                selectedProvider = selectedProvider,
                selectedModel = selectedModel,
                availableModels = viewModel.providerRegistry.getAllModels(),
                providerSettings = providerSettings,
                attachedDocument = attachedDoc,
                streamErrorFallbackProvider = streamFallbackId,
                onSearchQueryChange = viewModel::setConversationSearchQuery,
                onSelectConversation = viewModel::selectConversation,
                onNewConversation = { viewModel.createNewConversation() },
                onRenameConversation = viewModel::renameConversation,
                onDeleteConversation = viewModel::deleteConversation,
                onDraftChange = viewModel::setPendingChatDraft,
                onSendMessage = { text -> viewModel.sendChatMessage(text) },
                onEditAndResend = viewModel::editAndResendUserMessage,
                onRegenerate = { fallbackId ->
                    val cid = activeConversationId ?: return@AssistantScreen
                    viewModel.regenerateLastAssistantMessage(cid, fallbackId)
                },
                onCancelStream = viewModel::cancelActiveStream,
                onSelectProviderModel = { p, m -> viewModel.setSelectedChatProvider(p, m) },
                onAttachFile = viewModel::attachFileToChat,
                onClearAttachment = viewModel::clearChatAttachment,
                onSaveMessageToVault = { msg ->
                    viewModel.saveNoteToVault(
                        title = "AI Response (${msg.providerId?.uppercase() ?: "AI"})",
                        content = msg.content,
                        category = "AI responses",
                        tags = "ai,${msg.providerId ?: "assistant"}"
                    )
                },
                onSaveConversationToVault = viewModel::saveConversationToVault,
                onClearAllConversations = viewModel::clearAllConversations,
                onOpenProviderManagement = { navController.navigate(NomaticRoutes.PROVIDERS) },
                onShowStatus = viewModel::showStatus
            )
        }

        composable(NomaticRoutes.BROWSER) {
            val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
            MiniBrowserScreen(
                tabs = browserTabs,
                activeTabId = activeTabId,
                javaScriptEnabled = prefs.browserJavaScriptEnabled,
                httpsOnly = prefs.browserHttpsOnly,
                browserAiResult = browserAiResult,
                isBrowserAiLoading = isBrowserAiLoading,
                bookmarks = bookmarks,
                onSelectTab = viewModel::selectBrowserTab,
                onAddTab = { viewModel.addBrowserTab() },
                onCloseTab = viewModel::closeBrowserTab,
                onNavigateUrl = viewModel::updateActiveTabUrl,
                onPageLoaded = viewModel::onWebPageLoaded,
                onBookmarkCurrentPage = { url, title -> viewModel.addBookmark(url, title) },
                onDeleteBookmark = viewModel::deleteBookmark,
                onSummarizePage = { viewModel.analyzeActiveWebpageWithAI(null) },
                onAskQuestionAboutPage = { q -> viewModel.analyzeActiveWebpageWithAI(q) },
                onSavePageToVault = { title, content ->
                    viewModel.saveNoteToVault(
                        title = title,
                        content = content,
                        category = "Saved links",
                        tags = "web,browser"
                    )
                },
                onSharePageToAssistant = { prompt ->
                    viewModel.setPendingChatDraft(prompt)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                },
                onClearAiResult = viewModel::clearBrowserAiResult
            )
        }

        composable(NomaticRoutes.VAULT) {
            KnowledgeVaultScreen(
                notes = notes,
                onSaveNote = { id, title, content, cat, tags ->
                    viewModel.saveNoteToVault(id, title, content, cat, tags)
                },
                onTogglePin = viewModel::togglePinNote,
                onDeleteNote = viewModel::deleteNote,
                onExportJson = viewModel::buildWorkspaceExportJson,
                onImportJson = viewModel::importVaultJson,
                onOpenPromptLibrary = { navController.navigate(NomaticRoutes.PROMPTS) },
                onShowStatus = viewModel::showStatus
            )
        }

        composable(NomaticRoutes.TOOLS) {
            ToolsDirectoryScreen(
                onOpenDeveloperAssistant = { navController.navigate(NomaticRoutes.DEV_ASSISTANT) },
                onOpenDocumentIntelligence = { navController.navigate(NomaticRoutes.DOC_INTEL) },
                onOpenProductivityCentre = { navController.navigate(NomaticRoutes.PRODUCTIVITY) },
                onOpenAutomationCentre = { navController.navigate(NomaticRoutes.AUTOMATION) },
                onOpenUsageDashboard = { navController.navigate(NomaticRoutes.USAGE) },
                onOpenPromptLibrary = { navController.navigate(NomaticRoutes.PROMPTS) },
                onOpenProviderManagement = { navController.navigate(NomaticRoutes.PROVIDERS) },
                onOpenSettingsPrivacy = { navController.navigate(NomaticRoutes.SETTINGS) },
                onOpenModelArena = { navController.navigate(NomaticRoutes.MODEL_ARENA) },
                onOpenCodeDiffInspector = { navController.navigate(NomaticRoutes.CODE_DIFF) },
                onOpenLogAnalyzer = { navController.navigate(NomaticRoutes.LOG_ANALYZER) },
                onOpenWorkspaceMemory = { navController.navigate(NomaticRoutes.WORKSPACE_MEMORY) },
                onOpenPromptLab = { navController.navigate(NomaticRoutes.PROMPT_LAB) },
                onOpenGitRelease = { navController.navigate(NomaticRoutes.GIT_RELEASE) },
                onOpenScaffoldGen = { navController.navigate(NomaticRoutes.SCAFFOLD_GEN) }
            )
        }

        // Specialist Screens
        composable(NomaticRoutes.PROVIDERS) {
            AIProviderManagementScreen(
                settings = providerSettings,
                maskedOpenAiKey = maskedOpenAiKey,
                maskedGeminiKey = maskedGeminiKey,
                providerHealth = providerHealth,
                connectionTestStatus = connectionTestStatus,
                onSaveApiKey = viewModel::saveApiKey,
                onDeleteApiKey = viewModel::deleteApiKey,
                onTestConnection = viewModel::testProviderConnection,
                onUpdateSettings = viewModel::updateProviderSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.PROMPTS) {
            PromptLibraryScreen(
                prompts = prompts,
                onSavePrompt = viewModel::savePrompt,
                onToggleFavorite = viewModel::togglePromptFavorite,
                onDeletePrompt = viewModel::deletePrompt,
                onInsertIntoChat = { prompt ->
                    viewModel.setPendingChatDraft(prompt.content)
                    navigateToPrimary(navController, NomaticRoutes.ASSISTANT)
                },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.DEV_ASSISTANT) {
            DeveloperAssistantScreen(
                output = devOutput,
                isLoading = isDevLoading,
                onRunQuery = viewModel::runDeveloperAssistantQuery,
                onSaveToVault = { title, content, cat ->
                    viewModel.saveNoteToVault(title = title, content = content, category = cat, tags = "dev,runbook")
                },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.DOC_INTEL) {
            DocumentIntelligenceScreen(
                activeDocument = activeDocument,
                analysisOutput = docAnalysisOutput,
                isAnalyzing = isDocAnalyzing,
                onPickDocument = viewModel::loadDocumentForIntelligence,
                onAnalyzeMode = viewModel::analyzeActiveDocument,
                onSaveSummaryToVault = { title, content ->
                    viewModel.saveNoteToVault(title = title, content = content, category = "Documents", tags = "document,summary")
                },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.PRODUCTIVITY) {
            ProductivityCentreScreen(
                tasks = tasks,
                reminders = reminders,
                onAddTask = { title, desc, prio -> viewModel.addTask(title, desc, prio) },
                onToggleTask = viewModel::toggleTaskCompleted,
                onDeleteTask = viewModel::deleteTask,
                onAddReminder = viewModel::addReminder,
                onSnoozeReminder = viewModel::snoozeReminder,
                onToggleReminder = viewModel::completeOrToggleReminder,
                onDeleteReminder = viewModel::deleteReminder,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.AUTOMATION) {
            AutomationCentreScreen(
                prefs = prefs,
                maskedTelegramToken = maskedTelegramToken,
                onToggleAutomation = { key, enabled ->
                    scope.launch { viewModel.preferences.setAutomationToggle(key, enabled) }
                },
                onSaveTelegramConfig = { token, chatId ->
                    if (token.isNotBlank()) viewModel.saveApiKey("telegram", token)
                    scope.launch { viewModel.preferences.setTelegramChatId(chatId) }
                },
                onSendTelegramTest = viewModel::sendTelegramTestMessage,
                onRunLocalSummaryNow = viewModel::generateLocalDailySummaryNow,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.USAGE) {
            UsageDashboardScreen(
                usageLogs = usageLogs,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.SETTINGS) {
            SettingsAndPrivacyScreen(
                prefs = prefs,
                maskedOpenAiKey = maskedOpenAiKey,
                maskedGeminiKey = maskedGeminiKey,
                onToggleDarkMode = { enabled ->
                    scope.launch { viewModel.preferences.setDarkMode(enabled) }
                },
                onToggleBiometricLock = { enabled ->
                    scope.launch { viewModel.preferences.setBiometricLock(enabled) }
                },
                onSelectSearchEngine = { engine ->
                    scope.launch { viewModel.preferences.setDefaultSearchEngine(engine) }
                },
                onToggleBrowserJs = { enabled ->
                    scope.launch { viewModel.preferences.setBrowserJavaScript(enabled) }
                },
                onToggleBrowserHttps = { enabled ->
                    scope.launch { viewModel.preferences.setBrowserHttpsOnly(enabled) }
                },
                onOpenProviderManagement = { navController.navigate(NomaticRoutes.PROVIDERS) },
                onExportPersonalData = viewModel::buildWorkspaceExportJson,
                onClearConversationHistory = viewModel::clearAllConversationHistory,
                onClearBrowserHistory = viewModel::clearAllBrowserHistory,
                onClearCachedFiles = viewModel::clearCachedFiles,
                onDeleteAllLocalData = viewModel::deleteAllLocalData,
                onUpdateUserName = viewModel::updateUserDisplayName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.MODEL_ARENA) {
            ModelArenaScreen(
                availableModels = viewModel.availableModels,
                onRunArena = viewModel::runModelArena,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.CODE_DIFF) {
            CodeDiffInspectorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.LOG_ANALYZER) {
            LogAnalyzerScreen(
                onSaveToVault = { title, content, cat -> viewModel.saveNoteToVault(title = title, content = content, category = cat) },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.WORKSPACE_MEMORY) {
            val memories by viewModel.workspaceMemories.collectAsStateWithLifecycle()
            WorkspaceMemoryManagerScreen(
                memories = memories,
                onAddMemory = viewModel::addWorkspaceMemory,
                onDeleteMemory = viewModel::deleteNote,
                onClearAllMemories = viewModel::clearAllWorkspaceMemories,
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.PROMPT_LAB) {
            PromptLabScreen(
                onSavePrompt = { title, desc, content, cat, tags ->
                    viewModel.savePrompt(title = title, description = desc, content = content, category = cat, tags = tags)
                },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.GIT_RELEASE) {
            GitReleaseNotesScreen(
                onSaveToVault = { title, content, cat -> viewModel.saveNoteToVault(title = title, content = content, category = cat) },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NomaticRoutes.SCAFFOLD_GEN) {
            ProjectScaffoldGeneratorScreen(
                onSaveToVault = { title, content, cat -> viewModel.saveNoteToVault(title = title, content = content, category = cat) },
                onShowStatus = viewModel::showStatus,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun navigateToPrimary(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SharesheetActionModal(
    payload: SharedIncomingPayload,
    onDismiss: () -> Unit,
    onAskAi: (String) -> Unit,
    onSummarize: (String, Uri?) -> Unit,
    onSaveToVault: (String) -> Unit,
    onAnalyzeLink: (String) -> Unit,
    onCreateNote: (String) -> Unit
) {
    val text = payload.rawText ?: payload.uri?.toString().orEmpty()
    val isLink = text.startsWith("http://") || text.startsWith("https://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NOMATIC AI SHARESHEET") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = text.take(200),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onAskAi(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("1. ASK AI ASSISTANT") }

                OutlinedButton(
                    onClick = { onSummarize(text, payload.uri) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("2. SUMMARIZE CONTENT") }

                OutlinedButton(
                    onClick = { onSaveToVault(text) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("3. SAVE TO KNOWLEDGE VAULT") }

                if (isLink) {
                    OutlinedButton(
                        onClick = { onAnalyzeLink(text) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("4. ANALYZE LINK") }
                }

                OutlinedButton(
                    onClick = { onCreateNote(text) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("5. CREATE NOTE") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
private fun rememberNetworkOnlineState(context: Context): Boolean {
    var isOnline by remember {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(active)
        mutableStateOf(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != false)
    }

    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    return isOnline
}
