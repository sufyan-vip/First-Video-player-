package com.aether.player.domain

data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val lines: List<String>,
    val settings: String = "",
)

/**
 * Pure-Kotlin SRT / WebVTT shifting + parsing.
 * Used for subtitle-delay (external files) and the AI subtitle-translation workflow.
 */
object SubtitleShift {
    private val srtTiming = Regex("""(\d+):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d+):(\d{2}):(\d{2})[,.](\d{1,3})""")
    private val vttTiming = Regex("""(?:(\d+):)?(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(?:(\d+):)?(\d{2}):(\d{2})\.(\d{3})(.*)""")

    fun shiftSrt(content: String, delayMs: Int): String {
        if (delayMs == 0) return content
        return srtTiming.replace(content) { m ->
            val start = toMs(m.groupValues[1].toLong(), m.groupValues[2].toLong(), m.groupValues[3].toLong(), m.groupValues[4]) + delayMs
            val end = toMs(m.groupValues[5].toLong(), m.groupValues[6].toLong(), m.groupValues[7].toLong(), m.groupValues[8]) + delayMs
            "${formatSrt(start)} --> ${formatSrt(end)}"
        }
    }

    fun shiftVtt(content: String, delayMs: Int): String {
        if (delayMs == 0) return content
        val lines = content.lines()
        val headerEnd = lines.indexOfFirst { it.isBlank() }.takeIf { it >= 0 } ?: 0
        val header = lines.take(if (headerEnd == 0) 1 else headerEnd + 1)
        val body = lines.drop(header.size).joinToString("\n")
        val shifted = vttTiming.replace(body) { m ->
            val start = vttToMs(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4]) + delayMs
            val end = vttToMs(m.groupValues[5], m.groupValues[6], m.groupValues[7], m.groupValues[8]) + delayMs
            "${formatVtt(start)} --> ${formatVtt(end)}${m.groupValues[9]}"
        }
        return (header + shifted).joinToString("\n")
    }

    fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
        for (block in blocks) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.size < 2) continue
            val timingLine = lines.firstOrNull { srtTiming.containsMatchIn(it) } ?: continue
            val m = srtTiming.find(timingLine) ?: continue
            val idx = lines.first().trim().toIntOrNull() ?: (cues.size + 1)
            val textLines = lines.drop(lines.indexOf(timingLine) + 1).filter { it.isNotBlank() }
            if (textLines.isEmpty()) continue
            cues += SubtitleCue(
                index = idx,
                startMs = toMs(m.groupValues[1].toLong(), m.groupValues[2].toLong(), m.groupValues[3].toLong(), m.groupValues[4]),
                endMs = toMs(m.groupValues[5].toLong(), m.groupValues[6].toLong(), m.groupValues[7].toLong(), m.groupValues[8]),
                lines = textLines,
            )
        }
        return cues
    }

    fun buildSrt(cues: List<SubtitleCue>): String = buildString {
        cues.forEachIndexed { i, cue ->
            append(i + 1).append('\n')
            append(formatSrt(cue.startMs)).append(" --> ").append(formatSrt(cue.endMs)).append('\n')
            cue.lines.forEach { append(it).append('\n') }
            append('\n')
        }
    }

    fun parseVttTextBlocks(content: String): Pair<List<String>, List<SubtitleCue>> {
        val normalized = content.replace("\r\n", "\n")
        val lines = normalized.lines()
        val headerEnd = lines.indexOfFirst { it.isBlank() }.takeIf { it >= 0 } ?: minOf(1, lines.size)
        val header = lines.take(headerEnd)
        val cues = mutableListOf<SubtitleCue>()
        val body = lines.drop(headerEnd).joinToString("\n")
        val blocks = body.split(Regex("\n\\s*\n"))
        for (block in blocks) {
            val blines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (blines.isEmpty()) continue
            if (blines.first().startsWith("NOTE") || blines.first().startsWith("STYLE") || blines.first().startsWith("REGION")) continue
            val timingIdx = blines.indexOfFirst { vttTiming.containsMatchIn(it) }
            if (timingIdx < 0) continue
            val m = vttTiming.find(blines[timingIdx]) ?: continue
            val textLines = blines.drop(timingIdx + 1).filter { it.isNotBlank() }
            if (textLines.isEmpty()) continue
            cues += SubtitleCue(
                index = cues.size + 1,
                startMs = vttToMs(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4]),
                endMs = vttToMs(m.groupValues[5], m.groupValues[6], m.groupValues[7], m.groupValues[8]),
                lines = textLines,
                settings = m.groupValues[9].trim(),
            )
        }
        return header to cues
    }

    fun buildVtt(header: List<String>, cues: List<SubtitleCue>): String = buildString {
        val head = if (header.any { it.trim().startsWith("WEBVTT") }) header else listOf("WEBVTT")
        head.forEach { append(it).append('\n') }
        append('\n')
        cues.forEach { cue ->
            append(formatVtt(cue.startMs)).append(" --> ").append(formatVtt(cue.endMs))
            if (cue.settings.isNotBlank()) append(' ').append(cue.settings)
            append('\n')
            cue.lines.forEach { append(it).append('\n') }
            append('\n')
        }
    }

    private fun toMs(h: Long, m: Long, s: Long, ms: String): Long {
        val millis = ms.padEnd(3, '0').take(3).toLong()
        return ((h * 3600 + m * 60 + s) * 1000) + millis
    }

    private fun vttToMs(h: String, m: String, s: String, ms: String): Long {
        val hours = if (h.isEmpty()) 0L else h.toLong()
        return ((hours * 3600 + m.toLong() * 60 + s.toLong()) * 1000) + ms.toLong()
    }

    private fun formatSrt(msRaw: Long): String {
        val ms = msRaw.coerceAtLeast(0L)
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val milli = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, milli)
    }

    private fun formatVtt(msRaw: Long): String {
        val ms = msRaw.coerceAtLeast(0L)
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val milli = ms % 1000
        return "%02d:%02d:%02d.%03d".format(h, m, s, milli)
    }
}
