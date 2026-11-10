package com.example.core.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

data class ExtractedDocument(
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val pageCount: Int = 1,
    val extractedText: String,
    val previewBitmap: Bitmap? = null
)

object DocumentProcessor {
    private const val MAX_FILE_SIZE_BYTES = 15L * 1024L * 1024L // 15 MB limit
    private const val MAX_EXTRACTED_CHARS = 32_000

    suspend fun processUri(context: Context, uri: Uri): Result<ExtractedDocument> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: guessMimeFromPath(uri.toString())
            var fileName = "document"
            var fileSize = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: "document"
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("File exceeds 15 MB local processing limit (${fileSize / (1024 * 1024)} MB). Select a smaller document.")
                )
            }

            val isPdf = mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)
            val isImage = mimeType.startsWith("image/", ignoreCase = true)

            if (isPdf) {
                extractPdfDocument(context, uri, fileName, mimeType, fileSize)
            } else if (isImage) {
                Result.success(
                    ExtractedDocument(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = mimeType,
                        fileSizeBytes = fileSize,
                        pageCount = 1,
                        extractedText = "[Image Attachment: $fileName ($mimeType, ${formatSize(fileSize)})]\nReady for visual metadata & contextual analysis."
                    )
                )
            } else {
                val sb = StringBuilder()
                resolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        val buffer = CharArray(4096)
                        var read: Int
                        while (reader.read(buffer).also { read = it } != -1) {
                            sb.append(buffer, 0, read)
                            if (sb.length >= MAX_EXTRACTED_CHARS) break
                        }
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for $fileName"))

                val content = sb.toString().trim()
                if (content.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Document '$fileName' is empty or contains no readable text."))
                }

                Result.success(
                    ExtractedDocument(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = mimeType,
                        fileSizeBytes = fileSize,
                        pageCount = (content.length / 2500).coerceAtLeast(1),
                        extractedText = content.take(MAX_EXTRACTED_CHARS)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractPdfDocument(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): Result<ExtractedDocument> {
        val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("Unable to read PDF file descriptor."))

            var pageCount = 1
            var firstPageBitmap: Bitmap? = null

            android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                    if (pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val width = (page.width * 1.2f).toInt().coerceIn(200, 1080)
                            val height = (page.height * 1.2f).toInt().coerceIn(200, 1440)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            firstPageBitmap = bmp
                        }
                    }
                }
            }

            // Extract readable ASCII/UTF-8 text strings embedded in uncompressed PDF streams
            val rawBytes = tempFile.readBytes()
            val rawLatin = String(rawBytes, Charsets.ISO_8859_1)
            val textLiterals = Regex("\\(([^()\\\\]{3,200})\\)")
                .findAll(rawLatin)
                .map { it.groupValues[1] }
                .filter { token -> token.count { it.isLetterOrDigit() || it.isWhitespace() } > (token.length * 0.85) }
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val extractedText = if (textLiterals.length > 60) {
                "PDF Document: $fileName ($pageCount pages, ${formatSize(fileSize)})\n\nExtracted Text Stream:\n${textLiterals.take(MAX_EXTRACTED_CHARS)}"
            } else {
                "PDF Document: $fileName ($pageCount pages, ${formatSize(fileSize)})\n[Note: This PDF uses compressed/scanned streams. Page 1 rendered via Android PdfRenderer. Add specific context or questions below to analyze.]"
            }

            return Result.success(
                ExtractedDocument(
                    uriString = uri.toString(),
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSizeBytes = if (fileSize > 0) fileSize else tempFile.length(),
                    pageCount = pageCount,
                    extractedText = extractedText,
                    previewBitmap = firstPageBitmap
                )
            )
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun guessMimeFromPath(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".csv") -> "text/csv"
            else -> "text/plain"
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
