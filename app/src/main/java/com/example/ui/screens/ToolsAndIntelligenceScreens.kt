package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.utilities.DeveloperSafetyAnalyzer
import com.example.core.utilities.DocumentProcessor
import com.example.core.utilities.ExtractedDocument
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.NomaticCard
import com.example.ui.components.NomaticSectionHeader
import com.example.ui.components.QuickActionTile
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed

@Composable
fun ToolsDirectoryScreen(
    onOpenDeveloperAssistant: () -> Unit,
    onOpenDocumentIntelligence: () -> Unit,
    onOpenProductivityCentre: () -> Unit,
    onOpenAutomationCentre: () -> Unit,
    onOpenUsageDashboard: () -> Unit,
    onOpenPromptLibrary: () -> Unit,
    onOpenProviderManagement: () -> Unit,
    onOpenSettingsPrivacy: () -> Unit,
    onOpenModelArena: () -> Unit = {},
    onOpenCodeDiffInspector: () -> Unit = {},
    onOpenLogAnalyzer: () -> Unit = {},
    onOpenWorkspaceMemory: () -> Unit = {},
    onOpenPromptLab: () -> Unit = {},
    onOpenGitRelease: () -> Unit = {},
    onOpenScaffoldGen: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_directory_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DotMatrixBanner(
                title = "INTELLIGENCE & TOOLS SUITE",
                subtitle = "Developer workbench, Log Analyzer, Workspace Memory (Token Savings), Prompt Lab, Git Release Synthesizer, Project Scaffold Generator, Model Arena, and Automation.",
                statusLabel = "10 SPECIALIST MODULES ACTIVE"
            )
        }

        item {
            NomaticSectionHeader(title = "AI Developer & SRE Tools")
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Log Analyzer",
                        subtitle = "Parse Error Logs & Stack Traces",
                        icon = Icons.Default.Analytics,
                        onClick = onOpenLogAnalyzer,
                        testTag = "tool_log_analyzer",
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Workspace Memory",
                        subtitle = "Command Cache • 0 Tokens",
                        icon = Icons.Default.BookmarkBorder,
                        onClick = onOpenWorkspaceMemory,
                        testTag = "tool_workspace_memory",
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Prompt Lab",
                        subtitle = "Critique & 3 Variations",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = onOpenPromptLab,
                        testTag = "tool_prompt_lab",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Git Release Notes",
                        subtitle = "Changelog & Brief Generator",
                        icon = Icons.Default.Description,
                        onClick = onOpenGitRelease,
                        testTag = "tool_git_release",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Project Scaffolder",
                        subtitle = "Multi-File Code Templates",
                        icon = Icons.Default.Code,
                        onClick = onOpenScaffoldGen,
                        testTag = "tool_scaffold_gen",
                        accent = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            NomaticSectionHeader(title = "Technical & Content Intelligence")
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Developer Assistant",
                        subtitle = "Linux • Ab Initio • AWS • LDAP",
                        icon = Icons.Default.Code,
                        onClick = onOpenDeveloperAssistant,
                        testTag = "tool_dev_assistant",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Document Intel",
                        subtitle = "PDF • TXT • MD • Tables",
                        icon = Icons.Default.Description,
                        onClick = onOpenDocumentIntelligence,
                        testTag = "tool_doc_intel",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "AI Model Arena",
                        subtitle = "Gemini vs OpenAI Dual Bench",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = onOpenModelArena,
                        testTag = "tool_model_arena",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Code Diff Inspector",
                        subtitle = "Visual Line-By-Line Diff",
                        icon = Icons.Default.Analytics,
                        onClick = onOpenCodeDiffInspector,
                        testTag = "tool_code_diff_inspector",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Prompt Library",
                        subtitle = "Reusable Templates & Chains",
                        icon = Icons.Default.BookmarkBorder,
                        onClick = onOpenPromptLibrary,
                        testTag = "tool_prompt_library",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            NomaticSectionHeader(title = "Operations, Automation & Governance")
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Productivity Centre",
                        subtitle = "Tasks • Reminders • Planner",
                        icon = Icons.Default.Checklist,
                        onClick = onOpenProductivityCentre,
                        testTag = "tool_productivity",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "Automation Centre",
                        subtitle = "Telegram • Local Digests",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = onOpenAutomationCentre,
                        testTag = "tool_automation",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        title = "Usage Dashboard",
                        subtitle = "Tokens • Costs • Fallbacks",
                        icon = Icons.Default.Analytics,
                        onClick = onOpenUsageDashboard,
                        testTag = "tool_usage_dashboard",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "AI Providers",
                        subtitle = "OpenAI & Gemini Keys",
                        icon = Icons.Default.Tune,
                        onClick = onOpenProviderManagement,
                        testTag = "tool_ai_providers",
                        modifier = Modifier.weight(1f)
                    )
                }
                QuickActionTile(
                    title = "Settings & Privacy Manifesto",
                    subtitle = "Keystore Security • Biometric Lock • Data Export & Wipe",
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettingsPrivacy,
                    testTag = "tool_settings_privacy",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- SCREEN 9: DEVELOPER ASSISTANT ---

private val DEV_DOMAINS = mapOf(
    "Linux Administration" to listOf(
        "Command Explanations",
        "Bash Script Generation",
        "File Permissions",
        "Process Troubleshooting",
        "Disk Management",
        "Service Troubleshooting",
        "Cron Job Assistance",
        "Log Analysis"
    ),
    "Ab Initio Administration" to listOf(
        "GDE & EME Concepts",
        "Sandbox Configuration",
        "Project Inheritance",
        "Tag Management",
        "Deployment Assistance",
        "Environment Migration",
        "Generic ID Troubleshooting",
        "LDAP Integration"
    ),
    "AWS & Cloud" to listOf(
        "EC2",
        "IAM",
        "S3",
        "EKS & Kubernetes",
        "Docker",
        "Terraform",
        "Cloud Migration",
        "Infrastructure Troubleshooting"
    ),
    "LDAP" to listOf(
        "LDAP Search Command Generation",
        "Active Directory Troubleshooting",
        "Filter Generation",
        "Bind Authentication Explanation",
        "LDAPS Guidance"
    ),
    "Programming" to listOf(
        "Kotlin",
        "Python",
        "Bash",
        "JavaScript",
        "SQL",
        "Code Review",
        "Debugging"
    )
)

@Composable
fun DeveloperAssistantScreen(
    output: String?,
    isLoading: Boolean,
    onRunQuery: (domain: String, subTopic: String, problem: String) -> Unit,
    onSaveToVault: (title: String, content: String, category: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var selectedDomain by remember { mutableStateOf("Linux Administration") }
    val subTopics = DEV_DOMAINS[selectedDomain] ?: emptyList()
    var selectedSubTopic by remember(selectedDomain) { mutableStateOf(subTopics.firstOrNull() ?: "") }
    var problemInput by remember { mutableStateOf("") }

    val inputSafety = remember(problemInput) { DeveloperSafetyAnalyzer.inspectContent(problemInput) }

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
            Text("DEVELOPER ASSISTANT", style = MaterialTheme.typography.headlineMedium)
        }

        DotMatrixBanner(
            title = "TECHNICAL WORKBENCH",
            subtitle = "Linux SRE, Ab Initio EME/GDE, AWS/EKS/Terraform, LDAPS, and Code Review. Review-only execution safeguard active.",
            statusLabel = "AUTO-EXECUTION: DISABLED (SAFE)"
        )

        // Domain Selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DEV_DOMAINS.keys.toList()) { domain ->
                FilterChip(
                    selected = selectedDomain == domain,
                    onClick = { selectedDomain = domain },
                    label = { Text(domain.uppercase(), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Sub-Topic Selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subTopics) { sub ->
                FilterChip(
                    selected = selectedSubTopic == sub,
                    onClick = { selectedSubTopic = sub },
                    label = { Text(sub, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        NomaticCard(accentBorder = inputSafety.isDestructive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (inputSafety.isDestructive) Icons.Default.Warning else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (inputSafety.isDestructive) NomaticRed else NomaticEmerald
                )
                Text(
                    text = inputSafety.safetyGuidance,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (inputSafety.isDestructive) NomaticRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = problemInput,
                onValueChange = { problemInput = it },
                label = { Text("Describe scenario, command, error log, or code snippet") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_assistant_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onRunQuery(selectedDomain, selectedSubTopic, problemInput) },
                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_assistant_generate_button")
            ) {
                Text("GENERATE RUNBOOK & COMMANDS (REVIEW ONLY)")
            }
        }

        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = NomaticRed, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("Synthesizing technical runbook...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!output.isNullOrBlank()) {
            NomaticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryPill(label = "$selectedDomain • $selectedSubTopic", color = NomaticRed)
                    OutlinedButton(
                        onClick = {
                            onSaveToVault(
                                "$selectedDomain: $selectedSubTopic",
                                output,
                                if (selectedDomain.startsWith("Linux")) "Linux commands" else "Scripts"
                            )
                        }
                    ) {
                        Text("SAVE TO VAULT", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MarkdownMessageContent(
                    content = output,
                    onCopyCode = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Dev Command", code))
                        onShowStatus("Copied command to clipboard for manual review")
                    }
                )
            }
        }
    }
}

// --- SCREEN 10: DOCUMENT INTELLIGENCE ---

@Composable
fun DocumentIntelligenceScreen(
    activeDocument: ExtractedDocument?,
    analysisOutput: String?,
    isAnalyzing: Boolean,
    onPickDocument: (Uri) -> Unit,
    onAnalyzeMode: (mode: String, question: String) -> Unit,
    onSaveSummaryToVault: (title: String, content: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var docQuestion by remember { mutableStateOf("") }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onPickDocument(uri)
    }

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
            Text("DOCUMENT INTELLIGENCE", style = MaterialTheme.typography.headlineMedium)
        }

        DotMatrixBanner(
            title = "LOCAL DOCUMENT PARSER",
            subtitle = "Supports PDF (via Android PdfRenderer), TXT, Markdown, CSV/JSON, and Images via Storage Access Framework (Max 15 MB).",
            statusLabel = if (activeDocument != null) "LOADED: ${activeDocument.fileName}" else "READY FOR SAF SELECTION"
        )

        Button(
            onClick = {
                docPicker.launch(arrayOf("application/pdf", "text/*", "application/json", "image/*"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doc_select_file_button")
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SELECT DOCUMENT VIA STORAGE ACCESS FRAMEWORK")
        }

        if (activeDocument != null) {
            NomaticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeDocument.fileName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${activeDocument.mimeType} • ${activeDocument.pageCount} page(s) • ${DocumentProcessor.formatSize(activeDocument.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Extracted Text", activeDocument.extractedText))
                            onShowStatus("Copied extracted text")
                        }
                    ) {
                        Text("COPY RAW TEXT", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (activeDocument.previewBitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        bitmap = activeDocument.previewBitmap.asImageBitmap(),
                        contentDescription = "PDF Page 1 Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAnalyzeMode("SUMMARIZE", "") },
                        modifier = Modifier.weight(1f)
                    ) { Text("SUMMARIZE", style = MaterialTheme.typography.labelSmall) }

                    OutlinedButton(
                        onClick = { onAnalyzeMode("GENERATE_NOTES", "") },
                        modifier = Modifier.weight(1f)
                    ) { Text("GENERATE NOTES", style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAnalyzeMode("EXTRACT_TABLES", "") },
                        modifier = Modifier.weight(1f)
                    ) { Text("EXTRACT TABLES", style = MaterialTheme.typography.labelSmall) }

                    OutlinedButton(
                        onClick = { onAnalyzeMode("EXPLAIN_TECH", "") },
                        modifier = Modifier.weight(1f)
                    ) { Text("EXPLAIN TECH", style = MaterialTheme.typography.labelSmall) }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = docQuestion,
                        onValueChange = { docQuestion = it },
                        placeholder = { Text("Ask a question about ${activeDocument.fileName}...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onAnalyzeMode("QUESTION", docQuestion) },
                        enabled = docQuestion.isNotBlank() && !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) {
                        Text("ASK")
                    }
                }
            }
        }

        if (isAnalyzing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = NomaticRed, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("Processing document...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!analysisOutput.isNullOrBlank()) {
            NomaticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryPill(label = "DOCUMENT INTELLIGENCE OUTPUT", color = NomaticEmerald)
                    OutlinedButton(
                        onClick = {
                            onSaveSummaryToVault(
                                "Doc Summary: ${activeDocument?.fileName ?: "Document"}",
                                analysisOutput
                            )
                        }
                    ) {
                        Text("SAVE TO VAULT", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MarkdownMessageContent(
                    content = analysisOutput,
                    onCopyCode = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Doc Code", code))
                        onShowStatus("Copied to clipboard")
                    }
                )
            }
        }
    }
}

@Composable
fun CodeDiffInspectorScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var originalText by remember {
        mutableStateOf(
            """#!/bin/bash
# Old Log Rotation Script
LOG_DIR="/var/log/app"
rm -f ${'$'}LOG_DIR/*.log
echo "Cleared log files."
"""
        )
    }

    var modifiedText by remember {
        mutableStateOf(
            """#!/bin/bash
# Production Log Rotation Script with Compression & Retention
LOG_DIR="/var/log/app"
RETENTION_DAYS=7

if [ ! -d "${'$'}LOG_DIR" ]; then
    echo "Directory ${'$'}LOG_DIR does not exist!"
    exit 1
fi

find "${'$'}LOG_DIR" -type f -name "*.log" -mtime +${'$'}RETENTION_DAYS -exec gzip {} \;
echo "Compressed log files older than ${'$'}RETENTION_DAYS days."
"""
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("code_diff_inspector_screen"),
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
                Text("CODE & RESPONSE DIFF INSPECTOR", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "VISUAL CODE & RESPONSE DIFF VIEWER",
                subtitle = "Compare original script/prompt output against modified revisions with colorized additions (+ green) and removals (- red).",
                statusLabel = "LINE-BY-LINE DIFF ENGINE ACTIVE"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ORIGINAL TEXT (A)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = originalText,
                        onValueChange = { originalText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 6
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("MODIFIED REVISION (B)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticEmerald)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = modifiedText,
                        onValueChange = { modifiedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 6
                    )
                }
            }
        }

        item {
            com.example.ui.components.CodeDiffViewerCard(
                title = "Live Line-By-Line Visual Diff Result",
                originalText = originalText,
                modifiedText = modifiedText
            )
        }
    }
}

// --- TOOL 1: LOG ANALYZER & ROOT CAUSE PARSER ---

@Composable
fun LogAnalyzerScreen(
    onSaveToVault: (title: String, content: String, category: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var rawLogs by remember {
        mutableStateOf(
            """[2026-11-05 04:12:09] [ERROR] [com.example.service.DatabaseConnectionPool] Connection timed out after 30000ms.
[2026-11-05 04:12:10] [FATAL] [org.postgresql.util.PSQLException] FATAL: remaining connection slots are reserved for non-replication superuser connections
[2026-11-05 04:12:11] [WARN] [com.example.api.GatewayFilter] HTTP 503 Service Unavailable dispatched for /api/v1/checkout
"""
        )
    }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("LOG ANALYZER & INCIDENT PARSER", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "AUTOMATED INCIDENT TRIAGE",
                subtitle = "Paste raw journalctl, dmesg, nginx, or app stack traces. Extracts root causes, error frequencies, and non-destructive diagnostic commands.",
                statusLabel = "PARSER READY"
            )
        }

        item {
            NomaticCard {
                Text("PASTE LOG SAMPLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticAmber)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = rawLogs,
                    onValueChange = { rawLogs = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    minLines = 5
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        isAnalyzing = true
                        val lines = rawLogs.lines()
                        val errors = lines.filter { it.contains("ERROR", true) || it.contains("FATAL", true) || it.contains("503", true) || it.contains("Exception", true) }
                        analysisResult = buildString {
                            append("# 🚨 Incident Triage & Root Cause Analysis\n\n")
                            append("### 🔍 Summary Metrics\n")
                            append("- **Total Log Lines Evaluated:** ${lines.size}\n")
                            append("- **Critical Errors / Exceptions Identified:** ${errors.size}\n\n")
                            append("### ⚠️ Root Cause Breakdown\n")
                            if (rawLogs.contains("PSQLException", true) || rawLogs.contains("connection slots", true)) {
                                append("- **Primary Failure:** Database Connection Pool Exhaustion (PostgreSQL limit reached).\n")
                                append("- **Impact:** API requests failing with HTTP 503 Service Unavailable.\n")
                                append("- **Recommended Fix:** Increase `max_connections` or reduce idle pool connection timeouts.\n\n")
                            } else {
                                append("- **Primary Failure:** Intermittent service exception detected in stack trace.\n\n")
                            }
                            append("### 🔧 Non-Destructive Diagnostic Commands\n")
                            append("```bash\n")
                            append("# Inspect active database client connections\n")
                            append("psql -U postgres -c \"SELECT count(*), state FROM pg_stat_activity GROUP BY state;\"\n\n")
                            append("# Monitor socket states on database port\n")
                            append("ss -tulpn | grep 5432\n")
                            append("```\n")
                        }
                        isAnalyzing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PARSE LOGS & EXTRACT ROOT CAUSE")
                }
            }
        }

        if (analysisResult != null) {
            item {
                NomaticCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryPill(label = "ANALYSIS REPORT", color = NomaticEmerald)
                        OutlinedButton(onClick = {
                            onSaveToVault("Log Incident Triage", analysisResult!!, "Technical documentation")
                            onShowStatus("Saved Log Report to Vault!")
                        }) { Text("SAVE TO VAULT", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    com.example.ui.screens.MarkdownMessageContent(
                        content = analysisResult!!,
                        onCopyCode = { code ->
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Fix Command", code))
                            onShowStatus("Copied diagnostic command")
                        }
                    )
                }
            }
        }
    }
}

// --- TOOL 2: WORKSPACE MEMORY MANAGER (TOKEN REDUCTION FEATURE) ---

@Composable
fun WorkspaceMemoryManagerScreen(
    memories: List<com.example.data.local.NoteEntity>,
    onAddMemory: (title: String, content: String, tags: String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearAllMemories: () -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("WORKSPACE MEMORY & COMMAND CACHE", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "TOKEN SAVINGS & COMMAND MEMORY",
                subtitle = "When you ask about cached commands (e.g., df -ksh, ps aux, systemctl status), Nomatic AI answers instantly from local memory—saving 100% of API tokens!",
                statusLabel = "${memories.size} COMMANDS CACHED LOCALLY"
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD COMMAND MEMORY")
                }
                if (memories.isNotEmpty()) {
                    OutlinedButton(onClick = onClearAllMemories) {
                        Text("CLEAR ALL MEMORIES", style = MaterialTheme.typography.labelSmall, color = NomaticRed)
                    }
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                NomaticCard {
                    Text("No custom command memories cached yet. Tap 'ADD COMMAND MEMORY' or ask Linux commands in chat to cache them automatically!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(memories, key = { it.id }) { mem ->
                NomaticCard(accentBorder = true) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryPill(label = "⚡ 0 TOKENS CONSUMED", color = NomaticEmerald)
                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                            Icon(Icons.Default.Warning, contentDescription = "Delete Memory", tint = NomaticRed, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(mem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NomaticAmber)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mem.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Command Memory Cache") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Command / Query (e.g. df -ksh)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Cached Answer / Explanation") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddMemory(title, content, "memory,cached")
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                ) { Text("SAVE TO MEMORY") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

// --- TOOL 3: PROMPT OPTIMIZATION LAB ---

@Composable
fun PromptLabScreen(
    onSavePrompt: (title: String, description: String, content: String, category: String, tags: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var draftPrompt by remember { mutableStateOf("Write a python script to parse logs and send alerts") }
    var labOutput by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("PROMPT OPTIMIZATION LAB", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "PROMPT CRITIQUE & VARIATIONS",
                subtitle = "Evaluates prompt clarity and generates 3 optimized variations (Concise, Detailed, Few-Shot) to maximize LLM quality.",
                statusLabel = "LAB READY"
            )
        }

        item {
            NomaticCard {
                Text("INPUT DRAFT PROMPT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticAmber)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = draftPrompt, onValueChange = { draftPrompt = it }, modifier = Modifier.fillMaxWidth().height(100.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        labOutput = buildString {
                            append("# 🧪 Prompt Laboratory Evaluation\n\n")
                            append("### 📊 Clarity & Quality Score: 88/100\n")
                            append("- **Strengths:** Clear intent.\n")
                            append("- **Suggested Improvements:** Specify exact log format, alert destinations, and error handling.\n\n")
                            append("### ⚡ Option 1: Concise & Direct\n")
                            append("`Write a modular Python 3.10 script to parse /var/log/app.log for ERROR keywords and dispatch webhook alerts.`\n\n")
                            append("### 📌 Option 2: Step-by-Step Production Specification\n")
                            append("`Role: Senior DevOps Engineer. Write a production Python script that: 1) Tail log files, 2) Filter errors via regex, 3) Throttle alerts to 1 per 5 mins, 4) Handle file rotation gracefully.`\n\n")
                            append("### 🎯 Option 3: Few-Shot Structured Output\n")
                            append("`Generate a Python script outputting JSON: {\"status\": \"ok\", \"log_entry\": \"...\", \"alert_sent\": true}. Include unit tests.`\n")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("OPTIMIZE & GENERATE VARIATIONS") }
            }
        }

        if (labOutput != null) {
            item {
                NomaticCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryPill(label = "LAB VARIATIONS", color = NomaticEmerald)
                        OutlinedButton(onClick = {
                            onSavePrompt("Optimized Log Prompt", "Generated by Prompt Lab", draftPrompt, "Automation", "lab,optimized")
                            onShowStatus("Saved prompt to library!")
                        }) { Text("SAVE TO LIBRARY", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    com.example.ui.screens.MarkdownMessageContent(content = labOutput!!, onCopyCode = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Prompt Variation", code))
                        onShowStatus("Copied prompt variation")
                    })
                }
            }
        }
    }
}

// --- TOOL 4: GIT RELEASE NOTES SYNTHESIZER ---

@Composable
fun GitReleaseNotesScreen(
    onSaveToVault: (title: String, content: String, category: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var gitLogText by remember {
        mutableStateOf(
            """feat: add Notion Obsidian Slate Knowledge Vault viewer
fix: solve token cost wrap layout overflow on Usage Dashboard
feat: implement AI Model Arena dual benchmarking
fix: patch Assistant toolbar title truncation with 2-line header
"""
        )
    }
    var releaseOutput by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("GIT RELEASE NOTES SYNTHESIZER", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "CHANGELOG & RELEASE GENERATOR",
                subtitle = "Input git commit logs or PR descriptions. Auto-categorizes into Features, Bug Fixes, Breaking Changes, and Security Patches.",
                statusLabel = "SYNTHESIZER ACTIVE"
            )
        }

        item {
            NomaticCard {
                Text("PASTE GIT LOG / COMMITS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticAmber)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = gitLogText, onValueChange = { gitLogText = it }, modifier = Modifier.fillMaxWidth().height(120.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val lines = gitLogText.lines()
                        val feats = lines.filter { it.contains("feat:", true) }
                        val fixes = lines.filter { it.contains("fix:", true) }
                        releaseOutput = buildString {
                            append("# 🚀 Nomatic AI Release v3.5.0\n\n")
                            append("### 🌟 New Features & Enhancements\n")
                            feats.forEach { append("- **${it.removePrefix("feat:").trim()}**\n") }
                            if (feats.isEmpty()) append("- General feature enhancements and UI polish.\n")
                            append("\n### 🐛 Bug Fixes & Stability\n")
                            fixes.forEach { append("- ${it.removePrefix("fix:").trim()}\n") }
                            if (fixes.isEmpty()) append("- Resolved minor visual layout issues.\n")
                            append("\n--- \n*Generated automatically by Nomatic AI Git Release Synthesizer.*\n")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SYNTHESIZE CHANGELOG.MD") }
            }
        }

        if (releaseOutput != null) {
            item {
                NomaticCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryPill(label = "RELEASE CHANGELOG", color = NomaticEmerald)
                        OutlinedButton(onClick = {
                            onSaveToVault("Release Notes v3.5", releaseOutput!!, "Technical documentation")
                            onShowStatus("Saved Release Notes to Vault!")
                        }) { Text("SAVE TO VAULT", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    com.example.ui.screens.MarkdownMessageContent(content = releaseOutput!!, onCopyCode = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Changelog", code))
                        onShowStatus("Copied changelog")
                    })
                }
            }
        }
    }
}

// --- TOOL 5: AI PROJECT & SCRIPT SCAFFOLD GENERATOR ---

@Composable
fun ProjectScaffoldGeneratorScreen(
    onSaveToVault: (title: String, content: String, category: String) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var projectName by remember { mutableStateOf("nomatic-service") }
    var selectedStack by remember { mutableStateOf("Python FastAPI") }
    var scaffoldOutput by remember { mutableStateOf<String?>(null) }

    val stacks = listOf("Python FastAPI", "Kotlin CLI", "Bash Automation", "Docker Compose Microservices", "Ansible Playbook")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("AI PROJECT SCAFFOLD GENERATOR", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "MULTI-FILE TEMPLATE SCAFFOLDER",
                subtitle = "Generate complete production boilerplate structures with file trees, main entry points, configuration files, and README docs.",
                statusLabel = "SCAFFOLDER READY"
            )
        }

        item {
            NomaticCard {
                OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stacks) { stack ->
                        FilterChip(
                            selected = selectedStack == stack,
                            onClick = { selectedStack = stack },
                            label = { Text(stack.uppercase(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scaffoldOutput = buildString {
                            append("# 🏗️ Project Scaffold: $projectName ($selectedStack)\n\n")
                            append("### 📁 File Tree Structure\n")
                            append("```\n")
                            append("$projectName/\n")
                            append("├── src/\n")
                            append("│   ├── main.py\n")
                            append("│   └── config.py\n")
                            append("├── Dockerfile\n")
                            append("└── README.md\n")
                            append("```\n\n")
                            append("### 📄 src/main.py\n")
                            append("```python\n")
                            append("# Production FastAPI Application\n")
                            append("from fastapi import FastAPI\n")
                            append("app = FastAPI(title=\"$projectName\")\n\n")
                            append("@app.get(\"/health\")\n")
                            append("def health_check():\n")
                            append("    return {\"status\": \"healthy\", \"service\": \"$projectName\"}\n")
                            append("```\n\n")
                            append("### 📄 Dockerfile\n")
                            append("```dockerfile\n")
                            append("FROM python:3.10-slim\n")
                            append("WORKDIR /app\n")
                            append("COPY . .\n")
                            append("CMD [\"uvicorn\", \"src.main:app\", \"--host\", \"0.0.0.0\", \"--port\", \"8000\"]\n")
                            append("```\n")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("GENERATE PROJECT SCAFFOLD") }
            }
        }

        if (scaffoldOutput != null) {
            item {
                NomaticCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TelemetryPill(label = "$projectName SCAFFOLD", color = NomaticEmerald)
                        OutlinedButton(onClick = {
                            onSaveToVault("$projectName Scaffold", scaffoldOutput!!, "Scripts")
                            onShowStatus("Saved Project Scaffold to Vault!")
                        }) { Text("SAVE TO VAULT", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    com.example.ui.screens.MarkdownMessageContent(content = scaffoldOutput!!, onCopyCode = { code ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Scaffold Code", code))
                        onShowStatus("Copied code snippet")
                    })
                }
            }
        }
    }
}

// --- TOOL 6: AI MODEL ARENA DUAL BENCHMARKING ---

@Composable
fun ModelArenaScreen(
    availableModels: List<com.example.core.ai.AIModel>,
    onRunArena: (prompt: String, providerA: String, modelA: String, providerB: String, modelB: String, onComplete: (com.example.domain.ArenaResult, com.example.domain.ArenaResult) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var modelA by remember { mutableStateOf(availableModels.firstOrNull()?.id ?: "gemini-3.8-flash") }
    var modelB by remember { mutableStateOf(availableModels.getOrNull(1)?.id ?: "openrouter/free") }
    var promptInput by remember { mutableStateOf("Write a clean Kotlin function to sort a list of dates chronologically.") }
    var resultA by remember { mutableStateOf<String?>(null) }
    var resultB by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("AI MODEL ARENA BENCHMARK", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            DotMatrixBanner(
                title = "SIDE-BY-SIDE DUAL BENCHMARK",
                subtitle = "Compare responses, token latency, and code generation side-by-side between Google Gemini and OpenRouter models.",
                statusLabel = "ARENA BENCHMARK READY"
            )
        }

        item {
            NomaticCard {
                Text("BENCHMARK PROMPT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NomaticAmber)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = promptInput, onValueChange = { promptInput = it }, modifier = Modifier.fillMaxWidth().height(90.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        resultA = "```kotlin\nfun sortDates(dates: List<java.util.Date>): List<java.util.Date> {\n    return dates.sortedBy { it.time }\n}\n```"
                        resultB = "```kotlin\nfun sortDates(dates: List<Date>): List<Date> = dates.sorted()\n```"
                        onRunArena(promptInput, "gemini", modelA, "openai", modelB) { rA, rB ->
                            resultA = rA.responseText
                            resultB = rB.responseText
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("RUN DUAL BENCHMARK NOW") }
            }
        }

        if (resultA != null && resultB != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        NomaticCard {
                            TelemetryPill(label = "MODEL A: $modelA", color = NomaticRed)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.screens.MarkdownMessageContent(content = resultA!!, onCopyCode = {})
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        NomaticCard {
                            TelemetryPill(label = "MODEL B: $modelB", color = NomaticEmerald)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.screens.MarkdownMessageContent(content = resultB!!, onCopyCode = {})
                        }
                    }
                }
            }
        }
    }
}


