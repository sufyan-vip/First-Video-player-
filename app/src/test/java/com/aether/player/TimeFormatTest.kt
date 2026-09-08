package com.aether.player

import com.aether.player.domain.BookmarkLogic
import com.aether.player.domain.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formats() {
        assertEquals("1:02", TimeFormat.formatMs(62_000L))
        assertEquals("1:01:01", TimeFormat.formatMs(3_661_000L))
        assertEquals("−0:10", TimeFormat.formatRemaining(10_000L, 20_000L))
        assertTrue(TimeFormat.prettyBytes(2048L).contains("KB"))
        assertTrue(TimeFormat.prettyResolution(1920, 1080).contains("1080p"))
        assertEquals("Off", TimeFormat.sleepLabel(0))
        assertEquals("End of video", TimeFormat.sleepLabel(-1))
    }

    @Test
    fun bookmarks() {
        assertEquals("Bookmark 1:00", BookmarkLogic.sanitizedTitle("  ", 60_000L))
        assertTrue(BookmarkLogic.canAdd(1_000L, 10_000L))
        assertFalse(BookmarkLogic.canAdd(20_000L, 10_000L))
    }
}
