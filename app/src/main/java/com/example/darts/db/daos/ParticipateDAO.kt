package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.darts.db.entities.Participate

@Dao
interface ParticipateDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addParticipation(participate: Participate)
}