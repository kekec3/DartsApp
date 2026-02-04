package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player (
    @PrimaryKey(autoGenerate = true) val idPlayer: Int = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "avatar") val avatar: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (idPlayer != other.idPlayer) return false
        if (username != other.username) return false
        if (!avatar.contentEquals(other.avatar)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idPlayer
        result = 31 * result + username.hashCode()
        result = 31 * result + avatar.contentHashCode()
        return result
    }
}