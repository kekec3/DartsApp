package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlayerStateCricketTest {

    private lateinit var player: Player

    @Before
    fun setUp() {
        player = Player(1, "Bob")
    }

    // ── CricketNumber ───────────────────────────────────────────────────

    @Test
    fun `CricketNumber default marks is 0`() {
        val cn = CricketNumber()
        assertEquals(0, cn.marks)
    }

    @Test
    fun `CricketNumber custom marks`() {
        val cn = CricketNumber(3)
        assertEquals(3, cn.marks)
    }

    // ── PlayerStateCricket defaults ─────────────────────────────────────

    @Test
    fun `default score is 0`() {
        val state = PlayerStateCricket(player)
        assertEquals(0, state.score)
    }

    @Test
    fun `default legsWon is 0`() {
        val state = PlayerStateCricket(player)
        assertEquals(0, state.legsWon)
    }

    @Test
    fun `default dartsThrown is 0`() {
        val state = PlayerStateCricket(player)
        assertEquals(0, state.dartsThrown)
    }

    @Test
    fun `default numbers have all 7 cricket numbers at 0 marks`() {
        val state = PlayerStateCricket(player)
        val expectedNumbers = setOf(15, 16, 17, 18, 19, 20, 25)
        assertEquals(expectedNumbers.size, state.numbers.size)
        for (num in expectedNumbers) {
            assertNotNull("Number $num should exist", state.numbers[num])
            assertEquals("Number $num should have 0 marks", 0, state.numbers[num]?.marks)
        }
    }

    @Test
    fun `implements PlayerState interface`() {
        val state = PlayerStateCricket(player)
        assertTrue(state is PlayerState)
    }

    // ── Copy behavior ───────────────────────────────────────────────────

    @Test
    fun `copy modifies score`() {
        val state = PlayerStateCricket(player)
        val modified = state.copy(score = 100)
        assertEquals(100, modified.score)
    }

    @Test
    fun `copy modifies numbers`() {
        val state = PlayerStateCricket(player)
        val newNumbers = mapOf(20 to CricketNumber(3))
        val modified = state.copy(numbers = newNumbers)
        assertEquals(3, modified.numbers[20]?.marks)
        // Other numbers no longer exist since we replaced the whole map
        assertNull(modified.numbers[15])
    }

    @Test
    fun `copy preserves player reference`() {
        val state = PlayerStateCricket(player)
        val modified = state.copy(score = 50)
        assertSame(player, modified.player)
    }

    @Test
    fun `custom constructor with all fields`() {
        val numbers = mapOf(
            20 to CricketNumber(3),
            19 to CricketNumber(2),
        )
        val state = PlayerStateCricket(
            player = player,
            numbers = numbers,
            score = 80,
            legsWon = 2,
            dartsThrown = 15,
        )
        assertEquals(80, state.score)
        assertEquals(2, state.legsWon)
        assertEquals(15, state.dartsThrown)
        assertEquals(3, state.numbers[20]?.marks)
        assertEquals(2, state.numbers[19]?.marks)
    }

    @Test
    fun `closed number has 3 marks`() {
        val state = PlayerStateCricket(player)
        val closed = state.copy(
            numbers = mapOf(
                20 to CricketNumber(3),
                15 to CricketNumber(0),
                16 to CricketNumber(0),
                17 to CricketNumber(0),
                18 to CricketNumber(0),
                19 to CricketNumber(0),
                25 to CricketNumber(0),
            )
        )
        assertEquals(3, closed.numbers[20]?.marks)
    }
}
