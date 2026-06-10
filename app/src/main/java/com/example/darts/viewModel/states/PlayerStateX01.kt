package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player

data class PlayerStateX01(
    val player: Player,
    val score: Int = 501,
    val legsWon: Int = 0,
    val lastScore: Int = 0,
    val dartsThrown: Int = 0,

    val legDartsThrown: Int = 0,
    val legTotalScored: Int = 0,
    val legCheckoutAttempts: Int = 0,
    val legCheckoutsHit: Int = 0,
    val legHighestCheckout: Int = 0,
    val leg180s: Int = 0,
    val leg140Plus: Int = 0,
    val leg100Plus: Int = 0,
) : PlayerState
