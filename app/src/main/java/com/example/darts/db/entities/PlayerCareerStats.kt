package com.example.darts.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Running lifetime totals for a player, updated at the end of every leg.
 * Uses playerId as the primary key — one row per player, always upserted.
 *
 * Derived stats (computed in the ViewModel or UI layer, not stored):
 *   • Win rate          = matchesWon / matchesPlayed
 *   • Career average    = (totalScored / totalDartsThrown) * 3
 *   • Checkout %        = checkoutsHit / checkoutsAttempted
 */
@Entity(
    tableName = "player_career_stats",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["idPlayer"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlayerCareerStats(
    @PrimaryKey
    val playerId: Int,

    // Match-level
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,

    // Leg-level
    val legsPlayed: Int = 0,
    val legsWon: Int = 0,

    // Scoring
    val totalDartsThrown: Int = 0,
    val totalScored: Int = 0,

    // Checkout
    val checkoutsAttempted: Int = 0,
    val checkoutsHit: Int = 0,
    val highestCheckout: Int = 0,

    // Milestones
    val scores180: Int = 0,
    val scores140Plus: Int = 0,
    val scores100Plus: Int = 0,
)