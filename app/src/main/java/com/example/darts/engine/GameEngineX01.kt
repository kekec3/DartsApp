package com.example.darts.engine

import androidx.compose.runtime.toMutableStateList
import androidx.navigationevent.NavigationEventInfo
import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerStateX01

class GameEngineX01(
    players: List<PlayerStateX01>,
    val target: Int = 501,
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    val maxLegs: Int = 3
): GameEngine<PlayerStateX01> {

    private var state = GameState(
        playerStates = players,
        maxLegs = maxLegs
    )

    override fun getState(): GameState<PlayerStateX01> {
        return state
    }

    override fun submitTurn(turn: Turn): GameState<PlayerStateX01> {
        if (state.isFinished) return state

        val curr = state.currPlayer
        val currState = state.playerStates[curr]

        val scored = if (!masterIn) {
            turn.totalScore()
        } else {
            var valid = false
            var acc   = 0
            for (dart in turn.darts) {
                if (!valid && (dart.isDouble() || dart.isTriple())) valid = true
                if (valid) acc += dart.score()
            }
            acc
        }

        val newScore = currState.score - scored

        val (newPlayerStates, gameFinished, newLeg) = when {

            // Valid score
            newScore > 1 || (newScore > 0 && !doubleOut) -> {
                val updated = currState.copy(
                    score        = newScore,
                    lastScore    = scored,
                    dartsThrown  = currState.dartsThrown + turn.darts.size
                )
                Triple(state.playerStates.replaceAt(curr, updated), false, state.leg)
            }

            // Exact checkout
            newScore == 0 -> {
                val validCheckout = !doubleOut || turn.lastScoringDart()?.isDouble() == true
                if (!validCheckout) {
                    val updated = currState.copy(
                        lastScore   = 0,
                        dartsThrown = currState.dartsThrown + turn.darts.size
                    )
                    Triple(state.playerStates.replaceAt(curr, updated), false, state.leg)
                } else {
                    val newLegsWon = currState.legsWon + 1
                    val matchWon   = newLegsWon > state.maxLegs / 2

                    // Reset all scores for next leg, but keep the updated winner's legs
                    val updated = currState.copy(
                        score       = target,
                        lastScore   = scored,
                        dartsThrown = currState.dartsThrown + turn.darts.size,
                        legsWon     = newLegsWon
                    )
                    val resetStates = state.playerStates
                        .map { it.copy(score = target) }
                        .replaceAt(curr, updated)

                    Triple(resetStates, matchWon, if (matchWon) state.leg else state.leg + 1)
                }
            }

            // Bust
            else -> {
                val updated = currState.copy(
                    lastScore   = 0,
                    dartsThrown = currState.dartsThrown + turn.darts.size
                )
                Triple(state.playerStates.replaceAt(curr, updated), false, state.leg)
            }
        }

        state = state.copy(
            playerStates = newPlayerStates,
            currPlayer   = (curr + 1) % state.playerStates.size,
            isFinished   = gameFinished,
            leg          = newLeg
        )

        return state
    }

    private fun Turn.lastScoringDart(): DartThrow? {
        return darts.lastOrNull {!it.isMiss()}
    }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> {
        return toMutableList().also { it[index] = value }
    }
}