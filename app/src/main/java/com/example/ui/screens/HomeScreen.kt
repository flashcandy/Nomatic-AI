package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ai.ProviderHealthStatus
import com.example.data.local.BrowserHistoryEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PromptEntity
import com.example.data.local.ProviderSettingsEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.NomaticCard
import com.example.ui.components.NomaticSectionHeader
import com.example.ui.components.QuickActionTile
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import java.util.Calendar

@Composable
fun HomeDashboardScreen(
    userName: String,
    providerSettings: ProviderSettingsEntity,
    providerHealth: Map<String, ProviderHealthStatus>,
    recentConversations: List<ConversationEntity>,
    savedPrompts: List<PromptEntity>,
    recentPages: List<BrowserHistoryEntity>,
    recentNotes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    reminders: List<ReminderEntity>,
    onQuickAsk: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenBrowser: (String?) -> Unit,
    onOpenProviderSettings: () -> Unit,
    onOpenDocumentIntelligence: () -> Unit,
    onOpenDeveloperTools: () -> Unit,
    onOpenVaultEditor: () -> Unit,
    onOpenProductivity: () -> Unit,
    onOpenPrompts: () -> Unit,
    onOpenSettings: () -> Unit,
    onUsePrompt: (PromptEntity) -> Unit,
    onUpdateUserName: (String) -> Unit = {}
) {
    var quickInput by remember { mutableStateOf("") }
    var showCommandPaletteModal by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempUserNameInput by remember(userName) { mutableStateOf(userName) }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "GOOD MORNING"
        in 12..17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }

    val primaryId = providerSettings.primaryProvider
    val primaryStatus = providerHealth[primaryId] ?: ProviderHealthStatus.UNCONFIGURED
    val isReady = primaryStatus == ProviderHealthStatus.HEALTHY

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("EDIT OPERATOR DISPLAY NAME", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize your operator name displayed across the Nomatic AI workspace greeting and logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempUserNameInput,
                        onValueChange = { tempUserNameInput = it },
                        label = { Text("User / Operator Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserName(tempUserNameInput)
                        showEditNameDialog = false
                    }
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header & Provider Indicator
        item {
            DotMatrixBanner(
                title = "$greeting, ${userName.uppercase()}",
                subtitle = "Unified AI Workspace • Primary: ${primaryId.uppercase()} (${if (primaryId == "openai") providerSettings.openAiDefaultModel else providerSettings.geminiDefaultModel}) • Tap ✎ to edit name",
                statusLabel = "PROVIDER: ${primaryId.uppercase()} • ${primaryStatus.name}",
                isOnline = isReady,
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier.testTag("home_edit_user_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Operator Name",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onOpenProviderSettings,
                            modifier = Modifier.testTag("home_provider_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "AI Provider Management",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings and Privacy",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }

        // Command Palette / Global Search Trigger Bar (Ctrl + K)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCommandPaletteModal = true }
                    .testTag("home_command_palette_trigger")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Global Search", tint = NomaticRed)
                        Text(
                            text = "Global Search across Chats, Vault, Prompts, History...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TelemetryPill(label = "CTRL + K", color = NomaticEmerald)
                }
            }
        }

        // AI Quick Input Bar
        item {
            NomaticCard(accentBorder = true, padding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        placeholder = { Text("Ask Nomatic AI, analyze logs, or draft script...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_quick_ai_input")
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NomaticRed,
                        modifier = Modifier
                            .size(52.dp)
                            .clickable {
                                if (quickInput.isNotBlank()) {
                                    val q = quickInput
                                    quickInput = ""
                                    onQuickAsk(q)
                                } else {
                                    onNewConversation()
                                }
                            }
                            .testTag("home_quick_ai_send")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send to AI Assistant",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryPill(
                        label = "FALLBACK: ${if (providerSettings.autoFallbackEnabled) providerSettings.secondaryProvider.uppercase() else "OFF"}",
                        color = if (providerSettings.autoFallbackEnabled) NomaticEmerald else NomaticAmber
                    )
                    Text(
                        text = "Configure Keys & Models →",
                        style = MaterialTheme.typography.labelSmall,
                        color = NomaticRed,
                        modifier = Modifier.clickable(onClick = onOpenProviderSettings)
                    )
                }
            }
        }

        // 8 Required Quick Actions Grid (2x4)
        item {
            NomaticSectionHeader(title = "Workspace Quick Actions")
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "New AI Chat",
                        subtitle = "Multi-provider stream",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onNewConversation,
                        testTag = "qa_new_chat",
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Mini Browser",
                        subtitle = "Hardened WebView",
                        icon = Icons.Default.Language,
                        onClick = { onOpenBrowser(null) },
                        testTag = "qa_open_browser",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Summarize Web",
                        subtitle = "AI page synthesis",
                        icon = Icons.Default.Language,
                        onClick = { onOpenBrowser("SUMMARIZE") },
                        testTag = "qa_summarize_web",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Analyze PDF",
                        subtitle = "Doc intelligence",
                        icon = Icons.Default.Description,
                        onClick = onOpenDocumentIntelligence,
                        testTag = "qa_analyze_pdf",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Linux Assistant",
                        subtitle = "SRE & Ab Initio",
                        icon = Icons.Default.Code,
                        onClick = onOpenDeveloperTools,
                        testTag = "qa_linux_assistant",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "AI Models & Keys",
                        subtitle = "Gemini & OpenRouter",
                        icon = Icons.Default.Tune,
                        onClick = onOpenProviderSettings,
                        testTag = "qa_provider_models",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Create Note",
                        subtitle = "Knowledge Vault",
                        icon = Icons.Default.EditNote,
                        onClick = onOpenVaultEditor,
                        testTag = "qa_create_note",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Create Reminder",
                        subtitle = "Tasks & Alarms",
                        icon = Icons.Default.Alarm,
                        onClick = onOpenProductivity,
                        testTag = "qa_create_reminder",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Productivity Summary
        item {
            val pendingTasks = tasks.count { !it.isCompleted }
            val completedTasks = tasks.count { it.isCompleted }
            val activeReminders = reminders.count { it.isEnabled }
            NomaticSectionHeader(
                title = "Productivity Telemetry",
                actionLabel = "OPEN CENTRE",
                onActionClick = onOpenProductivity
            )
            NomaticCard(onClick = onOpenProductivity) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$pendingTasks",
                            style = MaterialTheme.typography.displayMedium,
                            color = NomaticRed
                        )
                        Text("OPEN TASKS", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text(
                            text = "$activeReminders",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("REMINDERS", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text(
                            text = "$completedTasks",
                            style = MaterialTheme.typography.displayMedium,
                            color = NomaticEmerald
                        )
                        Text("COMPLETED", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text(
                            text = "${recentNotes.size}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("VAULT ITEMS", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Recent Conversations
        item {
            NomaticSectionHeader(
                title = "Stored AI Chats (${recentConversations.size})",
                actionLabel = if (recentConversations.isNotEmpty()) "VIEW ALL" else "NEW CHAT",
                onActionClick = {
                    if (recentConversations.isNotEmpty()) {
                        onOpenConversation(recentConversations.first().id)
                    } else {
                        onNewConversation()
                    }
                }
            )
            if (recentConversations.isEmpty()) {
                NomaticCard {
                    Text(
                        text = "No previous chats stored yet. Start an AI session above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentConversations.take(4).forEach { conv ->
                        NomaticCard(onClick = { onOpenConversation(conv.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = NomaticRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TelemetryPill(label = conv.defaultProvider?.uppercase() ?: "AI")
                            }
                        }
                    }
                }
            }
        }

        // Saved Prompts Carousel
        item {
            NomaticSectionHeader(
                title = "Saved Prompts Library",
                actionLabel = "ALL PROMPTS",
                onActionClick = onOpenPrompts
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(savedPrompts.take(5), key = { it.id }) { prompt ->
                    NomaticCard(
                        modifier = Modifier.width(250.dp),
                        onClick = { onUsePrompt(prompt) }
                    ) {
                        TelemetryPill(label = prompt.category, color = NomaticRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = prompt.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prompt.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Recently Visited Pages & Recent Vault Documents
        item {
            NomaticSectionHeader(
                title = "Recent Browser & Vault Activity",
                actionLabel = "OPEN BROWSER",
                onActionClick = { onOpenBrowser(null) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentPages.take(2).forEach { page ->
                    NomaticCard(onClick = { onOpenBrowser(page.url) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = page.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TelemetryPill(label = "WEB")
                        }
                    }
                }
                recentNotes.take(2).forEach { note ->
                    NomaticCard(onClick = onOpenVaultEditor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = note.content.replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TelemetryPill(label = note.category ?: "VAULT", color = NomaticEmerald)
                        }
                    }
                }
            }
        }
    }

    // Unified Command Palette Modal (Global Search Across All Modules)
    if (showCommandPaletteModal) {
        CommandPaletteModal(
            conversations = recentConversations,
            notes = recentNotes,
            prompts = savedPrompts,
            history = recentPages,
            tasks = tasks,
            onSelectConversation = { id ->
                showCommandPaletteModal = false
                onOpenConversation(id)
            },
            onSelectNote = {
                showCommandPaletteModal = false
                onOpenVaultEditor()
            },
            onSelectPrompt = { prompt ->
                showCommandPaletteModal = false
                onUsePrompt(prompt)
            },
            onSelectBrowserPage = { url ->
                showCommandPaletteModal = false
                onOpenBrowser(url)
            },
            onSelectTask = {
                showCommandPaletteModal = false
                onOpenProductivity()
            },
            onDismiss = { showCommandPaletteModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandPaletteModal(
    conversations: List<ConversationEntity>,
    notes: List<NoteEntity>,
    prompts: List<PromptEntity>,
    history: List<BrowserHistoryEntity>,
    tasks: List<TaskEntity>,
    onSelectConversation: (String) -> Unit,
    onSelectNote: () -> Unit,
    onSelectPrompt: (PromptEntity) -> Unit,
    onSelectBrowserPage: (String) -> Unit,
    onSelectTask: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val q = query.trim().lowercase()

    val filteredChats = remember(conversations, q) {
        if (q.isEmpty()) conversations.take(4) else conversations.filter { it.title.lowercase().contains(q) }
    }
    val filteredNotes = remember(notes, q) {
        if (q.isEmpty()) notes.take(4) else notes.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) || it.category?.lowercase()?.contains(q) == true || it.tags.lowercase().contains(q) }
    }
    val filteredPrompts = remember(prompts, q) {
        if (q.isEmpty()) prompts.take(4) else prompts.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || it.category.lowercase().contains(q) }
    }
    val filteredPages = remember(history, q) {
        if (q.isEmpty()) history.take(4) else history.filter { it.title.lowercase().contains(q) || it.url.lowercase().contains(q) }
    }
    val filteredTasks = remember(tasks, q) {
        if (q.isEmpty()) tasks.take(4) else tasks.filter { it.title.lowercase().contains(q) || it.description?.lowercase()?.contains(q) == true }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NomaticRed)
                    Text("COMMAND PALETTE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TelemetryPill(label = "UNIFIED INDEX", color = NomaticEmerald)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search chats, notes, prompts, browser history, tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("command_palette_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Chats Section
                if (filteredChats.isNotEmpty()) {
                    item { NomaticSectionHeader(title = "Stored AI Chats (${filteredChats.size})") }
                    items(filteredChats, key = { "chat_${it.id}" }) { conv ->
                        NomaticCard(onClick = { onSelectConversation(conv.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = NomaticRed, modifier = Modifier.size(18.dp))
                                    Text(conv.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TelemetryPill(label = conv.defaultProvider?.uppercase() ?: "AI", color = NomaticRed)
                            }
                        }
                    }
                }

                // Vault Notes Section
                if (filteredNotes.isNotEmpty()) {
                    item { NomaticSectionHeader(title = "Knowledge Vault Notes (${filteredNotes.size})") }
                    items(filteredNotes, key = { "note_${it.id}" }) { note ->
                        NomaticCard(onClick = onSelectNote) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = NomaticAmber, modifier = Modifier.size(18.dp))
                                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TelemetryPill(label = note.category ?: "Notes", color = NomaticAmber)
                            }
                        }
                    }
                }

                // Prompts Section
                if (filteredPrompts.isNotEmpty()) {
                    item { NomaticSectionHeader(title = "Reusable Prompts (${filteredPrompts.size})") }
                    items(filteredPrompts, key = { "prompt_${it.id}" }) { prompt ->
                        NomaticCard(onClick = { onSelectPrompt(prompt) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = NomaticEmerald, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prompt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(prompt.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                TelemetryPill(label = prompt.category, color = NomaticEmerald)
                            }
                        }
                    }
                }

                // Browser History Section
                if (filteredPages.isNotEmpty()) {
                    item { NomaticSectionHeader(title = "Web Pages & Links (${filteredPages.size})") }
                    items(filteredPages, key = { "page_${it.id}" }) { page ->
                        NomaticCard(onClick = { onSelectBrowserPage(page.url) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(page.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(page.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tasks Section
                if (filteredTasks.isNotEmpty()) {
                    item { NomaticSectionHeader(title = "Tasks & Reminders (${filteredTasks.size})") }
                    items(filteredTasks, key = { "task_${it.id}" }) { task ->
                        NomaticCard(onClick = onSelectTask) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Checklist, contentDescription = null, tint = NomaticRed, modifier = Modifier.size(18.dp))
                                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TelemetryPill(label = task.priority, color = NomaticRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
