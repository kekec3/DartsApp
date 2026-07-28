package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.darts.db.entities.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDAO {

    @Query("SELECT * FROM players WHERE username NOT LIKE 'Guest%' ORDER BY username ASC")
    fun getAllPlayers() : Flow<List<Player>>

    @Query("SELECT * FROM players WHERE username = :username LIMIT 1")
    suspend fun getPlayerByUsername(username: String): Player?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlayer(player: Player) : Long

    @Query("SELECT * FROM players WHERE idPlayer = :playerId")
    suspend fun getPlayerById(playerId: Int): Player?


}