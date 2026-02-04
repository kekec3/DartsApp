package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "moments",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["idGame"],
            childColumns = ["idGame"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idGame")])
data class Moment (
    @PrimaryKey(autoGenerate = true) val idMoment: Int = 0,
    @ColumnInfo(name = "idGame") val idGame: Int,
    @ColumnInfo(name = "image") val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Moment

        if (idMoment != other.idMoment) return false
        if (idGame != other.idGame) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idMoment
        result = 31 * result + idGame
        result = 31 * result + image.contentHashCode()
        return result
    }
}