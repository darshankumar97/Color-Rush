package com.example.game.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayersFlow()

    suspend fun insertPlayer(player: PlayerEntity) {
        playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(id: String) {
        playerDao.deletePlayer(id)
    }

    suspend fun getPlayerById(id: String): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }
}
