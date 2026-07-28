package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// ==================== Upgraded Live Snake Preview ====================

data class PreviewParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var life: Float,
    var size: Float
)

@Composable
fun HexGridLiveSnakePreview(
    pointsEaten: Int,
    onPointsEatenChange: (Int) -> Unit,
    skinCycle: Int,
    onSkinCycleChange: (Int) -> Unit
) {
    var width by remember { mutableStateOf(300f) }
    var height by remember { mutableStateOf(200f) }

    val snakeSegments = remember { mutableStateListOf<Offset>() }
    var food by remember { mutableStateOf(Offset(150f, 100f)) }
    val particles = remember { mutableStateListOf<PreviewParticle>() }
    var targetOverride by remember { mutableStateOf<Offset?>(null) }
    var frameTick by remember { mutableStateOf(0) }

    val skinColors = listOf(
        listOf(Color(0xFF00F5D4), Color(0xFF00BFA5), Color(0xFFB2FFEB)),
        listOf(Color(0xFF9F7AEA), Color(0xFFED64A6), Color(0xFFFCE7F3)),
        listOf(Color(0xFFFFB300), Color(0xFFFFE066), Color(0xFFFFF8E1))
    )
    val curSkin = skinColors[skinCycle % skinColors.size]

    // Initialize snake
    LaunchedEffect(width, height) {
        if (snakeSegments.isEmpty() && width > 50f && height > 50f) {
            val cx = width / 2
            val cy = height / 2
            snakeSegments.clear()
            repeat(14) { i ->
                snakeSegments.add(Offset(cx - i * 11f, cy))
            }
            food = Offset(
                Random.nextFloat() * (width - 60f) + 30f,
                Random.nextFloat() * (height - 60f) + 30f
            )
        }
    }

    // Game Loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            frameTick++

            if (snakeSegments.isEmpty() || width <= 0f || height <= 0f) continue

            val head = snakeSegments.first()
            val target = targetOverride ?: food

            val dx = target.x - head.x
            val dy = target.y - head.y
            val dist = hypot(dx, dy)

            val speed = 3.8f
            var vx = 0f
            var vy = 0f

            if (dist > 3f) {
                val baseVx = (dx / dist) * speed
                val baseVy = (dy / dist) * speed
                val slither = sin(frameTick * 0.22f) * 1.1f
                vx = baseVx + (-baseVy / speed) * slither
                vy = baseVy + (baseVx / speed) * slither
            } else if (targetOverride != null) {
                targetOverride = null
            }

            val newHead = Offset(
                (head.x + vx).coerceIn(14f, width - 14f),
                (head.y + vy).coerceIn(14f, height - 14f)
            )

            // Update snake body
            val updated = mutableListOf<Offset>()
            updated.add(newHead)

            var prev = newHead
            for (i in 1 until snakeSegments.size) {
                val curr = snakeSegments[i]
                val d = hypot(curr.x - prev.x, curr.y - prev.y)
                if (d > 11f) {
                    val ratio = 11f / d
                    updated.add(Offset(prev.x + (curr.x - prev.x) * ratio, prev.y + (curr.y - prev.y) * ratio))
                } else {
                    updated.add(curr)
                }
                prev = updated.last()
            }

            snakeSegments.clear()
            snakeSegments.addAll(updated)

            // Eat food
            if (hypot(food.x - newHead.x, food.y - newHead.y) < 17f) {
                // Particles explosion
                repeat(18) {
                    val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                    val vel = Random.nextFloat() * 4.2f + 1.6f
                    particles.add(
                        PreviewParticle(
                            x = food.x, y = food.y,
                            vx = cos(angle) * vel,
                            vy = sin(angle) * vel,
                            color = curSkin.random(),
                            life = 1f,
                            size = Random.nextFloat() * 5f + 2f
                        )
                    )
                }

                snakeSegments.add(snakeSegments.lastOrNull() ?: newHead)
                onPointsEatenChange(pointsEaten + 1)

                if ((pointsEaten + 1) % 3 == 0) {
                    onSkinCycleChange(skinCycle + 1)
                }

                food = Offset(
                    Random.nextFloat() * (width - 70f) + 35f,
                    Random.nextFloat() * (height - 70f) + 35f
                )
            }

            // Update particles
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.935f
                p.vy *= 0.935f
                p.life -= 0.023f
                if (p.life <= 0f) iterator.remove()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1C)),
        border = BorderStroke(1.5.dp, Color(0xFF1E2A44))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF22FFCC)))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "LIVE VIPER PREVIEW",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(curSkin[0].copy(alpha = 0.18f))
                        .border(1.dp, curSkin[0].copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (skinCycle % 3) {
                            0 -> "NEON VIPER"
                            1 -> "CYBER GLOW"
                            else -> "SOLAR FLARE"
                        },
                        color = curSkin[0],
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF05080F), Color(0xFF0C1325))
                        )
                    )
                    .border(1.5.dp, Color(0xFF1F2B4A), RoundedCornerShape(16.dp))
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val gridAlpha by infiniteTransition.animateFloat(
                    0.09f, 0.22f,
                    infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            width = it.width.toFloat()
                            height = it.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                targetOverride = offset
                                repeat(12) {
                                    val r = Random
                                    val ang = r.nextFloat() * 2 * Math.PI.toFloat()
                                    val spd = r.nextFloat() * 4f + 1.8f
                                    particles.add(
                                        PreviewParticle(
                                            offset.x, offset.y,
                                            cos(ang) * spd, sin(ang) * spd,
                                            curSkin.random(), 1f,
                                            r.nextFloat() * 4.5f + 2.2f
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    // Hex Grid
                    val hexRadius = 19f
                    val dx = hexRadius * 1.6f
                    val dy = hexRadius * 1.732f

                    for (i in -1..(size.width / dx).toInt() + 2) {
                        for (j in -1..(size.height / dy).toInt() + 2) {
                            val cx = if (j % 2 == 0) i * dx * 2 else i * dx * 2 + dx
                            val cy = j * dy

                            val path = Path().apply {
                                for (k in 0..5) {
                                    val rad = Math.toRadians(k * 60.0)
                                    val px = cx + hexRadius * cos(rad).toFloat()
                                    val py = cy + hexRadius * sin(rad).toFloat()
                                    if (k == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF1F2B4A).copy(alpha = gridAlpha),
                                style = Stroke(width = 1f)
                            )
                        }
                    }

                    // Particles
                    particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.life * 0.9f),
                            radius = p.size * p.life,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // Food
                    val foodPulse = 1f + sin(frameTick * 0.22f).absoluteValue * 0.3f
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.2f),
                        radius = 13f * foodPulse,
                        center = food
                    )
                    drawCircle(
                        color = curSkin[1],
                        radius = 5.5f,
                        center = food
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.2f,
                        center = food
                    )

                    // Snake
                    snakeSegments.forEachIndexed { i, pos ->
                        val alpha = (1f - i.toFloat() / snakeSegments.size * 0.45f).coerceAtLeast(0.6f)
                        val radius = (8.5f * (1f - i.toFloat() / snakeSegments.size * 0.55f)).coerceAtLeast(3.8f)

                        drawCircle(
                            color = curSkin[i % curSkin.size].copy(alpha = alpha * 0.15f),
                            radius = radius * 2.4f,
                            center = pos
                        )
                        drawCircle(
                            color = curSkin[i % curSkin.size],
                            radius = radius,
                            center = pos
                        )

                        if (i == 0) {
                            // Head highlight
                            drawCircle(Color.White.copy(alpha = 0.7f), radius * 0.45f,
                                Offset(pos.x - radius * 0.25f, pos.y - radius * 0.25f))
                            // Crown
                            val crown = Path().apply {
                                moveTo(pos.x - 8f, pos.y - 7f)
                                lineTo(pos.x - 10f, pos.y - 14f)
                                lineTo(pos.x - 4f, pos.y - 10f)
                                lineTo(pos.x + 1f, pos.y - 17f)
                                lineTo(pos.x + 5f, pos.y - 10f)
                                lineTo(pos.x + 10f, pos.y - 14f)
                                lineTo(pos.x + 8f, pos.y - 7f)
                                close()
                            }
                            drawPath(crown, Color(0xFFFFE600))
                        }
                    }
                }

                // HUD Overlay
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "SYS_v4.1 • LIVE",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("VIPER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("LENGTH: ${snakeSegments.size}", color = Color(0xFF94A3B8), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("FPS: 60 • STABLE", color = Color(0xFF4ADE80), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            "TAP TO STEER",
                            color = Color(0xFFCBD5E1).copy(alpha = 0.75f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}