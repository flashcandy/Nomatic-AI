package com.example.core.utilities

enum class DiffType {
    ADDED,
    REMOVED,
    UNCHANGED
}

data class DiffLine(
    val type: DiffType,
    val lineAIndex: Int?,
    val lineBIndex: Int?,
    val text: String
)

data class CodeDiffResult(
    val lines: List<DiffLine>,
    val addedCount: Int,
    val removedCount: Int,
    val unchangedCount: Int
)

object CodeDiffUtility {

    fun computeDiff(textA: String, textB: String): CodeDiffResult {
        val linesA = textA.lines()
        val linesB = textB.lines()

        val dp = Array(linesA.size + 1) { IntArray(linesB.size + 1) }
        for (i in linesA.indices) {
            for (j in linesB.indices) {
                if (linesA[i] == linesB[j]) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = linesA.size
        var j = linesB.size

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && linesA[i - 1] == linesB[j - 1]) {
                result.add(
                    DiffLine(
                        type = DiffType.UNCHANGED,
                        lineAIndex = i,
                        lineBIndex = j,
                        text = linesA[i - 1]
                    )
                )
                i--
                j--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffType.ADDED,
                        lineAIndex = null,
                        lineBIndex = j,
                        text = linesB[j - 1]
                    )
                )
                j--
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffType.REMOVED,
                        lineAIndex = i,
                        lineBIndex = null,
                        text = linesA[i - 1]
                    )
                )
                i--
            }
        }

        result.reverse()

        val added = result.count { it.type == DiffType.ADDED }
        val removed = result.count { it.type == DiffType.REMOVED }
        val unchanged = result.count { it.type == DiffType.UNCHANGED }

        return CodeDiffResult(
            lines = result,
            addedCount = added,
            removedCount = removed,
            unchangedCount = unchanged
        )
    }
}
