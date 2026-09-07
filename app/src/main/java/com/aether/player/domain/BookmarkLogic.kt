package com.aether.player.domain

data class BookmarkMoment(
    val id: Long = 0,
    val videoId: String,
    val positionMs: Long,
    val title: String,
)

object BookmarkLogic {
    fun sanitizedTitle(raw: String, positionMs: Long): String {
        val trimmed = raw.trim()
        return trimmed.ifEmpty { "Bookmark ${TimeFormat.formatMs(positionMs)}" }
    }

    fun canAdd(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs < 0) return false
        if (durationMs > 0 && positionMs > durationMs) return false
        return true
    }

    fun nearest(bookmarks: List<BookmarkMoment>, positionMs: Long, windowMs: Long = 2_000L): BookmarkMoment? {
        return bookmarks.minByOrNull { kotlin.math.abs(it.positionMs - positionMs) }
            ?.takeIf { kotlin.math.abs(it.positionMs - positionMs) <= windowMs }
    }
}
