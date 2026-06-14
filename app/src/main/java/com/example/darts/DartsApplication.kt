package com.example.darts

import android.app.Application
import com.example.darts.db.repositories.GameRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DartsApplication : Application() {

    @Inject
    lateinit var gameRepository: GameRepository

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            gameRepository.cleanupUnfinishedGames()
        }
    }
}