package com.example.darts.db.hilt

import android.content.Context
import com.example.darts.db.DartsDatabase
import com.example.darts.db.daos.BattleDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DartsDatabase {
        return DartsDatabase.getDatabase(context)
    }
    @Provides
    fun providePlayerDao(db: DartsDatabase) = db.playerDao()
    @Provides
    fun provideGameDao(db: DartsDatabase) = db.gameDao()
    @Provides
    fun provideMomentDao(db: DartsDatabase) = db.momentDao()
    @Provides
    fun provideParticipateDao(db: DartsDatabase) = db.participateDao()
    @Provides
    fun provideBattleDao(db: DartsDatabase) =  db.battleDao()

    @Provides
    fun provideStatDao(db: DartsDatabase) = db.statDao()
}