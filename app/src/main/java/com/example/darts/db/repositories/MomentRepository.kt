package com.example.darts.db.repositories

import com.example.darts.db.daos.MomentDAO
import com.example.darts.db.entities.Moment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MomentRepository @Inject constructor(
    private val momentDAO: MomentDAO
) {
    fun getAllMoments(): Flow<List<Moment>> = momentDAO.getAllMoments()

    fun getMomentsForGame(gameId: Int): Flow<List<Moment>> = momentDAO.getMomentsForGame(gameId)

    suspend fun saveMoment(moment: Moment) {
        momentDAO.insertMoment(moment)
    }
}