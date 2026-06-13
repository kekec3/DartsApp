package com.example.darts.repository

import com.example.darts.db.daos.*
import com.example.darts.db.dao.StatDao
import com.example.darts.db.entities.*
import com.example.darts.dto.PlayerExportPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DartsExportRepository @Inject constructor(
    private val playerDao: PlayerDAO,
    private val battleDao: BattleDAO,
    private val gameDao: GameDAO,
    private val momentDao: MomentDAO,
    private val participateDao: ParticipateDAO,
    private val statDao: StatDao
) {
    /**
     * Feeds the user drop-down menu selection UI safely with a hot updates stream
     */
    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    /**
     * Gathers every database record associated with a single player ID
     * to package it into a single object payload.
     */
    suspend fun getExportPayloadForPlayer(playerId: Int): PlayerExportPayload {
        // 1. Fetch player metadata
        val player = playerDao.getPlayerById(playerId)
            ?: throw IllegalArgumentException("Player with ID $playerId not found.")

        // 2. Fetch career totals
        val careerStats = statDao.getCareerStats(playerId)

        // 3. Fetch all matches (participations) this user joined
        val participations = participateDao.getParticipationsByPlayerIdDirect(playerId)

        val targetBattleIds = participations.map { it.idBattle }.distinct()

        // 4. Fetch the respective battle entries
        val battles = if (targetBattleIds.isNotEmpty()) {
            battleDao.getBattlesByIds(targetBattleIds)
        } else {
            emptyList()
        }

        // 5. Fetch games associated with the player's battles
        // Using your existing gameDao flow query, collecting the current snapshot
        val games = gameDao.getAllGamesByPlayer(playerId)?.firstOrNull() ?: emptyList()
        val targetGameIds = games.map { it.idGame }.distinct()

        // 6. Gather all moments and leg breakdowns tied to those games
        val moments = mutableListOf<Moment>()
        val legStats = if (targetGameIds.isNotEmpty()) {
            statDao.getLegStatsForGames(targetGameIds)
        } else {
            emptyList()
        }

        for (gameId in targetGameIds) {
            // Collecting the first snapshot from the flow stream
            val gameMoments = momentDao.getMomentsForGame(gameId).firstOrNull()
            if (gameMoments != null) {
                moments.addAll(gameMoments)
            }
        }

        return PlayerExportPayload(
            player = player,
            careerStats = careerStats,
            battles = battles,
            participations = participations,
            games = games,
            moments = moments,
            legStats = legStats
        )
    }
}