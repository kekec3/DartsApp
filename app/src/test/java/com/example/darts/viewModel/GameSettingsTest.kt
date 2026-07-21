package com.example.darts.viewModel

import org.junit.Assert.*
import org.junit.Test

class GameSettingsTest {

    @Test
    fun `default settings use expected defaults`() {
        val settings = GameSettings()

        assertEquals("x01", settings.type)
        assertEquals("501", settings.startingScore)
        assertEquals(3, settings.legs)
        assertFalse(settings.doubleOut)
        assertFalse(settings.masterIn)
        assertFalse(settings.cutThroat)
        assertTrue(settings.showSuggestions)
        assertFalse(settings.trackLocation)
        assertEquals(-1, settings.startingPlayerId)
    }

    @Test
    fun `copy with modified values works`() {
        val settings = GameSettings(
            type = "cricket",
            startingScore = "301",
            legs = 5,
            doubleOut = true,
            masterIn = true,
            cutThroat = true,
            showSuggestions = false,
            trackLocation = true,
            startingPlayerId = 42
        )

        assertEquals("cricket", settings.type)
        assertEquals("301", settings.startingScore)
        assertEquals(5, settings.legs)
        assertTrue(settings.doubleOut)
        assertTrue(settings.masterIn)
        assertTrue(settings.cutThroat)
        assertFalse(settings.showSuggestions)
        assertTrue(settings.trackLocation)
        assertEquals(42, settings.startingPlayerId)
    }

    @Test
    fun `copy preserves unmodified values`() {
        val original = GameSettings(type = "x01", legs = 7)
        val modified = original.copy(legs = 5)

        assertEquals("x01", modified.type) // preserved
        assertEquals(5, modified.legs) // changed
    }

    @Test
    fun `startingPlayerId negative one indicates random`() {
        val settings = GameSettings(startingPlayerId = -1)
        assertEquals(-1, settings.startingPlayerId)
    }

    @Test
    fun `startingPlayerId negative two indicates default order`() {
        val settings = GameSettings(startingPlayerId = -2)
        assertEquals(-2, settings.startingPlayerId)
    }
}
