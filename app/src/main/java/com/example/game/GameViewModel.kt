package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.data.AppDatabase
import com.example.game.data.PlayerEntity
import com.example.game.data.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class FallingBlock(
    val id: String = UUID.randomUUID().toString(),
    val xPercentage: Float, // 0.0 to 1.0 representing horizontal placement
    val yPercentage: Float = -0.1f, // starts slightly above screen
    val color: BlockColor,
    val speed: Float,
    val isTapped: Boolean = false
)

data class StatsDashboard(
    val totalPlayers: Int = 0,
    val globalHighestScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val averageScore: Double = 0.0,
    val top5Players: List<PlayerEntity> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = PlayerRepository(db.playerDao())
    private val prefs = application.getSharedPreferences("color_rush_prefs", Context.MODE_PRIVATE)
    private val soundSynth = RetroSoundSynth()
    private val vibrationHelper = VibrationHelper(application)

    private val _gameState = MutableStateFlow(GameState.START)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _highScore = MutableStateFlow(0)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _playerName = MutableStateFlow("Ninja")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _targetColor = MutableStateFlow(BlockColor.RED)
    val targetColor: StateFlow<BlockColor> = _targetColor.asStateFlow()

    private val _targetTimerFraction = MutableStateFlow(1f) // Timer circle or bar for target rotation
    val targetTimerFraction: StateFlow<Float> = _targetTimerFraction.asStateFlow()

    private val _blocks = MutableStateFlow<List<FallingBlock>>(emptyList())
    val blocks: StateFlow<List<FallingBlock>> = _blocks.asStateFlow()

    private val _gameOverReason = MutableStateFlow("")
    val gameOverReason: StateFlow<String> = _gameOverReason.asStateFlow()

    // Database Flows
    val players: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    val activePlayer: StateFlow<PlayerEntity?> = combine(repository.allPlayers, _activePlayerId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val statsDashboard: StateFlow<StatsDashboard> = repository.allPlayers.map { list ->
        val totalPlayers = list.size
        val globalHighestScore = list.maxOfOrNull { it.highestScore } ?: 0
        val totalGames = list.sumOf { it.totalGamesPlayed }
        val averageHighScore = if (list.isNotEmpty()) list.map { it.highestScore }.average() else 0.0
        val top5 = list.sortedByDescending { it.highestScore }.take(5)
        StatsDashboard(
            totalPlayers = totalPlayers,
            globalHighestScore = globalHighestScore,
            totalGamesPlayed = totalGames,
            averageScore = averageHighScore,
            top5Players = top5
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsDashboard())

    private var gameJob: Job? = null
    private var spawnAccumulator = 0f
    private var targetRotationAccumulator = 0f

    // Config constants
    private val targetMaxDuration = 6.0f // target changes every 6s automatically to keep players alert
    private val baseFallSpeed = 0.16f     // base vertical movement rate per sec
    private var lastTickTime = 0L

    init {
        viewModelScope.launch {
            try {
                // Ensure there is at least one profile upon first-time launch
                val list = repository.allPlayers.first()
                if (list.isEmpty()) {
                    val defaultNinja = PlayerEntity(
                        id = UUID.randomUUID().toString(),
                        username = "Ninja",
                        highestScore = 0,
                        totalGamesPlayed = 0,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                    repository.insertPlayer(defaultNinja)
                    _activePlayerId.value = defaultNinja.id
                } else {
                    val lastSavedId = prefs.getString("last_active_player_id", null)
                    if (lastSavedId != null && list.any { it.id == lastSavedId }) {
                        _activePlayerId.value = lastSavedId
                    } else {
                        _activePlayerId.value = list.firstOrNull()?.id
                    }
                }
            } catch (e: Exception) {
                // fallback gracefully
            }

            // Bind values dynamically
            activePlayer.collect { player ->
                if (player != null) {
                    _playerName.value = player.username
                    _highScore.value = player.highestScore
                    prefs.edit().putString("last_active_player_id", player.id).apply()
                }
            }
        }
    }

    fun selectPlayer(id: String) {
        _activePlayerId.value = id
    }

    fun createPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newPlayer = PlayerEntity(
                id = UUID.randomUUID().toString(),
                username = name.trim().take(15),
                highestScore = 0,
                totalGamesPlayed = 0,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            repository.insertPlayer(newPlayer)
            _activePlayerId.value = newPlayer.id
        }
    }

    fun deletePlayer(id: String) {
        viewModelScope.launch {
            repository.deletePlayer(id)
            if (_activePlayerId.value == id) {
                _activePlayerId.value = null
            }
        }
    }

    fun editPlayerName(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getPlayerById(id)
            if (existing != null) {
                val updated = existing.copy(
                    username = newName.trim().take(15),
                    lastPlayedTimestamp = System.currentTimeMillis()
                )
                repository.insertPlayer(updated)
            }
        }
    }


    fun startGame() {
        _score.value = 0
        _blocks.value = emptyList()
        _gameOverReason.value = ""
        _targetColor.value = BlockColor.random()
        _targetTimerFraction.value = 1f
        spawnAccumulator = 0f
        targetRotationAccumulator = 0f
        _gameState.value = GameState.PLAYING
        
        lastTickTime = System.currentTimeMillis()
        startLoop()
    }

    fun pauseGame() {
        if (_gameState.value == GameState.PLAYING) {
            _gameState.value = GameState.PAUSED
            stopLoop()
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.PAUSED) {
            _gameState.value = GameState.PLAYING
            lastTickTime = System.currentTimeMillis()
            startLoop()
        }
    }

    fun backToMenu() {
        stopLoop()
        _gameState.value = GameState.START
    }

    private fun startLoop() {
        stopLoop()
        gameJob = viewModelScope.launch {
            while (_gameState.value == GameState.PLAYING) {
                val now = System.currentTimeMillis()
                val delta = (now - lastTickTime) / 1000f
                lastTickTime = now

                updateGame(delta)
                delay(16L) // ~60 FPS update
            }
        }
    }

    private fun stopLoop() {
        gameJob?.cancel()
        gameJob = null
    }

    private fun updateGame(delta: Float) {
        // 1. Update Automatic Target Color Rotation
        targetRotationAccumulator += delta
        val fraction = (targetMaxDuration - targetRotationAccumulator) / targetMaxDuration
        _targetTimerFraction.value = fraction.coerceIn(0f, 1f)

        if (targetRotationAccumulator >= targetMaxDuration) {
            // Auto swap target color
            rotateTargetColor()
            targetRotationAccumulator = 0f
            _targetTimerFraction.value = 1f
        }

        // 2. Physics & Falling blocks
        val speedFactor = 1.0f + (_score.value / 10) * 0.15f
        val currentSpeed = (baseFallSpeed * speedFactor).coerceAtMost(0.55f)

        val currentBlocks = _blocks.value.map { block ->
            block.copy(yPercentage = block.yPercentage + currentSpeed * delta)
        }

        // Check for missed blocks reaching the bottom
        for (block in currentBlocks) {
            if (block.yPercentage >= 1.02f && !block.isTapped) {
                if (block.color == _targetColor.value) {
                    // Missed a target color -> Game Over!
                    triggerGameOver("You missed the ${block.color.displayName} block!")
                    return
                }
            }
        }

        // Filter out blocks that completely went off-screen
        _blocks.value = currentBlocks.filter { it.yPercentage < 1.05f }

        // 3. Spawning blocks
        val spawnInterval = (1.6f - (_score.value / 10) * 0.14f).coerceAtLeast(0.55f)
        spawnAccumulator += delta
        if (spawnAccumulator >= spawnInterval) {
            spawnBlock(currentSpeed)
            spawnAccumulator = 0f
        }
    }

    private fun spawnBlock(speed: Float) {
        // Avoid spawning outside 5% and 95% threshold to ensure they fit nicely
        val xHex = (5..85).random() / 100f

        // Ensure 40% probability of matching current target color so that players don't wait too long
        val chooseTarget = (0..9).random() < 4
        val blockColor = if (chooseTarget) {
            _targetColor.value
        } else {
            BlockColor.random()
        }

        val newBlock = FallingBlock(
            xPercentage = xHex,
            yPercentage = -0.1f,
            color = blockColor,
            speed = speed
        )

        _blocks.update { it + newBlock }
    }

    fun tapBlock(id: String) {
        if (_gameState.value != GameState.PLAYING) return

        val blocksList = _blocks.value
        val block = blocksList.find { it.id == id } ?: return
        if (block.isTapped) return

        // Check if color matches target color
        if (block.color == _targetColor.value) {
            // Correct tap!
            val newScore = _score.value + 1
            _score.value = newScore

            // Update in-memory and shared config high score
            if (newScore > _highScore.value) {
                _highScore.value = newScore
                val player = activePlayer.value
                if (player != null) {
                    viewModelScope.launch {
                        repository.insertPlayer(player.copy(highestScore = newScore))
                    }
                }
            }

            // Remove tapped block from list and play effects
            _blocks.update { list -> list.filter { it.id != id } }

            // Level Up triggers every 10 points
            if (newScore % 10 == 0) {
                soundSynth.playLevelUp()
            } else {
                soundSynth.playTapCorrect()
            }
            vibrationHelper.vibrateSuccess()

            // When a block is successfully tapped, immediately pick a new random target color!
            rotateTargetColor()
        } else {
            // Tapped wrong color -> Game Over!
            triggerGameOver("You tapped ${block.color.displayName} instead of ${_targetColor.value.displayName}!")
        }
    }

    private fun rotateTargetColor() {
        val current = _targetColor.value
        _targetColor.value = BlockColor.random(exclude = current)
        targetRotationAccumulator = 0f
        _targetTimerFraction.value = 1f
    }

    private fun triggerGameOver(reason: String) {
        stopLoop()
        _gameOverReason.value = reason
        _gameState.value = GameState.GAME_OVER
        soundSynth.playTapWrong()
        vibrationHelper.vibrateWrong()

        val player = activePlayer.value
        if (player != null) {
            viewModelScope.launch {
                val finalHighScore = if (_score.value > player.highestScore) _score.value else player.highestScore
                repository.insertPlayer(
                    player.copy(
                        highestScore = finalHighScore,
                        totalGamesPlayed = player.totalGamesPlayed + 1,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

enum class GameState {
    START, PLAYING, PAUSED, GAME_OVER
}
