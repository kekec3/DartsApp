package com.example.darts.db.dao

import androidx.room.*
import com.example.darts.db.entities.Participate
import com.example.darts.db.entities.PlayerCareerStats
import com.example.darts.db.entities.PlayerLegStats
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class (not interface) so we can write the upsertCareerStats
 * transaction method with real logic in the body.
 */
@Dao
abstract class StatDao {

    // ── Leg stats ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLegStats(stats: List<PlayerLegStats>)

    /** All rows for a single leg — typically 2 rows (one per player). */
    @Query("""
        SELECT * FROM player_leg_stats
        WHERE gameId = :gameId AND legNumber = :legNumber
    """)
    abstract suspend fun getLegStats(gameId: Int, legNumber: Int): List<PlayerLegStats>

    /** All legs for a game, ordered — used to build the match summary. */
    @Query("""
        SELECT * FROM player_leg_stats
        WHERE gameId = :gameId
        ORDER BY legNumber ASC
    """)
    abstract suspend fun getAllLegStatsForGame(gameId: Int): List<PlayerLegStats>

    /**
     * All legs for a player across all games, in chronological order.
     * Used to build the per-leg average trend chart.
     * gameId is a reliable proxy for game creation order.
     */
    @Query("""
        SELECT * FROM player_leg_stats
        WHERE playerId = :playerId
        ORDER BY gameId ASC, legNumber ASC
    """)
    abstract fun observeLegStatsForPlayer(playerId: Int): Flow<List<PlayerLegStats>>

    // ── Career stats ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrReplaceCareerStats(stats: PlayerCareerStats)

    @Query("SELECT * FROM player_career_stats WHERE playerId = :playerId")
    abstract suspend fun getCareerStats(playerId: Int): PlayerCareerStats?

    @Query("SELECT * FROM player_career_stats WHERE playerId = :playerId")
    abstract fun observeCareerStats(playerId: Int): Flow<PlayerCareerStats?>

    /**
     * Read-modify-write in a single transaction.
     * Creates a fresh row if the player has no career stats yet.
     */
    @Transaction
    open suspend fun upsertCareerStats(
        playerId: Int,
        update: (PlayerCareerStats) -> PlayerCareerStats,
    ) {
        val existing = getCareerStats(playerId) ?: PlayerCareerStats(playerId = playerId)
        insertOrReplaceCareerStats(update(existing))
    }

    @Query("SELECT * FROM player_leg_stats WHERE gameId IN (:gameIds)")
    abstract suspend fun getLegStatsForGames(gameIds: List<Int>): List<PlayerLegStats>
}