package com.example.darts.engine

import android.util.Log
import com.example.darts.viewModel.states.CricketNumber
import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerStateCricket

class GameEngineCricket(
    players: List<PlayerStateCricket>,
    val cutTroath: Boolean = false,
    val maxLegs: Int = 3
) : GameEngine<PlayerStateCricket>() {

    private var state = GameState(
        playerStates = players,
        maxLegs = maxLegs
    )

    override fun processTurn(turn: Turn): GameState<PlayerStateCricket> {
        Log.d("Engine", "Process Turn")
        if (state.isFinished) return state

        val curr = state.currPlayer
        var newPlayerStates = state.playerStates.toMutableList()

        for (dart in turn.darts) {
            val number = dart.value

            newPlayerStates[curr] = newPlayerStates[curr].copy(
                dartsThrown = newPlayerStates[curr].dartsThrown + 1
            )

            if (number !in newPlayerStates[curr].numbers) continue

            val multiplier = dart.multiplier
            val hits = newPlayerStates[curr].numbers[number]?.marks ?: 0
            val newHits = hits + multiplier.mul

            val anyOtherPlayerNotClosed = newPlayerStates.indices.any { index ->
                index != curr && (newPlayerStates[index].numbers[number]?.marks ?: 0) < 3
            }

            if (newHits > 3 && anyOtherPlayerNotClosed) {
                val overflow = newHits - 3
                if (cutTroath) {
                    newPlayerStates = newPlayerStates.mapIndexed { index, playerState ->
                        if (index != curr && (playerState.numbers[number]?.marks ?: 0) < 3) {
                            playerState.copy(score = playerState.score + overflow * number)
                        } else {
                            playerState
                        }
                    }.toMutableList()
                } else {
                    newPlayerStates[curr] = newPlayerStates[curr].copy(
                        score = newPlayerStates[curr].score + overflow * number
                    )
                }
            }

            newPlayerStates[curr] = newPlayerStates[curr].copy(
                numbers = newPlayerStates[curr].numbers.toMutableMap().apply {
                    put(number, CricketNumber(newHits.coerceAtMost(3)))
                }
            )
        }

        val currScore = newPlayerStates[curr].score
        val currHasClosedAll = newPlayerStates[curr].numbers.values.all { it.marks >= 3 }
        val gameFinished = currHasClosedAll && if (cutTroath) {
            newPlayerStates.all { it.score >= currScore }  // lowest score wins
        } else {
            newPlayerStates.all { it.score <= currScore }  // highest score wins
        }

        if (!gameFinished) {
            // Normal turn — leg still in progress
            state = GameState(
                playerStates = newPlayerStates,
                maxLegs = maxLegs,
                isFinished = false,
                leg = state.leg,
                currPlayer = (curr + 1) % newPlayerStates.size,
                legJustCompleted = false,
                completedLegStats = emptyList()
            )
            return state
        }

        // Leg just finished — increment winner's legs
        newPlayerStates[curr] = newPlayerStates[curr].copy(
            legsWon = newPlayerStates[curr].legsWon + 1
        )

        val matchWon = newPlayerStates[curr].legsWon > maxLegs / 2

        // Snapshot stats BEFORE resetting the board
        val completedLegNumber = state.leg
        val snapshot = newPlayerStates.toList()

        // Reset board for next leg (skip if match is over)
        val nextStates = if (matchWon) {
            newPlayerStates
        } else {
            newPlayerStates.map { player ->
                player.copy(
                    numbers = mapOf(
                        15 to CricketNumber(),
                        16 to CricketNumber(),
                        17 to CricketNumber(),
                        18 to CricketNumber(),
                        19 to CricketNumber(),
                        20 to CricketNumber(),
                        25 to CricketNumber()
                    ),
                    score = 0
                )
            }.toMutableList()
        }

        state = GameState(
            playerStates = nextStates,
            maxLegs = maxLegs,
            isFinished = matchWon,
            leg = if (matchWon) state.leg else state.leg + 1,
            currPlayer = (state.currPlayer + 1) % newPlayerStates.size,
            legJustCompleted = true,
            completedLegNumber = completedLegNumber,
            completedLegWinnerIndex = curr,
            completedLegStats = snapshot
        )

        return state
    }

    override fun getState(): GameState<PlayerStateCricket> = state

    override fun resetGame() {
        state = GameState(
            playerStates = state.playerStates.map {
                it.copy(
                    numbers = mapOf(
                        15 to CricketNumber(),
                        16 to CricketNumber(),
                        17 to CricketNumber(),
                        18 to CricketNumber(),
                        19 to CricketNumber(),
                        20 to CricketNumber(),
                        25 to CricketNumber()
                    ),
                    score = 0,
                    legsWon = 0
                )
            },
            maxLegs = maxLegs
        )
    }
}