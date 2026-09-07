package com.aether.player

import com.aether.player.domain.PlaylistLogic
import com.aether.player.domain.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistLogicTest {
    @Test
    fun nextAndPrevious() {
        assertEquals(1, PlaylistLogic.nextIndex(3, 0, shuffle = false, RepeatMode.OFF))
        assertNull(PlaylistLogic.nextIndex(3, 2, shuffle = false, RepeatMode.OFF))
        assertEquals(0, PlaylistLogic.nextIndex(3, 2, shuffle = false, RepeatMode.ALL))
        assertEquals(2, PlaylistLogic.nextIndex(3, 2, shuffle = false, RepeatMode.ONE, userNext = false))
        assertEquals(1, PlaylistLogic.previousIndex(3, 2, RepeatMode.OFF))
        assertEquals(2, PlaylistLogic.previousIndex(3, 0, RepeatMode.ALL))
    }

    @Test
    fun mutateQueue() {
        val moved = PlaylistLogic.move(listOf("a", "b", "c"), 0, 2)
        assertEquals(listOf("b", "c", "a"), moved)
        val (next, idx) = PlaylistLogic.playNext(listOf("a", "b"), 0, "x")
        assertEquals(listOf("a", "x", "b"), next)
        assertEquals(0, idx)
        val (removed, newIdx) = PlaylistLogic.removeAt(listOf("a", "b", "c"), 0, 1)
        assertEquals(listOf("b", "c"), removed)
        assertEquals(0, newIdx)
    }
}
