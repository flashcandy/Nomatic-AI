package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ai.AIModel
import com.example.core.ai.ProviderHealthStatus
import com.example.core.utilities.ExtractedDocument
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ProviderSettingsEntity
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.NomaticCard
import com.example.ui.components.RenderFormattedMarkdownText
import com.example.ui.components.SlateBlockType
import com.example.ui.components.TelemetryPill
import com.example.ui.components.parseSlateMarkdownBlocks
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    messages: List<MessageEntity>,
    conversationSearchQuery: String,
    isStreaming: Boolean,
    streamingPartialText: String,
    streamingProviderBadge: String?,
    pendingDraft: String,
    selectedProvider: String?,
    selectedModel: String?,
    availableModels: List<AIModel>,
    providerSettings: ProviderSettingsEntity,
    attachedDocument: ExtractedDocument?,
    streamErrorFallbackProvider: String?,
    onSearchQueryChange: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onEditAndResend: (MessageEntity, String) -> Unit,
    onRegenerate: (String?) -> Unit,
    onCancelStream: () -> Unit,
    onSelectProviderModel: (String, String) -> Unit,
    onAttachFile: (Uri) -> Unit,
    onClearAttachment: () -> Unit,
    onSaveMessageToVault: (MessageEntity) -> Unit,
    onSaveConversationToVault: (String) -> Unit = {},
    onClearAllConversations: () -> Unit = {},
    onOpenProviderManagement: () -> Unit,
    onShowStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var showHistoryModal by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<ConversationEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onAttachFile(uri)
    }

    val currentProvider = selectedProvider ?: providerSettings.primaryProvider
    val currentModel = selectedModel ?: if (currentProvider == "openai") {
        providerSettings.openAiDefaultModel
    } else {
        providerSettings.geminiDefaultModel
    }

    val activeConversation = conversations.find { it.id == activeConversationId }
    val listState = rememberLazyListState()

    var localInputText by rememberSaveable(activeConversationId) { mutableStateOf(pendingDraft) }
    LaunchedEffect(pendingDraft) {
        if (pendingDraft != localInputText && pendingDraft.isNotEmpty()) {
            localInputText = pendingDraft
        }
    }

    LaunchedEffect(messages.size, isStreaming) {
        val totalItems = messages.size + if (isStreaming) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val doSendMessage = {
        val trimmed = localInputText.trim()
        if (!isStreaming && (trimmed.isNotBlank() || attachedDocument != null)) {
            val textToSend = localInputText
            localInputText = ""
            onDraftChange("")
            onSendMessage(textToSend)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Simplified Top Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { showHistoryModal = true },
                            modifier = Modifier.testTag("assistant_history_toggle")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        Text(
                            text = activeConversation?.title ?: "AI Workspace Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showHistoryModal = true }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onNewConversation,
                            modifier = Modifier.testTag("assistant_new_conversation")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Conversation")
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("assistant_more_actions")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (activeConversation != null) {
                                    DropdownMenuItem(
                                        text = { Text("Save Chat to Vault") },
                                        leadingIcon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onSaveConversationToVault(activeConversation.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename Session") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showRenameDialog = activeConversation
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Conversation") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            val exported = messages.joinToString("\n\n") { m ->
                                                "[${m.role.uppercase()}]\n${m.content}"
                                            }
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, activeConversation.title)
                                                putExtra(Intent.EXTRA_TEXT, exported)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Export Conversation"))
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("AI Provider Settings") },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenProviderManagement()
                                    }
                                )
                            }
                        }
                    }
                }

                // Compact Provider/Model Selector Row
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(availableModels, key = { "${it.providerId}_${it.id}" }) { m ->
                        val isSelected = currentProvider == m.providerId && currentModel == m.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectProviderModel(m.providerId, m.id) },
                            label = { Text(m.displayName, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("assistant_message_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && !isStreaming) {
                item {
                    DotMatrixBanner(
                        title = "Nomatic AI Assistant",
                        subtitle = "Ask questions, generate code, analyze documents, or synthesize knowledge with multi-provider AI.",
                        statusLabel = "Active Model: $currentModel"
                    )
                }
            }

            items(messages, key = { it.id }) { message ->
                MessageBubbleItem(
                    message = message,
                    onCopyText = { text ->
                        copyToClipboard(context, text)
                        onShowStatus("Copied to clipboard")
                    },
                    onEditMessage = { editingMessage = message },
                    onRegenerate = { onRegenerate(null) },
                    onRetryWithFallback = { fallbackId -> onRegenerate(fallbackId) },
                    onSaveToVault = { onSaveMessageToVault(message) },
                    fallbackProviderId = streamErrorFallbackProvider
                )
            }

            if (isStreaming) {
                item {
                    val estimatedTokens = remember(streamingPartialText) {
                        if (streamingPartialText.isBlank()) 0
                        else streamingPartialText.trim().split("\\s+".toRegex()).size
                    }
                    val providerBadgeText = streamingProviderBadge ?: currentProvider.uppercase()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, NomaticRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STREAMING • ${providerBadgeText.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NomaticRed
                                )
                                TextButton(
                                    onClick = onCancelStream,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = NomaticRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("STOP", style = MaterialTheme.typography.labelSmall, color = NomaticRed, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Synthesizing response...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NomaticAmber, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Synthesizing: ~$estimatedTokens tokens generated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            MarkdownMessageContent(
                                content = streamingPartialText.ifBlank { "Thinking..." },
                                onCopyCode = { code ->
                                    copyToClipboard(context, code)
                                    onShowStatus("Code block copied")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Attached Document Banner
        if (attachedDocument != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attached: ${attachedDocument.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NomaticEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearAttachment, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear attachment", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Composer Bar positioned cleanly above keyboard
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // In-Chat Model Switcher Pill Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showModelDropdown by remember { mutableStateOf(false) }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable { showModelDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = NomaticEmerald, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "$currentModel ∨",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName) },
                                    leadingIcon = {
                                        if (model.id == currentModel) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = NomaticEmerald, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        onSelectProviderModel(model.providerId, model.id)
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NomaticEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NomaticEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "⇄ ${currentProvider.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NomaticEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                IconButton(
                    onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*")) },
                    modifier = Modifier.testTag("assistant_attach_button")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                }

                OutlinedTextField(
                    value = localInputText,
                    onValueChange = {
                        localInputText = it
                        onDraftChange(it)
                    },
                    placeholder = { Text("Ask $currentModel...") },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { doSendMessage() }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_message_input")
                )

                Button(
                    onClick = doSendMessage,
                    enabled = !isStreaming && (localInputText.isNotBlank() || attachedDocument != null),
                    colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.testTag("assistant_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
    }

    // Rename Dialog
    val convToRename = showRenameDialog
    if (convToRename != null) {
        var newTitle by remember(convToRename) { mutableStateOf(convToRename.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameConversation(convToRename.id, newTitle)
                        showRenameDialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Edit Message Dialog
    val msgToEdit = editingMessage
    if (msgToEdit != null) {
        var updatedText by remember(msgToEdit) { mutableStateOf(msgToEdit.content) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit & Resend Message") },
            text = {
                OutlinedTextField(
                    value = updatedText,
                    onValueChange = { updatedText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditAndResend(msgToEdit, updatedText)
                        editingMessage = null
                    }
                ) { Text("Resend") }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("Cancel") }
            }
        )
    }

    // History Bottom Sheet
    if (showHistoryModal) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            onNewConversation()
                            showHistoryModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat")
                    }
                }

                OutlinedTextField(
                    value = conversationSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search chats...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        NomaticCard(
                            accentBorder = isActive,
                            onClick = {
                                onSelectConversation(conv.id)
                                showHistoryModal = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { onDeleteConversation(conv.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NomaticRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    onCopyText: (String) -> Unit,
    onEditMessage: () -> Unit,
    onRegenerate: () -> Unit,
    onRetryWithFallback: (String) -> Unit,
    onSaveToVault: () -> Unit,
    fallbackProviderId: String?
) {
    val isUser = message.role.equals("user", ignoreCase = true)
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isUser) NomaticRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryPill(
                        label = if (isUser) "You" else message.providerId?.uppercase() ?: "AI",
                        color = if (isUser) NomaticRed else NomaticEmerald
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { onCopyText(message.content) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        }
                        if (isUser) {
                            IconButton(onClick = onEditMessage, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                            }
                        } else {
                            IconButton(onClick = onSaveToVault, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to Vault", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    MarkdownMessageContent(
                        content = message.content,
                        onCopyCode = onCopyText
                    )
                }

                if (message.status == "FAILED" || message.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.errorMessage ?: "An error occurred during response synthesis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NomaticRed
                    )
                    if (fallbackProviderId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onRetryWithFallback(fallbackProviderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                        ) {
                            Text("Retry with Fallback")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownMessageContent(
    content: String,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedBlocks = remember(content) { parseSlateMarkdownBlocks(content) }
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parsedBlocks.forEach { block ->
            when (block.type) {
                SlateBlockType.H1, SlateBlockType.H2, SlateBlockType.H3 -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                SlateBlockType.CODE -> {
                    CodeBlockCard(
                        code = block.content,
                        language = block.meta ?: "bash",
                        onCopy = { onCopyCode(block.content) }
                    )
                }
                SlateBlockType.QUOTE -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NomaticAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NomaticAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = block.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                SlateBlockType.BULLET -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = NomaticRed)
                        RenderFormattedMarkdownText(text = block.content)
                    }
                }
                SlateBlockType.PARAGRAPH -> {
                    RenderFormattedMarkdownText(text = block.content)
                }
            }
        }
    }
}

// AI Provider Management Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIProviderManagementScreen(
    settings: ProviderSettingsEntity,
    maskedOpenAiKey: String?,
    maskedGeminiKey: String?,
    providerHealth: Map<String, ProviderHealthStatus>,
    connectionTestStatus: Map<String, String>? = null,
    onSaveApiKey: (String, String) -> Unit,
    onDeleteApiKey: (String) -> Unit,
    onTestConnection: (String) -> Unit,
    onUpdateSettings: ((ProviderSettingsEntity) -> ProviderSettingsEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var primaryProvider by remember { mutableStateOf(settings.primaryProvider) }
    var geminiKeyInput by remember { mutableStateOf("") }
    var openAiKeyInput by remember { mutableStateOf("") }
    var autoFallbackEnabled by remember { mutableStateOf(settings.autoFallbackEnabled) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("AI Provider Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NomaticCard {
                Text("Primary Provider Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = primaryProvider == "gemini",
                        onClick = { primaryProvider = "gemini" },
                        label = { Text("Google Gemini") }
                    )
                    FilterChip(
                        selected = primaryProvider == "openai",
                        onClick = { primaryProvider = "openai" },
                        label = { Text("OpenRouter / OpenAI") }
                    )
                }
            }

            NomaticCard {
                Text("Google Gemini Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Key Status: ${if (!maskedGeminiKey.isNullOrBlank()) "Configured ($maskedGeminiKey)" else "Not set"}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = { geminiKeyInput = it },
                    label = { Text("Set Gemini API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (geminiKeyInput.isNotBlank()) {
                                onSaveApiKey("gemini", geminiKeyInput)
                                geminiKeyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) { Text("Save Key") }
                    OutlinedButton(onClick = { onTestConnection("gemini") }) { Text("Test Gemini") }
                }
            }

            NomaticCard {
                Text("OpenRouter / OpenAI Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Key Status: ${if (!maskedOpenAiKey.isNullOrBlank()) "Configured ($maskedOpenAiKey)" else "Not set"}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = openAiKeyInput,
                    onValueChange = { openAiKeyInput = it },
                    label = { Text("Set OpenRouter / OpenAI Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (openAiKeyInput.isNotBlank()) {
                                onSaveApiKey("openai", openAiKeyInput)
                                openAiKeyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) { Text("Save Key") }
                    OutlinedButton(onClick = { onTestConnection("openai") }) { Text("Test OpenRouter") }
                }
            }

            NomaticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Fallback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = autoFallbackEnabled, onCheckedChange = { autoFallbackEnabled = it })
                }
            }

            Button(
                onClick = {
                    onUpdateSettings { current ->
                        current.copy(
                            primaryProvider = primaryProvider,
                            autoFallbackEnabled = autoFallbackEnabled
                        )
                    }
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Assistant Text", text))
}
