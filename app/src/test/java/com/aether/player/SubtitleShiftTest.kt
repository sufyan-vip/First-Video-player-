package com.aether.player

import com.aether.player.domain.SubtitleShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleShiftTest {
    @Test
    fun shiftSrtForward() {
        val input = "1\n00:00:01,000 --> 00:00:02,000\nHello\n"
        val out = SubtitleShift.shiftSrt(input, 1000)
        assertTrue(out.contains("00:00:02,000 --> 00:00:03,000"))
        assertTrue(out.contains("Hello"))
    }

    @Test
    fun shiftSrtClampsAtZero() {
        val input = "1\n00:00:01,000 --> 00:00:02,000\nHello\n"
        val out = SubtitleShift.shiftSrt(input, -5000)
        assertTrue(out.contains("00:00:00,000 --> 00:00:00,000"))
    }

    @Test
    fun shiftVttPreservesHeader() {
        val input = "WEBVTT\nKind: captions\n\n00:00:01.000 --> 00:00:02.000\nHi\n"
        val out = SubtitleShift.shiftVtt(input, 2000)
        assertTrue(out.startsWith("WEBVTT"))
        assertTrue(out.contains("00:00:03.000 --> 00:00:04.000"))
    }

    @Test
    fun srtRoundTrip() {
        val input = "1\n00:00:01,000 --> 00:00:02,000\nHello\n\n2\n00:00:03,500 --> 00:00:04,000\nWorld\n"
        val cues = SubtitleShift.parseSrt(input)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(listOf("World"), cues[1].lines)
        val rebuilt = SubtitleShift.buildSrt(cues)
        assertEquals(2, SubtitleShift.parseSrt(rebuilt).size)
    }

    @Test
    fun vttRoundTrip() {
        val input = "WEBVTT\n\nNOTE a comment\n\n00:01.000 --> 00:02.000\nHi there\n"
        val (header, cues) = SubtitleShift.parseVttTextBlocks(input)
        assertTrue(header.any { it.startsWith("WEBVTT") })
        assertEquals(1, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(listOf("Hi there"), cues[0].lines)
        val rebuilt = SubtitleShift.buildVtt(header, cues)
        assertTrue(rebuilt.contains("00:00:01.000 --> 00:00:02.000"))
    }
}
