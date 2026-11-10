package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteEntity
import com.example.data.local.PromptEntity
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.MarkdownTableCard
import com.example.ui.components.NomaticCard
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val VAULT_CATEGORIES = listOf(
    "All",
    "Notes",
    "Saved links",
    "AI responses",
    "Prompts",
    "Linux commands",
    "Scripts",
    "Technical documentation"
)

enum class VaultViewMode { GRID, LIST, FOLDERS }

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun KnowledgeVaultScreen(
    notes: List<NoteEntity>,
    onSaveNote: (id: String, title: String, content: String, category: String, tags: String) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onOpenPromptLibrary: () -> Unit,
    onShowStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortDescending by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(VaultViewMode.GRID) }

    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Multi-Select Synthesizer state
    var isSynthesizeMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSynthesizeModal by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery, selectedCategory, sortDescending) {
        notes.filter { note ->
            val matchesCat = selectedCategory == "All" ||
                note.category.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                note.title.contains(q, ignoreCase = true) ||
                note.content.contains(q, ignoreCase = true) ||
                note.tags.contains(q, ignoreCase = true)
            matchesCat && matchesSearch
        }.let { list ->
            if (sortDescending) list else list.sortedBy { it.updatedAt }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingNew = true },
                containerColor = NomaticRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("vault_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Note")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("vault_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Title Banner with Responsive Wrapping
            item {
                DotMatrixBanner(
                    title = "Knowledge Vault",
                    subtitle = "Organize notes, code snippets, web extractions & AI responses",
                    statusLabel = "${notes.size} Items Stored",
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val json = onExportJson()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Nomatic AI Vault Backup")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Vault JSON"))
                                }
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Export Vault")
                            }
                            IconButton(onClick = { showImportDialog = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Import Vault")
                            }
                        }
                    }
                )
            }

            // Search Bar & View Controls Toolbar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search titles, content, tags...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_search_input")
                        )

                        // Segmented View Mode Toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (viewMode == VaultViewMode.GRID) NomaticRed.copy(alpha = 0.2f) else Color.Transparent,
                                    modifier = Modifier.clickable { viewMode = VaultViewMode.GRID }
                                ) {
                                    Text(
                                        text = "Grid",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (viewMode == VaultViewMode.GRID) NomaticRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (viewMode == VaultViewMode.LIST) NomaticRed.copy(alpha = 0.2f) else Color.Transparent,
                                    modifier = Modifier.clickable { viewMode = VaultViewMode.LIST }
                                ) {
                                    Text(
                                        text = "List",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (viewMode == VaultViewMode.LIST) NomaticRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Segmented Category Filter Bar & AI Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(VAULT_CATEGORIES) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onOpenPromptLibrary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Prompts", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Polished Empty State
            if (filteredNotes.isEmpty()) {
                item {
                    NomaticCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = NomaticRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Vault Items Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCategory != "All") {
                                    "No items match your active search query or category filter."
                                } else {
                                    "Your Knowledge Vault is empty. Save notes, code snippets, links, or web extractions for quick access."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { isCreatingNew = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Note")
                            }
                        }
                    }
                }
            }

            // Render Items Grid vs List
            when (viewMode) {
                VaultViewMode.GRID -> {
                    val notePairs = filteredNotes.chunked(2)
                    items(notePairs, key = { pair -> pair.first().id }) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val note1 = pair[0]
                            Box(modifier = Modifier.weight(1f)) {
                                VaultCardItem(
                                    note = note1,
                                    isSynthesizeMode = isSynthesizeMode,
                                    isSelected = selectedNoteIds.contains(note1.id),
                                    onToggleSelect = {
                                        selectedNoteIds = if (selectedNoteIds.contains(note1.id)) selectedNoteIds - note1.id else selectedNoteIds + note1.id
                                    },
                                    onClick = { editingNote = note1 },
                                    onTogglePin = { onTogglePin(note1) },
                                    onDelete = { onDeleteNote(note1.id) },
                                    onCopy = {
                                        copyTextToClipboard(context, note1.content)
                                        onShowStatus("Copied '${note1.title}'")
                                    },
                                    onShare = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, note1.title)
                                            putExtra(Intent.EXTRA_TEXT, "${note1.title}\n\n${note1.content}")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Note"))
                                    }
                                )
                            }

                            if (pair.size > 1) {
                                val note2 = pair[1]
                                Box(modifier = Modifier.weight(1f)) {
                                    VaultCardItem(
                                        note = note2,
                                        isSynthesizeMode = isSynthesizeMode,
                                        isSelected = selectedNoteIds.contains(note2.id),
                                        onToggleSelect = {
                                            selectedNoteIds = if (selectedNoteIds.contains(note2.id)) selectedNoteIds - note2.id else selectedNoteIds + note2.id
                                        },
                                        onClick = { editingNote = note2 },
                                        onTogglePin = { onTogglePin(note2) },
                                        onDelete = { onDeleteNote(note2.id) },
                                        onCopy = {
                                            copyTextToClipboard(context, note2.content)
                                            onShowStatus("Copied '${note2.title}'")
                                        },
                                        onShare = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, note2.title)
                                                putExtra(Intent.EXTRA_TEXT, "${note2.title}\n\n${note2.content}")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Note"))
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                VaultViewMode.LIST, VaultViewMode.FOLDERS -> {
                    items(filteredNotes, key = { it.id }) { note ->
                        VaultCardItem(
                            note = note,
                            isSynthesizeMode = isSynthesizeMode,
                            isSelected = selectedNoteIds.contains(note.id),
                            onToggleSelect = {
                                selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                            },
                            onClick = { editingNote = note },
                            onTogglePin = { onTogglePin(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onCopy = {
                                copyTextToClipboard(context, note.content)
                                onShowStatus("Copied '${note.title}'")
                            },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, note.title)
                                    putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Note"))
                            }
                        )
                    }
                }
            }
        }
    }

    if (isCreatingNew || editingNote != null) {
        val currentNote = editingNote ?: NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "Untitled Note",
            content = "# New Note\nWrite your thoughts, code snippets, or documentation here.",
            category = "Notes",
            tags = "note",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        com.example.ui.components.NotionObsidianSlateSheet(
            note = currentNote,
            onSaveNote = { id, title, content, category, tags ->
                onSaveNote(id, title, content, category, tags)
                isCreatingNew = false
                editingNote = null
            },
            onDismiss = {
                isCreatingNew = false
                editingNote = null
            },
            onShowStatus = onShowStatus
        )
    }

    if (showImportDialog) {
        var importJsonText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Vault JSON Backup") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = { Text("Paste exported Nomatic AI JSON backup...") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportJson(importJsonText)
                        showImportDialog = false
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VaultCardItem(
    note: NoteEntity,
    isSynthesizeMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val isPinned = note.tags.contains("PINNED", ignoreCase = true)
    val isCodeOrCommand = note.category == "Linux commands" || note.category == "Scripts" || note.content.contains("```")
    val isTable = note.content.contains("| ---") || note.content.contains("| :-")

    NomaticCard(
        accentBorder = isPinned || isSelected,
        onClick = if (isSynthesizeMode) onToggleSelect else onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSynthesizeMode) {
                    IconButton(onClick = onToggleSelect, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.StarBorder,
                            contentDescription = "Select",
                            tint = if (isSelected) NomaticEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TelemetryPill(label = note.category ?: "Notes", color = NomaticRed)
                if (isPinned) {
                    TelemetryPill(label = "Pinned", color = NomaticAmber)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (isPinned) NomaticAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NomaticRed, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = note.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDate(note.updatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (isCodeOrCommand) {
            val codeContent = note.content.removePrefix("```sh").removePrefix("```bash").removePrefix("```python").removePrefix("```").removeSuffix("```").trim()
            CodeBlockCard(
                code = codeContent.take(250),
                onCopy = onCopy
            )
        } else if (isTable) {
            MarkdownTableCard(tableMarkdown = note.content.take(600))
        } else {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (note.tags.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tags: ${note.tags}",
                style = MaterialTheme.typography.labelSmall,
                color = NomaticEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Vault Note", text))
}

@Composable
fun PromptLibraryScreen(
    prompts: List<PromptEntity>,
    onSavePrompt: (id: String, title: String, description: String, content: String, category: String, tags: String, isFav: Boolean) -> Unit,
    onToggleFavorite: (PromptEntity) -> Unit,
    onDeletePrompt: (String) -> Unit,
    onInsertIntoChat: (PromptEntity) -> Unit,
    onShowStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingPrompt by remember { mutableStateOf<PromptEntity?>(null) }
    var isAddingPrompt by remember { mutableStateOf(false) }

    val categories = remember(prompts) {
        listOf("All") + prompts.map { it.category }.distinct()
    }

    val filtered = remember(prompts, searchQuery, selectedCategory) {
        prompts.filter { p ->
            val catOk = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim()
            val qOk = q.isEmpty() ||
                p.title.contains(q, ignoreCase = true) ||
                p.description.contains(q, ignoreCase = true) ||
                p.content.contains(q, ignoreCase = true) ||
                p.tags.contains(q, ignoreCase = true)
            catOk && qOk
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Prompt Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingPrompt = true },
                containerColor = NomaticRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Prompt")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search prompts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { prompt ->
                NomaticCard(onClick = { onInsertIntoChat(prompt) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prompt.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onToggleFavorite(prompt) }) {
                            Icon(
                                imageVector = if (prompt.isFavorite) Icons.Default.PushPin else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (prompt.isFavorite) NomaticAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prompt.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onInsertIntoChat(prompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) {
                        Text("Use Prompt")
                    }
                }
            }
        }
    }
}
