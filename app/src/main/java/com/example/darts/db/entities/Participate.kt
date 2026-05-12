package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "participate",
    primaryKeys = ["idPlayer", "idBattle"],
    foreignKeys = [
        ForeignKey(
            entity = Battle::class,
            parentColumns = ["idBattle"],
            childColumns = ["idBattle"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["idPlayer"],
            childColumns = ["idPlayer"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("idPlayer"),
        Index("idBattle")
    ])
data class Participate (
    val idPlayer: Int,
    val idBattle: Int,
    @ColumnInfo(name = "outcome") val outcome: Boolean = false
)