package com.example.darts.db.repositories

import com.example.darts.db.dao.StatDao
import com.example.darts.db.entities.PlayerCareerStats
import com.example.darts.db.entities.PlayerLegStats
import com.example.darts.viewModel.states.PlayerStateX01
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatRepository @Inject constructor(
    private val statDao: StatDao,
) {
    // ── Leg stats ─────────────────────────────────────────────────────────

    /**
     * Convert a list of [PlayerStateX01] snapshots (from [GameState.completedLegStats])
     * into [PlayerLegStats] rows and persist them.
     *
     * Returns the saved rows so the caller can immediately forward them to
     * [updateCareerStats] without a redundant DB read.
     *
     * @param winnerIndex index into [playerStates] identifying who won the leg
     */
    suspend fun saveLegStats(rows: List<PlayerLegStats>): List<PlayerLegStats> {
        statDao.insertLegStats(rows)
        return rows
    }

    suspend fun getLegStats(gameId: Int, legNumber: Int): List<PlayerLegStats> =
        statDao.getLegStats(gameId, legNumber)

    suspend fun getMatchStats(gameId: Int): List<PlayerLegStats> =
        statDao.getAllLegStatsForGame(gameId)

    // ── Career stats ──────────────────────────────────────────────────────

    /**
     * Add a completed leg's stats into each player's lifetime totals.
     *
     * Call this once per leg, passing the rows returned from [saveLegStats].
     * Set [isMatchEnd] = true only on the last leg of the match.
     *
     * @param matchWinnerId idPlayer of the match winner (used only when [isMatchEnd] = true)
     */
    suspend fun updateCareerStats(
        legStats: List<PlayerLegStats>,
        isMatchEnd: Boolean,
        matchWinnerId: Int? = null,
    ) {
        legStats.forEach { leg ->
            val isMatchWinner = isMatchEnd && leg.playerId == matchWinnerId
            statDao.upsertCareerStats(leg.playerId) { existing ->
                existing.copy(
                    matchesPlayed = existing.matchesPlayed + if (isMatchEnd) 1 else 0,
                    matchesWon = existing.matchesWon + if (isMatchWinner) 1 else 0,
                    legsPlayed = existing.legsPlayed + 1,
                    legsWon = existing.legsWon + if (leg.won) 1 else 0,
                    totalDartsThrown = existing.totalDartsThrown + leg.dartsThrown,
                    totalScored = existing.totalScored + leg.totalScored,
                    checkoutsAttempted = existing.checkoutsAttempted + leg.checkoutAttempts,
                    checkoutsHit = existing.checkoutsHit + leg.checkoutsHit,
                    highestCheckout = maxOf(existing.highestCheckout, leg.highestCheckout),
                    scores180 = existing.scores180 + leg.scores180,
                    scores140Plus = existing.scores140Plus + leg.scores140Plus,
                    scores100Plus = existing.scores100Plus + leg.scores100Plus,
                )
            }
        }
    }

    fun observeCareerStats(playerId: Int): Flow<PlayerCareerStats?> =
        statDao.observeCareerStats(playerId)
}