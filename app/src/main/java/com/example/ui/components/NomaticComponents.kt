package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MonospaceTextStyle
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun DotMatrixBanner(
    title: String,
    subtitle: String,
    statusLabel: String,
    isOnline: Boolean = true,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val borderColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 14.dp.toPx()
                val radius = 1.2.dp.toPx()
                var x = step
                while (x < size.width) {
                    var y = step
                    while (y < size.height) {
                        drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) NomaticEmerald else NomaticRed)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    trailingContent?.invoke()
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NomaticCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBorder: Boolean = false,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (accentBorder) {
        NomaticRed.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}

@Composable
fun NomaticSectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NomaticRed)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onActionClick)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun TelemetryPill(
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accent) NomaticRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (accent) NomaticRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (accent) NomaticRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (accent) NomaticRed else MaterialTheme.colorScheme.outline)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class CodeTheme(
    val displayName: String,
    val containerBg: Color,
    val headerBg: Color,
    val codeTextColor: Color,
    val lineNumColor: Color
) {
    VS_CODE("VS Code", Color(0xFF121217), Color(0xFF1C1C24), Color(0xFFE5E7EB), Color(0xFF6B7280)),
    MONOKAI("Monokai", Color(0xFF1E1E24), Color(0xFF272822), Color(0xFFF8F8F2), Color(0xFF75715E)),
    MATRIX("Matrix", Color(0xFF040F08), Color(0xFF0A1F11), Color(0xFF34D399), Color(0xFF059669))
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String = "bash",
    isDestructive: Boolean = false,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLineNumbers by remember { mutableStateOf(true) }
    var currentTheme by remember { mutableStateOf(CodeTheme.VS_CODE) }

    val lines = remember(code) { code.trim().lines() }
    val extension = remember(language) {
        when (language.lowercase()) {
            "bash", "sh", "shell" -> "sh"
            "python", "py" -> "py"
            "kotlin", "kt" -> "kt"
            "json" -> "json"
            "sql" -> "sql"
            "yaml", "yml" -> "yaml"
            "html" -> "html"
            "cpp", "c++" -> "cpp"
            "javascript", "js" -> "js"
            else -> "txt"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = currentTheme.containerBg),
        border = BorderStroke(1.dp, if (isDestructive) NomaticRed else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDestructive) NomaticRed.copy(alpha = 0.18f) else currentTheme.headerBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = language.ifBlank { "code" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) NomaticRed else NomaticAmber
                    )
                    if (isDestructive) {
                        TelemetryPill(
                            label = "Review before execution",
                            color = Color.White,
                            backgroundColor = NomaticRed
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = currentTheme.headerBg,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showLineNumbers = !showLineNumbers }
                    ) {
                        Text(
                            text = if (showLineNumbers) "# ON" else "# OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NomaticRed,
                        modifier = Modifier.clickable(onClick = onCopy)
                    ) {
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NomaticEmerald,
                        modifier = Modifier.clickable {
                            val filename = "nomatic_script_$language.$extension"
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, filename)
                                putExtra(android.content.Intent.EXTRA_TEXT, code)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Script ($filename)"))
                        }
                    ) {
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Code Body with Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                if (showLineNumbers) {
                    Column(
                        modifier = Modifier.padding(end = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.indices.forEach { idx ->
                            Text(
                                text = "${idx + 1}",
                                style = MonospaceTextStyle,
                                color = currentTheme.lineNumColor
                            )
                        }
                    }
                }
                Column {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            style = MonospaceTextStyle,
                            color = currentTheme.codeTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeDiffViewerCard(
    title: String = "CODE & RESPONSE DIFF INSPECTOR",
    originalText: String,
    modifiedText: String,
    modifier: Modifier = Modifier
) {
    val diffResult = remember(originalText, modifiedText) {
        com.example.core.utilities.CodeDiffUtility.computeDiff(originalText, modifiedText)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NomaticAmber
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryPill(label = "+${diffResult.addedCount} Added", color = NomaticEmerald)
                    TelemetryPill(label = "-${diffResult.removedCount} Removed", color = NomaticRed)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                diffResult.lines.forEach { line ->
                    val bgColor = when (line.type) {
                        com.example.core.utilities.DiffType.ADDED -> NomaticEmerald.copy(alpha = 0.15f)
                        com.example.core.utilities.DiffType.REMOVED -> NomaticRed.copy(alpha = 0.15f)
                        com.example.core.utilities.DiffType.UNCHANGED -> Color.Transparent
                    }

                    val textColor = when (line.type) {
                        com.example.core.utilities.DiffType.ADDED -> NomaticEmerald
                        com.example.core.utilities.DiffType.REMOVED -> NomaticRed
                        com.example.core.utilities.DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
                    }

                    val prefix = when (line.type) {
                        com.example.core.utilities.DiffType.ADDED -> "+ "
                        com.example.core.utilities.DiffType.REMOVED -> "- "
                        com.example.core.utilities.DiffType.UNCHANGED -> "  "
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$prefix${line.text}",
                            style = MonospaceTextStyle,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownTableCard(
    tableMarkdown: String,
    modifier: Modifier = Modifier
) {
    val rows = remember(tableMarkdown) {
        tableMarkdown.lines().map { it.trim() }.filter { it.startsWith("|") && it.endsWith("|") }
    }
    if (rows.isEmpty()) return

    val headers = rows.firstOrNull()?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    val dataRows = rows.drop(1).filter { !it.contains("---") && !it.contains(":-") }.map { row ->
        row.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                headers.forEach { h ->
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NomaticRed,
                        modifier = Modifier.widthIn(min = 80.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            dataRows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    r.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 80.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class SlateBlockType { H1, H2, H3, CODE, QUOTE, BULLET, PARAGRAPH }
data class SlateBlock(val type: SlateBlockType, val content: String, val meta: String? = null)

fun parseSlateMarkdownBlocks(text: String): List<SlateBlock> {
    val result = mutableListOf<SlateBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            result.add(SlateBlock(SlateBlockType.CODE, codeLines.joinToString("\n"), lang.ifBlank { "bash" }))
            i++
            continue
        }

        if (trimmed.startsWith("# ")) {
            result.add(SlateBlock(SlateBlockType.H1, trimmed.removePrefix("# ").trim()))
        } else if (trimmed.startsWith("## ")) {
            result.add(SlateBlock(SlateBlockType.H2, trimmed.removePrefix("## ").trim()))
        } else if (trimmed.startsWith("### ")) {
            result.add(SlateBlock(SlateBlockType.H3, trimmed.removePrefix("### ").trim()))
        } else if (trimmed.startsWith("> ")) {
            result.add(SlateBlock(SlateBlockType.QUOTE, trimmed.removePrefix("> ").trim()))
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            result.add(SlateBlock(SlateBlockType.BULLET, trimmed.substring(2).trim()))
        } else if (trimmed.isNotBlank()) {
            result.add(SlateBlock(SlateBlockType.PARAGRAPH, trimmed))
        }

        i++
    }

    return result
}

@Composable
fun RenderFormattedMarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var remaining = text
            while (remaining.isNotEmpty()) {
                val boldMatch = Regex("^\\*\\*(.*?)\\*\\*").find(remaining)
                val italicMatch = Regex("^\\*(.*?)\\*").find(remaining)
                val codeMatch = Regex("^`(.*?)`").find(remaining)

                when {
                    boldMatch != null -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldMatch.groupValues[1])
                        }
                        remaining = remaining.substring(boldMatch.value.length)
                    }
                    italicMatch != null -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicMatch.groupValues[1])
                        }
                        remaining = remaining.substring(italicMatch.value.length)
                    }
                    codeMatch != null -> {
                        withStyle(SpanStyle(fontFamily = SpaceMonoFontFamily, background = Color(0x1F888888))) {
                            append(codeMatch.groupValues[1])
                        }
                        remaining = remaining.substring(codeMatch.value.length)
                    }
                    else -> {
                        append(remaining.first())
                        remaining = remaining.substring(1)
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotionObsidianSlateSheet(
    note: com.example.data.local.NoteEntity,
    onSaveNote: (id: String, title: String, content: String, category: String, tags: String) -> Unit,
    onDismiss: () -> Unit,
    onShowStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var docTitle by remember(note) { mutableStateOf(note.title) }
    var docCategory by remember(note) { mutableStateOf(note.category ?: "Notes") }
    var docTags by remember(note) { mutableStateOf(note.tags ?: "") }
    var docContent by remember(note) { mutableStateOf(note.content) }
    var isPreviewMode by remember { mutableStateOf(true) }
    var showMetadataInfo by remember { mutableStateOf(false) }

    val wordCount = remember(docContent) {
        if (docContent.isBlank()) 0 else docContent.trim().split(Regex("\\s+")).size
    }
    val readingTime = remember(wordCount) {
        maxOf(1, (wordCount / 200.0).toInt())
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxHeight(0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = docTitle.ifBlank { "Untitled Document" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showMetadataInfo = !showMetadataInfo },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showMetadataInfo) Icons.Default.KeyboardArrowUp else Icons.Default.Info,
                            contentDescription = "Toggle Document Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { isPreviewMode = !isPreviewMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPreviewMode) "Edit" else "Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NomaticRed,
                        modifier = Modifier.clickable {
                            onSaveNote(note.id, docTitle, docContent, docCategory, docTags)
                            onShowStatus("Saved Note '$docTitle'")
                            onDismiss()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showMetadataInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$wordCount words • ~$readingTime min read",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = docCategory,
                            onValueChange = { docCategory = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = docTags,
                            onValueChange = { docTags = it },
                            label = { Text("Tags") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isPreviewMode) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = docTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val parsedBlocks = parseSlateMarkdownBlocks(docContent)
                        items(parsedBlocks) { block ->
                            when (block.type) {
                                SlateBlockType.H1 -> {
                                    Text(
                                        text = block.content,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                SlateBlockType.H2 -> {
                                    Text(
                                        text = block.content,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                SlateBlockType.H3 -> {
                                    Text(
                                        text = block.content,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                SlateBlockType.QUOTE -> {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NomaticAmber.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, NomaticAmber.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = block.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                SlateBlockType.CODE -> {
                                    CodeBlockCard(
                                        code = block.content,
                                        language = block.meta ?: "bash",
                                        onCopy = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.content))
                                            onShowStatus("Copied snippet")
                                        }
                                    )
                                }
                                SlateBlockType.BULLET -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                } else {
                    OutlinedTextField(
                        value = docContent,
                        onValueChange = { docContent = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("Write your document here in markdown...") }
                    )
                }
            }
        }
    }
}
