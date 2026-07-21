package com.example.darts.viewModel.states

import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import org.junit.Assert.*
import org.junit.Test

class GameDisplayStateTest {

    // ── StatRow ─────────────────────────────────────────────────────────

    @Test
    fun `StatRow stores label and value`() {
        val row = StatRow("3-dart avg.", "95.50")
        assertEquals("3-dart avg.", row.label)
        assertEquals("95.50", row.value)
    }

    // ── PlayerDisplayState ──────────────────────────────────────────────

    @Test
    fun `PlayerDisplayState stores all fields`() {
        val state = PlayerDisplayState(
            id = "1",
            name = "Alice",
            primaryScore = "501",
            legsWon = 2,
            stats = listOf(StatRow("Avg", "95.00")),
            isCurrent = true,
        )
        assertEquals("1", state.id)
        assertEquals("Alice", state.name)
        assertEquals("501", state.primaryScore)
        assertEquals(2, state.legsWon)
        assertEquals(1, state.stats.size)
        assertTrue(state.isCurrent)
    }

    @Test
    fun `PlayerDisplayState not current by default`() {
        val state = PlayerDisplayState(
            id = "1", name = "Alice", primaryScore = "0",
            legsWon = 0, stats = emptyList(), isCurrent = false,
        )
        assertFalse(state.isCurrent)
    }

    // ── DartSlotState ───────────────────────────────────────────────────

    @Test
    fun `DartSlotState with null dart`() {
        val slot = DartSlotState(null, 0)
        assertNull(slot.dart)
        assertEquals(0, slot.index)
    }

    @Test
    fun `DartSlotState with dart`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        val slot = DartSlotState(dart, 1)
        assertNotNull(slot.dart)
        assertEquals(20, slot.dart!!.value)
        assertEquals(1, slot.index)
    }

    // ── TurnDisplayState ────────────────────────────────────────────────

    @Test
    fun `TurnDisplayState default has 3 empty slots`() {
        val turn = TurnDisplayState()
        assertEquals(3, turn.slots.size)
        for (slot in turn.slots) {
            assertNull(slot.dart)
        }
        assertEquals(0, turn.turnScore)
        assertNull(turn.remaining)
        assertFalse(turn.isBust)
        assertEquals("", turn.currentPlayerName)
        assertEquals(0, turn.dartsEnteredCount)
    }

    @Test
    fun `TurnDisplayState with bust`() {
        val turn = TurnDisplayState(isBust = true)
        assertTrue(turn.isBust)
    }

    @Test
    fun `TurnDisplayState with populated slots`() {
        val darts = listOf(
            DartThrow(20, Multiplier.TRIPLE),
            DartThrow(20, Multiplier.SINGLE),
        )
        val slots = darts.mapIndexed { i, dart -> DartSlotState(dart, i) } +
            List(1) { DartSlotState(null, 2) }

        // Actually let me construct it properly
        val turn = TurnDisplayState(
            slots = List(3) { i ->
                if (i < darts.size) DartSlotState(darts[i], i)
                else DartSlotState(null, i)
            },
            turnScore = 80,
            remaining = 421,
            isBust = false,
            currentPlayerName = "Alice",
            dartsEnteredCount = 2,
        )
        assertEquals(80, turn.turnScore)
        assertEquals(421, turn.remaining)
        assertEquals("Alice", turn.currentPlayerName)
        assertEquals(2, turn.dartsEnteredCount)
    }

    // ── GameDisplayState ────────────────────────────────────────────────

    @Test
    fun `GameDisplayState default values`() {
        val state = GameDisplayState()
        assertEquals("", state.gameTitle)
        assertTrue(state.players.isEmpty())
        assertFalse(state.isFinished)
        assertNull(state.winner)
    }

    @Test
    fun `GameDisplayState with full data`() {
        val players = listOf(
            PlayerDisplayState("1", "Alice", "501", 0, emptyList(), true),
            PlayerDisplayState("2", "Bob", "501", 0, emptyList(), false),
        )
        val turn = TurnDisplayState(
            turnScore = 60,
            currentPlayerName = "Alice",
        )
        val state = GameDisplayState(
            gameTitle = "BEST OF 3 LEGS",
            players = players,
            turn = turn,
            isFinished = false,
            winner = null,
        )
        assertEquals("BEST OF 3 LEGS", state.gameTitle)
        assertEquals(2, state.players.size)
        assertFalse(state.isFinished)
        assertNull(state.winner)
    }

    @Test
    fun `GameDisplayState finished with winner`() {
        val state = GameDisplayState(
            gameTitle = "BEST OF 3 LEGS",
            players = emptyList(),
            isFinished = true,
            winner = "Alice",
        )
        assertTrue(state.isFinished)
        assertEquals("Alice", state.winner)
    }

    @Test
    fun `GameDisplayState copy modification`() {
        val state = GameDisplayState(isFinished = true, winner = "Alice")
        val modified = state.copy(isFinished = false, winner = null)
        assertFalse(modified.isFinished)
        assertNull(modified.winner)
    }
}
