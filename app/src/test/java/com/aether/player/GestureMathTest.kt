package com.aether.player

import com.aether.player.domain.GestureMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMathTest {
    @Test
    fun sides() {
        assertTrue(GestureMath.isLeftSide(10f, 100f))
        assertFalse(GestureMath.isLeftSide(80f, 100f))
    }

    @Test
    fun seekAndClamp() {
        val delta = GestureMath.seekDeltaMs(100f, 1000f, 600_000L, 1f)
        assertTrue(delta > 0)
        assertEquals(50_000L, GestureMath.applySeek(40_000, 10_000, 100_000))
        assertEquals(0L, GestureMath.applySeek(1_000, -50_000, 100_000))
        assertEquals(100_000L, GestureMath.applySeek(90_000, 50_000, 100_000))
    }

    @Test
    fun brightnessVolumeZoom() {
        val b = GestureMath.brightnessDelta(-100f, 1000f, 1f)
        assertTrue(b > 0)
        assertEquals(1f, GestureMath.clampUnit(4f), 0f)
        assertEquals(2f, GestureMath.zoomFactor(1f, 2f), 0.01f)
        assertEquals(8f, GestureMath.zoomFactor(8f, 2f), 0.01f)
        assertEquals(10_000L, GestureMath.doubleTapSeekMs(10))
        assertEquals(2f, GestureMath.speedBoost(1f, 2f), 0f)
    }
}
