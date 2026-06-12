package com.example.darts.dto

import com.example.darts.db.entities.*

data class PlayerExportPayload(
    val player: Player,
    val careerStats: PlayerCareerStats?,
    val battles: List<Battle>,
    val participations: List<Participate>,
    val games: List<Game>,
    val moments: List<Moment>,
    val legStats: List<PlayerLegStats>
)