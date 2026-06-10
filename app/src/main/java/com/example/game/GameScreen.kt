package com.example.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.random.Random

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.game.data.PlayerEntity

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState(initial = GameState.START)
    val score by viewModel.score.collectAsState(initial = 0)
    val highScore by viewModel.highScore.collectAsState(initial = 0)
    val targetColor by viewModel.targetColor.collectAsState(initial = BlockColor.RED)
    val targetTimerFraction by viewModel.targetTimerFraction.collectAsState(initial = 1f)
    val blocks by viewModel.blocks.collectAsState(initial = emptyList())
    val gameOverReason by viewModel.gameOverReason.collectAsState(initial = "")
    val playerName by viewModel.playerName.collectAsState(initial = "Ninja")

    val statsDashboard by viewModel.statsDashboard.collectAsState()
    val globalBest = statsDashboard.globalHighestScore

    // Background Gradient color schemes based on Theme mode
    val isSystemDark = isSystemInDarkTheme()
    val bgGradient = if (isSystemDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F0E17),
                Color(0xFF1E1B2E),
                Color(0xFF0B1426)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }

    val primaryTextColor = if (isSystemDark) Color.White else Color(0xFF0F0E17)
    val secondaryTextColor = if (isSystemDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Star particle ambience
        AmbientDustedStars()

        when (gameState) {
            GameState.START -> {
                StartScreenContent(
                    viewModel = viewModel,
                    textColor = primaryTextColor,
                    subColor = secondaryTextColor
                )
            }
            GameState.PLAYING, GameState.PAUSED -> {
                val gameBlur = if (gameState == GameState.PAUSED) 4.dp else 0.dp
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(gameBlur)
                ) {
                    // Header HUD with current score, pause button, and high score
                    GameHeaderHud(
                        score = score,
                        personalBest = highScore,
                        globalBest = globalBest,
                        onPauseClick = { viewModel.pauseGame() },
                        textColor = primaryTextColor,
                        subColor = secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Color card
                    TargetColorBar(
                        targetColor = targetColor,
                        timerFraction = targetTimerFraction
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Physics Game Canvas Constraint Area
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val boxWidth = maxWidth
                        val boxHeight = maxHeight

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Using standard Kotlin `for` loop to support @Composable child items without lambda compilation issues
                            for (block in blocks) {
                                key(block.id) {
                                    FallingGemItem(
                                        block = block,
                                        onTap = { viewModel.tapBlock(block.id) },
                                        boxWidth = boxWidth,
                                        boxHeight = boxHeight
                                    )
                                }
                            }
                        }
                    }
                }

                if (gameState == GameState.PAUSED) {
                    PausedOverlay(
                        onResumeClick = { viewModel.resumeGame() },
                        onRestartClick = { viewModel.startGame() },
                        onHomeClick = { viewModel.backToMenu() }
                    )
                }
            }
            GameState.GAME_OVER -> {
                GameOverScreenContent(
                    playerName = playerName,
                    score = score,
                    highScore = highScore,
                    reason = gameOverReason,
                    onPlayAgainClick = { viewModel.startGame() },
                    onHomeClick = { viewModel.backToMenu() },
                    textColor = primaryTextColor,
                    subColor = secondaryTextColor
                )
            }
        }
    }
}

