package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class GameViewModel(application: Application) : AndroidViewModel(application) {
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

    private var gameJob: Job? = null
    private var spawnAccumulator = 0f
    private var targetRotationAccumulator = 0f

    // Config constants
    private val targetMaxDuration = 6.0f // target changes every 6s automatically to keep players alert
    private val baseFallSpeed = 0.16f     // base vertical movement rate per sec
    private var lastTickTime = 0L

    init {
        val lastSavedName = prefs.getString("last_player_name", "Ninja") ?: "Ninja"
        _playerName.value = lastSavedName
        _highScore.value = prefs.getInt("high_score_${lastSavedName.trim().lowercase()}", 0)
    }

    fun setPlayerName(name: String) {
        val cleanName = name.take(15) // limit name size to prevent overflow
        _playerName.value = cleanName
        prefs.edit().putString("last_player_name", cleanName).apply()
        
        val key = "high_score_${cleanName.trim().lowercase()}"
        _highScore.value = prefs.getInt(key, 0)
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
                val key = "high_score_${_playerName.value.trim().lowercase()}"
                prefs.edit().putInt(key, newScore).apply()
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
    }
}

enum class GameState {
    START, PLAYING, PAUSED, GAME_OVER
}
