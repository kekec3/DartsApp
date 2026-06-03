package com.example.darts.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class MomentType{
    PHOTO, AUDIO, EMOJI
}

class MomentConverters {
    @TypeConverter
    fun fromMomentType(value: MomentType): String = value.name

    @TypeConverter
    fun toMomentType(value: String): MomentType = MomentType.valueOf(value)
}

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
@TypeConverters(MomentConverters::class)
data class Moment (
    @PrimaryKey(autoGenerate = true) val idMoment: Int = 0,
    @ColumnInfo(name = "idGame") val idGame: Int,
    @ColumnInfo(name = "type") val type: MomentType,
    @ColumnInfo(name = "contentValue") val contentValue: String
)