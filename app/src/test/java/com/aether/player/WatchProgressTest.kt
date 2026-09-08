package com.aether.player

import com.aether.player.domain.WatchProgressLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchProgressTest {
    @Test
    fun fractionAndRemaining() {
        assertEquals(0.5f, WatchProgressLogic.progressFraction(50_000L, 100_000L), 0.001f)
        assertEquals(40_000L, WatchProgressLogic.remainingMs(60_000L, 100_000L))
        assertEquals(0L, WatchProgressLogic.remainingMs(200_000L, 100_000L))
    }

    @Test
    fun completionThreshold() {
        assertFalse(WatchProgressLogic.isCompleted(10_000L, 100_000L, 0.92f))
        assertTrue(WatchProgressLogic.isCompleted(93_000L, 100_000L, 0.92f))
        assertTrue(WatchProgressLogic.isCompleted(98_000L, 100_000L, 0.99f))
    }

    @Test
    fun resumeOffer() {
        assertFalse(WatchProgressLogic.shouldOfferResume(1_000L, 100_000L))
        assertTrue(WatchProgressLogic.shouldOfferResume(20_000L, 100_000L))
        assertFalse(WatchProgressLogic.shouldOfferResume(99_000L, 100_000L))
        assertEquals(0L, WatchProgressLogic.resumePositionOrStart(20_000L, 100_000L, startOver = true))
        assertEquals(20_000L, WatchProgressLogic.resumePositionOrStart(20_000L, 100_000L, startOver = false))
    }
}
