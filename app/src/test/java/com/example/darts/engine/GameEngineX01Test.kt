package com.example.darts.engine

import com.example.darts.db.entities.Player
import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerStateX01
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameEngineX01Test {

    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var player3: Player

    @Before
    fun setUp() {
        player1 = Player(1, "Alice")
        player2 = Player(2, "Bob")
        player3 = Player(3, "Charlie")
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private fun turnOf(vararg darts: Pair<Int, Int>): Turn {
        return Turn(darts.map { (value, multiplier) ->
            DartThrow(value, when (multiplier) {
                1 -> Multiplier.SINGLE
                2 -> Multiplier.DOUBLE
                3 -> Multiplier.TRIPLE
                else -> throw IllegalArgumentException("Invalid multiplier: $multiplier")
            })
        })
    }

    /**
     * Play a single-player turn to reduce score. Both players must alternate turns.
     * Helper for quickly setting up specific score scenarios.
     */
    private fun playScore(engine: GameEngineX01, scoreToReduce: Int) {
        // We need to play turns that exactly reduce the score
        // For player 1 to play, player 2 must also take turns
        var remaining = scoreToReduce
        while (remaining > 0) {
            val currentScore = engine.getState().playerStates[0].score
            val toThrow = minOf(remaining, 180)
            // Submit a turn for current player
            if (engine.getState().currPlayer == 0) {
                engine.submitTurn(turnOf(20 to 3, 20 to 3, (toThrow - 120) / 1 to 1))
                remaining -= toThrow
            } else {
                // Player 2's turn - just throw a single 1
                engine.submitTurn(turnOf(1 to 1))
            }
        }
    }

    private fun d20() = DartThrow(20, Multiplier.SINGLE)

    // ── Basic scoring ───────────────────────────────────────────────────

    @Test
    fun `initial state has correct scores`() {
        val players = listOf(
            PlayerStateX01(player1, score = 501),
            PlayerStateX01(player2, score = 501),
        )
        val engine = GameEngineX01(players, target = 501)
        val state = engine.getState()

        assertEquals(501, state.playerStates[0].score)
        assertEquals(501, state.playerStates[1].score)
        assertEquals(0, state.currPlayer)
        assertFalse(state.isFinished)
        assertEquals(1, state.leg)
    }

    @Test
    fun `scoring reduces score correctly`() {
        val players = listOf(
            PlayerStateX01(player1, score = 501),
            PlayerStateX01(player2, score = 501),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 1, 20 to 1, 20 to 1)) // 60

        val state = engine.getState()
        assertEquals(441, state.playerStates[0].score)
        assertEquals(1, state.currPlayer) // Turn passed to player 2
    }

    @Test
    fun `scoring alternates players`() {
        val players = listOf(
            PlayerStateX01(player1, score = 501),
            PlayerStateX01(player2, score = 501),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 1, 20 to 1, 20 to 1)) // Player 1: 60
        assertEquals(1, engine.getState().currPlayer)

        engine.submitTurn(turnOf(20 to 3, 20 to 1)) // Player 2: 80
        assertEquals(0, engine.getState().currPlayer)

        // Verify scores
        val state = engine.getState()
        assertEquals(441, state.playerStates[0].score) // 501 - 60
        assertEquals(421, state.playerStates[1].score) // 501 - 80
    }

    @Test
    fun `tracks darts thrown correctly`() {
        val players = listOf(
            PlayerStateX01(player1, score = 501),
            PlayerStateX01(player2, score = 501),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 1, 20 to 1, 20 to 1))
        assertEquals(3, engine.getState().playerStates[0].dartsThrown)

        engine.submitTurn(turnOf(20 to 3, 5 to 1))
        assertEquals(2, engine.getState().playerStates[1].dartsThrown)
    }

    // ── Bust scenarios ──────────────────────────────────────────────────

    @Test
    fun `going below zero is a bust`() {
        // Use target=40 to start player at 40, then bust with 120
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40)

        engine.submitTurn(turnOf(20 to 3, 20 to 3)) // 120 - bust!

        val state = engine.getState()
        assertEquals(40, state.playerStates[0].score) // Score restored
        assertEquals(0, state.playerStates[0].lastScore) // lastScore is 0 on bust
        assertEquals(1, state.currPlayer) // Turn passed
    }

    @Test
    fun `landing on 1 with doubleOut is a bust`() {
        // Target=41, then score 40 with singles (not a double), leaving 1
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 41, doubleOut = true)

        engine.submitTurn(turnOf(20 to 1, 20 to 1)) // 40, leaves 1 - bust because doubleOut and didn't finish on double

        val state = engine.getState()
        assertEquals(41, state.playerStates[0].score) // Score restored (bust)
        assertEquals(1, state.currPlayer)
    }

    @Test
    fun `bust does not count score`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        engine.submitTurn(turnOf(20 to 3, 20 to 3)) // 120 > 40 = bust

        val state = engine.getState()
        assertEquals(0, state.playerStates[0].lastScore)
    }

    // ── Double out ──────────────────────────────────────────────────────

    @Test
    fun `exact checkout with double on doubleOut wins the leg`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        val state = engine.submitTurn(turnOf(20 to 2)) // D20 = 40, exact checkout

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
        assertEquals(2, state.leg)
    }

    @Test
    fun `exact checkout without double on doubleOut is bust`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        val state = engine.submitTurn(turnOf(20 to 1, 20 to 1)) // S20+S20 = 40, not a double

        assertEquals(40, state.playerStates[0].score) // Score restored (bust)
        assertFalse(state.legJustCompleted)
    }

    @Test
    fun `exact checkout with bullseye on doubleOut wins the leg`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 50, doubleOut = true)

        val state = engine.submitTurn(turnOf(25 to 2)) // Bull = 50, counts as double

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
    }

    @Test
    fun `exact checkout with single on non-doubleOut wins the leg`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = false)

        val state = engine.submitTurn(turnOf(20 to 1, 20 to 1)) // 40, exact

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
    }

    // ── Master in ───────────────────────────────────────────────────────

    @Test
    fun `master in requires double or triple to start scoring`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501, masterIn = true)

        // All singles — masterIn requires a double/triple to open scoring
        engine.submitTurn(turnOf(20 to 1, 20 to 1, 20 to 1))

        val state = engine.getState()
        assertEquals(501, state.playerStates[0].score)
    }

    @Test
    fun `master in allows scoring after double`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501, masterIn = true)

        // D20 opens, then S20 and S20 count
        engine.submitTurn(turnOf(20 to 2, 20 to 1, 20 to 1))

        val state = engine.getState()
        // D20=40, S20=20, S20=20. Total=80. 501-80=421
        assertEquals(421, state.playerStates[0].score)
    }

    @Test
    fun `master in allows scoring after triple`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501, masterIn = true)

        // T20 opens, then S20 counts
        engine.submitTurn(turnOf(20 to 3, 20 to 1))

        val state = engine.getState()
        // T20=60, S20=20. Total=80. 501-80=421
        assertEquals(421, state.playerStates[0].score)
    }

    @Test
    fun `master in only applies at full score`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501, masterIn = true)

        // Turn 1 (player 1): T20 opens, S20+S20 accumulate. Total = 100. Score: 501 -> 401
        engine.submitTurn(turnOf(20 to 3, 20 to 1, 20 to 1))
        assertEquals(401, engine.getState().playerStates[0].score)

        // Turn 2 (player 2): All singles at full score. MasterIn prevents scoring.
        engine.submitTurn(turnOf(20 to 1, 20 to 1, 20 to 1))
        assertEquals(501, engine.getState().playerStates[1].score)

        // Turn 3 (player 1): Score is 401 < 501, masterIn no longer applies.
        // All darts count regardless of multiplier.
        engine.submitTurn(turnOf(20 to 1, 20 to 1)) // S20+S20 = 40
        assertEquals(361, engine.getState().playerStates[0].score) // 401 - 40
    }

    // ── Checkout ────────────────────────────────────────────────────────

    @Test
    fun `checkout increments legs won`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // T20,T20,T20 = 180, leaves 321
        // Player 2 turn
        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // T20,T20,T20 = 180, leaves 321
        // Player 1 turn
        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // T20,T20,T20 = 180, leaves 141
        // Player 2 turn
        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // T20,T20,T20 = 180, leaves 141
        // Player 1 turn: needs 141 = T20(60) + T20(60) + T7(21)
        val state = engine.submitTurn(turnOf(20 to 3, 20 to 3, 7 to 3))

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.playerStates[0].legsWon)
        // Score is reset to target after leg completion
        assertEquals(501, state.playerStates[0].score)
    }

    @Test
    fun `checkout records leg stats snapshot`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        val state = engine.submitTurn(turnOf(20 to 2)) // D20 = 40

        assertTrue(state.legJustCompleted)
        assertEquals(1, state.completedLegNumber)
        assertEquals(0, state.completedLegWinnerIndex)

        val winnerStats = state.completedLegStats[0]
        assertEquals(40, winnerStats.legTotalScored)
        assertEquals(1, winnerStats.legDartsThrown)
        assertEquals(1, winnerStats.legCheckoutAttempts)
        assertEquals(1, winnerStats.legCheckoutsHit)
        assertEquals(40, winnerStats.legHighestCheckout)
    }

    @Test
    fun `checkout increments leg counter`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        val state = engine.submitTurn(turnOf(20 to 2))
        assertEquals(2, state.leg)
    }

    // ── Leg management ──────────────────────────────────────────────────

    @Test
    fun `leg stats are reset after leg completion`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        val state = engine.submitTurn(turnOf(20 to 2)) // Player 1 wins leg 1

        // Score should reset to target
        assertEquals(40, state.playerStates[0].score)
        assertEquals(40, state.playerStates[1].score)

        // Leg-specific stats should be reset
        assertEquals(0, state.playerStates[0].legDartsThrown)
        assertEquals(0, state.playerStates[0].legTotalScored)
        assertEquals(0, state.playerStates[0].legCheckoutAttempts)
        assertEquals(0, state.playerStates[0].legCheckoutsHit)
        assertEquals(0, state.playerStates[0].legHighestCheckout)
    }

    @Test
    fun `match finishes when player wins majority of legs`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true, maxLegs = 3)

        // Leg 1: Player 1 wins
        engine.submitTurn(turnOf(20 to 2))
        // Leg 2: Both players reset to 40. Player 2's turn (currPlayer=1).
        engine.submitTurn(turnOf(20 to 2)) // Player 2 wins leg 2
        // Leg 3: Player 1's turn (currPlayer=0).
        engine.submitTurn(turnOf(20 to 2)) // Player 1 wins leg 3

        val state = engine.getState()
        assertTrue(state.isFinished)
        assertEquals(2, state.playerStates[0].legsWon)
    }

    @Test
    fun `match does not finish prematurely`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = false, maxLegs = 5)

        // Leg 1: Player 1 wins
        engine.submitTurn(turnOf(20 to 1, 20 to 1))

        assertFalse(engine.getState().isFinished)
        assertEquals(1, engine.getState().playerStates[0].legsWon)
    }

    @Test
    fun `maxLegs of 1 means first leg wins match`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = false, maxLegs = 1)

        val state = engine.submitTurn(turnOf(20 to 1, 20 to 1)) // 40 = exact checkout

        assertTrue(state.isFinished)
        assertEquals(1, state.playerStates[0].legsWon)
    }

    // ── Milestones ──────────────────────────────────────────────────────

    @Test
    fun `scoring 180 is tracked as milestone`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3))

        assertEquals(1, engine.getState().playerStates[0].leg180s)
    }

    @Test
    fun `scoring 140-179 is tracked as 140 plus`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 1)) // 60+60+20 = 140

        assertEquals(1, engine.getState().playerStates[0].leg140Plus)
    }

    @Test
    fun `scoring 100-139 is tracked as 100 plus`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 1, 20 to 1)) // 60+20+20 = 100

        assertEquals(1, engine.getState().playerStates[0].leg100Plus)
    }

    @Test
    fun `milestones are tracked separately per leg and reset`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = false)

        // Player 1: 40
        engine.submitTurn(turnOf(20 to 1, 20 to 1)) // 40 = checkout
        // Player 2 turn (just passes)

        // After leg reset, leg-specific milestones should be 0
        assertEquals(0, engine.getState().playerStates[0].leg100Plus)
        assertEquals(0, engine.getState().playerStates[0].legDartsThrown)
    }

    // ── Undo ────────────────────────────────────────────────────────────

    @Test
    fun `undo last turn reverts state`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // 180
        assertEquals(321, engine.getState().playerStates[0].score)

        val (newState, lastTurn) = engine.undoTurn()
        assertEquals(501, newState.playerStates[0].score)
        assertEquals(180, lastTurn.totalScore())
    }

    @Test
    fun `undo on empty history returns current state`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        val (state, turn) = engine.undoTurn()
        assertEquals(501, state.playerStates[0].score)
        assertTrue(turn.darts.isEmpty())
    }

    @Test
    fun `undo restores correct player order`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
            PlayerStateX01(player3),
        )
        val engine = GameEngineX01(players, target = 501)

        // Player 1 throws
        engine.submitTurn(turnOf(20 to 3, 20 to 3)) // 120, score=381
        // Player 2 throws
        engine.submitTurn(turnOf(20 to 3, 20 to 1)) // 80, score=421

        assertEquals(2, engine.getState().currPlayer) // Player 3's turn

        val (state, _) = engine.undoTurn()
        assertEquals(1, state.currPlayer) // Back to Player 2's turn
        assertEquals(501, state.playerStates[1].score) // Player 2's score restored
    }

    @Test
    fun `undo after multiple turns`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        // Turn 1: P1 scores 60
        engine.submitTurn(turnOf(20 to 3)) // 60
        // Turn 2: P2 scores 40
        engine.submitTurn(turnOf(20 to 1, 20 to 1)) // 40
        // Turn 3: P1 scores 100
        engine.submitTurn(turnOf(20 to 3, 20 to 1, 20 to 1)) // 100

        assertEquals(341, engine.getState().playerStates[0].score) // 501-60-100

        // Undo turn 3
        val (stateAfterOneUndo, _) = engine.undoTurn()
        assertEquals(441, stateAfterOneUndo.playerStates[0].score) // 501-60

        // Undo turn 2
        val (stateAfterTwoUndos, _) = engine.undoTurn()
        // After undoing turn 2 (P2's 40pt turn), P2's score goes back to 501
        assertEquals(501, stateAfterTwoUndos.playerStates[1].score)
        assertEquals(441, stateAfterTwoUndos.playerStates[0].score) // 501-60
    }

    // ── Turn history ────────────────────────────────────────────────────

    @Test
    fun `turn history is recorded`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3))
        assertEquals(1, engine.turnHistory.size)

        engine.submitTurn(turnOf(20 to 1))
        assertEquals(2, engine.turnHistory.size)
    }

    @Test
    fun `turn history records correct dart throws`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 501)

        val turn = turnOf(20 to 3, 20 to 1)
        engine.submitTurn(turn)

        val recordedTurn = engine.turnHistory[0]
        assertEquals(2, recordedTurn.darts.size)
        assertEquals(60, recordedTurn.darts[0].score())
        assertEquals(20, recordedTurn.darts[1].score())
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `game ignores turns after finished`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 2, doubleOut = true, maxLegs = 1)

        engine.submitTurn(turnOf(1 to 2)) // D1 = 2, checkout

        assertTrue(engine.getState().isFinished)

        // This turn should be ignored by the engine (no state change)
        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3))
        // Turn is recorded in turnHistory before isFinished check, so size is 2
        assertEquals(2, engine.turnHistory.size)
    }

    @Test
    fun `three player game works correctly`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
            PlayerStateX01(player3),
        )
        val engine = GameEngineX01(players, target = 501)

        assertEquals(0, engine.getState().currPlayer)
        engine.submitTurn(turnOf(20 to 1))
        assertEquals(1, engine.getState().currPlayer)
        engine.submitTurn(turnOf(20 to 1))
        assertEquals(2, engine.getState().currPlayer)
        engine.submitTurn(turnOf(20 to 1))
        assertEquals(0, engine.getState().currPlayer)
    }

    @Test
    fun `custom target works`() {
        val players = listOf(
            PlayerStateX01(player1),
        )
        val engine = GameEngineX01(players, target = 301)

        assertEquals(301, engine.getState().playerStates[0].score)
    }

    @Test
    fun `checkout attempt tracking increments on low scores`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 170, doubleOut = false)

        // 170 is within checkout range (<= 170)
        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 1)) // 140

        val state = engine.getState()
        assertEquals(1, state.playerStates[0].legCheckoutAttempts)
    }

    @Test
    fun `checkout attempt not tracked when score is above 170`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 200, doubleOut = false)

        engine.submitTurn(turnOf(20 to 3, 20 to 3, 20 to 3)) // 180

        val state = engine.getState()
        assertEquals(0, state.playerStates[0].legCheckoutAttempts) // 200 > 170
    }

    @Test
    fun `multiple legs track cumulative darts thrown`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true, maxLegs = 3)

        // Leg 1: Player 1 checks out with D20 (1 dart)
        engine.submitTurn(turnOf(20 to 2))
        var state = engine.getState()
        assertEquals(1, state.playerStates[0].dartsThrown)

        // Leg 2: Player 1 needs to score 40, Player 2 needs to score 40
        // Player 2 turn first (currPlayer=1 after leg 1)
        engine.submitTurn(turnOf(20 to 2)) // Player 2 wins leg 2
        // Leg 3: Player 1 turn
        engine.submitTurn(turnOf(20 to 2)) // Player 1 wins leg 3

        state = engine.getState()
        // Player 1 threw in leg 1 (1 dart) and leg 3 (1 dart) = 2 total
        // Player 2 threw in leg 2 (1 dart)
        assertEquals(2, state.playerStates[0].dartsThrown)
    }

    @Test
    fun `resetGame resets everything`() {
        val players = listOf(
            PlayerStateX01(player1),
            PlayerStateX01(player2),
        )
        val engine = GameEngineX01(players, target = 40, doubleOut = true)

        engine.submitTurn(turnOf(20 to 2)) // Player 1 wins
        engine.resetGame()

        val state = engine.getState()
        assertEquals(40, state.playerStates[0].score)
        assertEquals(40, state.playerStates[1].score)
        assertEquals(0, state.playerStates[0].legsWon)
        assertEquals(0, state.playerStates[1].legsWon)
        assertFalse(state.isFinished)
        assertEquals(1, state.leg)
    }
}
