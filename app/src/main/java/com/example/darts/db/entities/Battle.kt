package com.example.darts.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battles")
data class Battle(
    @PrimaryKey(autoGenerate = true) val idBattle: Int = 0,
    val name: String,
    val dateCreated: String
)