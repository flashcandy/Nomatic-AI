package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.BookmarkEntity
import com.example.domain.BrowserTab
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.MarkdownTableCard
import com.example.ui.components.NomaticCard
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticAmber
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed
import org.json.JSONObject

data class DevPortalPreset(val title: String, val url: String, val icon: String)
private val PRESET_DEV_PORTALS = listOf(
    DevPortalPreset("GitHub", "https://github.com", "🐙"),
    DevPortalPreset("Linux Man", "https://man7.org/linux/man-pages/", "🐧"),
    DevPortalPreset("Android Docs", "https://developer.android.com", "📘"),
    DevPortalPreset("Python Docs", "https://docs.python.org/3/", "🐍"),
    DevPortalPreset("Docker Docs", "https://docs.docker.com", "🐳"),
    DevPortalPreset("AWS Docs", "https://docs.aws.amazon.com", "⚡")
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniBrowserScreen(
    tabs: List<BrowserTab>,
    activeTabId: String,
    javaScriptEnabled: Boolean,
    httpsOnly: Boolean,
    browserAiResult: String?,
    isBrowserAiLoading: Boolean,
    bookmarks: List<BookmarkEntity> = emptyList(),
    onSelectTab: (String) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (String) -> Unit,
    onNavigateUrl: (String) -> Unit,
    onPageLoaded: (tabId: String, url: String, title: String, readableText: String) -> Unit,
    onBookmarkCurrentPage: (url: String, title: String) -> Unit,
    onDeleteBookmark: (String) -> Unit = {},
    onSummarizePage: () -> Unit,
    onAskQuestionAboutPage: (String) -> Unit,
    onSavePageToVault: (title: String, content: String) -> Unit,
    onSharePageToAssistant: (String) -> Unit,
    onClearAiResult: () -> Unit
) {
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()
    var urlInput by remember(activeTab.url) { mutableStateOf(activeTab.url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(false) }
    var showAiDrawer by remember { mutableStateOf(false) }
    var pageQuestion by remember { mutableStateOf("") }
    var showAddLinkDialog by remember { mutableStateOf(false) }

    var showExtractSheet by remember { mutableStateOf(false) }
    var extractedTables by remember { mutableStateOf<List<String>>(emptyList()) }
    var extractedCode by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Multi-Tab Strip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val selected = tab.id == activeTab.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) NomaticRed.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (selected) NomaticRed else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable { onSelectTab(tab.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.title.take(18),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onCloseTab(tab.id) }
                                )
                            }
                        }
                    }
                    item {
                        IconButton(
                            onClick = onAddTab,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("browser_add_tab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // ⭐ FAVORITE DEVELOPER & GITHUB LINKS QUICK LAUNCHER STRIP
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NomaticAmber,
                            modifier = Modifier.clickable { showAddLinkDialog = true }
                        ) {
                            Text(
                                text = "+ ADD LINK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Preset Developer Portals
                    items(PRESET_DEV_PORTALS) { portal ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable { onNavigateUrl(portal.url) }
                        ) {
                            Text(
                                text = "${portal.icon} ${portal.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Custom User Saved Bookmarks
                    items(bookmarks) { b ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NomaticEmerald.copy(alpha = 0.16f),
                            border = BorderStroke(0.5.dp, NomaticEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onNavigateUrl(b.url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📌 ${b.title.take(15)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NomaticEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NomaticRed,
                                    modifier = Modifier.clickable { onDeleteBookmark(b.id) }
                                )
                            }
                        }
                    }
                }

                // URL Input & Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "HTTPS Security",
                                tint = if (urlInput.startsWith("https://")) NomaticEmerald else NomaticRed,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        placeholder = { Text("Search or enter HTTPS URL...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_url_input")
                    )

                    Button(
                        onClick = { onNavigateUrl(urlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("browser_go_button")
                    ) {
                        Text("GO", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // AI Webpage Action Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAiDrawer = true
                                onSummarizePage()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("browser_ai_summarize")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NomaticRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI SUMMARY", style = MaterialTheme.typography.labelSmall)
                        }

                        // 🌐 WEB PAGE TABLE & CODE EXTRACTOR BUTTON
                        OutlinedButton(
                            onClick = {
                                val js = """
                                    (function(){
                                        var result = { tables: [], codeBlocks: [] };
                                        var tables = document.querySelectorAll('table');
                                        tables.forEach(function(tbl) {
                                            var rows = tbl.querySelectorAll('tr');
                                            var markdown = "";
                                            rows.forEach(function(row, rIdx) {
                                                var cells = row.querySelectorAll('th, td');
                                                var line = "|";
                                                cells.forEach(function(c) {
                                                    line += " " + c.innerText.trim().replace(/\n/g, " ") + " |";
                                                });
                                                if (line.length > 2) {
                                                    markdown += line + "\n";
                                                    if (rIdx === 0) {
                                                        var sep = "|";
                                                        cells.forEach(function() { sep += " --- |"; });
                                                        markdown += sep + "\n";
                                                    }
                                                }
                                            });
                                            if (markdown.trim()) result.tables.push(markdown);
                                        });
                                        var codeNodes = document.querySelectorAll('pre, code');
                                        codeNodes.forEach(function(node) {
                                            var txt = node.innerText.trim();
                                            if (txt.length > 20 && !result.codeBlocks.includes(txt) && !txt.includes("\n| ")) {
                                                result.codeBlocks.push(txt.slice(0, 4000));
                                            }
                                        });
                                        return JSON.stringify(result);
                                    })();
                                """.trimIndent()

                                webViewRef?.evaluateJavascript(js) { jsonRaw ->
                                    try {
                                        val cleaned = jsonRaw?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\n", "\n")
                                        if (!cleaned.isNullOrBlank() && cleaned != "null") {
                                            val obj = JSONObject(cleaned)
                                            val tablesArr = obj.optJSONArray("tables")
                                            val codeArr = obj.optJSONArray("codeBlocks")
                                            val extractedT = mutableListOf<String>()
                                            val extractedC = mutableListOf<String>()
                                            if (tablesArr != null) {
                                                for (i in 0 until tablesArr.length()) {
                                                    extractedT.add(tablesArr.getString(i))
                                                }
                                            }
                                            if (codeArr != null) {
                                                for (i in 0 until codeArr.length()) {
                                                    extractedC.add(codeArr.getString(i))
                                                }
                                            }
                                            extractedTables = extractedT
                                            extractedCode = extractedC
                                            showExtractSheet = true
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("browser_extract_tables_code")
                        ) {
                            Text("🌐 EXTRACT TABLES & CODE", style = MaterialTheme.typography.labelSmall, color = NomaticEmerald, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showAiDrawer = !showAiDrawer },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ASK PAGE", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onBookmarkCurrentPage(activeTab.url, activeTab.title) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark Page", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                onSavePageToVault(
                                    activeTab.title,
                                    "Source URL: ${activeTab.url}\n\n${activeTab.readableContent.ifBlank { browserAiResult.orEmpty() }}"
                                )
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Page to Vault", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                onSharePageToAssistant("Analyze this webpage: ${activeTab.title} (${activeTab.url})\n\n${activeTab.readableContent.take(6000)}")
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Send to Assistant", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (isPageLoading) {
                    LinearProgressIndicator(
                        color = NomaticRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Collapsible AI Page Analysis Drawer
        if (showAiDrawer || browserAiResult != null || isBrowserAiLoading) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryPill(label = "WEBPAGE INTELLIGENCE", color = NomaticRed)
                        TextButton(
                            onClick = {
                                showAiDrawer = false
                                onClearAiResult()
                            }
                        ) {
                            Text("CLOSE", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pageQuestion,
                            onValueChange = { pageQuestion = it },
                            placeholder = { Text("Ask a question about this webpage...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onAskQuestionAboutPage(pageQuestion) },
                            enabled = pageQuestion.isNotBlank() && !isBrowserAiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                        ) {
                            Text("ASK")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (isBrowserAiLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = NomaticRed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Analyzing readable webpage content...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (!browserAiResult.isNullOrBlank()) {
                        NomaticCard {
                            Text(
                                text = browserAiResult,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    onSavePageToVault("Web Summary: ${activeTab.title}", browserAiResult)
                                }
                            ) {
                                Text("SAVE SUMMARY TO VAULT", color = NomaticRed)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
        }

        // Hardened Android WebView
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Strict Browser Security Configuration (Section 4 Screen 6)
                        settings.apply {
                            this.javaScriptEnabled = javaScriptEnabled
                            allowFileAccess = false
                            allowContentAccess = false
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = false
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }
                        // Never expose addJavascriptInterface to untrusted websites
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUri = request?.url ?: return false
                                val scheme = reqUri.scheme?.lowercase() ?: ""
                                if (httpsOnly && scheme == "http") {
                                    view?.loadUrl(reqUri.toString().replaceFirst("http://", "https://"))
                                    return true
                                }
                                // Block file://, intent://, javascript:// schemes
                                if (scheme != "https" && scheme != "http") {
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isPageLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isPageLoading = false
                                val loadedUrl = url ?: activeTab.url
                                val pageTitle = view?.title ?: loadedUrl
                                if (javaScriptEnabled) {
                                    view?.evaluateJavascript(
                                        "(function(){ return document.body ? document.body.innerText.slice(0, 20000) : ''; })();"
                                    ) { rawJsString ->
                                        val cleaned = rawJsString
                                            ?.removeSurrounding("\"")
                                            ?.replace("\\n", "\n")
                                            ?.replace("\\\"", "\"")
                                            .orEmpty()
                                        onPageLoaded(activeTab.id, loadedUrl, pageTitle, cleaned)
                                    }
                                } else {
                                    onPageLoaded(activeTab.id, loadedUrl, pageTitle, "")
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                // CRITICAL SECURITY: Never bypass SSL certificate errors
                                handler?.cancel()
                                isPageLoading = false
                            }
                        }
                        webViewRef = this
                        loadUrl(activeTab.url)
                    }
                },
                update = { webView ->
                    webView.settings.javaScriptEnabled = javaScriptEnabled
                    if (webView.url != activeTab.url) {
                        webView.loadUrl(activeTab.url)
                    }
                }
            )
        }

        // 🌐 EXTRACTED ASSETS BOTTOM SHEET
        if (showExtractSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExtractSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EXTRACTED WEBPAGE ASSETS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NomaticEmerald
                            )
                            Text(
                                text = "Found ${extractedTables.size} Table(s) & ${extractedCode.size} Code Snippet(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showExtractSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Button(
                        onClick = {
                            val combinedMarkdown = buildString {
                                append("# Extracted Assets: ${activeTab.title}\n")
                                append("Source: ${activeTab.url}\n\n")
                                if (extractedTables.isNotEmpty()) {
                                    append("## Data Tables\n\n")
                                    extractedTables.forEachIndexed { idx, tbl ->
                                        append("### Table ${idx + 1}\n\n$tbl\n\n")
                                    }
                                }
                                if (extractedCode.isNotEmpty()) {
                                    append("## Code Snippets\n\n")
                                    extractedCode.forEachIndexed { idx, code ->
                                        append("### Snippet ${idx + 1}\n\n```\n$code\n```\n\n")
                                    }
                                }
                            }
                            onSavePageToVault("Extracted Assets: ${activeTab.title}", combinedMarkdown)
                            showExtractSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE ALL TO KNOWLEDGE VAULT", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    if (extractedTables.isEmpty() && extractedCode.isEmpty()) {
                        Text(
                            text = "No structured HTML tables or code blocks found on this page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    if (extractedTables.isNotEmpty()) {
                        Text(
                            text = "DATA TABLES (${extractedTables.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NomaticEmerald
                        )
                        extractedTables.forEachIndexed { index, tableMd ->
                            Text("Table #${index + 1}", style = MaterialTheme.typography.labelSmall)
                            MarkdownTableCard(tableMarkdown = tableMd)
                        }
                    }

                    if (extractedCode.isNotEmpty()) {
                        Text(
                            text = "CODE SNIPPETS (${extractedCode.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NomaticAmber
                        )
                        val context = androidx.compose.ui.platform.LocalContext.current
                        extractedCode.forEachIndexed { index, codeTxt ->
                            Text("Code Snippet #${index + 1}", style = MaterialTheme.typography.labelSmall)
                            CodeBlockCard(
                                code = codeTxt,
                                onCopy = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Code Snippet", codeTxt)
                                    clipboard?.setPrimaryClip(clip)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // 📌 ADD CUSTOM LINK / REPO DIALOG
        if (showAddLinkDialog) {
            var customTitle by remember { mutableStateOf("") }
            var customUrl by remember { mutableStateOf("https://") }

            AlertDialog(
                onDismissRequest = { showAddLinkDialog = false },
                title = { Text("ADD CUSTOM LINK / GITHUB REPO") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Save your favorite documentation portal, API reference, or GitHub repository for 1-tap launcher access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Link Title (e.g. Kotlin Docs)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("URL or GitHub Link") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                onBookmarkCurrentPage(
                                    customUrl.trim(),
                                    customTitle.ifBlank { customUrl.trim() }
                                )
                                showAddLinkDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NomaticRed)
                    ) {
                        Text("ADD LINK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddLinkDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}
