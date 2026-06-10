package com.example.darts.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the final stats for one player in one leg.
 * Written once at leg end — never updated afterwards.
 *
 * Relationships:
 *   gameId  → Game.idGame   (one game has many legs)
 *   playerId → Player.idPlayer
 *
 * Reading patterns:
 *   • LegSummaryScreen  → getLegStats(gameId, legNumber)
 *   • MatchSummaryScreen → getAllLegStatsForGame(gameId), then aggregate per player
 */
@Entity(
    tableName = "player_leg_stats",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["idGame"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["idPlayer"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("gameId"),
        Index("playerId"),
        Index(value = ["gameId", "legNumber"]),
    ],
)
data class PlayerLegStats(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val gameId: Int,
    val playerId: Int,
    val legNumber: Int,
    val won: Boolean,

    // Scoring
    val dartsThrown: Int,
    val totalScored: Int,
    val average: Float,         // (totalScored / dartsThrown) * 3 -> 3-dart average // (totalMarks / dartsThrown) * 3 -> MPR

    // Checkout
    val checkoutAttempts: Int,
    val checkoutsHit: Int,      // 0 or 1 per leg
    val highestCheckout: Int,

    // Milestones
    val scores180: Int,
    val scores140Plus: Int,     // 140 – 179
    val scores100Plus: Int,     // 100 – 139
)