@Composable
fun GameHeaderHud(
    score: Int,
    personalBest: Int,
    globalBest: Int,
    onPauseClick: () -> Unit,
    textColor: Color,
    subColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SCORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = subColor,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "$score",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                modifier = Modifier.testTag("current_score_hud")
            )
        }

        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(textColor.copy(alpha = 0.08f))
                .testTag("pause_button")
        ) {
            PauseIcon(tint = textColor)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "PERSONAL BEST",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = subColor,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Personal Star",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$personalBest",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.testTag("high_score_hud")
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ALL-TIME BEST",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0A84FF),
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "All-Time Star",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$globalBest",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun PauseIcon(tint: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(tint, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(tint, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun TargetColorBar(
    targetColor: BlockColor,
    timerFraction: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = targetColor.color.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.5.dp, targetColor.color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TAP THIS COLOR ONLY",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = targetColor.color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = targetColor.displayName.uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = targetColor.color,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("target_color_name")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Depleting countdown progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(targetColor.color.copy(alpha = 0.15f))
            ) {
                val animatedFraction by animateFloatAsState(
                    targetValue = timerFraction,
                    animationSpec = tween(150, easing = LinearEasing),
                    label = "timerWidth"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(targetColor.color, targetColor.color.copy(alpha = 0.6f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun FallingGemItem(
    block: FallingBlock,
    onTap: () -> Unit,
    boxWidth: Dp,
    boxHeight: Dp
) {
    val size = 62.dp
    
    // Position conversion relative to layout box size
    val xOffset = (boxWidth - size) * block.xPercentage
    val yOffset = boxHeight * block.yPercentage

    // Glowing heartbeat heartbeat vector pulse effect
    val pulseTransition = rememberInfiniteTransition(label = "blockPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .size(size)
            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            .clip(RoundedCornerShape(18.dp))
            .background(block.color.color.copy(alpha = 0.18f))
            .border(3.2.dp, block.color.color, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Disable full ripple container to prevent performance drop on low-end emulators
                onClick = onTap
            )
            .testTag("block_${block.color.name}"),
        contentAlignment = Alignment.Center
    ) {
        // Inner diamond visual jewel accent
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer(rotationZ = 45f)
                .background(block.color.color.copy(alpha = 0.45f))
        )
        // Capital letter representative tag
        Text(
            text = block.color.displayName.first().toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AmbientDustedStars() {
    val starsList = remember {
        List(25) {
            Offset(Random.nextFloat(), Random.nextFloat())
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        starsList.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = Random.nextInt(4, 9).toFloat(),
                center = Offset(star.x * width, star.y * height)
            )
        }
    }
}

@Composable
fun StartScreenContent(
    viewModel: GameViewModel,
    textColor: Color,
    subColor: Color
) {
    val players by viewModel.players.collectAsState()
    val activePlayer by viewModel.activePlayer.collectAsState()
    val statsDashboard by viewModel.statsDashboard.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = PLAYERS, 1 = LEADERBOARD, 2 = STATS

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PlayerEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<PlayerEntity?>(null) }

    var newPlayerName by remember { mutableStateOf("") }
    var editPlayerNameText by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header Banner
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD60A), Color(0xFFFF453A), Color(0xFF03A9F4))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Game Logo Core",
                tint = Color.White,
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer(rotationZ = 12f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "COLOR RUSH",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = textColor,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Text(
            text = "THE REACTION ARCADE CLASSIC",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF30D158),
            letterSpacing = 2.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs using Segmented Chips or sleek Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(textColor.copy(alpha = 0.05f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("👤 PROFILES", "🏆 HALL OF FAME", "🏅 MILESTONES")
            tabs.forEachIndexed { index, title ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent)
                        .clickable { activeTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else subColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Tab Content Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                0 -> {
                    // PLAYERS TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT PLAYER PROFILE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = subColor,
                                letterSpacing = 1.sp
                            )
                            
                            // Add button
                            OutlinedButton(
                                onClick = {
                                    newPlayerName = ""
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF30D158)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF30D158)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("add_player_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (players.isEmpty()) {
                            // Empty State
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = subColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No players registered.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap 'ADD' to build your game profile!",
                                        fontSize = 12.sp,
                                        color = subColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(players) { player ->
                                    val isActive = activePlayer?.id == player.id
                                    val borderStroke = if (isActive) {
                                        BorderStroke(2.dp, Color(0xFF0A84FF))
                                    } else {
                                        BorderStroke(1.dp, textColor.copy(alpha = 0.08f))
                                    }
                                    val cardBgColor = if (isActive) {
                                        Color(0xFF0A84FF).copy(alpha = 0.05f)
                                    } else {
                                        textColor.copy(alpha = 0.03f)
                                    }

                                    Card(
                                        onClick = { viewModel.selectPlayer(player.id) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                        border = borderStroke,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("player_card_${player.username}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar badge
                                            val nameLetter = player.username.firstOrNull()?.toString()?.uppercase() ?: "P"
                                            val avatarColors = listOf(Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFFD60A), Color(0xFFFF453A), Color(0xFFBF5AF2))
                                            val colorIndex = Math.abs(player.username.hashCode()) % avatarColors.size
                                            val avatarColor = avatarColors[colorIndex]

                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(avatarColor.copy(alpha = 0.15f))
                                                    .border(1.5.dp, avatarColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = nameLetter,
                                                    color = avatarColor,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = player.username,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                    if (isActive) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFF0A84FF))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                color = Color.White,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Best: ${player.highestScore} pts  •  Played: ${player.totalGamesPlayed}",
                                                    fontSize = 12.sp,
                                                    color = subColor
                                                )
                                                Text(
                                                    text = "Last: " + sdf.format(Date(player.lastPlayedTimestamp)),
                                                    fontSize = 10.sp,
                                                    color = subColor.copy(alpha = 0.7f),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            // Action Buttons inside profile item
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        editPlayerNameText = player.username
                                                        showEditDialog = player
                                                    },
                                                    modifier = Modifier.size(36.dp).testTag("edit_player_${player.username}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Profile Name",
                                                        tint = Color(0xFFFFD60A),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showDeleteDialog = player },
                                                    modifier = Modifier.size(36.dp).testTag("delete_player_${player.username}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Profile",
                                                        tint = Color(0xFFFF453A),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom major Start button
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.startGame() },
                            enabled = activePlayer != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_game_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0A84FF),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            val activeLabel = activePlayer?.username?.uppercase() ?: "NONE"
                            Text(
                                text = "PLAY AS $activeLabel ⚡",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                1 -> {
                    // LOCAL HALL OF FAME TAB
                    val sortedPlayers = players.sortedByDescending { it.highestScore }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "🏆 OFFLINE HALL OF FAME",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD60A),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Top scores achieved by local offline profiles.",
                            fontSize = 11.sp,
                            color = subColor,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        if (sortedPlayers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No high scores recorded yet.", color = subColor)
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Header Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("RANK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = subColor, modifier = Modifier.width(50.dp))
                                        Text("LOCAL PROFILE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = subColor, modifier = Modifier.weight(1f))
                                        Text("BEST RECORD", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = subColor)
                                    }

                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.08f)))

                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        itemsIndexed(sortedPlayers) { index, player ->
                                            val isCurrentActive = activePlayer?.id == player.id
                                            val rankNum = index + 1
                                            val rankBadge = when (rankNum) {
                                                1 -> "🥇"
                                                2 -> "🥈"
                                                3 -> "🥉"
                                                else -> "#$rankNum"
                                            }

                                            val rowBg = if (isCurrentActive) {
                                                Color(0xFF0A84FF).copy(alpha = 0.1f)
                                            } else {
                                                Color.Transparent
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(rowBg)
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Rank
                                                Text(
                                                    text = rankBadge,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rankNum <= 3) Color.Unspecified else subColor,
                                                    modifier = Modifier.width(50.dp)
                                                )

                                                // Name & secondary details
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = player.username,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Medium,
                                                            color = textColor
                                                        )
                                                        if (isCurrentActive) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFF0A84FF))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            ) {
                                                                Text(
                                                                    text = "YOU",
                                                                    color = Color.White,
                                                                    fontSize = 7.sp,
                                                                    fontWeight = FontWeight.Black
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "${player.totalGamesPlayed} sessions played",
                                                        fontSize = 11.sp,
                                                        color = subColor
                                                    )
                                                }

                                                // Score
                                                Text(
                                                    text = "${player.highestScore} pts",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rankNum == 1) Color(0xFFFFD60A) else textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ACTIVE PROFILE PROGRESS & METRICS TAB
                    val activeHighScore = activePlayer?.highestScore ?: 0
                    val activeGamesPlayed = activePlayer?.totalGamesPlayed ?: 0

                    val achievementsList = listOf(
                        GameMilestone(
                            title = "First Steps",
                            description = "Complete 1 gameplay session.",
                            icon = "🏁",
                            isUnlocked = activeGamesPlayed >= 1
                        ),
                        GameMilestone(
                            title = "Speed Novice",
                            description = "Reach a score of 10 points on any run.",
                            icon = "🥉",
                            isUnlocked = activeHighScore >= 10
                        ),
                        GameMilestone(
                            title = "Double Digits",
                            description = "Reach a score of 20 points on any run.",
                            icon = "🥈",
                            isUnlocked = activeHighScore >= 20
                        ),
                        GameMilestone(
                            title = "Color Veteran",
                            description = "Complete 5 total gameplay sessions.",
                            icon = "🔥",
                            isUnlocked = activeGamesPlayed >= 5
                        ),
                        GameMilestone(
                            title = "Blitz Commander",
                            description = "Reach a score of 35 points.",
                            icon = "🥇",
                            isUnlocked = activeHighScore >= 35
                        ),
                        GameMilestone(
                            title = "Arcade Legend",
                            description = "Reach a score of 50 points.",
                            icon = "👑",
                            isUnlocked = activeHighScore >= 50
                        ),
                        GameMilestone(
                            title = "Unstoppable Force",
                            description = "Complete 15 total gameplay sessions.",
                            icon = "🌌",
                            isUnlocked = activeGamesPlayed >= 15
                        )
                    )

                    val unlockedCount = achievementsList.count { it.isUnlocked }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ACTIVE PROFILE PROGRESS & ACHIEVEMENTS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = subColor,
                            letterSpacing = 1.sp
                        )

                        // Progress Stats Row
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SELECTED PROFILE",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = subColor
                                    )
                                    Text(
                                        text = activePlayer?.username ?: "Ninja",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "HIGH SCORE",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = subColor
                                        )
                                        Text(
                                            text = "$activeHighScore",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFD60A)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "RUNS PLAYED",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = subColor
                                        )
                                        Text(
                                            text = "$activeGamesPlayed",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF30D158)
                                        )
                                    }
                                }
                            }
                        }

                        // Summary of milestones unlocked
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A84FF).copy(alpha = 0.06f)),
                            border = BorderStroke(1.dp, Color(0xFF0A84FF).copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🏆 MILESTONES UNLOCKED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0A84FF)
                                )
                                Text(
                                    text = "$unlockedCount / ${achievementsList.size} COMPLETED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0A84FF)
                                )
                            }
                        }

                        // Milestones list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (milestone in achievementsList) {
                                MilestoneItemCard(
                                    achievement = milestone,
                                    textColor = textColor,
                                    subColor = subColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Player Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a unique username for this player:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = { newPlayerName = it.take(15) },
                        placeholder = { Text("e.g. Speedster") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_player_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlayerName.isNotBlank()) {
                            viewModel.createPlayer(newPlayerName)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("add_player_confirm_button")
                ) {
                    Text("CREATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showEditDialog != null) {
        val player = showEditDialog!!
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Rename Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a new username for ${player.username}:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPlayerNameText,
                        onValueChange = { editPlayerNameText = it.take(15) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_player_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPlayerNameText.isNotBlank()) {
                            viewModel.editPlayerName(player.id, editPlayerNameText)
                            showEditDialog = null
                        }
                    },
                    modifier = Modifier.testTag("edit_player_confirm_button")
                ) {
                    Text("RENAME")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        val player = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF453A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Profile?", fontWeight = FontWeight.Bold)
            } },
            text = {
                Text("Are you sure you want to delete profile '${player.username}'? All high scores and history will be permanently lost.", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(player.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                    modifier = Modifier.testTag("delete_player_confirm_button")
                ) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f)
                )
                Text(text = icon, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun InstructionRow(num: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A84FF).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num,
                color = Color(0xFF0A84FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PausedOverlay(
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "GAME PAUSED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Take a breath, then dive back in!",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Action Card Menu
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                modifier = Modifier.fillMaxWidth(0.85f),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onResumeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESUME", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onRestartClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("restart_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RESTART", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = onHomeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quit_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MAIN MENU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverScreenContent(
    playerName: String,
    score: Int,
    highScore: Int,
    reason: String,
    onPlayAgainClick: () -> Unit,
    onHomeClick: () -> Unit,
    textColor: Color,
    subColor: Color
) {
    val isNewRecord = score == highScore && score > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF453A).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF453A)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GAME OVER",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFF453A),
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "PLAYER: ${playerName.uppercase()}",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.8f),
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Explanation card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF453A).copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFFFF453A).copy(alpha = 0.25f))
        ) {
            Text(
                text = reason,
                fontSize = 14.sp,
                color = if (isSystemInDarkTheme()) Color(0xFFFF8282) else Color(0xFFC21807),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
                    .testTag("game_over_reason")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Side-by-side matches stats views
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR MATCH",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = subColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$score",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.04f)),
                border = BorderStroke(
                    width = if (isNewRecord) 2.dp else 1.dp,
                    color = if (isNewRecord) Color(0xFFFFD60A) else textColor.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HIGH SCORE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = subColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$highScore",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isNewRecord) Color(0xFFFFD60A) else textColor
                    )
                }
            }
        }

        if (isNewRecord) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFFFD60A))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🏆 NEW PERSONAL BEST RECORD!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(45.dp))

        Button(
            onClick = onPlayAgainClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("play_again_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = "PLAY AGAIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("home_from_game_over_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, textColor.copy(alpha = 0.25f))
        ) {
            Text(
                text = "BACK TO MENU",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Data model for offline-first single-player milestone tracking
data class GameMilestone(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

@Composable
fun MilestoneItemCard(
    achievement: GameMilestone,
    textColor: Color,
    subColor: Color
) {
    val opacity = if (achievement.isUnlocked) 1f else 0.45f
    val cardBg = if (achievement.isUnlocked) textColor.copy(alpha = 0.05f) else textColor.copy(alpha = 0.02f)
    val cardBorderColor = if (achievement.isUnlocked) Color(0xFF30D158).copy(alpha = 0.35f) else textColor.copy(alpha = 0.06f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = opacity)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Milestone Icon/Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (achievement.isUnlocked) Color(0xFF30D158).copy(alpha = 0.12f) else textColor.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Milestone Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = subColor
                )
            }

            // Milestone Unlock State Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (achievement.isUnlocked) Color(0xFF30D158).copy(alpha = 0.15f) else textColor.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (achievement.isUnlocked) "UNLOCKED" else "LOCKED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (achievement.isUnlocked) Color(0xFF30D158) else subColor
                )
            }
        }
    }
}
