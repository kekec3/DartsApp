package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player
import org.junit.Assert.*
import org.junit.Test

class GameStateTest {

    private val player1 = Player(1, "Alice")
    private val player2 = Player(2, "Bob")

    @Test
    fun `default currPlayer is 0`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertEquals(0, state.currPlayer)
    }

    @Test
    fun `default leg is 1`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertEquals(1, state.leg)
    }

    @Test
    fun `default maxLegs is 3`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertEquals(3, state.maxLegs)
    }

    @Test
    fun `default isFinished is false`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertFalse(state.isFinished)
    }

    @Test
    fun `default legJustCompleted is false`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertFalse(state.legJustCompleted)
    }

    @Test
    fun `default completedLegNumber is 0`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertEquals(0, state.completedLegNumber)
    }

    @Test
    fun `default completedLegWinnerIndex is -1`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertEquals(-1, state.completedLegWinnerIndex)
    }

    @Test
    fun `default completedLegStats is empty`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        assertTrue(state.completedLegStats.isEmpty())
    }

    @Test
    fun `copy modifies specific fields`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)))
        val modified = state.copy(
            isFinished = true,
            leg = 2,
            currPlayer = 1,
        )
        assertTrue(modified.isFinished)
        assertEquals(2, modified.leg)
        assertEquals(1, modified.currPlayer)
    }

    @Test
    fun `copy preserves unmodified fields`() {
        val state = GameState(playerStates = listOf(PlayerStateX01(player1)), maxLegs = 5)
        val modified = state.copy(isFinished = true)
        assertEquals(5, modified.maxLegs)
        assertEquals(1, modified.leg)
    }

    @Test
    fun `works with PlayerStateCricket generic parameter`() {
        val state = GameState(playerStates = listOf(PlayerStateCricket(player1)))
        assertEquals(1, state.playerStates.size)
        assertTrue(state.playerStates[0] is PlayerStateCricket)
    }

    @Test
    fun `completedLegStats preserves generic type`() {
        val stats = listOf(PlayerStateX01(player1, score = 0), PlayerStateX01(player2, score = 0))
        val state = GameState(
            playerStates = listOf(PlayerStateX01(player1), PlayerStateX01(player2)),
            legJustCompleted = true,
            completedLegNumber = 1,
            completedLegWinnerIndex = 0,
            completedLegStats = stats,
        )
        assertEquals(2, state.completedLegStats.size)
        assertEquals(1, state.completedLegNumber)
        assertEquals(0, state.completedLegWinnerIndex)
    }
}
