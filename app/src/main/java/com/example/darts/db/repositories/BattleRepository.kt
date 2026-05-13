package com.example.darts.db.repositories

import com.example.darts.db.daos.BattleDAO
import com.example.darts.db.daos.ParticipateDAO
import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Participate
import com.example.darts.db.entities.Player
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BattleRepository @Inject constructor(
    private val battleDao: BattleDAO,
    private val participateDao: ParticipateDAO,
    private val playerDao: PlayerDAO
) {
    // Get all available players for selection
    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    suspend fun createBattleWithPlayers(name: String, playerIds: List<Int>) {
        val battle = Battle(
            name = name,
            dateCreated = System.currentTimeMillis().toString()
        )
        val battleId = battleDao.insertBattle(battle).toInt()

        for(p in playerIds){
            participateDao.addParticipation(Participate(idBattle = battleId, idPlayer = p))
        }
    }

    suspend fun insertPlayer(player:Player) : Long{
        return playerDao.addPlayer(player)
    }

    fun getAllBattles(): Flow<List<Battle>> = battleDao.getAllBattles()

    fun getPlayersOfBattle(id: Int) : Flow<List<Player>> = battleDao.getPlayersInBattle(id)
}