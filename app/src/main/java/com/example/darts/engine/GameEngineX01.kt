package com.example.darts.engine

import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerStateX01

class GameEngineX01(
    players: List<PlayerStateX01>,
    val target: Int = 501,
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    val maxLegs: Int = 3,
) : GameEngine<PlayerStateX01>() {

    private var state = GameState(
        playerStates = players.map { it.copy(score = target) },
        maxLegs = maxLegs,
    )

    override fun getState(): GameState<PlayerStateX01> = state

    override fun resetGame() {
        state = GameState(
            playerStates = state.playerStates.map { it.copy(score = target, legsWon = 0) },
            maxLegs = maxLegs,
        )
    }

    override fun processTurn(turn: Turn): GameState<PlayerStateX01> {
        if (state.isFinished) return state

        val curr = state.currPlayer
        val cs = state.playerStates[curr]

        // Is the player standing in checkout range at the start of this visit?
        val isCheckoutAttempt = cs.score <= 170

        // Bug fix: masterIn only applies while the player hasn't opened (score == target).
        // Previously this ran on every turn, so a player already at e.g. 300 still
        // needed to start each visit with a double/triple or score nothing.
        val scored = if (!masterIn || cs.score < target) {
            turn.totalScore()
        } else {
            var valid = false
            var acc = 0
            for (dart in turn.darts) {
                if (!valid && (dart.isDouble() || dart.isTriple())) valid = true
                if (valid) acc += dart.score()
            }
            acc
        }

        val newScore = cs.score - scored

        state = when {

            // ── Exact checkout ────────────────────────────────────────────
            newScore == 0 -> {
                val validCheckout = !doubleOut || turn.lastScoringDart()?.isDouble() == true

                if (!validCheckout) {
                    // Hit the right number but with the wrong dart type (e.g. single
                    // instead of double when doubleOut is on). Score reverts; no points.
                    val updated = cs.copy(
                        lastScore = 0,
                        dartsThrown = cs.dartsThrown + turn.darts.size,
                        legDartsThrown = cs.legDartsThrown + turn.darts.size,
                        legCheckoutAttempts = if (isCheckoutAttempt) cs.legCheckoutAttempts + 1
                        else cs.legCheckoutAttempts,
                    )
                    state.copy(
                        playerStates = state.playerStates.replaceAt(curr, updated),
                        currPlayer = (curr + 1) % state.playerStates.size,
                        legJustCompleted = false,
                        completedLegStats = emptyList(),
                    )
                } else {
                    // ── Valid checkout ─────────────────────────────────────
                    val newLegsWon = cs.legsWon + 1
                    val matchWon = newLegsWon > state.maxLegs / 2
                    val completedLegNumber = state.leg

                    // Build the winner's final-leg state (before reset).
                    val completedWinner = cs.copy(
                        score = 0,
                        lastScore = scored,
                        dartsThrown = cs.dartsThrown + turn.darts.size,
                        legsWon = newLegsWon,
                        legDartsThrown = cs.legDartsThrown + turn.darts.size,
                        legTotalScored = cs.legTotalScored + scored,
                        legCheckoutAttempts = cs.legCheckoutAttempts + 1,
                        legCheckoutsHit = cs.legCheckoutsHit + 1,
                        legHighestCheckout = maxOf(cs.legHighestCheckout, scored),
                    ).withMilestones(scored)

                    // Snapshot every player's leg stats before the reset.
                    val snapshot: List<PlayerStateX01> =
                        state.playerStates.replaceAt(curr, completedWinner)

                    // Reset per-leg counters for the next leg.
                    val resetStates = snapshot.map { ps ->
                        ps.copy(
                            score = target,
                            legDartsThrown = 0,
                            legTotalScored = 0,
                            legCheckoutAttempts = 0,
                            legCheckoutsHit = 0,
                            legHighestCheckout = 0,
                            leg180s = 0,
                            leg140Plus = 0,
                            leg100Plus = 0,
                        )
                    }

                    state.copy(
                        playerStates = resetStates,
                        currPlayer = (curr + 1) % state.playerStates.size,
                        isFinished = matchWon,
                        leg = if (matchWon) state.leg else state.leg + 1,
                        // Signal the ViewModel — true for every leg end, including last.
                        legJustCompleted = true,
                        completedLegNumber = completedLegNumber,
                        completedLegWinnerIndex = curr,
                        completedLegStats = snapshot,
                    )
                }
            }

            // ── Bust: went below 0, or landed on 1 with doubleOut ─────────
            newScore < 0 || (newScore == 1 && doubleOut) -> {
                val updated = cs.copy(
                    lastScore = 0,
                    dartsThrown = cs.dartsThrown + turn.darts.size,
                    legDartsThrown = cs.legDartsThrown + turn.darts.size,
                    legCheckoutAttempts = if (isCheckoutAttempt) cs.legCheckoutAttempts + 1
                    else cs.legCheckoutAttempts,
                )
                state.copy(
                    playerStates = state.playerStates.replaceAt(curr, updated),
                    currPlayer = (curr + 1) % state.playerStates.size,
                    legJustCompleted = false,
                    completedLegStats = emptyList(),
                )
            }

            // ── Valid score (leg continues) ────────────────────────────────
            else -> {
                val updated = cs.copy(
                    score = newScore,
                    lastScore = scored,
                    dartsThrown = cs.dartsThrown + turn.darts.size,
                    legDartsThrown = cs.legDartsThrown + turn.darts.size,
                    legTotalScored = cs.legTotalScored + scored,
                    legCheckoutAttempts = if (isCheckoutAttempt) cs.legCheckoutAttempts + 1
                    else cs.legCheckoutAttempts,
                ).withMilestones(scored)

                state.copy(
                    playerStates = state.playerStates.replaceAt(curr, updated),
                    currPlayer = (curr + 1) % state.playerStates.size,
                    legJustCompleted = false,
                    completedLegStats = emptyList(),
                )
            }
        }

        return state
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun PlayerStateX01.withMilestones(scored: Int) = copy(
        leg180s = if (scored == 180) leg180s + 1 else leg180s,
        leg140Plus = if (scored in 140..179) leg140Plus + 1 else leg140Plus,
        leg100Plus = if (scored in 100..139) leg100Plus + 1 else leg100Plus,
    )

    private fun Turn.lastScoringDart(): DartThrow? = darts.lastOrNull { !it.isMiss() }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
        toMutableList().also { it[index] = value }
}