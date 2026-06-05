package com.example.game.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val username: String,
    val highestScore: Int,
    val totalGamesPlayed: Int,
    val lastPlayedTimestamp: Long
)
