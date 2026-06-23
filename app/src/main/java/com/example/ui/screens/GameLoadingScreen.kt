package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    var statusText by remember { mutableStateOf("INITIALIZING COGNITIVE INTERFACES...") }

    val themeAccentColor = remember(arenaTheme) { getThemeAccentColor(arenaTheme) }
    val themeGlowColor = remember(arenaTheme) { getThemeGlowColor(arenaTheme) }

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

    val visibleOpponents = remember { mutableStateListOf<Pair<String, Int>>() } // Name, Ping

    LaunchedEffect(Unit) {
        val totalTicks = 100
        for (i in 1..totalTicks) {
            progress = i / 100f

            // Adjust matchmaking statuses dynamically with rich details
            when (i) {
                1 -> statusText = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") "ALLOCATING SECURE SERVER CHANNELS #$privateRoomCode..." else "CONNECTING TO THE CLOSEST CYBER NODE COORDINATES..."
                15 -> {
                    statusText = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") "PRE-SPAWNING HIGH-TIER FORCE AI ENTITIES..." else "STAGGERING STYLIZATION MESHES FOR ${arenaTheme.displayName.uppercase()}..."
                    visibleOpponents.add(totalOpList[0] to (32..98).random())
                    lobbyConnectionCount++
                }
                30 -> {
                    statusText = "PARSING COMPATIBLE ACTIVE SEED CHANNELS..."
                    visibleOpponents.add(totalOpList[1] to (20..74).random())
                    lobbyConnectionCount++
                }
                45 -> {
                    statusText = "COLLECTING COALITION SNAKES (4/10)..."
                    visibleOpponents.add(totalOpList[2] to (55..110).random())
                    visibleOpponents.add(totalOpList[3] to (45..90).random())
                    lobbyConnectionCount += 2
                }
                60 -> {
                    statusText = "INJECTING MATCH ARCHITECTURE PROTOCOLS (6/10)..."
                    visibleOpponents.add(totalOpList[4] to (30..80).random())
                    visibleOpponents.add(totalOpList[5] to (40..95).random())
                    lobbyConnectionCount += 2
                }
                78 -> {
                    statusText = "SECURED MAIN DATA INTEGRATION PATHWAY (8/10)..."
                    visibleOpponents.add(totalOpList[6] to (25..60).random())
                    visibleOpponents.add(totalOpList[7] to (65..120).random())
                    lobbyConnectionCount += 2
                }
                92 -> {
                    statusText = "FINAL CALIBRATION: INITIATING ARENA SHIELDS..."
                    lobbyConnectionCount++
                }
            }
            delay(28) // Simulate ~2.8s total loading phase
        }
        onLoadingFinished()
    }

    // Radar scanning rotational pointer angle animation
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

    // Breathing pulse for radial glows
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Dark cyber vault
            .testTag("game_loading_container")
    ) {
        // High fidelity grid layout adapting smoothly on any screen dimension
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Part: High Tech Radar Scanner Visuals
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    RadarVisualizer(
                        themeAccentColor = themeAccentColor,
                        themeGlowColor = themeGlowColor,
                        sweepAngle = sweepAngle,
                        pulseGlow = pulseGlow,
                        lobbyConnectionCount = lobbyConnectionCount,
                        sizeDp = 220
                    )
                }

                // Right Part: Matchmaking statistics, connect stream & progressive slider
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    MatchMetadataHeader(
                        gameMode = gameMode,
                        arenaTheme = arenaTheme,
                        privateRoomCode = privateRoomCode,
                        themeAccentColor = themeAccentColor
                    )

                    OpponentFeedsStream(
                        opponents = visibleOpponents,
                        themeAccentColor = themeAccentColor
                    )

                    LoadingStatusBar(
                        progress = progress,
                        statusText = statusText,
                        accentColor = themeAccentColor
                    )
                }
            }
        } else {
            // Adaptive Portrait Layout: Stack components beautifully with vertical Spacers to prevent clashing text
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MatchMetadataHeader(
                    gameMode = gameMode,
                    arenaTheme = arenaTheme,
                    privateRoomCode = privateRoomCode,
                    themeAccentColor = themeAccentColor
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RadarVisualizer(
                        themeAccentColor = themeAccentColor,
                        themeGlowColor = themeGlowColor,
                        sweepAngle = sweepAngle,
                        pulseGlow = pulseGlow,
                        lobbyConnectionCount = lobbyConnectionCount,
                        sizeDp = 180
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OpponentFeedsStream(
                        opponents = visibleOpponents,
                        themeAccentColor = themeAccentColor,
                        maxCards = 4
                    )

                    LoadingStatusBar(
                        progress = progress,
                        statusText = statusText,
                        accentColor = themeAccentColor
                    )
                }
            }
        }
    }
}

// ========== Sub-Composable Helpers for Loading Screens ==========

