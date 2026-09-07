package com.aether.player.domain

object GestureMath {
    const val DEFAULT_SEEK_SENSITIVITY = 1f
    const val DEFAULT_BRIGHTNESS_SENSITIVITY = 1f
    const val DEFAULT_VOLUME_SENSITIVITY = 1f

    fun isLeftSide(x: Float, width: Float): Boolean {
        if (width <= 0f) return true
        return x < width / 2f
    }

    fun seekDeltaMs(
        dragPx: Float,
        widthPx: Float,
        durationMs: Long,
        sensitivity: Float = DEFAULT_SEEK_SENSITIVITY,
    ): Long {
        if (widthPx <= 0f || durationMs <= 0L) return 0L
        val fraction = (dragPx / widthPx) * sensitivity.coerceIn(0.25f, 3f)
        val window = durationMs.coerceAtMost(10 * 60 * 1000L).toFloat()
        return (fraction * window).toLong()
    }

    fun applySeek(currentMs: Long, deltaMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return (currentMs + deltaMs).coerceAtLeast(0L)
        return (currentMs + deltaMs).coerceIn(0L, durationMs)
    }

    fun brightnessDelta(
        dragPx: Float,
        heightPx: Float,
        sensitivity: Float = DEFAULT_BRIGHTNESS_SENSITIVITY,
    ): Float {
        if (heightPx <= 0f) return 0f
        return (-dragPx / heightPx) * sensitivity.coerceIn(0.25f, 3f)
    }

    fun volumeDelta(
        dragPx: Float,
        heightPx: Float,
        maxVolume: Int,
        sensitivity: Float = DEFAULT_VOLUME_SENSITIVITY,
    ): Int {
        if (heightPx <= 0f || maxVolume <= 0) return 0
        val fraction = (-dragPx / heightPx) * sensitivity.coerceIn(0.25f, 3f)
        return (fraction * maxVolume).toInt()
    }

    fun clampUnit(value: Float): Float = value.coerceIn(0f, 1f)

    fun doubleTapSeekMs(intervalSeconds: Int): Long =
        intervalSeconds.coerceIn(5, 60) * 1000L

    fun zoomFactor(previous: Float, scaleGesture: Float): Float =
        (previous * scaleGesture).coerceIn(1f, 8f)

    fun speedBoost(base: Float, boost: Float): Float =
        (if (boost > 0f) boost else 2f).coerceIn(1.25f, 4f).let { target ->
            maxOf(base, target)
        }
}
