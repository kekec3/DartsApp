package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlayerStateX01Test {

    private lateinit var player: Player

    @Before
    fun setUp() {
        player = Player(1, "Alice")
    }

    @Test
    fun `default score is 501`() {
        val state = PlayerStateX01(player)
        assertEquals(501, state.score)
    }

    @Test
    fun `default legsWon is 0`() {
        val state = PlayerStateX01(player)
        assertEquals(0, state.legsWon)
    }

    @Test
    fun `default lastScore is 0`() {
        val state = PlayerStateX01(player)
        assertEquals(0, state.lastScore)
    }

    @Test
    fun `default dartsThrown is 0`() {
        val state = PlayerStateX01(player)
        assertEquals(0, state.dartsThrown)
    }

    @Test
    fun `default leg stats are all zero`() {
        val state = PlayerStateX01(player)
        assertEquals(0, state.legDartsThrown)
        assertEquals(0, state.legTotalScored)
        assertEquals(0, state.legCheckoutAttempts)
        assertEquals(0, state.legCheckoutsHit)
        assertEquals(0, state.legHighestCheckout)
        assertEquals(0, state.leg180s)
        assertEquals(0, state.leg140Plus)
        assertEquals(0, state.leg100Plus)
    }

    @Test
    fun `copy preserves player reference`() {
        val state = PlayerStateX01(player)
        val copy = state.copy(score = 301)
        assertSame(player, copy.player)
    }

    @Test
    fun `copy modifies specified fields`() {
        val state = PlayerStateX01(player)
        val modified = state.copy(score = 301, legsWon = 2, dartsThrown = 15)
        assertEquals(301, modified.score)
        assertEquals(2, modified.legsWon)
        assertEquals(15, modified.dartsThrown)
    }

    @Test
    fun `copy preserves unmodified fields`() {
        val state = PlayerStateX01(player, legsWon = 3, dartsThrown = 30)
        val modified = state.copy(score = 201)
        assertEquals(201, modified.score)
        assertEquals(3, modified.legsWon) // preserved
        assertEquals(30, modified.dartsThrown) // preserved
    }

    @Test
    fun `implements PlayerState interface`() {
        val state = PlayerStateX01(player)
        assertTrue(state is PlayerState)
    }

    @Test
    fun `custom constructor values`() {
        val state = PlayerStateX01(
            player = player,
            score = 301,
            legsWon = 5,
            dartsThrown = 42,
            leg180s = 2,
        )
        assertEquals(301, state.score)
        assertEquals(5, state.legsWon)
        assertEquals(42, state.dartsThrown)
        assertEquals(2, state.leg180s)
    }
}
