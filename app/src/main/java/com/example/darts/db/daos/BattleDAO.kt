package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleDAO {
    // Keep your existing methods...
    @Insert
    suspend fun insertBattle(battle: Battle): Long

    @Query("SELECT * FROM battles WHERE name NOT LIKE 'QuickPlay%' ORDER BY idBattle DESC")
    fun getAllBattles(): Flow<List<Battle>>

    @Query("SELECT p.* FROM players p JOIN participate pa ON p.idPlayer = pa.idPlayer WHERE pa.idBattle = :battleId")
    fun getPlayersInBattle(battleId: Int): Flow<List<Player>>

    // ADD THIS HELPER FOR EXPORT:
    @Query("SELECT * FROM battles WHERE idBattle IN (:battleIds)")
    suspend fun getBattlesByIds(battleIds: List<Int>): List<Battle>

    @Query("SELECT * FROM battles WHERE name = :name LIMIT 1")
    suspend fun getBattleByName(name: String): Battle?
}