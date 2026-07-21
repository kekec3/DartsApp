package com.example.darts.engine

import com.example.darts.db.entities.Player
import com.example.darts.viewModel.states.CricketNumber
import com.example.darts.viewModel.states.PlayerStateCricket
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameEngineCricketTest {

    private lateinit var player1: Player
    private lateinit var player2: Player

    @Before
    fun setUp() {
        player1 = Player(1, "Alice")
        player2 = Player(2, "Bob")
    }

    private fun turnOf(vararg pairs: Pair<Int, Int>): Turn {
        return Turn(pairs.map { (value, multiplierCode) ->
            val multiplier = when (multiplierCode) {
                1 -> Multiplier.SINGLE
                2 -> Multiplier.DOUBLE
                3 -> Multiplier.TRIPLE
                else -> Multiplier.SINGLE
            }
            DartThrow(value, multiplier)
        })
    }

    private fun numbersWith(vararg pairs: Pair<Int, Int>): Map<Int, CricketNumber> {
        val base = mutableMapOf<Int, Int>()
        for (i in listOf(15, 16, 17, 18, 19, 20, 25)) base[i] = 0
        for ((k, v) in pairs) base[k] = v
        return base.mapValues { CricketNumber(it.value) }
    }

    // ── Initial state ───────────────────────────────────────────────────

    @Test
    fun `initial state has all numbers unmarked`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        val state = engine.getState()

        for (ps in state.playerStates) {
            for (num in setOf(15, 16, 17, 18, 19, 20, 25)) {
                assertEquals("Number $num should be unmarked", 0, ps.numbers[num]?.marks)
            }
            assertEquals(0, ps.score)
            assertEquals(0, ps.legsWon)
        }
        assertEquals(0, state.currPlayer)
        assertEquals(1, state.leg)
    }

    // ── Basic scoring (marks) ───────────────────────────────────────────

    @Test
    fun `hitting a number records a mark`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        engine.submitTurn(turnOf(20 to 1))
        assertEquals(1, engine.getState().playerStates[0].numbers[20]?.marks)
    }

    @Test
    fun `double records two marks`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        engine.submitTurn(turnOf(20 to 2))
        assertEquals(2, engine.getState().playerStates[0].numbers[20]?.marks)
    }

    @Test
    fun `triple records three marks`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        engine.submitTurn(turnOf(20 to 3))
        assertEquals(3, engine.getState().playerStates[0].numbers[20]?.marks)
    }

    @Test
    fun `marks are capped at 3`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        engine.submitTurn(turnOf(20 to 3, 20 to 2))
        assertEquals(3, engine.getState().playerStates[0].numbers[20]?.marks)
    }

    @Test
    fun `non-cricket numbers are ignored`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)
        engine.submitTurn(turnOf(1 to 1, 2 to 1, 3 to 1))
        for (num in setOf(15, 16, 17, 18, 19, 20, 25)) {
            assertEquals(0, engine.getState().playerStates[0].numbers[num]?.marks)
        }
    }

    // ── Point scoring (non-cutthroat) ───────────────────────────────────

    @Test
    fun `extra marks on closed number score points`() {
        // Player 1 has 20 closed (3 marks), D20 gives 2 overflow marks = 40 points
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(20 to 3))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 2))

        val state = engine.getState()
        assertEquals(40, state.playerStates[0].score) // 2 * 20 = 40
    }

    @Test
    fun `extra marks on closed number score points with triple`() {
        // Player 1 has 20 closed (3 marks), T20 gives 3 overflow marks = 60 points
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(20 to 3))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 3))

        val state = engine.getState()
        assertEquals(60, state.playerStates[0].score) // 3 * 20 = 60
    }

    @Test
    fun `extra marks score when opponent has not closed the number`() {
        // Player 1 has 20 closed, player 2 hasn't. D20 gives overflow = 40 points.
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(20 to 3))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 2))

        assertEquals(40, engine.getState().playerStates[0].score)
    }

    // ── Cutthroat mode ──────────────────────────────────────────────────

    @Test
    fun `cutthroat mode adds points to opponents for extra marks`() {
        // Player 1 has 20 closed, T20 overflow = 60 points go to opponent
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(20 to 3))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players, cutTroath = true)

        engine.submitTurn(turnOf(20 to 3))

        val state = engine.getState()
        assertEquals(0, state.playerStates[0].score)
        assertEquals(60, state.playerStates[1].score)
    }

    @Test
    fun `cutthroat mode adds points proportionally across opponents`() {
        val player3 = Player(3, "Charlie")
        // Player 1 has 20 closed, D20 overflow = 40.
        // Players 2 and 3 both haven't closed 20, so both get 40 each in cutthroat.
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(20 to 3))
        val players = listOf(p1, PlayerStateCricket(player2), PlayerStateCricket(player3))
        val engine = GameEngineCricket(players, cutTroath = true)

        engine.submitTurn(turnOf(20 to 2))

        val state = engine.getState()
        assertEquals(0, state.playerStates[0].score)
        assertEquals(40, state.playerStates[1].score)
        assertEquals(40, state.playerStates[2].score)
    }

    // ── Leg completion ──────────────────────────────────────────────────

    @Test
    fun `leg completes when a player closes all numbers`() {
        // Player 1 needs just 20 to close all (25 already closed)
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(
            15 to 3, 16 to 3, 17 to 3, 18 to 3, 19 to 3,
            20 to 0, 25 to 3
        ))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        val state = engine.submitTurn(turnOf(20 to 3)) // T20 closes 20

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
    }

    @Test
    fun `leg does not complete until all numbers are closed`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 3))
        assertFalse(engine.getState().legJustCompleted)
    }

    @Test
    fun `leg resets board after completion`() {
        // Player 1 has all numbers at 3 marks except 25 at 2 marks
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(
            15 to 3, 16 to 3, 17 to 3, 18 to 3, 19 to 3, 20 to 3,
            25 to 2
        ))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        // D25 (2 marks) closes 25 and wins the leg
        val state = engine.submitTurn(turnOf(25 to 2))

        assertTrue(state.legJustCompleted)

        // After reset, all numbers should be 0 marks
        for (ps in state.playerStates) {
            for (num in listOf(15, 16, 17, 18, 19, 20, 25)) {
                assertEquals(0, ps.numbers[num]?.marks ?: 0)
            }
        }
    }

    // ── Match completion ────────────────────────────────────────────────

    @Test
    fun `match is not finished after one leg win in best of 3`() {
        val allClosed = numbersWith(15 to 3, 16 to 3, 17 to 3, 18 to 3, 19 to 3, 20 to 3, 25 to 3)
        val p1 = PlayerStateCricket(player1).copy(numbers = allClosed)
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players, maxLegs = 3)

        val state = engine.submitTurn(turnOf(20 to 1))

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
        assertFalse(state.isFinished)
    }

    @Test
    fun `match finishes at correct leg count`() {
        // Player 1 has all numbers at 3 marks except 25 at 2 marks
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(
            15 to 3, 16 to 3, 17 to 3, 18 to 3, 19 to 3, 20 to 3,
            25 to 2
        ))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players, maxLegs = 1)

        val state = engine.submitTurn(turnOf(25 to 2)) // Close 25 and win match

        assertTrue(state.isFinished)
        assertEquals(1, state.playerStates[0].legsWon)
    }

    // ── Undo ────────────────────────────────────────────────────────────

    @Test
    fun `undo reverts cricket state`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 1))
        assertEquals(1, engine.getState().playerStates[0].numbers[20]?.marks)

        val (state, _) = engine.undoTurn()
        assertEquals(0, state.playerStates[0].numbers[20]?.marks)
    }

    // ── Turn history ────────────────────────────────────────────────────

    @Test
    fun `turn history records turns`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(20 to 1))
        assertEquals(1, engine.turnHistory.size)
        engine.submitTurn(turnOf(15 to 1))
        assertEquals(2, engine.turnHistory.size)
    }

    // ── Darts thrown tracking ───────────────────────────────────────────

    @Test
    fun `darts thrown is tracked even for non-scoring numbers`() {
        val players = listOf(PlayerStateCricket(player1), PlayerStateCricket(player2))
        val engine = GameEngineCricket(players)

        engine.submitTurn(turnOf(1 to 1, 2 to 1, 3 to 1))
        assertEquals(3, engine.getState().playerStates[0].dartsThrown)
    }

    @Test
    fun `resetGame resets cricket state`() {
        val p1 = PlayerStateCricket(player1).copy(numbers = numbersWith(
            15 to 3, 16 to 3, 17 to 3, 18 to 3, 19 to 3, 20 to 3,
            25 to 2
        ))
        val players = listOf(p1, PlayerStateCricket(player2))
        val engine = GameEngineCricket(players, maxLegs = 3)

        engine.submitTurn(turnOf(25 to 2))
        engine.resetGame()

        val state = engine.getState()
        assertEquals(0, state.playerStates[0].score)
        assertEquals(0, state.playerStates[0].legsWon)
        assertEquals(0, state.playerStates[0].numbers[20]?.marks)
    }
}
