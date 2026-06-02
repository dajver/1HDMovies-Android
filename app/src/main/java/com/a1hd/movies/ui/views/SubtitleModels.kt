package com.a1hd.movies.ui.views

import java.io.Serializable

data class SubtitleTrack(
    val label: String,
    val url: String,
    val language: String
) : Serializable

data class SubtitleCue(
    val startTime: Double,
    val endTime: Double,
    val text: String
)

object VTTParser {

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.split("\n\n")

        for (block in blocks) {
            val lines = block.trim().split("\n")

            for ((i, line) in lines.withIndex()) {
                if (!line.contains("-->")) continue

                val parts = line.split("-->")
                if (parts.size != 2) continue

                val startTime = parseTimestamp(parts[0].trim())
                val endTimePart = parts[1].trim().split(" ").firstOrNull() ?: ""
                val endTime = parseTimestamp(endTimePart)

                if (startTime == null || endTime == null) continue

                val textLines = lines.drop(i + 1)
                val text = textLines.joinToString("\n")
                    .replace(Regex("<[^>]+>"), "")
                    .trim()

                if (text.isNotEmpty()) {
                    cues.add(SubtitleCue(startTime, endTime, text))
                }
                break
            }
        }
        return cues.sortedBy { it.startTime }
    }

    private fun parseTimestamp(str: String): Double? {
        val cleaned = str.trim()
        val parts = cleaned.split(":")
        if (parts.size < 2) return null

        return if (parts.size == 3) {
            val h = parts[0].toDoubleOrNull() ?: return null
            val m = parts[1].toDoubleOrNull() ?: return null
            val s = parts[2].replace(",", ".").toDoubleOrNull() ?: return null
            h * 3600 + m * 60 + s
        } else {
            val m = parts[0].toDoubleOrNull() ?: return null
            val s = parts[1].replace(",", ".").toDoubleOrNull() ?: return null
            m * 60 + s
        }
    }
}
