package com.aether.player.domain

import java.net.URI
import java.util.Locale

object UrlValidator {
    private val playableSchemes = setOf("http", "https", "rtsp", "rtmp", "udp", "file", "content")
    private val playableExtensions = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts",
        "flv", "wmv", "mpeg", "mpg", "m3u8", "mpd", "mp3", "aac", "flac",
        "ogg", "wav", "m4a", "opus",
    )

    fun normalize(input: String): String = input.trim()

    fun isPlayableUrl(input: String): Boolean {
        val raw = normalize(input)
        if (raw.isEmpty()) return false
        val candidate = if (raw.contains("://")) raw else "https://$raw"
        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
            if (scheme !in playableSchemes) return false
            if (scheme == "http" || scheme == "https") {
                val host = uri.host ?: return false
                if (host.isBlank()) return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun looksLikeStream(input: String): Boolean {
        val lower = normalize(input).lowercase(Locale.US)
        return lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains("rtsp://")
    }

    fun extensionOf(input: String): String? {
        val path = normalize(input).substringBefore("?").substringAfterLast('/')
        val ext = path.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)
        return ext.takeIf { it in playableExtensions }
    }
}
