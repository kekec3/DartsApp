package com.example.darts.db.repositories

import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.Player
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDAO
) {

    fun getAllPlayers(): Flow<List<Player>> =
        playerDao.getAllPlayers()

    suspend fun getPlayerById(
        playerId: Int
    ): Player? =
        playerDao.getPlayerById(playerId)
}