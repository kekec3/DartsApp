package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.darts.db.entities.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDAO {

    @Query("SELECT * FROM games")
    fun getAllGames() : Flow<List<Game>>?

    @Query("SELECT * FROM games g JOIN participate p ON g.idBattle = p.idBattle WHERE p.idPlayer = :player")
    fun getAllGamesByPlayer(player: Int) : Flow<List<Game>>?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGame(game: Game)
}