@Composable
fun MatchMetadataHeader(
    gameMode: String,
    arenaTheme: ArenaTheme,
    privateRoomCode: String,
    themeAccentColor: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(themeAccentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = gameMode.uppercase(),
                    color = themeAccentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") "ESTABLISHING PRIVATE LOBBY" else "MATCHMAKING ESTABLISHED",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = themeAccentColor.copy(alpha = 0.4f),
                    offset = Offset(0f, 0f),
                    blurRadius = 10f
                )
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (privateRoomCode.isNotBlank() && gameMode == "Private Room") "Arena Grid: ${arenaTheme.displayName} | LOBBY KEY: #$privateRoomCode" else "Arena Grid: ${arenaTheme.displayName}",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RadarVisualizer(
    themeAccentColor: Color,
    themeGlowColor: Color,
    sweepAngle: Float,
    pulseGlow: Float,
    lobbyConnectionCount: Int,
    sizeDp: Int
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(0xFF070B19))
            .border(2.dp, themeAccentColor.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // Concentric Glowing Radar rings
            drawCircle(
                color = themeAccentColor.copy(alpha = pulseGlow * 0.12f),
                radius = radius * 0.9f,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = themeAccentColor.copy(alpha = 0.08f),
                radius = radius * 0.6f,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = themeAccentColor.copy(alpha = 0.05f),
                radius = radius * 0.3f,
                style = Stroke(width = 1f)
            )

            // High tech crosshairs
            drawLine(
                color = themeAccentColor.copy(alpha = 0.15f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = themeAccentColor.copy(alpha = 0.15f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1f
            )

            // Animated sweep ray
            val radians = Math.toRadians(sweepAngle.toDouble())
            val sweepTargetX = (center.x + radius * cos(radians)).toFloat()
            val sweepTargetY = (center.y + radius * sin(radians)).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(themeAccentColor, Color.Transparent),
                    start = center,
                    end = Offset(sweepTargetX, sweepTargetY)
                ),
                start = center,
                end = Offset(sweepTargetX, sweepTargetY),
                strokeWidth = 3f
            )

            // Display flickering satellite tracked dots (representing other players)
            drawCircle(Color(0xFFFF3366), radius = 5f, center = Offset(center.x - radius * 0.4f, center.y + radius * 0.3f))
            drawCircle(themeAccentColor, radius = 5f, center = Offset(center.x + radius * 0.5f, center.y - radius * 0.5f))
            drawCircle(Color(0xFFE2E8F0), radius = 4f, center = Offset(center.x - radius * 0.2f, center.y - radius * 0.6f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = null,
                tint = themeAccentColor,
                modifier = Modifier.size(if (sizeDp < 200) 32.dp else 40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$lobbyConnectionCount / 10",
                color = Color.White,
                fontSize = if (sizeDp < 200) 15.sp else 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    shadow = Shadow(
                        color = themeAccentColor,
                        offset = Offset(0f, 0f),
                        blurRadius = 8f
                    )
                )
            )
            Text(
                text = "SURVEY SCANNING",
                color = Color(0xFF64748B),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun OpponentFeedsStream(
    opponents: List<Pair<String, Int>>,
    themeAccentColor: Color,
    maxCards: Int = 3
) {
    Column {
        Text(
            text = "CONNECTED SECTOR INGRESS CODES:",
            color = themeAccentColor.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0F1F))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (opponents.isEmpty()) {
                    Text(
                        text = "WAITING FOR CORRELATIONS...",
                        color = Color(0xFF475569),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    opponents.take(maxCards).forEach { (name, ping) ->
                        CyberOpponentRow(name = name, ping = ping, accentColor = themeAccentColor)
                    }
                }
            }

            if (opponents.size > maxCards) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    opponents.drop(maxCards).take(maxCards).forEach { (name, ping) ->
                        CyberOpponentRow(name = name, ping = ping, accentColor = themeAccentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun CyberOpponentRow(name: String, ping: Int, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111827))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = name,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
        }
        Text(
            text = "${ping}ms",
            color = if (ping < 60) Color(0xFF10B981) else Color(0xFFF59E0B),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LoadingStatusBar(
    progress: Float,
    statusText: String,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = statusText,
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor,
                                accentColor
                            )
                        )
                    )
            )
        }
    }
}

// Helper methods to get color depending on arenaTheme
private fun getThemeAccentColor(theme: ArenaTheme): Color {
    return when (theme) {
        ArenaTheme.CYBER_CITY -> Color(0xFF00FFCC) // Glowing Cyber
        ArenaTheme.LAVA_WORLD -> Color(0xFFFF5722) // Lava Orange
        ArenaTheme.FROZEN_ARENA -> Color(0xFF00E5FF) // Icy Freeze Cyber
        ArenaTheme.JUNGLE_TEMPLE -> Color(0xFF4CAF50) // Emerald Green
        ArenaTheme.SPACE_STATION -> Color(0xFFE040FB) // Void Violet Purple
        ArenaTheme.NEON_GRID -> Color(0xFFEEFF41) // Electric Volt Yellow-Green
    }
}

private fun getThemeGlowColor(theme: ArenaTheme): Color {
    return when (theme) {
        ArenaTheme.CYBER_CITY -> Color(0x3300FFCC)
        ArenaTheme.LAVA_WORLD -> Color(0x33FF5722)
        ArenaTheme.FROZEN_ARENA -> Color(0x3300E5FF)
        ArenaTheme.JUNGLE_TEMPLE -> Color(0x334CAF50)
        ArenaTheme.SPACE_STATION -> Color(0x33E040FB)
        ArenaTheme.NEON_GRID -> Color(0x33EEFF41)
    }
}
