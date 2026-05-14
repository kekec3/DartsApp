package com.example.darts.db.repositories

import com.example.darts.db.daos.GameDAO
import com.example.darts.db.entities.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDAO: GameDAO
) {
    // Get all games specifically for the current Battle
    fun getGamesByBattle(battleId: Int): Flow<List<Game>> {
        // You might need to add this specific query to your GameDAO
        // For now, we can filter from all or add a specific DAO method
        return gameDAO.getAllGamesByBattle(battleId)
    }

    suspend fun createNewGame(game: Game): Int {
        return gameDAO.addGame(game).toInt()
    }

    fun getGamesForPlayer(playerId: Int): Flow<List<Game>>? {
        return gameDAO.getAllGamesByPlayer(playerId)
    }

    suspend fun updateGameLocation(gameId: Int, coords: String) {
        gameDAO.updateLocation(gameId, coords)
    }
}