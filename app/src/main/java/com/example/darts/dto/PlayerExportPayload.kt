package com.example.darts.dto

import com.example.darts.db.entities.*


data class MomentAttachment(
    val idMoment: Int, // Connects the file back to its specific DB record
    val momentType: String, // "PHOTO" or "AUDIO"
    val fileName: String,   // e.g., "moment_123.jpg"
    val base64Data: String  // The actual binary file converted to string
)

data class PlayerExportPayload(
    val players: List<Player>,
    val careerStats: List<PlayerCareerStats> = emptyList(),
    val battles: List<Battle>,
    val participations: List<Participate>,
    val games: List<Game>,
    val moments: List<Moment>,
    val legStats: List<PlayerLegStats>,
    // ADD THIS: Holds all physical image and audio binaries
    val attachments: List<MomentAttachment> = emptyList()
)