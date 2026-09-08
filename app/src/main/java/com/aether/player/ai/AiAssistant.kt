package com.aether.player.ai

import com.aether.player.data.db.VideoEntity
import com.aether.player.domain.AiCommand
import com.aether.player.domain.BookmarkMoment
import com.aether.player.domain.TimeFormat
import com.aether.player.playback.ChapterItem
import org.json.JSONObject

/**
 * Grounded assistant workflows. Every prompt carries an explicit honesty rule:
 * the model must only use the provided context and must never claim to have
 * watched the video.
 */
class AiAssistant(private val client: AiClient) {

    fun buildContext(
        video: VideoEntity,
        bookmarks: List<BookmarkMoment>,
        chapters: List<ChapterItem>,
        positionMs: Long,
        durationMs: Long,
    ): String = buildString {
        appendLine("Video: ${video.title}")
        appendLine("Duration: ${TimeFormat.formatMs(durationMs)}")
        appendLine("Current position: ${TimeFormat.formatMs(positionMs)}")
        if (video.width > 0) appendLine("Resolution: ${video.width}x${video.height}")
        if (!video.folderName.isNullOrBlank()) appendLine("Folder: ${video.folderName}")
        appendLine("Size: ${TimeFormat.prettyBytes(video.sizeBytes)}")
        if (bookmarks.isNotEmpty()) {
            appendLine("Bookmarks:")
            bookmarks.sortedBy { it.positionMs }.forEach {
                appendLine("- ${TimeFormat.formatMs(it.positionMs)} — ${it.title}")
            }
        } else {
            appendLine("Bookmarks: none")
        }
        if (chapters.isNotEmpty()) {
            appendLine("Chapters:")
            chapters.forEachIndexed { i, c ->
                appendLine("- ${i + 1}. ${c.title} (${TimeFormat.formatMs(c.durationMs)})")
            }
        } else {
            appendLine("Chapters: none detected")
        }
    }

    suspend fun explainVideo(context: String): String =
        client.generate(
            system = HONESTY,
            prompt = "Explain what can be known about this video from its metadata and markers only.\n\n$context",
        )

    suspend fun summarize(context: String): String =
        client.generate(
            system = HONESTY,
            prompt = "Write a short structured summary of this video's markers (bookmarks/chapters/position). " +
                "If there are no markers, say so and suggest adding bookmarks.\n\n$context",
        )

    suspend fun suggestChapters(context: String): String =
        client.generate(
            system = HONESTY,
            prompt = "Propose chapter titles with timestamps derived ONLY from the bookmarks below. " +
                "Reply as plain lines: \"MM:SS — Title\". If there are no bookmarks, reply with exactly: NO_BOOKMARKS.\n\n$context",
        )

    suspend fun ask(question: String, context: String): String =
        client.generate(
            system = HONESTY,
            prompt = "Context about the current video:\n$context\n\nQuestion: $question",
        )

    suspend fun parseCommand(input: String, context: String, durationMs: Long): AiCommand? {
        val raw = client.generate(
            system = "You translate playback commands into JSON. Reply with ONLY one JSON object, no other text. " +
                "Schema: {\"action\":\"seek|play|pause|next|previous|speed\",\"positionMs\":number,\"speed\":number}. " +
                "Use positionMs only for seek (milliseconds, 0..$durationMs). Use speed only for speed (0.25..4). " +
                "If the request is not a playback command, reply {\"action\":\"none\"}.",
            prompt = "Command: $input\n\n$context",
            temperature = 0.0,
        )
        return runCatching {
            val json = JSONObject(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
            when (json.optString("action")) {
                "seek" -> AiCommand.Seek(json.optLong("positionMs", -1).takeIf { it >= 0 } ?: return null)
                "play" -> AiCommand.Play
                "pause" -> AiCommand.Pause
                "next" -> AiCommand.Next
                "previous" -> AiCommand.Previous
                "speed" -> AiCommand.Speed(json.optDouble("speed", -1.0).toFloat().takeIf { it > 0 } ?: return null)
                else -> null
            }
        }.getOrNull()
    }

    /**
     * Translates subtitle cue texts. Numbered protocol keeps ordering robust;
     * any line that fails to round-trip keeps its original text.
     */
    suspend fun translateTexts(texts: List<String>, targetLang: String): List<String> {
        if (texts.isEmpty()) return emptyList()
        val numbered = texts.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n")
        val raw = client.generate(
            system = "You are a subtitle translator. Reply with ONLY the translated numbered lines, " +
                "same count, same numbering, no explanations.",
            prompt = "Translate each numbered subtitle line to $targetLang. Keep line breaks inside a line " +
                "by using <br>. Lines:\n$numbered",
            temperature = 0.0,
        )
        val parsed = mutableMapOf<Int, String>()
        raw.lines().forEach { line ->
            val m = Regex("""^(\d+)\.\s?(.*)$""").find(line.trim())
            if (m != null) {
                parsed[m.groupValues[1].toIntOrNull() ?: -1] = m.groupValues[2].replace("<br>", "\n")
            }
        }
        return texts.mapIndexed { i, original -> parsed[i + 1]?.takeIf { it.isNotBlank() } ?: original }
    }

    companion object {
        const val HONESTY =
            "You are the Aether on-device-video assistant. Answer ONLY from the context provided. " +
                "You cannot watch videos. Never claim to have watched, seen, or heard the content. " +
                "If the context lacks the answer, say what is missing instead of guessing."
    }
}
