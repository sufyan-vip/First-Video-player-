package com.aether.player.domain

object TimeFormat {
    fun formatMs(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun formatRemaining(positionMs: Long, durationMs: Long): String {
        val remaining = WatchProgressLogic.remainingMs(positionMs, durationMs)
        return "−${formatMs(remaining)}"
    }

    fun prettyBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return "%.1f %s".format(value, units[unit])
    }

    fun prettyResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Unknown"
        val label = when {
            height >= 2160 || width >= 3840 -> "4K"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            else -> "${height}p"
        }
        return "${width}×${height} · $label"
    }

    fun sleepLabel(minutes: Int): String = when (minutes) {
        0 -> "Off"
        -1 -> "End of video"
        else -> "$minutes min"
    }
}
