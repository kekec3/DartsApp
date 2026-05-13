package com.example.darts.engine

import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerState

interface GameEngine<T: PlayerState> {
    fun submitTurn(turn: Turn): GameState<T>
    fun getState(): GameState<T>
}