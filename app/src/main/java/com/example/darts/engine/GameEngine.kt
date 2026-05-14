package com.example.darts.engine

import android.util.Log
import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerState

abstract class GameEngine<T: PlayerState> {

    var turnHistory: List<Turn> = emptyList()
    fun submitTurn(turn: Turn): GameState<T> {
        turnHistory = turnHistory.plus(turn)
        return processTurn(turn)
    }
    abstract fun processTurn(turn: Turn): GameState<T>
    abstract fun getState(): GameState<T>

    fun undoTurn(): Pair<GameState<T>, Turn> {
        if (turnHistory.isEmpty()) return Pair(getState(), Turn())
        val lastTurn = turnHistory.last()
        turnHistory = turnHistory.dropLast(1)
        resetGame()
        for (turn in turnHistory) {
            processTurn(turn)
        }
        return Pair(getState(), lastTurn)
    }

    abstract fun resetGame()
}