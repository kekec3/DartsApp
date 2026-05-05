package com.example.darts.viewModel.states

data class GameState<T: PlayerState> (
    val playerStates: List<T>,
    val currPlayer:Int = 0,
    val leg: Int = 1,
    val maxLegs: Int = 3,
    val isFinished: Boolean = false
)
