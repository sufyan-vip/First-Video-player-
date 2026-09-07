package com.aether.player.domain

object WatchProgressLogic {
    const val DEFAULT_COMPLETION_THRESHOLD = 0.92f
    const val MIN_RESUME_POSITION_MS = 5_000L
    const val END_PADDING_MS = 3_000L

    fun clampPosition(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return positionMs.coerceAtLeast(0L)
        return positionMs.coerceIn(0L, durationMs)
    }

    fun progressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (clampPosition(positionMs, durationMs).toDouble() / durationMs.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    }

    fun remainingMs(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return (durationMs - clampPosition(positionMs, durationMs)).coerceAtLeast(0L)
    }

    fun isCompleted(
        positionMs: Long,
        durationMs: Long,
        threshold: Float = DEFAULT_COMPLETION_THRESHOLD,
    ): Boolean {
        if (durationMs <= 0L) return false
        val fraction = progressFraction(positionMs, durationMs)
        val nearEnd = remainingMs(positionMs, durationMs) <= END_PADDING_MS
        return fraction >= threshold.coerceIn(0.5f, 0.99f) || nearEnd
    }

    fun shouldOfferResume(
        positionMs: Long,
        durationMs: Long,
        threshold: Float = DEFAULT_COMPLETION_THRESHOLD,
    ): Boolean {
        val pos = clampPosition(positionMs, durationMs)
        if (pos < MIN_RESUME_POSITION_MS) return false
        return !isCompleted(pos, durationMs, threshold)
    }

    fun resumePositionOrStart(
        positionMs: Long,
        durationMs: Long,
        startOver: Boolean,
        threshold: Float = DEFAULT_COMPLETION_THRESHOLD,
    ): Long {
        if (startOver) return 0L
        return if (shouldOfferResume(positionMs, durationMs, threshold)) {
            clampPosition(positionMs, durationMs)
        } else {
            0L
        }
    }
}
