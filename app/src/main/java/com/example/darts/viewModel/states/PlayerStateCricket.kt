package com.example.darts.viewModel.states

import com.example.darts.db.entities.Player

data class CricketNumber(
    val marks: Int = 0
)

data class PlayerStateCricket(
    val player: Player,
    val numbers: Map<Int, CricketNumber> = mapOf(
        15 to CricketNumber(),
        16 to CricketNumber(),
        17 to CricketNumber(),
        18 to CricketNumber(),
        19 to CricketNumber(),
        20 to CricketNumber(),
        25 to CricketNumber()
    ),
    val score: Int = 0,
    val legsVon: Int = 0,
) : PlayerState
