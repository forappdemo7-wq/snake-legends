package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.TipsAndUpdates
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AppLoadingScreen(
    onLoadingFinished: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var loadingStatusText by remember { mutableStateOf("BOOTING COGNITIVE MATRIX...") }

    // Stepped loading phase simulation
    LaunchedEffect(Unit) {
        val totalSteps = 100
        for (i in 1..totalSteps) {
            progress = i / 100f
            when (i) {
                1 -> loadingStatusText = "BOOTING COGNITIVE MATRIX..."
                15 -> loadingStatusText = "VERIFYING CRYPTO STORAGE SECRETS..."
                35 -> loadingStatusText = "SYNCHRONIZING LEADERBOARD REGISTRY..."
                55 -> loadingStatusText = "CALIBRATING HAPTIC ENGINE RESPONSIVENESS..."
                75 -> loadingStatusText = "LOADING CYBER COSMETIC SKINS..."
                90 -> loadingStatusText = "STABILIZING FORCEFIELD ENGINE..."
                98 -> loadingStatusText = "ARENA COALITION COMPLETE. RUNNING..."
            }
            delay(25) // ~2.5s total loading speed
        }
        onLoadingFinished()
    }

    // Continuous pulse for atmospheric lighting grid
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Parallel drifting backdrop cyber-grid offset to create infinite movement
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    // Tips Carousel that rotates dynamically
    val professionalTips = remember {
        listOf(
            "DIAGNOSTIC: Aegis Shield grants full 3-second invulnerability.",
            "TACTICAL: Use Speed Drive Burst to outrun hostile snake traps.",
            "STRATEGY: Trap opponents against outer lethal energy barriers.",
            "INFO: Sub-Zero Blast will flash-freeze nearby snake navigation.",
            "CONFIGURATION: Tail length increases snake blast radius potential."
        )
    }
    var tipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            tipIndex = (tipIndex + 1) % professionalTips.size
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Sleek near-black deep space
            .testTag("app_loading_container")
    ) {
        // Holographic Parallax Cyber Grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = 100f
            val cellHeight = 100f
            val width = size.width
            val height = size.height

            // Glowing ambient radial background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2200FFCC), Color(0x0200FFCC), Color.Transparent),
                    center = Offset(width / 2f, height / 2f),
                    radius = height * 0.9f
                )
            )

            // Horizontal lines scrolling downwards
            var y = gridOffset % cellHeight
            while (y < height) {
                drawLine(
                    color = Color(0x1300FFCC),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.2f
                )
                y += cellHeight
            }

            // Vertical lines scrolling rightwards
            var x = gridOffset % cellWidth
            while (x < width) {
                drawLine(
                    color = Color(0x1300FFCC),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.2f
                )
                x += cellWidth
            }
        }

        // Main content column structured for perfect portrait/landscape flexibility
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = if (isLandscape) 16.dp else 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.SpaceEvenly else Arrangement.SpaceBetween
        ) {
            // Header Spacer or Decoration
            if (!isLandscape) {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 1: Logo & Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        tint = Color(0xFF00FFCC).copy(alpha = pulseAlpha),
                        modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp)
                    )
                    Text(
                        text = "SNAKE LEGENDS",
                        color = Color.White,
                        fontSize = if (isLandscape) 28.sp else 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = if (isLandscape) 4.sp else 6.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF00FFCC).copy(alpha = 0.5f),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "CYBERNETIC ARENA NETWORKS v2.6.5",
                    color = Color(0xFF00FFCC).copy(alpha = 0.6f),
                    fontSize = if (isLandscape) 8.sp else 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Section 2: Loading State Indicator (Centered elegantly)
            Column(
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.50f else 0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = loadingStatusText,
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 8.sp else 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color(0xFF00FFCC),
                        fontSize = if (isLandscape) 11.sp else 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Upgraded glowing premium progress loading bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    // Under-bar cyber neon backdrop glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0EA5E9),
                                        Color(0xFF00FFCC),
                                        Color(0xFF00E5FF)
                                    )
                                )
                            )
                    )
                }
            }

            // Section 3: Interactive Dynamic Tactical Pro-Tips Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.65f else 0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF00FFCC).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1F00FFCC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = "Tactical Info",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "TACTICAL ENHANCEMENT REPORT",
                            color = Color(0xFF00FFCC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        Crossfade(
                            targetState = professionalTips[tipIndex],
                            animationSpec = tween(500),
                            label = "tip_transition"
                        ) { tip ->
                            Text(
                                text = tip,
                                color = Color(0xFF94A3B8),
                                fontSize = if (isLandscape) 10.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
