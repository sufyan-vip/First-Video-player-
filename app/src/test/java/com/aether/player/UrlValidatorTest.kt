package com.aether.player

import com.aether.player.domain.UrlValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun acceptsHttpsMedia() {
        assertTrue(UrlValidator.isPlayableUrl("https://example.com/movie.mp4"))
        assertTrue(UrlValidator.isPlayableUrl("http://cdn.example.org/live.m3u8"))
        assertTrue(UrlValidator.isPlayableUrl("example.com/file.mkv"))
    }

    @Test
    fun rejectsGarbage() {
        assertFalse(UrlValidator.isPlayableUrl(""))
        assertFalse(UrlValidator.isPlayableUrl("not a url"))
        assertFalse(UrlValidator.isPlayableUrl("ftp://files.example.com/a.mp4"))
        assertFalse(UrlValidator.isPlayableUrl("javascript:alert(1)"))
    }

    @Test
    fun streamDetection() {
        assertTrue(UrlValidator.looksLikeStream("https://x/master.m3u8"))
        assertTrue(UrlValidator.looksLikeStream("https://x/manifest.mpd"))
        assertFalse(UrlValidator.looksLikeStream("https://x/file.mp4"))
    }
}
