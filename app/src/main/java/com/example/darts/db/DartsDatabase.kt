package com.example.darts.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.example.darts.db.daos.GameDAO
import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.Game
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.Participate
import com.example.darts.db.entities.Player

@Database(entities = arrayOf(Player::class, Game::class, Participate::class, Moment::class), version = 1, exportSchema = false)
abstract class DartsDatabase: RoomDatabase() {

    abstract fun playerDao() : PlayerDAO
    abstract fun gameDao() : GameDAO

    companion object {

        private var instance: DartsDatabase? = null

        fun getDatabase(context: Context) : DartsDatabase {
            return instance?: synchronized(this) {
                val go = Room.databaseBuilder(
                    context.applicationContext,
                    DartsDatabase::class.java,
                    "dartsDatabase"
                ).addCallback(DartsDatabaseCallback()).build()
                instance = go
                return@synchronized go
            }
        }
    }

    private class DartsDatabaseCallback() : RoomDatabase.Callback() {

        override fun onOpen(connection: SQLiteConnection) {
            super.onOpen(connection)
        }
    }
}