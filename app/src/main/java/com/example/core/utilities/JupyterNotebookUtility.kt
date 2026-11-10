package com.example.core.utilities

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class JupyterCellType {
    CODE,
    MARKDOWN
}

data class JupyterCell(
    val id: String = UUID.randomUUID().toString(),
    var cellType: JupyterCellType,
    var sourceText: String,
    var executionCount: Int? = null,
    var outputLog: String? = null,
    var executionTimeMs: Long? = null,
    var isExecuting: Boolean = false,
    var hasExecuted: Boolean = false
)

object JupyterNotebookUtility {

    /**
     * Parses raw markdown/text content from Knowledge Vault into Jupyter Notebook Cells.
     * Separates markdown prose from ```code blocks``` into distinct cells.
     */
    fun parseContentToNotebook(content: String): List<JupyterCell> {
        val cells = mutableListOf<JupyterCell>()
        if (content.isBlank()) {
            cells.add(
                JupyterCell(
                    cellType = JupyterCellType.MARKDOWN,
                    sourceText = "# New Jupyter Notebook\nType markdown documentation or code below."
                )
            )
            cells.add(
                JupyterCell(
                    cellType = JupyterCellType.CODE,
                    sourceText = "# Python 3 / Bash Execution Cell\nprint('Hello Nomatic Jupyter Kernel')"
                )
            )
            return cells
        }

        val lines = content.lines()
        val currentChunk = StringBuilder()
        var inCodeBlock = false
        var codeBlockLang = "python"
        var execCounter = 1

        for (line in lines) {
            if (line.trimStart().startsWith("```")) {
                if (!inCodeBlock) {
                    // Flush accumulated markdown before entering code block
                    if (currentChunk.isNotBlank()) {
                        cells.add(
                            JupyterCell(
                                cellType = JupyterCellType.MARKDOWN,
                                sourceText = currentChunk.toString().trim()
                            )
                        )
                        currentChunk.clear()
                    }
                    inCodeBlock = true
                    codeBlockLang = line.trimStart().removePrefix("```").trim().ifBlank { "python" }
                } else {
                    // Exiting code block
                    if (currentChunk.isNotBlank()) {
                        cells.add(
                            JupyterCell(
                                cellType = JupyterCellType.CODE,
                                sourceText = currentChunk.toString().trim(),
                                executionCount = execCounter++,
                                outputLog = "Out [$execCounter]: Execution ready on Nomatic Kernel."
                            )
                        )
                        currentChunk.clear()
                    }
                    inCodeBlock = false
                }
            } else {
                currentChunk.append(line).append("\n")
            }
        }

        // Flush remaining chunk
        if (currentChunk.isNotBlank()) {
            cells.add(
                JupyterCell(
                    cellType = if (inCodeBlock) JupyterCellType.CODE else JupyterCellType.MARKDOWN,
                    sourceText = currentChunk.toString().trim(),
                    executionCount = if (inCodeBlock) execCounter else null
                )
            )
        }

        return if (cells.isEmpty()) {
            listOf(
                JupyterCell(
                    cellType = JupyterCellType.MARKDOWN,
                    sourceText = content
                )
            )
        } else {
            cells
        }
    }

    /**
     * Re-assembles Jupyter Notebook cells back into clean multi-line markdown content
     * for saving into Room Database NoteEntity.
     */
    fun assembleNotebookToContent(cells: List<JupyterCell>): String {
        val sb = StringBuilder()
        cells.forEachIndexed { index, cell ->
            if (cell.cellType == JupyterCellType.MARKDOWN) {
                sb.append(cell.sourceText.trim()).append("\n\n")
            } else {
                sb.append("```python\n")
                    .append(cell.sourceText.trim())
                    .append("\n```\n\n")
            }
        }
        return sb.toString().trim()
    }

    /**
     * Serializes Jupyter Cells into official v4 .ipynb JSON format
     * for export to VS Code / Jupyter Lab / Google Colab.
     */
    fun exportToIpynbJson(title: String, cells: List<JupyterCell>): String {
        val root = JSONObject()
        root.put("nbformat", 4)
        root.put("nbformat_minor", 2)

        val metadata = JSONObject()
        val langInfo = JSONObject()
        langInfo.put("name", "python")
        langInfo.put("version", "3.10")
        metadata.put("language_info", langInfo)

        val kernelSpec = JSONObject()
        kernelSpec.put("name", "python3")
        kernelSpec.put("display_name", "Python 3 (Nomatic Kernel)")
        metadata.put("kernelspec", kernelSpec)

        root.put("metadata", metadata)

        val jsonCells = JSONArray()
        cells.forEach { cell ->
            val jsonCell = JSONObject()
            if (cell.cellType == JupyterCellType.MARKDOWN) {
                jsonCell.put("cell_type", "markdown")
                jsonCell.put("metadata", JSONObject())
                val sourceArray = JSONArray()
                cell.sourceText.lines().forEach { sourceArray.put(it + "\n") }
                jsonCell.put("source", sourceArray)
            } else {
                jsonCell.put("cell_type", "code")
                jsonCell.put("execution_count", cell.executionCount ?: 1)
                jsonCell.put("metadata", JSONObject())

                val outputsArray = JSONArray()
                if (!cell.outputLog.isNullOrBlank()) {
                    val streamOutput = JSONObject()
                    streamOutput.put("output_type", "stream")
                    streamOutput.put("name", "stdout")
                    val textArray = JSONArray()
                    cell.outputLog!!.lines().forEach { textArray.put(it + "\n") }
                    streamOutput.put("text", textArray)
                    outputsArray.put(streamOutput)
                }
                jsonCell.put("outputs", outputsArray)

                val sourceArray = JSONArray()
                cell.sourceText.lines().forEach { sourceArray.put(it + "\n") }
                jsonCell.put("source", sourceArray)
            }
            jsonCells.put(jsonCell)
        }

        root.put("cells", jsonCells)
        return root.toString(2)
    }
}
