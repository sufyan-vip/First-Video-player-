package com.aether.player.domain

sealed interface AiCommand {
    data class Seek(val positionMs: Long) : AiCommand
    data object Play : AiCommand
    data object Pause : AiCommand
    data object Next : AiCommand
    data object Previous : AiCommand
    data class Speed(val value: Float) : AiCommand
    data class Subtitles(val enabled: Boolean) : AiCommand
}

/**
 * Local (offline) parser for natural-language playback commands.
 * The AI provider is only consulted when this returns null.
 */
object AiCommandParser {
    private val hms = Regex("""(?:(\d+)\s*:\s*)?([0-5]?\d)\s*:\s*([0-5]\d)""")
    private val spoken = Regex("""(\d+(?:\.\d+)?)\s*(hours?|hrs?|h|minutes?|mins?|m(?!s)|seconds?|secs?|s)\b""")
    private val speed = Regex("""(\d+(?:\.\d+)?)\s*x\b""")

    fun parseLocal(input: String, durationMs: Long, bookmarks: List<BookmarkMoment>): AiCommand? {
        val text = input.trim().lowercase()
        if (text.isEmpty()) return null

        if (text.contains("subtitle") || text.contains("caption")) {
            if (text.contains("off") || text.contains("disable") || text.contains("hide")) {
                return AiCommand.Subtitles(false)
            }
            if (text.contains("on") || text.contains("enable") || text.contains("show")) {
                return AiCommand.Subtitles(true)
            }
        }

        speed.find(text)?.let { m ->
            val v = m.groupValues[1].toFloatOrNull()?.coerceIn(0.25f, 4f)
            if (v != null && (text.contains("speed") || text.contains("play") || text.contains("x"))) {
                return AiCommand.Speed(v)
            }
        }

        hms.find(text)?.let { m ->
            val h = m.groupValues[1].ifEmpty { "0" }.toLong()
            val minutes = m.groupValues[2].toLong()
            val seconds = m.groupValues[3].toLong()
            val ms = ((h * 3600) + (minutes * 60) + seconds) * 1000L
            return AiCommand.Seek(ms.coerceIn(0L, durationMs.coerceAtLeast(0L)))
        }

        spoken.find(text)?.let { m ->
            val value = m.groupValues[1].toDoubleOrNull() ?: return@let
            val unit = m.groupValues[2]
            val ms = when {
                unit.startsWith("h") -> (value * 3_600_000).toLong()
                unit.startsWith("m") -> (value * 60_000).toLong()
                else -> (value * 1000).toLong()
            }
            if (text.contains("jump") || text.contains("seek") || text.contains("go to") || text.contains("move to") || text.contains("skip to")) {
                return AiCommand.Seek(ms.coerceIn(0L, durationMs.coerceAtLeast(0L)))
            }
        }

        val bookmarkHit = bookmarks.firstOrNull { b ->
            val title = b.title.lowercase()
            title.isNotBlank() && text.contains(title) && !title.startsWith("bookmark ")
        } ?: bookmarks.firstOrNull { b ->
            val custom = b.title.lowercase().removePrefix("bookmark ").trim()
            custom.length >= 4 && text.contains(custom)
        }
        if (bookmarkHit != null && (text.contains("jump") || text.contains("go") || text.contains("seek") || text.contains("bookmark"))) {
            return AiCommand.Seek(bookmarkHit.positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)))
        }

        return when {
            text.contains("pause") || text.contains("stop") || text.contains("hold on") -> AiCommand.Pause
            text.contains("resume") || text.contains("continue") || text == "play" ||
                text.startsWith("play ") || text.contains("start playing") -> AiCommand.Play
            text.contains("next") -> AiCommand.Next
            text.contains("previous") || text.contains("last one") || text == "back" ||
                text.contains("go back") -> AiCommand.Previous
            else -> null
        }
    }
}
