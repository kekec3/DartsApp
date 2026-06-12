package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.darts.db.entities.Participate
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipateDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addParticipation(participate: Participate)

    @Query("SELECT * FROM participate WHERE idPlayer = :playerId")
    fun getParticipationsByPlayerId(playerId: Int): Flow<List<Participate>>

    // ADD THIS HELPER FOR EXPORT:
    @Query("SELECT * FROM participate WHERE idPlayer = :playerId")
    suspend fun getParticipationsByPlayerIdDirect(playerId: Int): List<Participate>
}