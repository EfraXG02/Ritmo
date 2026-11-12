package com.example

import com.example.model.ArtistShuffleVibe
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.ui.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFormatTime() {
        assertEquals("0:00", FormatUtils.formatTime(0L))
        assertEquals("1:05", FormatUtils.formatTime(65000L))
        assertEquals("3:30", FormatUtils.formatTime(210000L))
        assertEquals("10:00", FormatUtils.formatTime(600000L))
    }

    @Test
    fun testTrackFavoriteToggle() {
        val track = Track("test_1", "Yellow", "Coldplay", isFavorite = false)
        val fav = track.copy(isFavorite = true)
        assertTrue(fav.isFavorite)
        assertFalse(track.isFavorite)
    }

    @Test
    fun testArtistShuffleVibes() {
        assertEquals("Todas as Músicas", ArtistShuffleVibe.ALL.label)
        assertEquals("Maiores Sucessos", ArtistShuffleVibe.TOP_HITS.label)
        assertEquals("Acústico & Calmo", ArtistShuffleVibe.ACOUSTIC.label)
    }

    @Test
    fun testRepeatModes() {
        assertEquals(3, RepeatMode.values().size)
    }
}
