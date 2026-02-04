package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "participate",
    primaryKeys = ["idPlayer", "idGame"],
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["idGame"],
            childColumns = ["idGame"],
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
        Index("idGame")
    ])
data class Participate (
    val idPlayer: Int,
    val idGame: Int,
    @ColumnInfo(name = "outcome") val outcome: Boolean
)