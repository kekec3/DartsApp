package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player

data class PlayerStateX01(
    val player: Player,
    val score: Int = 501,
    val legsWon: Int = 0,
    val lastScore: Int = 0,
    val dartsThrown: Int = 0
) : PlayerState
