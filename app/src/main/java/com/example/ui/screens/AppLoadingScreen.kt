package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@Composable
fun AppLoadingScreen(
    onLoadingFinished: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var loadingStatusText by remember { mutableStateOf("BOOTING COGNITIVE MATRIX...") }
    var isFinished by remember { mutableStateOf(false) }

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

    // Stepped loading phase simulation
    LaunchedEffect(Unit) {
        val totalSteps = 100
        for (i in 1..totalSteps) {
            progress = i / 100f
            loadingStatusText = when (i) {
                1 -> "BOOTING COGNITIVE MATRIX..."
                15 -> "VERIFYING CRYPTO STORAGE SECRETS..."
                35 -> "SYNCHRONIZING LEADERBOARD REGISTRY..."
                55 -> "CALIBRATING HAPTIC ENGINE RESPONSIVENESS..."
                75 -> "LOADING CYBER COSMETIC SKINS..."
                90 -> "STABILIZING FORCEFIELD ENGINE..."
                98 -> "ARENA COALITION COMPLETE. RUNNING..."
                else -> loadingStatusText // keep current text
            }
            delay(25) // ~2.5s total loading speed
        }
        isFinished = true
        onLoadingFinished()
    }

    // Ensure onLoadingFinished is not called after disposal
    DisposableEffect(Unit) {
        onDispose {
            // Prevent callback if screen is removed early
            // (No action needed, but we ensure the callback isn't called twice)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A)) // Super‑dark slate black
            .testTag("app_loading_container")
    ) {
        // Futuristic grid backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = 80f
            val cellHeight = 80f
            val width = size.width
            val height = size.height

            // Glowing radial centre
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1F00FFCC), Color.Transparent),
                    center = Offset(width / 2f, height / 2f),
                    radius = height * 0.8f
                )
            )

            // Grid wires
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0x1100FFCC),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += cellWidth
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0x1100FFCC),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += cellHeight
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.81f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand heading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Power,
                    contentDescription = null,
                    tint = Color(0xFF00FFCC).copy(alpha = pulseAlpha),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "SNAKE LEGENDS",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "CYBERNETIC ARENA NETWORKS v2.6.0",
                color = Color(0xFF00FFCC).copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Neon progress bar
            Column(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0x3300FFCC)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loadingStatusText,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}