package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = Battle::class,
            parentColumns = ["idBattle"],
            childColumns = ["idBattle"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idBattle")]
)
data class Game (
    @PrimaryKey(autoGenerate = true) val idGame: Int = 0,
    @ColumnInfo(name = "idBattle") val idBattle: Int, // Added this link
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "finished") val finished: Boolean = false,
    @ColumnInfo(name = "history") val history: String? = null
)