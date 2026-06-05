package com.example.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
                val playerName by viewModel.playerName.collectAsState(initial = "Ninja")
                StartScreenContent(
                    highScore = highScore,
                    playerName = playerName,
                    onNameChange = { viewModel.setPlayerName(it) },
                    onStartClick = { viewModel.startGame() },
                    textColor = primaryTextColor,
                    subColor = secondaryTextColor
                )
            }
            GameState.PLAYING, GameState.PAUSED -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header HUD with current score, pause button, and high score
                    GameHeaderHud(
                        score = score,
                        highScore = highScore,
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
    highScore: Int,
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
                text = "BEST SCORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = subColor,
                letterSpacing = 1.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "High Score Star",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$highScore",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.testTag("high_score_hud")
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
    highScore: Int,
    playerName: String,
    onNameChange: (String) -> Unit,
    onStartClick: () -> Unit,
    textColor: Color,
    subColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big branding logo banner
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD60A), Color(0xFFFF453A), Color(0xFFBF5AF2))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Game Logo Core",
                tint = Color.White,
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer(rotationZ = 12f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "COLOR RUSH",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = textColor,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Text(
            text = "THE REACTION ARCADE CLASSIC",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF30D158),
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Elegant Username Input Form
        OutlinedTextField(
            value = playerName,
            onValueChange = onNameChange,
            label = { Text("ENTER YOUR NAME", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .testTag("player_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                focusedLabelColor = Color(0xFF0A84FF),
                unfocusedLabelColor = subColor
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // High Score Record Block
        val cleanLabel = if (playerName.trim().isNotEmpty()) playerName.uppercase() else "PERSONAL"
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(0.85f),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Trophy Sign",
                    tint = Color(0xFFFFD60A),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$cleanLabel RECORD",
                        fontSize = 10.sp,
                        color = subColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$highScore POINTS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions summary panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.03f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "HOW TO PLAY",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = subColor,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                InstructionRow(num = "1", text = "Tap falling elements matching the target color at the top.")
                InstructionRow(num = "2", text = "Tapping wrong colors causes immediate terminal game-over.")
                InstructionRow(num = "3", text = "Never let correct-colored blocks slip past the bottom screen.")
                InstructionRow(num = "4", text = "Every 10 points speeds up gravitation velocity!")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("start_game_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A84FF)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "START RUSH",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
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
            .background(Color.Black.copy(alpha = 0.72f))
            .blur(8.dp)
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
