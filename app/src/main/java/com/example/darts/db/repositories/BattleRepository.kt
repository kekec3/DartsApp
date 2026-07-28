package com.example.darts.db.repositories

import com.example.darts.db.daos.BattleDAO
import com.example.darts.db.daos.ParticipateDAO
import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Participate
import com.example.darts.db.entities.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BattleRepository @Inject constructor(
    private val battleDao: BattleDAO,
    private val participateDao: ParticipateDAO,
    private val playerDao: PlayerDAO
) {
    // Get all available players for selection
    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    suspend fun createBattleWithPlayers(name: String, playerIds: List<Int>): Int {
        val battle = Battle(
            name = name,
            dateCreated = System.currentTimeMillis().toString()
        )
        val battleId = battleDao.insertBattle(battle).toInt()

        for(p in playerIds){
            participateDao.addParticipation(Participate(idBattle = battleId, idPlayer = p))
        }
        return battleId
    }

    suspend fun insertPlayer(player:Player) : Long{
        return playerDao.addPlayer(player)
    }

    fun getAllBattles(): Flow<List<Battle>> = battleDao.getAllBattles()

    suspend fun getPlayersOfBattle(id: Int): List<Player> = battleDao.getPlayersInBattle(id).first()

    suspend fun  ensureGuestEntitiesExist() {
        val exists = playerDao.getPlayerByUsername("Guest1") != null
        if (exists)
            return

        val guestIds = mutableListOf<Int>()
        for (i in 1..4) {
            val  id =playerDao.addPlayer(Player(username = "Guest$i", avatar = "default_avatar.png")).toInt()
            guestIds.add(id)
        }

        for (i in 2..4) {
            val battle = Battle(name = "QuickPlay${i}", dateCreated = System.currentTimeMillis().toString())
            val battleId = battleDao.insertBattle(battle).toInt()

            for (j in 0 until i) {
                participateDao.addParticipation(Participate(idBattle = battleId, idPlayer = guestIds[j]))
            }
        }
    }

    suspend fun getQuickPlayBattleId(playerCount: Int): Int {
        ensureGuestEntitiesExist()
        val battleName = "QuickPlay$playerCount"
        return battleDao.getBattleByName(battleName)?.idBattle ?: run {
            val guestIds = mutableListOf<Int>()
            for (i in 1..playerCount) {
                playerDao.getPlayerByUsername("Guest$i")?.let { guestIds.add(it.idPlayer) }
            }
            createBattleWithPlayers(battleName, guestIds)
        }
    }
}