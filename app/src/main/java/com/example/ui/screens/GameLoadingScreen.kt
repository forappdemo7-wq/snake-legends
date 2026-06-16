package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ArenaTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameLoadingScreen(
    gameMode: String,
    arenaTheme: ArenaTheme,
    privateRoomCode: String = "",
    onLoadingFinished: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var lobbyConnectionCount by remember { mutableStateOf(1) }
    var statusText by remember { mutableStateOf("INITIALIZING DRIVERS...") }
    var isFinished by remember { mutableStateOf(false) }

    // Pre-shuffled list of opponent names – stable across recompositions
    val totalOpList = remember {
        listOf(
            "KryptonSlayer (CYBER)",
            "OuroborosKing (VIPER)",
            "AlphaConstrictor",
            "NeonTitan (APEX)",
            "CobaltFangs",
            "StealthGlitch",
            "ViperGlow",
            "SpecterTail",
            "GlitchStriker"
        ).shuffled()
    }

    val visibleOpponents = remember { mutableStateListOf<String>() }

    // Derived state for progress text
    val progressPercent by derivedStateOf { (progress * 100).toInt() }

    // Loading simulation with cancellation protection
    LaunchedEffect(Unit) {
        val totalTicks = 100
        for (i in 1..totalTicks) {
            progress = i / 100f

            // Update status and opponent list based on progress
            when (i) {
                1 -> statusText = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") {
                    "ALLOCATING PRIVATE SERVER CHANNEL #$privateRoomCode..."
                } else {
                    "COMMUNICATING WITH DISTRIBUTED COORDINATES..."
                }
                15 -> {
                    statusText = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") {
                        "SPAWNING CUSTOM BOTS FOR ENFORCED ARENA RULES..."
                    } else {
                        "PROVISIONING STYLIZATION FOR ${arenaTheme.displayName.uppercase()}..."
                    }
                    visibleOpponents.add(totalOpList[0])
                    lobbyConnectionCount++
                }
                30 -> {
                    statusText = "GRID QUEUE ESTABLISHED: MATCHMAKING..."
                    visibleOpponents.add(totalOpList[1])
                    lobbyConnectionCount++
                }
                45 -> {
                    statusText = "COLLECTING COALITION SNAKES (4/10)..."
                    visibleOpponents.add(totalOpList[2])
                    visibleOpponents.add(totalOpList[3])
                    lobbyConnectionCount += 2
                }
                60 -> {
                    statusText = "ESTABLISHING FORCEFIELD CHANNELS (6/10)..."
                    visibleOpponents.add(totalOpList[4])
                    visibleOpponents.add(totalOpList[5])
                    lobbyConnectionCount += 2
                }
                78 -> {
                    statusText = "FINALIZING MATCH PROTOCOLS (9/10)..."
                    visibleOpponents.add(totalOpList[6])
                    visibleOpponents.add(totalOpList[7])
                    lobbyConnectionCount += 2
                }
                92 -> {
                    statusText = "ALL OPPONENTS SECURED. READY..."
                    lobbyConnectionCount++
                }
            }
            delay(28) // ~2.8 seconds total
        }
        isFinished = true
        onLoadingFinished()
    }

    // Ensure onLoadingFinished is not called if the screen is disposed early
    DisposableEffect(Unit) {
        onDispose {
            // The callback will be skipped if the composable is removed before loading completes
        }
    }

    // Radar sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(24.dp)
            .testTag("game_loading_container")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Radar Scanner
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color(0x0C00FFCC))
                        .border(1.5.dp, Color(0x3300FFCC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        drawCircle(color = Color(0x1F00FFCC), radius = radius * 0.7f, style = Stroke(width = 1f))
                        drawCircle(color = Color(0x1F00FFCC), radius = radius * 0.4f, style = Stroke(width = 1f))
                        drawLine(Color(0x1500FFCC), start = Offset(0f, center.y), end = Offset(size.width, center.y))
                        drawLine(Color(0x1500FFCC), start = Offset(center.x, 0f), end = Offset(center.x, size.height))

                        val rads = Math.toRadians(sweepAngle.toDouble())
                        val sweepTargetX = (center.x + radius * cos(rads)).toFloat()
                        val sweepTargetY = (center.y + radius * sin(rads)).toFloat()

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00FFCC), Color.Transparent),
                                start = center,
                                end = Offset(sweepTargetX, sweepTargetY)
                            ),
                            start = center,
                            end = Offset(sweepTargetX, sweepTargetY),
                            strokeWidth = 3f
                        )

                        // Dots indicating detected players
                        drawCircle(Color(0xFFFF3366), radius = 4f, center = Offset(center.x - 40f, center.y + 30f))
                        drawCircle(Color(0xFF00FFCC), radius = 4f, center = Offset(center.x + 50f, center.y - 45f))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$lobbyConnectionCount / 10",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "GRID SYSTEM ONLINE",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Column: Details and opponent list
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = gameMode.uppercase(),
                    color = Color(0xFFFF9900),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") "PRIVATE LOBBY CREATED" else "MATCHMAKING ESTABLISHED",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") {
                        "Arena Theme: ${arenaTheme.displayName} | LOBBY KEY: #$privateRoomCode"
                    } else {
                        "Arena Theme: ${arenaTheme.displayName}"
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Opponent connection list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B19))
                        .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "GRID INGRESS CODES:",
                            color = Color(0xFF00FFCC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                visibleOpponents.take(3).forEach { op ->
                                    Text(text = "🛡️ Connected: $op", color = Color.White, fontSize = 8.sp, maxLines = 1)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                visibleOpponents.drop(3).take(3).forEach { op ->
                                    Text(text = "🛡️ Connected: $op", color = Color.White, fontSize = 8.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0x3300FFCC)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$progressPercent%",
                        color = Color(0xFF00FFCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}