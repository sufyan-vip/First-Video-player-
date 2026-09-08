package com.aether.player

import com.aether.player.domain.AiCommand
import com.aether.player.domain.AiCommandParser
import com.aether.player.domain.BookmarkMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiCommandParserTest {
    private val marks = listOf(
        BookmarkMoment(1L, "v", 90_000L, "Opening Credits"),
        BookmarkMoment(2L, "v", 120_000L, "Bookmark 2:00"),
    )

    @Test
    fun timestampSeek() {
        assertEquals(AiCommand.Seek(83_000L), AiCommandParser.parseLocal("jump to 1:23", 600_000L, marks))
        assertEquals(AiCommand.Seek(3_661_000L), AiCommandParser.parseLocal("go to 1:01:01", 9_999_999L, marks))
    }

    @Test
    fun seekClampsToDuration() {
        assertEquals(AiCommand.Seek(100_000L), AiCommandParser.parseLocal("jump to 99:00", 100_000L, marks))
    }

    @Test
    fun bookmarkSeek() {
        assertEquals(AiCommand.Seek(90_000L), AiCommandParser.parseLocal("go to opening credits", 600_000L, marks))
    }

    @Test
    fun transportAndSpeed() {
        assertEquals(AiCommand.Pause, AiCommandParser.parseLocal("pause", 100L, marks))
        assertEquals(AiCommand.Play, AiCommandParser.parseLocal("play", 100L, marks))
        assertEquals(AiCommand.Next, AiCommandParser.parseLocal("next video", 100L, marks))
        assertEquals(AiCommand.Previous, AiCommandParser.parseLocal("go back", 100L, marks))
        assertEquals(AiCommand.Speed(1.5f), AiCommandParser.parseLocal("1.5x speed", 100L, marks))
        assertEquals(AiCommand.Subtitles(false), AiCommandParser.parseLocal("subtitles off", 100L, marks))
    }

    @Test
    fun unknownIsNull() {
        assertNull(AiCommandParser.parseLocal("hello world", 100L, marks))
        assertNull(AiCommandParser.parseLocal("", 100L, marks))
    }
}
