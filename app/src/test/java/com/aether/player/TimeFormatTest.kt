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
        assertEquals("1:02", TimeFormat.formatMs(62_000))
        assertEquals("1:01:01", TimeFormat.formatMs(3_661_000))
        assertEquals("−0:10", TimeFormat.formatRemaining(10_000, 20_000))
        assertTrue(TimeFormat.prettyBytes(2048).contains("KB"))
        assertTrue(TimeFormat.prettyResolution(1920, 1080).contains("1080p"))
        assertEquals("Off", TimeFormat.sleepLabel(0))
        assertEquals("End of video", TimeFormat.sleepLabel(-1))
    }

    @Test
    fun bookmarks() {
        assertEquals("Bookmark 1:00", BookmarkLogic.sanitizedTitle("  ", 60_000))
        assertTrue(BookmarkLogic.canAdd(1_000, 10_000))
        assertFalse(BookmarkLogic.canAdd(20_000, 10_000))
    }
}
