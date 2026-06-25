package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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

// ========== Live Snake Preview (Hex Grid) ==========
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
        listOf(Color(0xFF00FFCC), Color(0xFF008B8B), Color(0xFFEDFDF9)),
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFFF1F2)),
        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFFFFFDE7))
    )
    val curSkin = skinColors[skinCycle % skinColors.size]

    LaunchedEffect(width, height) {
        if (snakeSegments.isEmpty() && width > 0f && height > 0f) {
            val cx = width / 2
            val cy = height / 2
            repeat(14) { i ->
                snakeSegments.add(Offset(cx - i * 10f, cy))
            }
            food = Offset(
                Random.nextFloat() * (width - 40f) + 20f,
                Random.nextFloat() * (height - 40f) + 20f
            )
        }
    }

    LaunchedEffect(Unit) {
        val random = Random(System.currentTimeMillis())
        while (true) {
            delay(16)
            frameTick++

            if (snakeSegments.isEmpty() || width <= 0f || height <= 0f) continue

            val head = snakeSegments.first()
            val target = targetOverride ?: food

            val dx = target.x - head.x
            val dy = target.y - head.y
            val dist = hypot(dx, dy)

            val speed = 3.6f
            var vx = 0f
            var vy = 0f

            if (dist > 2f) {
                val baseVx = (dx / dist) * speed
                val baseVy = (dy / dist) * speed
                val slitherFreq = 0.2f
                val slitherAmp = 1.0f
                val perpX = -baseVy
                val perpY = baseVx
                val slitherOffset = sin(frameTick * slitherFreq) * slitherAmp
                vx = baseVx + (perpX / speed) * slitherOffset
                vy = baseVy + (perpY / speed) * slitherOffset
            } else {
                if (targetOverride != null) targetOverride = null
            }

            val newHead = Offset(
                (head.x + vx).coerceIn(12f, width - 12f),
                (head.y + vy).coerceIn(12f, height - 12f)
            )
            if (newHead.x == 12f || newHead.x == width - 12f || newHead.y == 12f || newHead.y == height - 12f) {
                targetOverride = null
            }

            val updatedSegments = ArrayList<Offset>(snakeSegments.size)
            updatedSegments.add(newHead)

            var prev = newHead
            val targetDist = 9.0f
            for (i in 1 until snakeSegments.size) {
                val curr = snakeSegments[i]
                val sDx = curr.x - prev.x
                val sDy = curr.y - prev.y
                val sDist = hypot(sDx, sDy)
                if (sDist > targetDist) {
                    val ratio = targetDist / sDist
                    updatedSegments.add(Offset(prev.x + sDx * ratio, prev.y + sDy * ratio))
                } else {
                    updatedSegments.add(curr)
                }
                prev = updatedSegments.last()
            }

            snakeSegments.clear()
            snakeSegments.addAll(updatedSegments)

            val fDist = hypot(food.x - newHead.x, food.y - newHead.y)
            if (fDist < 16f) {
                repeat(16) {
                    val ang = random.nextFloat() * 2f * Math.PI.toFloat()
                    val pSpeed = random.nextFloat() * 4.0f + 1.2f
                    particles.add(
                        PreviewParticle(
                            x = food.x,
                            y = food.y,
                            vx = cos(ang) * pSpeed,
                            vy = sin(ang) * pSpeed,
                            color = curSkin.random(),
                            life = 1f,
                            size = random.nextFloat() * 4.5f + 1.5f
                        )
                    )
                }

                val tail = snakeSegments.lastOrNull() ?: newHead
                snakeSegments.add(tail)

                onPointsEatenChange(pointsEaten + 1)
                if ((pointsEaten + 1) % 3 == 0) {
                    onSkinCycleChange(skinCycle + 1)
                }

                food = Offset(
                    random.nextFloat() * (width - 60f) + 30f,
                    random.nextFloat() * (height - 60f) + 30f
                )
            }

            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.94f
                p.vy *= 0.94f
                p.life -= 0.02f
                if (p.life <= 0f) iterator.remove()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
        border = BorderStroke(1.2.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFCC))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SNAKE PREVIEW",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(curSkin[0].copy(alpha = 0.15f))
                        .border(1.dp, curSkin[0].copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (skinCycle % 3) {
                            0 -> "VIPER: NEON VIPER"
                            1 -> "VIPER: CYBER GLOW"
                            else -> "VIPER: SOLAR FLARE"
                        },
                        color = curSkin[0],
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B13))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "hex")
                val gridAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.08f,
                    targetValue = 0.18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alphaGrid"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            width = size.width.toFloat()
                            height = size.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                targetOverride = offset
                                repeat(10) {
                                    val r = Random(System.nanoTime())
                                    val ang = r.nextFloat() * 2f * Math.PI.toFloat()
                                    val spd = r.nextFloat() * 3.5f + 1.2f
                                    particles.add(
                                        PreviewParticle(
                                            x = offset.x,
                                            y = offset.y,
                                            vx = cos(ang) * spd,
                                            vy = sin(ang) * spd,
                                            color = curSkin.random(),
                                            life = 1f,
                                            size = r.nextFloat() * 4f + 2f
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    // Hexagonal grid
                    val hexRadius = 18f
                    val dx = hexRadius * 1.5f
                    val dy = hexRadius * kotlin.math.sqrt(3f)

                    for (i in 0..(size.width / dx).toInt() + 1) {
                        for (j in 0..(size.height / dy).toInt() + 1) {
                            val cx = if (j % 2 == 0) i * dx * 2f else i * dx * 2f + dx
                            val cy = j * dy

                            val hexPath = Path().apply {
                                for (corner in 0..5) {
                                    val rad = Math.toRadians(corner * 60.0)
                                    val px = cx + hexRadius * cos(rad).toFloat()
                                    val py = cy + hexRadius * sin(rad).toFloat()
                                    if (corner == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(
                                path = hexPath,
                                color = Color(0xFF1E293B).copy(alpha = gridAlpha),
                                style = Stroke(width = 0.8f)
                            )
                        }
                    }

                    targetOverride?.let { tgt ->
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.35f),
                            radius = 11f + sin(frameTick * 0.15f).absoluteValue * 3f,
                            center = tgt,
                            style = Stroke(width = 1.2f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(tgt.x, 0f),
                            end = Offset(tgt.x, size.height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(0f, tgt.y),
                            end = Offset(size.width, tgt.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.life),
                            radius = p.size * p.life,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // Food
                    val pulseScale = 1.0f + sin(frameTick * 0.18f).absoluteValue * 0.25f
                    val outerGlow = 7.5f * pulseScale
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.15f),
                        radius = outerGlow * 2.2f,
                        center = food
                    )
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.4f),
                        radius = outerGlow * 1.5f,
                        center = food,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = curSkin[1],
                        radius = 4.2f,
                        center = food
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.8f,
                        center = food
                    )

                    // Snake
                    for (i in snakeSegments.indices.reversed()) {
                        val pos = snakeSegments[i]
                        val rPercent = 1.0f - (i.toFloat() / snakeSegments.size.toFloat()) * 0.5f
                        val radius = (7.0f * rPercent).coerceAtLeast(3.2f)

                        drawCircle(
                            color = curSkin[i % curSkin.size].copy(alpha = 0.12f),
                            radius = radius * 2.5f,
                            center = pos
                        )
                        drawCircle(
                            color = curSkin[i % curSkin.size],
                            radius = radius,
                            center = pos
                        )

                        if (i == 0) {
                            drawCircle(
                                color = Color.White,
                                radius = radius * 0.45f,
                                center = Offset(pos.x - radius * 0.2f, pos.y - radius * 0.2f)
                            )
                            // Crown
                            val crownPath = Path().apply {
                                moveTo(pos.x - 7f, pos.y - 6f)
                                lineTo(pos.x - 9f, pos.y - 12f)
                                lineTo(pos.x - 3f, pos.y - 9f)
                                lineTo(pos.x + 0f, pos.y - 15f)
                                lineTo(pos.x + 3f, pos.y - 9f)
                                lineTo(pos.x + 9f, pos.y - 12f)
                                lineTo(pos.x + 7f, pos.y - 6f)
                                close()
                            }
                            drawPath(crownPath, color = Color(0xFFFFD700))
                            drawPath(crownPath, color = Color(0xFFFFC107), style = Stroke(width = 0.8f))
                        }
                    }
                }

                // Overlay text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PREVIEW_SYSTEM: v3.2",
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VIPER",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "SNAKE_LENGTH: ${snakeSegments.size}",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SYS_FPS: 60FPS · STABLE",
                                color = Color(0xFF22C55E),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "TAP TO STEER VIPER",
                            color = Color(0xFF64748B).copy(alpha = 0.8f),
                            fontSize = 7.5.sp,
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
