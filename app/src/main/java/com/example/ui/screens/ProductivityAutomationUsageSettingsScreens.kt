package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.datastore.WorkspacePrefsState
import com.example.data.local.AIUsageEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.NomaticCard
import com.example.ui.components.NomaticSectionHeader
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PRODUCTIVITY_TABS = listOf(
    "Tasks",
    "Reminders",
    "Daily Planner",
    "Learning Tracker",
    "Shopping",
    "Completed"
)

@Composable
fun ProductivityCentreScreen(
    tasks: List<TaskEntity>,
    reminders: List<ReminderEntity>,
    onAddTask: (title: String, description: String?, priority: String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddReminder: (title: String, description: String?, scheduledAt: Long, recurrence: String) -> Unit,
    onSnoozeReminder: (ReminderEntity, Int) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableStateOf("Tasks") }
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("HIGH") }
    var reminderOffsetMinutes by remember { mutableStateOf("60") }
    var reminderRecurrence by remember { mutableStateOf("NONE") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("productivity_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("PRODUCTIVITY CENTRE", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRODUCTIVITY_TABS) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.uppercase(), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (selectedTab == "Reminders") {
            item {
                NomaticCard(accentBorder = true) {
                    Text("Schedule New Reminder", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Reminder Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description / Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = reminderOffsetMinutes,
                            onValueChange = { reminderOffsetMinutes = it.filter { ch -> ch.isDigit() } },
                            label = { Text("In Minutes") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        listOf("NONE", "DAILY", "WEEKLY").forEach { rec ->
                            FilterChip(
                                selected = reminderRecurrence == rec,
                                onClick = { reminderRecurrence = rec },
                                label = { Text(rec, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val mins = reminderOffsetMinutes.toLongOrNull() ?: 60L
                            val triggerAt = System.currentTimeMillis() + (mins * 60_000L)
                            onAddReminder(newTitle, newDescription, triggerAt, reminderRecurrence)
                            newTitle = ""
                            newDescription = ""
                        },
                        enabled = newTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AlarmAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCHEDULE REMINDER")
                    }
                }
            }

            items(reminders, key = { it.id }) { rem ->
                NomaticCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TelemetryPill(
                                    label = dateFormatter.format(Date(rem.scheduledAt)),
                                    color = if (rem.isEnabled) NomaticRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TelemetryPill(label = "RECURRENCE: ${rem.recurrence ?: "NONE"}")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!rem.description.isNullOrBlank()) {
                                Text(
                                    text = rem.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = rem.isEnabled,
                            onCheckedChange = { onToggleReminder(rem) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onSnoozeReminder(rem, 15) }) {
                            Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SNOOZE +15M", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onDeleteReminder(rem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NomaticRed)
                        }
                    }
                }
            }
        } else {
            // Tasks, Daily Planner, Learning Tracker, Shopping Checklist, Completed
            val mappedCategory = when (selectedTab) {
                "Daily Planner" -> "PLANNER"
                "Learning Tracker" -> "LEARNING"
                "Shopping" -> "SHOPPING"
                else -> selectedPriority
            }

            if (selectedTab != "Completed") {
                item {
                    NomaticCard {
                        Text("Add to $selectedTab", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Item Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newDescription,
                            onValueChange = { newDescription = it },
                            label = { Text("Optional Details") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (selectedTab == "Tasks") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("HIGH", "MEDIUM", "LOW").forEach { prio ->
                                    FilterChip(
                                        selected = selectedPriority == prio,
                                        onClick = { selectedPriority = prio },
                                        label = { Text(prio, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onAddTask(newTitle, newDescription, mappedCategory)
                                newTitle = ""
                                newDescription = ""
                            },
                            enabled = newTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_add_button")
                        ) {
                            Text("ADD ITEM")
                        }
                    }
                }
            }

            val filteredTasks = tasks.filter { t ->
                when (selectedTab) {
                    "Completed" -> t.isCompleted
                    "Daily Planner" -> !t.isCompleted && t.priority == "PLANNER"
                    "Learning Tracker" -> !t.isCompleted && t.priority == "LEARNING"
                    "Shopping" -> !t.isCompleted && t.priority == "SHOPPING"
                    else -> !t.isCompleted && t.priority in listOf("HIGH", "MEDIUM", "LOW")
                }
            }

            items(filteredTasks, key = { it.id }) { task ->
                NomaticCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                )
                                if (!task.description.isNullOrBlank()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        TelemetryPill(
                            label = task.priority,
                            color = if (task.priority == "HIGH") NomaticRed else NomaticEmerald
                        )
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NomaticRed)
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 13: AUTOMATION CENTRE ---

@Composable
fun AutomationCentreScreen(
    prefs: WorkspacePrefsState,
    maskedTelegramToken: String?,
    onToggleAutomation: (String, Boolean) -> Unit,
    onSaveTelegramConfig: (token: String, chatId: String) -> Unit,
    onSendTelegramTest: (String) -> Unit,
    onRunLocalSummaryNow: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var botTokenInput by remember { mutableStateOf("") }
    var chatIdInput by remember(prefs.automationTelegramChatId) { mutableStateOf(prefs.automationTelegramChatId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("AUTOMATION CENTRE", style = MaterialTheme.typography.headlineMedium)
        }

        DotMatrixBanner(
            title = "CONTROLLED AUTOMATIONS",
            subtitle = "All automations are strictly opt-in. Nomatic AI never executes external commands or transmits personal data without explicit consent.",
            statusLabel = "OPT-IN GOVERNANCE"
        )

        NomaticCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Telegram Bot Integration", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (maskedTelegramToken != null) "Stored Token: $maskedTelegramToken" else "No Bot Token Stored",
                        style = MaterialTheme.typography.labelSmall,
                        color = NomaticEmerald
                    )
                }
                Switch(
                    checked = prefs.automationTelegramEnabled,
                    onCheckedChange = { onToggleAutomation("telegram", it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = botTokenInput,
                onValueChange = { botTokenInput = it },
                label = { Text("Telegram Bot Token (Encrypted via Keystore)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = chatIdInput,
                onValueChange = { chatIdInput = it },
                label = { Text("Telegram Target Chat ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSaveTelegramConfig(botTokenInput, chatIdInput)
                        botTokenInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SAVE BOT CONFIG")
                }
                OutlinedButton(
                    onClick = { onSendTelegramTest("Nomatic AI Workspace Test Ping") },
                    enabled = prefs.automationTelegramEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SEND TEST PING")
                }
            }
        }

        NomaticCard {
            AutomationToggleRow(
                title = "Reminder Notifications",
                subtitle = "Trigger high-priority local notifications for scheduled workspace reminders",
                checked = prefs.automationReminderNotifications,
                onCheckedChange = { onToggleAutomation("reminders", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Scheduled Local Summaries",
                subtitle = "Compile daily local digest of tasks, reminders & vault notes into Knowledge Vault",
                checked = prefs.automationScheduledSummaries,
                onCheckedChange = { onToggleAutomation("summaries", it) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRunLocalSummaryNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GENERATE LOCAL WORKSPACE DIGEST NOW")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Share-to-Assistant Routing",
                subtitle = "Accept text, links, and PDFs from Android Sharesheet",
                checked = prefs.automationShareToAssistant,
                onCheckedChange = { onToggleAutomation("share", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Clipboard-to-Assistant Quick Action",
                subtitle = "Offer one-tap prompt to analyze copied clipboard text or URLs",
                checked = prefs.automationClipboardToAssistant,
                onCheckedChange = { onToggleAutomation("clipboard", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Background Document Indexing",
                subtitle = "Extract text from attached documents locally on background I/O threads",
                checked = prefs.automationBackgroundDocProcessing,
                onCheckedChange = { onToggleAutomation("bg_doc", it) }
            )
        }
    }
}

@Composable
private fun AutomationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- SCREEN 14: USAGE DASHBOARD ---

@Composable
fun UsageDashboardScreen(
    usageLogs: List<AIUsageEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val now = System.currentTimeMillis()
    val dayAgo = now - 86_400_000L
    val monthAgo = now - (30L * 86_400_000L)

    val openAiCount = usageLogs.count { it.providerId == "openai" }
    val geminiCount = usageLogs.count { it.providerId == "gemini" }
    val failedCount = usageLogs.count { it.requestStatus == "FAILED" }
    val fallbackCount = usageLogs.count { it.requestStatus == "FALLBACK_SUCCESS" }
    val dailyCount = usageLogs.count { it.createdAt >= dayAgo }
    val monthlyCount = usageLogs.count { it.createdAt >= monthAgo }

    val reportedInputTokens = usageLogs.mapNotNull { it.inputTokens }.sum()
    val reportedOutputTokens = usageLogs.mapNotNull { it.outputTokens }.sum()
    val totalEstimatedCost = usageLogs.mapNotNull { it.estimatedCost }.sum()

    val byModel = usageLogs.groupBy { "${it.providerId.uppercase()} • ${it.modelId}" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("usage_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("USAGE DASHBOARD", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "API TELEMETRY & COST LEDGER",
                subtitle = "Tracks provider-reported token usage and transparent cost estimates based on published per-1M token pricing. Never fabricates billing metrics.",
                statusLabel = "${usageLogs.size} TOTAL REQUESTS LOGGED"
            )
        }

        item {
            NomaticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("$openAiCount", style = MaterialTheme.typography.displayMedium, color = NomaticRed)
                        Text("OPENAI REQS", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text("$geminiCount", style = MaterialTheme.typography.displayMedium, color = NomaticEmerald)
                        Text("GEMINI REQS", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text("$fallbackCount", style = MaterialTheme.typography.displayMedium, color = NomaticAmber)
                        Text("FALLBACKS", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text("$failedCount", style = MaterialTheme.typography.displayMedium)
                        Text("FAILED", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$reportedInputTokens",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "INPUT TOKENS",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$reportedOutputTokens",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "OUTPUT TOKENS",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.2f),
                        horizontalAlignment = Alignment.End
                    ) {
                        val formattedCost = if (totalEstimatedCost == 0.0) "$0.00" else String.format(Locale.US, "$%.4f", totalEstimatedCost)
                        Text(
                            text = formattedCost,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NomaticEmerald,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "EST. API COST (USD)",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "24h Window: $dailyCount requests • 30d Window: $monthlyCount requests",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            NomaticSectionHeader(title = "Breakdown by Provider & Model")
        }

        if (byModel.isEmpty()) {
            item {
                NomaticCard {
                    Text(
                        text = "No AI API calls recorded yet. Once you send messages with OpenAI or Gemini, provider-reported token usage and cost estimates will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(byModel.entries.toList(), key = { it.key }) { (modelKey, entries) ->
                val inTok = entries.mapNotNull { it.inputTokens }.sum()
                val outTok = entries.mapNotNull { it.outputTokens }.sum()
                val cost = entries.mapNotNull { it.estimatedCost }.sum()
                NomaticCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(modelKey, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TelemetryPill(label = "${entries.size} CALLS", color = NomaticRed)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Provider-Reported Tokens: IN $inTok / OUT $outTok • Est. Cost: ${String.format(Locale.US, "$%.4f", cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- SCREEN 15: SETTINGS AND PRIVACY ---

@Composable
fun SettingsAndPrivacyScreen(
    prefs: WorkspacePrefsState,
    maskedOpenAiKey: String?,
    maskedGeminiKey: String?,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onToggleBrowserJs: (Boolean) -> Unit,
    onToggleBrowserHttps: (Boolean) -> Unit,
    onOpenProviderManagement: () -> Unit,
    onExportPersonalData: () -> String,
    onClearConversationHistory: () -> Unit,
    onClearBrowserHistory: () -> Unit,
    onClearCachedFiles: () -> Unit,
    onDeleteAllLocalData: () -> Unit,
    onBack: () -> Unit,
    onUpdateUserName: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showConfirmWipeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("SETTINGS & PRIVACY", style = MaterialTheme.typography.headlineMedium)
        }

        // User Profile & Custom Display Name
        NomaticSectionHeader(title = "User Profile & Custom Display Name")
        NomaticCard {
            var nameInput by remember(prefs.userDisplayName) { mutableStateOf(prefs.userDisplayName) }
            Text("Operator / User Display Name", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Customizes the greeting header (e.g. GOOD MORNING, ${prefs.userDisplayName.uppercase()}) across your workspace.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Enter your name...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_user_name_input")
                )
                Button(
                    onClick = { onUpdateUserName(nameInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                ) {
                    Text("SAVE")
                }
            }
        }

        // Appearance & Security
        NomaticSectionHeader(title = "Appearance & Access Control")
        NomaticCard {
            AutomationToggleRow(
                title = "Nothing OS Dark Mode (Default)",
                subtitle = "High-contrast carbon palette with crimson telemetry accents",
                checked = prefs.darkMode,
                onCheckedChange = onToggleDarkMode
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Biometric & Device Lock",
                subtitle = "Require authentication before unlocking Nomatic AI workspace",
                checked = prefs.biometricLockEnabled,
                onCheckedChange = onToggleBiometricLock
            )
        }

        // AI Provider & Keystore Summary
        NomaticSectionHeader(title = "AI Providers & Encrypted Keystore")
        NomaticCard(onClick = onOpenProviderManagement) {
            Text("Personal API Key Vault Status", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• OpenAI Key: ${maskedOpenAiKey ?: "Not Configured"}\n• Google Gemini Key: ${maskedGeminiKey ?: "Not Configured"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenProviderManagement,
                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OPEN AI PROVIDER & KEY MANAGEMENT")
            }
        }

        // Browser Security Settings
        NomaticSectionHeader(title = "Mini Browser Hardening")
        NomaticCard {
            Text("Default Search Engine", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DuckDuckGo", "Google", "Brave", "Startpage").forEach { engine ->
                    FilterChip(
                        selected = prefs.defaultSearchEngine == engine,
                        onClick = { onSelectSearchEngine(engine) },
                        label = { Text(engine, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Enforce HTTPS-Only Upgrading",
                subtitle = "Automatically upgrade HTTP URLs and reject invalid SSL certificates",
                checked = prefs.browserHttpsOnly,
                onCheckedChange = onToggleBrowserHttps
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AutomationToggleRow(
                title = "Enable WebView JavaScript",
                subtitle = "Disable when browsing untrusted pages; no native JS bridges are ever exposed",
                checked = prefs.browserJavaScriptEnabled,
                onCheckedChange = onToggleBrowserJs
            )
        }

        // Privacy Manifesto (Section 7 Requirement)
        NomaticSectionHeader(title = "Privacy & Data Residency Manifesto")
        NomaticCard {
            Text("What Stays 100% Local on Your Device", style = MaterialTheme.typography.titleMedium, color = NomaticEmerald)
            Text(
                text = "• Room SQLite Database: All conversation histories, Knowledge Vault notes, Prompt Library templates, bookmarks, tasks, and reminders.\n" +
                    "• Android Keystore AES-256-GCM: Personal API keys are stored encrypted in private internal storage and never exported in backups.\n" +
                    "• Offline Modules: Notes, prompts, tasks, reminders, developer reference runbooks, and local PDF extraction work completely offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("What Is Sent to OpenAI or Google Gemini", style = MaterialTheme.typography.titleMedium, color = NomaticAmber)
            Text(
                text = "• Only when you actively initiate an AI chat, webpage summary, or document analysis, the prompt and relevant text excerpt are transmitted over TLS 1.3 HTTPS directly to your selected provider (api.openai.com, openrouter.ai, or generativelanguage.googleapis.com) using your personal API key.\n" +
                    "• Security Notice: Local encryption cannot fully protect keys on rooted or compromised devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Local Database & Data Governance
        NomaticSectionHeader(title = "Local Data & Storage Management")
        NomaticCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val json = onExportPersonalData()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Nomatic AI Personal Data Export")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Personal Data"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EXPORT PERSONAL DATA (JSON WITHOUT KEYS)")
                }

                OutlinedButton(
                    onClick = onClearConversationHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLEAR CONVERSATION HISTORY")
                }

                OutlinedButton(
                    onClick = onClearBrowserHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLEAR BROWSER HISTORY")
                }

                OutlinedButton(
                    onClick = onClearCachedFiles,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLEAR CACHED FILES")
                }

                Button(
                    onClick = { showConfirmWipeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_data_button")
                ) {
                    Text("DELETE ALL LOCAL DATA & KEYS")
                }
            }
        }

        // About Nomatic AI
        NomaticCard {
            Text("ABOUT NOMATIC AI", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Nomatic AI v1.0 • Your Personal AI Workspace\nArchitecture: Clean Architecture + MVVM • Room SQLite • Android Keystore AES-256-GCM • Multi-Provider AI Router.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showConfirmWipeDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmWipeDialog = false },
            title = { Text("Delete All Local Data?") },
            text = {
                Text("This will permanently wipe all conversations, Vault notes, custom prompts, tasks, browser history, and Android Keystore encrypted API keys.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllLocalData()
                        showConfirmWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                ) {
                    Text("PERMANENTLY DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmWipeDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
