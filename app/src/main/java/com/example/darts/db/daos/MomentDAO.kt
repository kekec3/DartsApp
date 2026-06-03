package com.example.darts.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.darts.db.entities.Moment
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDAO {

    @Query("SELECT * FROM moments")
    fun getAllMoments() : Flow<List<Moment>>

    @Query("SELECT * FROM moments WHERE idGame = :gameId")
    fun getMomentsForGame(gameId: Int) : Flow<List<Moment>>

    @Query("SELECT * FROM moments WHERE  moments.type = 'PHOTO'")
    fun getPhotos(): Flow<List<Moment>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: Moment)
}