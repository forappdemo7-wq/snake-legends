package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.*
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowButton
import com.example.ui.components.VirtualJoystick
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val engine = viewModel.gameEngine
    val textMeasurer = rememberTextMeasurer()

    val cyberNeonCityBg = ImageBitmap.imageResource(id = com.example.R.drawable.img_cyber_neon_city_1782378963453)
    val volcanicWastelandBg = ImageBitmap.imageResource(id = com.example.R.drawable.img_volcanic_wasteland_1782378977998)
    val deepSpaceNebulaBg = ImageBitmap.imageResource(id = com.example.R.drawable.img_deep_space_nebula_1782378992376)

    var tickState by remember { mutableStateOf(0) }
    var isWideViewportMode by remember { mutableStateOf(false) }

    // Floating touch inputs
    var joystickAngle by remember { mutableStateOf<Float?>(null) }
    var joystickCenter by remember { mutableStateOf<Offset?>(null) }
    var joystickTouch by remember { mutableStateOf<Offset?>(null) }
    var isBoosting by remember { mutableStateOf(false) }
    val boostButtonScale by animateFloatAsState(
        targetValue = if (isBoosting) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "boostScale"
    )
    var triggerAbility by remember { mutableStateOf(false) }

    val player = engine.playerSnake
    val playerThickness = player?.thicknessFactor ?: 1.0f
    val zoomSizeMultiplier = 1.0f / (1.0f + (playerThickness - 1.0f) * 0.22f)
    val targetScale = (if (isWideViewportMode) 0.58f else 1.0f) * (if (isBoosting) 0.82f else 1.0f) * zoomSizeMultiplier
    val scaleFactor by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "viewportScaleAnim"
    )
    var isPaused by remember { mutableStateOf(false) }
    var lowGraphicsMode by remember { mutableStateOf(false) }
    var joystickOnRightSide by remember { mutableStateOf(false) }
    var isSettingsOpenInPause by remember { mutableStateOf(false) }
    var showSecurityTerminalInPause by remember { mutableStateOf(false) }
    var soundVolumeFraction by remember { mutableStateOf(0.8f) }

    // Run custom high-performance game tick loop synced directly with coroutine clock (60 ticks / frames per second)
    LaunchedEffect(isPaused) {
        while (isActive) {
            if (!isPaused && !engine.isGameOver) {
                if (viewModel.multiplayerManager.authoritativeServer != null) {
                    // SERVER-AUTHORITATIVE MULTIPLAYER: Send inputs ONLY. Server simulates physics and broadcasts snapshots
                    viewModel.multiplayerManager.broadcastPlayerPos(
                        x = 0f,
                        y = 0f,
                        angle = joystickAngle ?: (engine.playerSnake?.angle ?: 0f),
                        speed = 0f,
                        length = 0,
                        score = 0,
                        isBoosting = isBoosting,
                        body = emptyList(),
                        triggerAbility = triggerAbility
                    )
                    if (triggerAbility) {
                        triggerAbility = false // consumer reset
                    }
                } else {
                    // CLIENT-AUTHORITATIVE FALLBACK
                    engine.syncMultiplayerSnakes(viewModel.multiplayerManager.peerSnakes)

                    engine.onTick(
                        joystickAngle = joystickAngle,
                        isBoosting = isBoosting,
                        abilityTriggered = triggerAbility
                    )
                    if (triggerAbility) {
                        triggerAbility = false // consumer reset
                    }

                    // Broadcast local coordinates to Socket.IO room relay
                    engine.playerSnake?.let { p ->
                        val primHex = String.format("#%02X%02X%02X", (p.primaryColor.red * 255).toInt(), (p.primaryColor.green * 255).toInt(), (p.primaryColor.blue * 255).toInt())
                        val secHex = String.format("#%02X%02X%02X", (p.secondaryColor.red * 255).toInt(), (p.secondaryColor.green * 255).toInt(), (p.secondaryColor.blue * 255).toInt())
                        viewModel.multiplayerManager.broadcastPlayerPos(
                            x = p.position.x,
                            y = p.position.y,
                            angle = p.angle,
                            speed = p.speed,
                            length = p.length,
                            score = p.score,
                            isBoosting = p.isBoosting,
                            body = p.body,
                            isAlive = p.isAlive,
                            primaryHex = primHex,
                            secondaryHex = secHex
                        )
                    }
                }

                tickState++
            }
            delay(16) // ~60 FPS update constraints
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep space slate backcolor
    ) {

        // 1. Core 2D Game Canvas (Hardware accelerated coordinate renderer)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_field_canvas")
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Translate camera relative to player position with dynamic camera shake impact physics
            val px = player?.position?.x ?: 1000f
            val py = player?.position?.y ?: 1000f

            val shakeOffset = if (engine.cameraShake > 0f) {
                val s = engine.cameraShake
                Offset(
                    (kotlin.random.Random.nextFloat() * s * 2f) - s,
                    (kotlin.random.Random.nextFloat() * s * 2f) - s
                )
            } else {
                Offset.Zero
            }

            fun worldToViewport(worldPos: Vector2D): Offset {
                val vx = (worldPos.x - px) * scaleFactor + centerX + shakeOffset.x
                val vy = (worldPos.y - py) * scaleFactor + centerY + shakeOffset.y
                return Offset(vx, vy)
            }

            // Universal Cosmic Nebula gas background drifting layer
            val nebulaColors = when (engine.arenaTheme) {
                ArenaTheme.CYBER_CITY -> listOf(Color(0xFFE65100), Color(0xFFFFB74D), Color(0xFFFFD54F))
                ArenaTheme.SPACE_STATION -> listOf(Color(0xFF9933FF), Color(0xFF7E57C2), Color(0xFF4A148C))
                ArenaTheme.LAVA_WORLD -> listOf(Color(0xFFFF4500), Color(0xFFFF8800), Color(0xFFDD2C00))
                ArenaTheme.FROZEN_ARENA -> listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF006064))
                ArenaTheme.JUNGLE_TEMPLE -> listOf(Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFF1B5E20))
                ArenaTheme.NEON_GRID -> listOf(Color(0xFFFF007F), Color(0xFFD500F9), Color(0xAA3D5AFE))
            }
            
            val nebulaCenters = listOf(
                Vector2D(400f, 400f) to (nebulaColors.getOrNull(0) ?: Color(0xFF00FFCC)),
                Vector2D(1600f, 500f) to (nebulaColors.getOrNull(1) ?: Color(0xFFE040FB)),
                Vector2D(600f, 1500f) to (nebulaColors.getOrNull(2) ?: Color(0xFF00FFCC)),
                Vector2D(1400f, 1400f) to (nebulaColors.getOrNull(0) ?: Color(0xFF00FFCC))
            )

            if (!lowGraphicsMode) {
                nebulaCenters.forEachIndexed { idx, pair ->
                    val worldPos = pair.first
                    val color = pair.second
                    // Slowly drift around
                    val driftX = sin(tickState * 0.005f + idx) * 35f
                    val driftY = cos(tickState * 0.005f + idx) * 35f
                    val finalWorldPos = Vector2D(worldPos.x + driftX, worldPos.y + driftY)
                    val viewportPos = worldToViewport(finalWorldPos)
                    val baseRadius = (220f + idx * 40f) * scaleFactor

                    // Slow breathing pulse
                    val pulse = 1.0f + 0.12f * sin(tickState * 0.015f + idx)
                    val currentRadius = baseRadius * pulse

                    // Layer concentric circles to build a beautiful glowing nebula cluster
                    drawCircle(
                        color = color.copy(alpha = 0.02f),
                        radius = currentRadius * 1.8f,
                        center = viewportPos
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.05f),
                        radius = currentRadius * 1.1f,
                        center = viewportPos
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.10f),
                        radius = currentRadius * 0.55f,
                        center = viewportPos
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.025f),
                        radius = currentRadius * 0.22f,
                        center = viewportPos
                    )
                }
            }

            // Draw Background grid / themes
            when (engine.arenaTheme) {
                ArenaTheme.CYBER_CITY -> {
                    // 1. Draw sand tile grid (using viewport aligned iteration)
                    val tileSize = 200f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            // Richly textured alternate dune colors
                            val color = if ((tx + ty) % 2 == 0) Color(0xFFE5A65D) else Color(0xFFDF9E52)
                            drawRect(
                                color = color,
                                topLeft = tileTopLeft,
                                size = Size(tileBottomRight.x - tileTopLeft.x + 1f, tileBottomRight.y - tileTopLeft.y + 1f)
                            )
                        }
                    }

                    // 2. Beautiful sand wind ripples/curves (like dunes)
                    val duneSpacing = 400f
                    val minDX = ((px - centerX / scaleFactor) / duneSpacing).toInt() - 1
                    val maxDX = ((px + centerX / scaleFactor) / duneSpacing).toInt() + 1
                    val minDY = ((py - centerY / scaleFactor) / duneSpacing).toInt() - 1
                    val maxDY = ((py + centerY / scaleFactor) / duneSpacing).toInt() + 1
                    for (dx in minDX..maxDX) {
                        for (dy in minDY..maxDY) {
                            val wx = dx * duneSpacing
                            val wy = dy * duneSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (dx * 13 + dy * 29) % 100
                            if (seed % 3 == 0) {
                                val rippleWidth = 90f * scaleFactor
                                val rippleHeight = 35f * scaleFactor
                                drawArc(
                                    color = Color(0xFFC7843B).copy(alpha = 0.4f),
                                    startAngle = 0f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(vpos.x - rippleWidth / 2, vpos.y - rippleHeight / 2),
                                    size = Size(rippleWidth, rippleHeight),
                                    style = Stroke(width = 2.5f * scaleFactor)
                                )
                            }
                        }
                    }

                    // 3. Render Sand Pit Elements: Oasis, Sinuous wall barriers, Cacti, Dinosaur skeletons, skulls
                    val elementSpacing = 500f
                    val minGX = ((px - centerX / scaleFactor) / elementSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / elementSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / elementSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / elementSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * elementSpacing
                            val wy = gy * elementSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (gx * 17 + gy * 31) % 100

                            // 3a. Oasis Water Pool
                            if (seed % 5 == 0) {
                                val poolRad = (60f + seed % 15) * scaleFactor
                                // Rocky sand rim
                                drawCircle(
                                    color = Color(0xFF7E5225),
                                    radius = poolRad + 10f * scaleFactor,
                                    center = vpos
                                )
                                // Shallow teal shore
                                drawCircle(
                                    color = Color(0xFF26A69A).copy(alpha = 0.6f),
                                    radius = poolRad + 3f * scaleFactor,
                                    center = vpos
                                )
                                // Blue Oasis Water
                                drawCircle(
                                    color = Color(0xFF0288D1),
                                    radius = poolRad,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF29B6F6),
                                    radius = poolRad * 0.6f,
                                    center = vpos
                                )
                            }

                            // 3b. High Quality Saguaro Cactus
                            if (seed % 5 == 1) {
                                val cacH = 50f * scaleFactor
                                val cacW = 12f * scaleFactor
                                // Main trunk
                                drawRect(
                                    color = Color(0xFF2E7D32),
                                    topLeft = Offset(vpos.x - cacW / 2, vpos.y - cacH / 2),
                                    size = Size(cacW, cacH)
                                )
                                // Left arm
                                drawRect(
                                    color = Color(0xFF1B5E20),
                                    topLeft = Offset(vpos.x - cacW * 1.6f, vpos.y - cacH * 0.15f),
                                    size = Size(cacW * 1.6f, cacW)
                                )
                                drawRect(
                                    color = Color(0xFF1B5E20),
                                    topLeft = Offset(vpos.x - cacW * 1.6f, vpos.y - cacH * 0.4f),
                                    size = Size(cacW, cacH * 0.35f)
                                )
                                // Right arm
                                drawRect(
                                    color = Color(0xFF2E7D32),
                                    topLeft = Offset(vpos.x + cacW * 0.5f, vpos.y + cacH * 0.05f),
                                    size = Size(cacW * 1.6f, cacW)
                                )
                                drawRect(
                                    color = Color(0xFF2E7D32),
                                    topLeft = Offset(vpos.x + cacW * 1.1f, vpos.y - cacH * 0.2f),
                                    size = Size(cacW, cacH * 0.3f)
                                )
                            }

                            // 3c. Ancient Dinosaur Skeleton Ribs
                            if (seed % 5 == 2) {
                                val spineLen = 60f * scaleFactor
                                // Draw spine
                                drawLine(
                                    color = Color(0xFFEFEBE9),
                                    start = Offset(vpos.x - spineLen / 2, vpos.y),
                                    end = Offset(vpos.x + spineLen / 2, vpos.y),
                                    strokeWidth = 4.5f * scaleFactor
                                )
                                // Draw 4 ribs
                                repeat(4) { rIdx ->
                                    val rx = vpos.x - spineLen / 2 + rIdx * (spineLen / 3f)
                                    drawArc(
                                        color = Color(0xFFEFEBE9),
                                        startAngle = 180f,
                                        sweepAngle = 180f,
                                        useCenter = false,
                                        topLeft = Offset(rx - 10f * scaleFactor, vpos.y - 20f * scaleFactor),
                                        size = Size(20f * scaleFactor, 40f * scaleFactor),
                                        style = Stroke(width = 3.5f * scaleFactor)
                                    )
                                }
                            }

                            // 3d. Sinuous Sandstone clay walls / paths
                            if (seed % 5 == 3 && !lowGraphicsMode) {
                                val wallLen = 90f * scaleFactor
                                val wallThick = 15f * scaleFactor
                                // Layered 3D clay wall with shadow
                                drawLine(
                                    color = Color(0x33000000), // Shadow
                                    start = Offset(vpos.x - wallLen/2, vpos.y - 8f * scaleFactor),
                                    end = Offset(vpos.x + wallLen/2, vpos.y + 12f * scaleFactor),
                                    strokeWidth = wallThick
                                )
                                drawLine(
                                    color = Color(0xFF8D6E63), // Base rock
                                    start = Offset(vpos.x - wallLen/2, vpos.y - 10f * scaleFactor),
                                    end = Offset(vpos.x + wallLen/2, vpos.y + 10f * scaleFactor),
                                    strokeWidth = wallThick
                                )
                                drawLine(
                                    color = Color(0xFFA1887F), // Highlight
                                    start = Offset(vpos.x - wallLen/2, vpos.y - 10f * scaleFactor),
                                    end = Offset(vpos.x + wallLen/2, vpos.y + 10f * scaleFactor),
                                    strokeWidth = wallThick * 0.4f
                                )
                            }

                            // 3e. Animal Skull Bone
                            if (seed % 5 == 4) {
                                val skullSize = 14f * scaleFactor
                                // Draw skull base
                                drawCircle(
                                    color = Color(0xFFEFEBE9),
                                    radius = skullSize,
                                    center = vpos
                                )
                                // Draw snout
                                drawRect(
                                    color = Color(0xFFEFEBE9),
                                    topLeft = Offset(vpos.x - skullSize * 0.6f, vpos.y),
                                    size = Size(skullSize * 1.2f, skullSize * 1.1f)
                                )
                                // Draw hollow eye sockets
                                drawCircle(
                                    color = Color(0xFF424242),
                                    radius = skullSize * 0.3f,
                                    center = Offset(vpos.x - skullSize * 0.4f, vpos.y - skullSize * 0.2f)
                                )
                                drawCircle(
                                    color = Color(0xFF424242),
                                    radius = skullSize * 0.3f,
                                    center = Offset(vpos.x + skullSize * 0.4f, vpos.y - skullSize * 0.2f)
                                )
                            }
                        }
                    }

                    // Bounds - Desert Canyon Rocky Cliffs
                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    
                    // Outer thick canyon wall border wall
                    drawRect(
                        color = Color(0xFF6D4C41),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 12f * scaleFactor)
                    )
                    // Inner lighter gold sandstone highlight border
                    drawRect(
                        color = Color(0xFFFFD54F),
                        topLeft = Offset(topLeft.x + 9f * scaleFactor, topLeft.y + 9f * scaleFactor),
                        size = Size(
                            (bottomRight.x - topLeft.x) - 18f * scaleFactor,
                            (bottomRight.y - topLeft.y) - 18f * scaleFactor
                        ),
                        style = Stroke(width = 3f * scaleFactor)
                    )
                }

                ArenaTheme.SPACE_STATION -> {
                    // Seamless high-fidelity deep space nebula background image tiling
                    val tileSize = 200f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            val tileW = (tileBottomRight.x - tileTopLeft.x + 1f).toInt()
                            val tileH = (tileBottomRight.y - tileTopLeft.y + 1f).toInt()
                            if (tileW > 0 && tileH > 0) {
                                drawImage(
                                    image = deepSpaceNebulaBg,
                                    srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                    srcSize = androidx.compose.ui.unit.IntSize(deepSpaceNebulaBg.width, deepSpaceNebulaBg.height),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(tileTopLeft.x.toInt(), tileTopLeft.y.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(tileW, tileH)
                                )
                            }
                        }
                    }

                    // Draw star parallax arrays
                    val starsCount = if (lowGraphicsMode) 15 else 50
                    for (i in 0 until starsCount) {
                        val starX = (i * 473 % engine.arenaWidth.toInt()).toFloat()
                        val starY = (i * 911 % engine.arenaHeight.toInt()).toFloat()
                        val starViewport = worldToViewport(Vector2D(starX, starY))

                        if (starViewport.x >= 0 && starViewport.x <= canvasWidth &&
                            starViewport.y >= 0 && starViewport.y <= canvasHeight
                        ) {
                            val pulse = if (i % 2 == 0) (cos(tickState * 0.1f + i) + 1f) / 2f else 1.0f
                            drawCircle(
                                color = Color.White.copy(alpha = 0.3f + pulse * 0.5f),
                                radius = 2f + pulse * 3f,
                                center = starViewport
                            )
                        }
                    }

                    // Draw metal hull panels grid
                    val panelSpacing = 300f
                    var lx = fontSpacingAlign(px, centerX, panelSpacing)
                    val rX = px + centerX + panelSpacing
                    while (lx <= rX) {
                        val vx = (lx - px) * scaleFactor + centerX
                        drawLine(
                            color = Color(0x1F7E57C2),
                            start = Offset(vx, 0f),
                            end = Offset(vx, canvasHeight),
                            strokeWidth = 1.5f
                        )
                        lx += panelSpacing
                    }

                    // Space station mechanical plates & warning indicators
                    val plateSpacing = 600f
                    val minGX = ((px - centerX / scaleFactor) / plateSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / plateSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / plateSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / plateSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * plateSpacing
                            val wy = gy * plateSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (gx * 23 + gy * 41) % 100
                            
                            // Circular Mechanical Ventilator Fan
                            if (seed % 5 == 0) {
                                val radius = 50f * scaleFactor
                                drawCircle(
                                    color = Color(0xFF1E1E2F),
                                    radius = radius,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF9933FF).copy(alpha = 0.3f),
                                    radius = radius,
                                    center = vpos,
                                    style = Stroke(width = 3f * scaleFactor)
                                )
                                repeat(4) { idx ->
                                    val angle = (tickState * 0.02f + idx * Math.PI.toFloat() / 2f)
                                    val bx = vpos.x + cos(angle) * radius * 0.8f
                                    val by = vpos.y + sin(angle) * radius * 0.8f
                                    drawLine(
                                        color = Color(0xFF7E57C2),
                                        start = vpos,
                                        end = Offset(bx, by),
                                        strokeWidth = 4f * scaleFactor
                                    )
                                }
                            }
                            // Yellow/Black warning stripe plates
                            if (seed % 5 == 2 && !lowGraphicsMode) {
                                val w = 60f * scaleFactor
                                val h = 15f * scaleFactor
                                val topLeftOffset = Offset(vpos.x - w / 2, vpos.y - h / 2)
                                drawRect(
                                    color = Color(0xFFE5C100),
                                    topLeft = topLeftOffset,
                                    size = Size(w, h)
                                )
                                var sx = 0f
                                while (sx < w) {
                                    drawLine(
                                        color = Color(0xFF111111),
                                        start = Offset(topLeftOffset.x + sx, topLeftOffset.y),
                                        end = Offset(topLeftOffset.x + sx + 10f * scaleFactor, topLeftOffset.y + h),
                                        strokeWidth = 3f * scaleFactor
                                    )
                                    sx += 15f * scaleFactor
                                }
                            }
                        }
                    }

                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    drawRect(
                        color = Color(0xFF9933FF),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 4f)
                    )
                }

                ArenaTheme.LAVA_WORLD -> {
                    // 1. Seamless high-fidelity volcanic wasteland background image tiling
                    val tileSize = 200f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            val tileW = (tileBottomRight.x - tileTopLeft.x + 1f).toInt()
                            val tileH = (tileBottomRight.y - tileTopLeft.y + 1f).toInt()
                            if (tileW > 0 && tileH > 0) {
                                drawImage(
                                    image = volcanicWastelandBg,
                                    srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                    srcSize = androidx.compose.ui.unit.IntSize(volcanicWastelandBg.width, volcanicWastelandBg.height),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(tileTopLeft.x.toInt(), tileTopLeft.y.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(tileW, tileH)
                                )
                            }
                        }
                    }
                    
                    // 2. Render glowing fault line cracks between basalt plates
                    val faultSpacing = 400f
                    var lx = fontSpacingAlign(px, centerX, faultSpacing)
                    val rX = px + centerX + faultSpacing
                    val pulse = (cos(tickState * 0.05f) + 1.2f) / 2.2f

                    while (lx <= rX) {
                        val vx = (lx - px) * scaleFactor + centerX
                        drawLine(
                            color = Color(0xFFFF3300).copy(alpha = 0.35f + pulse * 0.45f),
                            start = Offset(vx, 0f),
                            end = Offset(vx + 150f * scaleFactor, canvasHeight),
                            strokeWidth = 7f * scaleFactor
                        )
                        drawLine(
                            color = Color(0xFFFFD54F).copy(alpha = 0.45f + pulse * 0.55f),
                            start = Offset(vx, 0f),
                            end = Offset(vx + 150f * scaleFactor, canvasHeight),
                            strokeWidth = 2.2f * scaleFactor
                        )
                        lx += faultSpacing
                    }

                    // 3. Render Volcano Arena Elements: Lava Lakes, Active Volcanic Craters, Lava Rivers, Skulls
                    val elementSpacing = 500f
                    val minGX = ((px - centerX / scaleFactor) / elementSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / elementSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / elementSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / elementSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * elementSpacing
                            val wy = gy * elementSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (gx * 19 + gy * 47) % 100

                            // 3a. Molten Lava Lake
                            if (seed % 4 == 0) {
                                val baseRadius = (60f + seed % 15) * scaleFactor
                                val bubblePulse = sin(tickState * 0.08f + seed) * 4f * scaleFactor
                                
                                drawCircle(
                                    color = Color(0xFF211515),
                                    radius = baseRadius + 12f * scaleFactor,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFFFF3300),
                                    radius = baseRadius,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFFFF9100),
                                    radius = baseRadius * 0.75f,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFFFFEA00).copy(alpha = 0.85f),
                                    radius = baseRadius * 0.45f,
                                    center = vpos
                                )
                                if (!lowGraphicsMode) {
                                    // Rising heat bubbles
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        radius = (6f * scaleFactor + bubblePulse).coerceAtLeast(0.1f),
                                        center = Offset(vpos.x - baseRadius * 0.3f, vpos.y - baseRadius * 0.2f)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        radius = (4f * scaleFactor + bubblePulse * 0.5f).coerceAtLeast(0.1f),
                                        center = Offset(vpos.x + baseRadius * 0.4f, vpos.y + baseRadius * 0.3f)
                                    )
                                }
                            }
                            
                            // 3b. Active smoking volcanic vent/cone
                            if (seed % 4 == 1) {
                                val coneRad = 40f * scaleFactor
                                drawCircle(
                                    color = Color(0xFF1A0F0F),
                                    radius = coneRad,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF3E2723),
                                    radius = coneRad * 0.82f,
                                    center = vpos,
                                    style = Stroke(width = 4f * scaleFactor)
                                )
                                val pulseVent = 1f + sin(tickState * 0.12f + seed) * 0.3f
                                drawCircle(
                                    color = Color(0xFFFF3300),
                                    radius = coneRad * 0.4f * pulseVent,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFFFFEA00),
                                    radius = coneRad * 0.22f,
                                    center = vpos
                                )
                            }

                            // 3c. Lava River/Stream flow path
                            if (seed % 4 == 2 && !lowGraphicsMode) {
                                val streamWidth = 80f * scaleFactor
                                val streamHeight = 30f * scaleFactor
                                drawArc(
                                    color = Color(0xFFFF3300).copy(alpha = 0.7f),
                                    startAngle = 45f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(vpos.x - streamWidth/2, vpos.y - streamHeight/2),
                                    size = Size(streamWidth, streamHeight),
                                    style = Stroke(width = 12f * scaleFactor)
                                )
                                drawArc(
                                    color = Color(0xFFFFEA00).copy(alpha = 0.85f),
                                    startAngle = 45f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(vpos.x - streamWidth/2, vpos.y - streamHeight/2),
                                    size = Size(streamWidth, streamHeight),
                                    style = Stroke(width = 4f * scaleFactor)
                                )
                            }

                            // 3d. Scorched Ribs/Bones
                            if (seed % 4 == 3) {
                                val sizeBone = 35f * scaleFactor
                                drawLine(
                                    color = Color(0xFF7D6060),
                                    start = Offset(vpos.x - sizeBone/2, vpos.y),
                                    end = Offset(vpos.x + sizeBone/2, vpos.y),
                                    strokeWidth = 3f * scaleFactor
                                )
                                repeat(3) { rIdx ->
                                    val rx = vpos.x - sizeBone/2 + rIdx * (sizeBone/2f)
                                    drawLine(
                                        color = Color(0xFF7D6060),
                                        start = Offset(rx, vpos.y - 10f * scaleFactor),
                                        end = Offset(rx, vpos.y + 10f * scaleFactor),
                                        strokeWidth = 2.5f * scaleFactor
                                    )
                                }
                            }
                        }
                    }

                    // Bounds - Thick Volcanic Magma and Black Rock Walls
                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    
                    // Outer dark rocky border wall
                    drawRect(
                        color = Color(0xFF1B0B0B),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 12f * scaleFactor)
                    )
                    // Flowing Lava outer ring
                    drawRect(
                        color = Color(0xFFFF3300),
                        topLeft = Offset(topLeft.x + 6f * scaleFactor, topLeft.y + 6f * scaleFactor),
                        size = Size(
                            (bottomRight.x - topLeft.x) - 12f * scaleFactor,
                            (bottomRight.y - topLeft.y) - 12f * scaleFactor
                        ),
                        style = Stroke(width = 5f * scaleFactor)
                    )
                    // Inner glowing highlight
                    drawRect(
                        color = Color(0xFFFFEA00),
                        topLeft = Offset(topLeft.x + 11f * scaleFactor, topLeft.y + 11f * scaleFactor),
                        size = Size(
                            (bottomRight.x - topLeft.x) - 22f * scaleFactor,
                            (bottomRight.y - topLeft.y) - 22f * scaleFactor
                        ),
                        style = Stroke(width = 1.5f * scaleFactor)
                    )
                }

                ArenaTheme.FROZEN_ARENA -> {
                    // 1. Ice blue tile grid
                    val tileSize = 200f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            // Multi-tone ice textures
                            val color = if ((tx + ty) % 2 == 0) Color(0xFFE0F7FA) else Color(0xFFB2EBF2)
                            drawRect(
                                color = color,
                                topLeft = tileTopLeft,
                                size = Size(tileBottomRight.x - tileTopLeft.x + 1f, tileBottomRight.y - tileTopLeft.y + 1f)
                            )
                        }
                    }

                    // 2. Frost cracks/reflections on the ice
                    val sheetSpacing = 300f
                    var lx = fontSpacingAlign(px, centerX, sheetSpacing)
                    val rX = px + centerX + sheetSpacing
                    val pulse = (sin(tickState * 0.04f) + 1f) / 2f

                    while (lx <= rX) {
                        val vx = (lx - px) * scaleFactor + centerX
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f + pulse * 0.15f),
                            start = Offset(vx, 0f),
                            end = Offset(vx - 100f * scaleFactor, canvasHeight),
                            strokeWidth = 2.5f * scaleFactor
                        )
                        lx += sheetSpacing
                    }

                    // 3. Render Frost Bite Elements: Ice crystal spires, Snowy Pine Trees, Deep Blue pools, snow drifts
                    val frostSpacing = 450f
                    val minGX = ((px - centerX / scaleFactor) / frostSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / frostSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / frostSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / frostSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * frostSpacing
                            val wy = gy * frostSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (gx * 31 + gy * 59) % 100

                            // 3a. Deep Frozen Water Pool
                            if (seed % 4 == 0) {
                                val poolRad = (60f + seed % 15) * scaleFactor
                                drawCircle(
                                    color = Color(0xFFECEFF1),
                                    radius = poolRad + 8f * scaleFactor,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF0091EA),
                                    radius = poolRad,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF00E5FF),
                                    radius = poolRad * 0.65f,
                                    center = vpos
                                )
                            }

                            // 3b. 3D Diamond Ice Spire / Crystal
                            if (seed % 4 == 1) {
                                val sizeCrys = 30f * scaleFactor
                                // Draw left facet
                                val leftFacet = Path().apply {
                                    moveTo(vpos.x, vpos.y - sizeCrys)
                                    lineTo(vpos.x - sizeCrys * 0.6f, vpos.y)
                                    lineTo(vpos.x, vpos.y + sizeCrys)
                                    close()
                                }
                                // Draw right facet
                                val rightFacet = Path().apply {
                                    moveTo(vpos.x, vpos.y - sizeCrys)
                                    lineTo(vpos.x + sizeCrys * 0.6f, vpos.y)
                                    lineTo(vpos.x, vpos.y + sizeCrys)
                                    close()
                                }
                                drawPath(path = leftFacet, color = Color(0xFF80DEEA))
                                drawPath(path = rightFacet, color = Color(0xFF4DD0E1))
                                drawLine(
                                    color = Color.White,
                                    start = Offset(vpos.x, vpos.y - sizeCrys),
                                    end = Offset(vpos.x, vpos.y + sizeCrys),
                                    strokeWidth = 2.5f * scaleFactor
                                )
                            }

                            // 3c. Beautiful Snowy Pine Tree
                            if (seed % 4 == 2) {
                                val treeH = 55f * scaleFactor
                                val treeW = 35f * scaleFactor
                                // Draw brown trunk
                                drawRect(
                                    color = Color(0xFF5D4037),
                                    topLeft = Offset(vpos.x - 4f * scaleFactor, vpos.y),
                                    size = Size(8f * scaleFactor, 15f * scaleFactor)
                                )
                                // Draw 3 overlapping triangular layers of branches
                                repeat(3) { layer ->
                                    val ly = vpos.y - layer * 14f * scaleFactor
                                    val lw = treeW * (1.0f - layer * 0.25f)
                                    val lh = treeH * 0.4f
                                    val pinePath = Path().apply {
                                        moveTo(vpos.x, ly - lh)
                                        lineTo(vpos.x - lw / 2, ly)
                                        lineTo(vpos.x + lw / 2, ly)
                                        close()
                                    }
                                    drawPath(path = pinePath, color = Color(0xFF00796B))
                                    // Snow cap
                                    val snowPath = Path().apply {
                                        moveTo(vpos.x, ly - lh)
                                        lineTo(vpos.x - lw / 3, ly - lh * 0.3f)
                                        lineTo(vpos.x + lw / 3, ly - lh * 0.3f)
                                        close()
                                    }
                                    drawPath(path = snowPath, color = Color.White)
                                }
                            }

                            // 3d. Fluffy Snowdrift / Mound
                            if (seed % 4 == 3 && !lowGraphicsMode) {
                                val snowRad = 24f * scaleFactor
                                drawCircle(
                                    color = Color.White,
                                    radius = snowRad,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFFE0F7FA),
                                    radius = snowRad * 0.8f,
                                    center = Offset(vpos.x - 4f * scaleFactor, vpos.y - 3f * scaleFactor)
                                )
                            }
                        }
                    }

                    // Bounds - Glacier Ice Borders
                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    
                    // Thick glacier wall
                    drawRect(
                        color = Color(0xFF00E5FF),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 12f * scaleFactor)
                    )
                    // Soft snowy cap border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(topLeft.x + 8f * scaleFactor, topLeft.y + 8f * scaleFactor),
                        size = Size(
                            (bottomRight.x - topLeft.x) - 16f * scaleFactor,
                            (bottomRight.y - topLeft.y) - 16f * scaleFactor
                        ),
                        style = Stroke(width = 4f * scaleFactor)
                    )
                }

                ArenaTheme.JUNGLE_TEMPLE -> {
                    // 1. Lush grass tile checkerboard grid
                    val tileSize = 150f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            // Multi-tone checkerboard grass grid
                            val color = if ((tx + ty) % 2 == 0) Color(0xFF33691E) else Color(0xFF2E7D32)
                            drawRect(
                                color = color,
                                topLeft = tileTopLeft,
                                size = Size(tileBottomRight.x - tileTopLeft.x + 1f, tileBottomRight.y - tileTopLeft.y + 1f)
                            )
                        }
                    }

                    // 2. Subtle overgrown moss vines drifting vertically
                    val templeSpacing = 350f
                    var lx = fontSpacingAlign(px, centerX, templeSpacing)
                    val rX = px + centerX + templeSpacing

                    while (lx <= rX) {
                        val vx = (lx - px) * scaleFactor + centerX
                        drawLine(
                            color = Color(0xFF1B5E20).copy(alpha = 0.25f),
                            start = Offset(vx, 0f),
                            end = Offset(vx + 50f * scaleFactor, canvasHeight),
                            strokeWidth = 3f * scaleFactor
                        )
                        lx += templeSpacing
                    }

                    // 3. Render Forest Ruins Elements: Overgrown Stone Ruins, Banyan Bush canopies, pools, runic stones, flowers
                    val jungleSpacing = 450f
                    val minGX = ((px - centerX / scaleFactor) / jungleSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / jungleSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / jungleSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / jungleSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * jungleSpacing
                            val wy = gy * jungleSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val seed = (gx * 37 + gy * 73) % 100

                            // 3a. Deep Temple Moss Water Pool
                            if (seed % 5 == 0) {
                                val pondRad = (55f + seed % 15) * scaleFactor
                                drawCircle(
                                    color = Color(0xFF4E342E), // Mud rim
                                    radius = pondRad + 8f * scaleFactor,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF1B5E20), // Algae/Moss rim
                                    radius = pondRad + 3f * scaleFactor,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF006064), // Deep pond water
                                    radius = pondRad,
                                    center = vpos
                                )
                                drawCircle(
                                    color = Color(0xFF00838F), // Shallows
                                    radius = pondRad * 0.6f,
                                    center = vpos
                                )
                            }

                            // 3b. Overgrown Aztec Stone Pillars / Ruins
                            if (seed % 5 == 1) {
                                val w = 55f * scaleFactor
                                val h = 35f * scaleFactor
                                // Grey brick background
                                drawRect(
                                    color = Color(0xFF546E7A),
                                    topLeft = Offset(vpos.x - w/2, vpos.y - h/2),
                                    size = Size(w, h)
                                )
                                // Inner crack highlight
                                drawRect(
                                    color = Color(0xFF78909C),
                                    topLeft = Offset(vpos.x - w/2 + 2f * scaleFactor, vpos.y - h/2 + 2f * scaleFactor),
                                    size = Size(w - 4f * scaleFactor, h - 4f * scaleFactor),
                                    style = Stroke(width = 1.5f * scaleFactor)
                                )
                                // Overgrowing green vine lines
                                drawLine(
                                    color = Color(0xFF1B5E20),
                                    start = Offset(vpos.x - w/2, vpos.y - h/4),
                                    end = Offset(vpos.x + w/2, vpos.y + h/4),
                                    strokeWidth = 3f * scaleFactor
                                )
                            }

                            // 3c. Overlapping Bush / Banyan Leaf Canopy
                            if (seed % 5 == 2) {
                                val leafRad = 28f * scaleFactor
                                // Shadow base
                                drawCircle(
                                    color = Color(0xFF1B5E20),
                                    radius = leafRad * 1.3f,
                                    center = vpos
                                )
                                // Main dark green leaf
                                drawCircle(
                                    color = Color(0xFF2E7D32),
                                    radius = leafRad,
                                    center = Offset(vpos.x - 8f * scaleFactor, vpos.y - 4f * scaleFactor)
                                )
                                // Highlight medium green leaf
                                drawCircle(
                                    color = Color(0xFF4CAF50),
                                    radius = leafRad * 0.8f,
                                    center = Offset(vpos.x + 8f * scaleFactor, vpos.y + 4f * scaleFactor)
                                )
                                // Pop light green leaf
                                drawCircle(
                                    color = Color(0xFF81C784),
                                    radius = leafRad * 0.5f,
                                    center = Offset(vpos.x, vpos.y - 10f * scaleFactor)
                                )
                            }

                            // 3d. Standing Ancient Runic monolith stone
                            if (seed % 5 == 3) {
                                val stoneW = 20f * scaleFactor
                                val stoneH = 45f * scaleFactor
                                // Arch pillar
                                drawRoundRect(
                                    color = Color(0xFF455A64),
                                    topLeft = Offset(vpos.x - stoneW/2, vpos.y - stoneH/2),
                                    size = Size(stoneW, stoneH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scaleFactor)
                                )
                                // Glowing runic glyph in neon green
                                drawLine(
                                    color = Color(0xFF69F0AE),
                                    start = Offset(vpos.x, vpos.y - stoneH*0.3f),
                                    end = Offset(vpos.x, vpos.y + stoneH*0.3f),
                                    strokeWidth = 3f * scaleFactor
                                )
                            }

                            // 3e. Exotic Tropical Flowers / Mushrooms
                            if (seed % 5 == 4 && !lowGraphicsMode) {
                                val sizeFl = 6f * scaleFactor
                                // Draw a cluster of 3 flowers (red petals, yellow center)
                                repeat(3) { fIdx ->
                                    val fx = vpos.x + (fIdx - 1) * 14f * scaleFactor
                                    val fy = vpos.y + (fIdx % 2) * 8f * scaleFactor
                                    // Petals
                                    drawCircle(
                                        color = Color(0xFFE91E63),
                                        radius = sizeFl,
                                        center = Offset(fx, fy)
                                    )
                                    // Center
                                    drawCircle(
                                        color = Color(0xFFFFEB3B),
                                        radius = sizeFl * 0.4f,
                                        center = Offset(fx, fy)
                                    )
                                }
                            }
                        }
                    }

                    // Bounds - Jungle Canopy Enclosure
                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    
                    // Mossy Stone border
                    drawRect(
                        color = Color(0xFF37474F),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 12f * scaleFactor)
                    )
                    // Hanging Moss / Vines border highlight
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(topLeft.x + 8f * scaleFactor, topLeft.y + 8f * scaleFactor),
                        size = Size(
                            (bottomRight.x - topLeft.x) - 16f * scaleFactor,
                            (bottomRight.y - topLeft.y) - 16f * scaleFactor
                        ),
                        style = Stroke(width = 3f * scaleFactor)
                    )
                }

                ArenaTheme.NEON_GRID -> {
                    // Seamless high-fidelity Cyber-Neon City background image tiling
                    val tileSize = 200f
                    val minTX = ((px - centerX / scaleFactor) / tileSize).toInt() - 1
                    val maxTX = ((px + centerX / scaleFactor) / tileSize).toInt() + 1
                    val minTY = ((py - centerY / scaleFactor) / tileSize).toInt() - 1
                    val maxTY = ((py + centerY / scaleFactor) / tileSize).toInt() + 1

                    for (tx in minTX..maxTX) {
                        for (ty in minTY..maxTY) {
                            val wx = tx * tileSize
                            val wy = ty * tileSize
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val tileTopLeft = worldToViewport(Vector2D(wx, wy))
                            val tileBottomRight = worldToViewport(Vector2D(wx + tileSize, wy + tileSize))
                            
                            val tileW = (tileBottomRight.x - tileTopLeft.x + 1f).toInt()
                            val tileH = (tileBottomRight.y - tileTopLeft.y + 1f).toInt()
                            if (tileW > 0 && tileH > 0) {
                                drawImage(
                                    image = cyberNeonCityBg,
                                    srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                    srcSize = androidx.compose.ui.unit.IntSize(cyberNeonCityBg.width, cyberNeonCityBg.height),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(tileTopLeft.x.toInt(), tileTopLeft.y.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(tileW, tileH)
                                )
                            }
                        }
                    }

                    val gridSpacing = 150f
                    val startX = (px - centerX) - (px - centerX) % gridSpacing
                    val endX = (px + centerX) + gridSpacing
                    val startY = (py - centerY) - (py - centerY) % gridSpacing
                    val endY = (py + centerY) + gridSpacing

                    var x = startX
                    while (x <= endX) {
                        val viewportLineX = (x - px) * scaleFactor + centerX
                        drawLine(
                            color = Color(0x33FF007F), // Neon Pink
                            start = Offset(viewportLineX, 0f),
                            end = Offset(viewportLineX, canvasHeight),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }

                    var y = startY
                    while (y <= endY) {
                        val viewportLineY = (y - py) * scaleFactor + centerY
                        drawLine(
                            color = Color(0x33FF007F), // Neon Pink
                            start = Offset(0f, viewportLineY),
                            end = Offset(canvasWidth, viewportLineY),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }

                    // Glowing neon digital corners/junctions
                    val nodeSpacing = 300f
                    val minGX = ((px - centerX / scaleFactor) / nodeSpacing).toInt() - 1
                    val maxGX = ((px + centerX / scaleFactor) / nodeSpacing).toInt() + 1
                    val minGY = ((py - centerY / scaleFactor) / nodeSpacing).toInt() - 1
                    val maxGY = ((py + centerY / scaleFactor) / nodeSpacing).toInt() + 1

                    for (gx in minGX..maxGX) {
                        for (gy in minGY..maxGY) {
                            val wx = gx * nodeSpacing
                            val wy = gy * nodeSpacing
                            if (wx < 0f || wx > engine.arenaWidth || wy < 0f || wy > engine.arenaHeight) continue
                            val vpos = worldToViewport(Vector2D(wx, wy))
                            val pulse = kotlin.math.abs(sin(tickState * 0.1f + gx + gy))
                            
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f + pulse * 0.25f),
                                radius = 6f * scaleFactor,
                                center = vpos
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 2.5f * scaleFactor,
                                center = vpos
                            )
                        }
                    }

                    // Bounds
                    val topLeft = worldToViewport(Vector2D(0f, 0f))
                    val bottomRight = worldToViewport(Vector2D(engine.arenaWidth, engine.arenaHeight))
                    drawRect(
                        color = Color(0xFFFF007F),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x),
                            (bottomRight.y - topLeft.y)
                        ),
                        style = Stroke(width = 4f)
                    )
                }
            }

            // Draw Battle Royale Safe Zone overlay boundary
            if (engine.gameMode == "Battle Royale") {
                val boundaryCenter = worldToViewport(engine.safeZoneCenter)
                val boundRadius = engine.safeZoneRadius * scaleFactor
                drawCircle(
                    color = Color(0xFFFF3366),
                    radius = boundRadius,
                    center = boundaryCenter,
                    style = Stroke(width = 3f)
                )

                // draw alert tick outline
                drawCircle(
                    color = Color.White.copy(alpha = (cos(tickState * 0.1f) + 1f) / 2f * 0.6f),
                    radius = boundRadius + 5f,
                    center = boundaryCenter,
                    style = Stroke(width = 1.5f)
                )
            }

            // Draw Hazards
            for (hazard in engine.hazards) {
                val viewportPos = worldToViewport(hazard.position)
                val rad = hazard.size * scaleFactor
                if (viewportPos.x >= -rad && viewportPos.x <= canvasWidth + rad &&
                    viewportPos.y >= -rad && viewportPos.y <= canvasHeight + rad
                ) {
                    when (hazard.type) {
                        "electro_gate" -> {
                            val pulse = (sin(tickState * 0.15f) + 1f) / 2f
                            val color = if ((hazard.state % 120) < 84) Color(0xFF00FFCC) else Color(0xFFFF3333).copy(alpha = 0.4f)
                            drawCircle(
                                color = color.copy(alpha = 0.12f),
                                radius = rad * (1.2f + pulse * 0.2f),
                                center = viewportPos
                            )
                            drawCircle(
                                color = color.copy(alpha = 0.8f),
                                radius = rad,
                                center = viewportPos,
                                style = Stroke(width = 3f)
                            )
                            if ((hazard.state % 120) < 84) {
                                drawLine(
                                    color = Color.White,
                                    start = Offset(viewportPos.x - rad * 0.5f, viewportPos.y),
                                    end = Offset(viewportPos.x + rad * 0.5f, viewportPos.y),
                                    strokeWidth = 2f
                                )
                            }
                        }
                        "lava_pit" -> {
                            val pulse = (cos(tickState * 0.08f) + 1f) / 2f
                            drawCircle(
                                color = Color(0xFFFF3300).copy(alpha = 0.2f + pulse * 0.15f),
                                radius = rad * 1.3f,
                                center = viewportPos
                            )
                            drawCircle(
                                color = Color(0xFFFF5500),
                                radius = rad,
                                center = viewportPos
                            )
                            drawCircle(
                                color = Color(0xFFFFCC00),
                                radius = rad * 0.5f * (0.8f + pulse * 0.4f),
                                center = viewportPos
                            )
                        }
                        "ice_spike" -> {
                            val pulse = (sin(tickState * 0.05f) + 1f) / 2f
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                radius = rad * 1.2f,
                                center = viewportPos
                            )
                            drawCircle(
                                color = Color(0xFFE0F7FA),
                                radius = rad * (0.7f + pulse * 0.2f),
                                center = viewportPos,
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = rad * 0.3f,
                                center = viewportPos
                            )
                        }
                        "totem" -> {
                            val pulse = (sin(tickState * 0.06f) + 1f) / 2f
                            drawCircle(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f + pulse * 0.1f),
                                radius = rad * 1.5f,
                                center = viewportPos
                            )
                            drawCircle(
                                color = Color(0xFF81C784),
                                radius = rad,
                                center = viewportPos,
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = rad * 0.4f,
                                center = viewportPos
                            )
                        }
                        "quantum_vortex" -> {
                            val pulse = (cos(tickState * 0.1f) + 1f) / 2f
                            for (j in 0..2) {
                                val radiusMult = 1f - j * 0.25f
                                drawCircle(
                                    color = Color(0xFF9933FF).copy(alpha = 0.15f + j * 0.1f),
                                    radius = rad * radiusMult * (1f + pulse * 0.15f),
                                    center = viewportPos
                                )
                            }
                            drawCircle(
                                color = Color.Black,
                                radius = rad * 0.35f,
                                center = viewportPos
                            )
                        }
                        "neon_gate" -> {
                            val isBlocked = (hazard.state % 180) > 90
                            val laserColor = if (isBlocked) Color(0xFFFF007F) else Color(0x33FF007F)
                            drawCircle(
                                color = laserColor.copy(alpha = 0.15f),
                                radius = rad * 1.2f,
                                center = viewportPos
                            )
                            drawCircle(
                                color = laserColor,
                                radius = rad * 0.8f,
                                center = viewportPos,
                                style = Stroke(width = 3.5f)
                            )
                        }
                    }
                }
            }

            // Draw Orbs
            for (orb in engine.orbs) {
                val viewportPos = worldToViewport(orb.position)
                val rad = orb.size * scaleFactor

                if (viewportPos.x >= -rad && viewportPos.x <= canvasWidth + rad &&
                    viewportPos.y >= -rad && viewportPos.y <= canvasHeight + rad
                ) {
                    if (orb.isCelestialOrb) {
                        // Pulsing, shifting dual halo
                        val pulse = 1f + (sin(tickState * 0.15f) + 1f) / 2f * 0.35f
                        drawCircle(
                            color = orb.color.copy(alpha = 0.18f),
                            radius = rad * 3.2f * pulse,
                            center = viewportPos
                        )
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.45f),
                            radius = rad * 2.0f,
                            center = viewportPos,
                            style = Stroke(width = 2.5f * scaleFactor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), tickState * 0.6f))
                        )
                    } else {
                        // Outer glow
                        drawCircle(
                            color = orb.color.copy(alpha = 0.25f),
                            radius = rad * 2.2f,
                            center = viewportPos
                        )
                    }
                    // Inner solid
                    drawCircle(
                        color = orb.color,
                        radius = rad,
                        center = viewportPos
                    )
                    // Core highlight
                    drawCircle(
                        color = Color.White,
                        radius = rad * 0.4f,
                        center = viewportPos
                    )
                }
            }

            // Draw Arena Power-Ups
            for (powerUp in engine.powerUps) {
                val viewportPos = worldToViewport(powerUp.position)
                val rad = powerUp.size * scaleFactor
                
                if (viewportPos.x >= -rad && viewportPos.x <= canvasWidth + rad &&
                    viewportPos.y >= -rad && viewportPos.y <= canvasHeight + rad
                ) {
                    val pulse = 1f + (sin(tickState * 0.15f) + 1f) / 2f * 0.25f
                    val sizeGlow = rad * 2.8f * pulse
                    
                    drawCircle(
                        color = powerUp.color.copy(alpha = 0.18f),
                        radius = sizeGlow,
                        center = viewportPos
                    )
                    
                    drawCircle(
                        color = powerUp.color.copy(alpha = 0.75f),
                        radius = rad * pulse,
                        center = viewportPos,
                        style = Stroke(width = 2.5f * scaleFactor)
                    )
                    
                    drawCircle(
                        color = powerUp.color,
                        radius = rad * 0.5f,
                        center = viewportPos
                    )
                    
                    drawCircle(
                        color = Color.White,
                        radius = rad * 0.25f,
                        center = viewportPos
                    )
                    
                    val perkName = when (powerUp.type) {
                        PowerUpType.MAGNET -> "MAGNET"
                        PowerUpType.DOUBLE_POINTS -> "2X PTS"
                        PowerUpType.SHIELD -> "SHIELD"
                        PowerUpType.GROWTH -> "GROWTH"
                        PowerUpType.GHOST -> "GHOST"
                        PowerUpType.SPEED_BOOST -> "BOOST"
                    }
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(perkName),
                        style = TextStyle(
                            color = powerUp.color,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            background = Color.Black.copy(alpha = 0.65f)
                        )
                    )
                    drawText(
                        textLayoutResult,
                        topLeft = Offset(viewportPos.x - textLayoutResult.size.width / 2f, viewportPos.y - rad * 2.2f)
                    )
                }
            }

            // Draw Snakes (Body segments & Heads)
            for (snake in engine.snakes) {
                if (!snake.isAlive) continue

                // Body rendering via segments list (drawn backwards so head sits on top)
                for (i in snake.body.size - 1 downTo 1 step 2) {
                    val pos = snake.body[i]
                    val viewPos = worldToViewport(pos)
                    val segmentDiameter = 18f * scaleFactor * snake.thicknessFactor

                    if (viewPos.x >= -segmentDiameter && viewPos.x <= canvasWidth + segmentDiameter &&
                        viewPos.y >= -segmentDiameter && viewPos.y <= canvasHeight + segmentDiameter
                    ) {
                        // Vary body diameter from tail to head
                        val scaleRatio = (1f - (i.toFloat() / snake.body.size.toFloat()) * 0.4f)
                        val rad = (11f * scaleRatio * snake.thicknessFactor) * scaleFactor

                        val primaryColor = if (snake.activePowerUpType == PowerUpType.GHOST) Color.White.copy(alpha = 0.35f) else snake.primaryColor
                        val secondaryColor = if (snake.activePowerUpType == PowerUpType.GHOST) Color(0xFF90A4AE).copy(alpha = 0.2f) else snake.secondaryColor

                        val gradBrush = Brush.radialGradient(
                            colors = listOf(primaryColor, secondaryColor),
                            center = viewPos,
                            radius = rad
                        )

                        drawCircle(
                            brush = gradBrush,
                            radius = rad,
                            center = viewPos
                        )

                        // Light shine scale dot
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = rad * 0.4f,
                            center = viewPos - Offset(rad * 0.2f, rad * 0.2f)
                        )
                    }
                }

                // Render Head
                val headPos = worldToViewport(snake.position)
                val headRad = 15f * scaleFactor * snake.thicknessFactor

                // Draw Head Outer Shell
                val headColor = if (snake.activePowerUpType == PowerUpType.GHOST) Color.White.copy(alpha = 0.45f) else snake.primaryColor
                drawCircle(
                    color = headColor,
                    radius = headRad,
                    center = headPos
                )

                // Face orientation details
                val ex = cos(snake.angle.toDouble()).toFloat()
                val ey = sin(snake.angle.toDouble()).toFloat()
                val eyesX = headPos.x + ex * headRad * 0.5f
                val eyesY = headPos.y + ey * headRad * 0.5f

                // Eyes details
                val leftEyeAngle = snake.angle + 0.4f
                val rightEyeAngle = snake.angle - 0.4f
                val leX = headPos.x + cos(leftEyeAngle.toDouble()).toFloat() * headRad * 0.6f
                val leY = headPos.y + sin(leftEyeAngle.toDouble()).toFloat() * headRad * 0.6f
                val reX = headPos.x + cos(rightEyeAngle.toDouble()).toFloat() * headRad * 0.6f
                val reY = headPos.y + sin(rightEyeAngle.toDouble()).toFloat() * headRad * 0.6f

                val eyeRadius = 3.5f * scaleFactor * snake.thicknessFactor
                val pupilRadius = 1.5f * scaleFactor * snake.thicknessFactor

                drawCircle(Color.White, radius = eyeRadius, center = Offset(leX, leY))
                drawCircle(Color.Black, radius = pupilRadius, center = Offset(leX, leY))
                drawCircle(Color.White, radius = eyeRadius, center = Offset(reX, reY))
                drawCircle(Color.Black, radius = pupilRadius, center = Offset(reX, reY))

                // If player shield is active, draw neon forcefield ring
                if (snake.specialAbilityActive || snake.activePowerUpType == PowerUpType.SHIELD) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.3f + (sin(tickState * 0.2f) + 1f) / 2f * 0.3f),
                        radius = headRad * 2.5f,
                        center = headPos,
                        style = Stroke(width = 3f)
                    )
                }

                // If magnet active, draw pulsing ring
                if (snake.activePowerUpType == PowerUpType.MAGNET) {
                    drawCircle(
                        color = Color(0xFFE040FB).copy(alpha = 0.25f + (sin(tickState * 0.22f) + 1f) / 2f * 0.25f),
                        radius = headRad * 2.5f,
                        center = headPos,
                        style = Stroke(width = 2f * scaleFactor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), tickState * 0.5f))
                    )
                }

                // If double points active, draw gold shine ring
                if (snake.activePowerUpType == PowerUpType.DOUBLE_POINTS) {
                    drawCircle(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.25f + (sin(tickState * 0.18f) + 1f) / 2f * 0.25f),
                        radius = headRad * 2.2f,
                        center = headPos,
                        style = Stroke(width = 2.5f)
                    )
                }

                // If ghost mode active, draw pulsing ring (platinum color)
                if (snake.activePowerUpType == PowerUpType.GHOST) {
                    drawCircle(
                        color = Color(0xFFB0BEC5).copy(alpha = 0.3f + (sin(tickState * 0.15f) + 1f) / 2f * 0.3f),
                        radius = headRad * 2.3f,
                        center = headPos,
                        style = Stroke(width = 2.5f * scaleFactor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), tickState * 0.8f))
                    )
                }

                // If speed boost active, draw blazing speed ring
                if (snake.activePowerUpType == PowerUpType.SPEED_BOOST) {
                    drawCircle(
                        color = Color(0xFFFF5722).copy(alpha = 0.4f + (sin(tickState * 0.3f) + 1f) / 2f * 0.4f),
                        radius = headRad * 2.4f,
                        center = headPos,
                        style = Stroke(width = 3.5f * scaleFactor)
                    )
                }

                // Draw Name Tag label
                if (headPos.x >= 0 && headPos.x <= canvasWidth &&
                    headPos.y >= 0 && headPos.y <= canvasHeight
                ) {
                    val label = if (snake.isPlayer) "${snake.name} (${snake.score})" else snake.name
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            background = Color.Black.copy(alpha = 0.4f)
                        )
                    )
                    drawText(
                        textLayoutResult,
                        topLeft = Offset(headPos.x - textLayoutResult.size.width / 2f, headPos.y - headRad - 22f)
                    )
                }
            }

            // Draw Cosmos Particles
            if (!lowGraphicsMode) {
                for (p in engine.particles) {
                    val pView = worldToViewport(p.position)
                    val r = p.size * scaleFactor
                    if (pView.x >= -50f && pView.x <= canvasWidth + 50f && pView.y >= -50f && pView.y <= canvasHeight + 50f) {
                        if (p.isStar) {
                            // Thin sparkling white-core cross
                            val spikeLength = r * 2.3f
                            val strokeW = (r * 0.25f).coerceAtLeast(1.5f)
                            drawLine(
                                color = Color.White.copy(alpha = p.alpha),
                                start = Offset(pView.x, pView.y - spikeLength),
                                end = Offset(pView.x, pView.y + spikeLength),
                                strokeWidth = strokeW
                            )
                            drawLine(
                                color = Color.White.copy(alpha = p.alpha),
                                start = Offset(pView.x - spikeLength, pView.y),
                                end = Offset(pView.x + spikeLength, pView.y),
                                strokeWidth = strokeW
                            )
                            // Outer cosmic halo glow
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha * 0.45f),
                                radius = r * 1.5f,
                                center = pView
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = p.alpha * 0.9f),
                                radius = r * 0.45f,
                                center = pView
                            )
                        } else if (p.isNebula) {
                            // Fluffy expanding gaseous space dust puff
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha * 0.08f),
                                radius = r * 3.2f,
                                center = pView
                            )
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha * 0.28f),
                                radius = r * 1.8f,
                                center = pView
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = p.alpha * 0.15f),
                                radius = r * 0.7f,
                                center = pView
                            )
                        } else {
                            // Standard micro-spark particle
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha),
                                radius = r,
                                center = pView
                            )
                        }
                    }
                }
            }

            // Draw Floating texts
            for (txt in engine.floatingTexts) {
                val viewPos = worldToViewport(txt.position)
                if (viewPos.x >= 0 && viewPos.x <= canvasWidth && viewPos.y >= 0 && viewPos.y <= canvasHeight) {
                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString(txt.text),
                        style = TextStyle(
                            color = txt.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 3f)
                        )
                    )
                    drawText(
                        textLayout,
                        topLeft = Offset(viewPos.x - textLayout.size.width / 2f, viewPos.y),
                        alpha = txt.alpha
                    )
                }
            }

            // 1.1 Boosting vignette edge glow & speed-lines effect
            if (isBoosting) {
                // Outer vignette red-orange aura
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFFFF3366).copy(alpha = 0.15f)),
                        center = Offset(centerX, centerY),
                        radius = maxOf(canvasWidth, canvasHeight) * 0.7f
                    ),
                    size = size
                )
                
                // Custom wind stream speed lines flying towards the center or past the screen!
                if (!lowGraphicsMode) {
                    val random = kotlin.random.Random(1337)
                    for (i in 0 until 18) {
                        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                        val speedFactor = 15f
                        val baseDist = (random.nextFloat() * 500f + 200f)
                        val offsetDist = (baseDist + (tickState * speedFactor)) % 800f
                        
                        val startX = centerX + cos(angle.toDouble()).toFloat() * offsetDist
                        val startY = centerY + sin(angle.toDouble()).toFloat() * offsetDist
                        val endX = centerX + cos(angle.toDouble()).toFloat() * (offsetDist + 40f)
                        val endY = centerY + sin(angle.toDouble()).toFloat() * (offsetDist + 40f)
                        
                        drawLine(
                            color = Color(0xFFFFDD44).copy(alpha = (1f - (offsetDist / 800f)).coerceIn(0f, 0.4f)),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }

        // 2. HUD Interface Elements Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // CONTROLLER & GENERAL ACTIONS BAR
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PAUSE BUTTON
                IconButton(
                    onClick = { isPaused = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.7f), CircleShape)
                        .testTag("in_game_pause")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause Game", tint = Color(0xFF00FFCC))
                }

                // TABLET WIDE VIEW DYNAMIC SCALE HOT-TOGGLE
                Button(
                    onClick = { isWideViewportMode = !isWideViewportMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWideViewportMode) Color(0xFF00FFCC) else Color.Black.copy(alpha = 0.5f),
                        contentColor = if (isWideViewportMode) Color.Black else Color(0xFF00FFCC)
                    ),
                    border = BorderStroke(2.dp, Color(0xFF00FFCC).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("in_game_tablet_view_toggle")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWideViewportMode) Icons.Default.ZoomIn else Icons.Default.AspectRatio,
                            contentDescription = "Tablet View Toggle",
                            modifier = Modifier.size(16.dp),
                            tint = if (isWideViewportMode) Color.Black else Color(0xFF00FFCC)
                        )
                        Text(
                            text = if (isWideViewportMode) "TABLET VIEW" else "MOBILE VIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // DYNAMIC HIGH-VISIBILITY KILLFEED
            val currentKills = remember(tickState) {
                synchronized(engine.killEvents) {
                    engine.killEvents.toList()
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 60.dp)
                    .width(220.dp)
                    .testTag("in_game_killfeed"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentKills.forEach { kill ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (kill.killerName == player?.name || kill.victimName == player?.name) {
                                            Color(0xFF00FFCC) // Dynamic glowing highlight on player actions
                                        } else {
                                            Color(0xFFFF3366) // Combat notification crimson/magenta
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = kill.killerName ?: "GRID DANGER",
                                        color = if (kill.killerName == player?.name) Color(0xFF00FFCC) else Color(0xFFF1F5F9),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    
                                    Text(
                                        text = kill.weaponOrCause,
                                        color = Color(0xFFFFCC00),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "ELIMINATED",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = kill.victimName,
                                        color = if (kill.victimName == player?.name) Color(0xFFFF3366) else Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SURVIVORS REMAINING DETAILS / BATTLE ROYALE ALERTS
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color(0x3300FFCC), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = engine.gameMode.uppercase(),
                    color = Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${engine.alivePlayersCount} SURVIVORS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // ACTIVE MAP EVENT OVERLAY
            if (engine.activeWeather != "NORMAL") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF3366).copy(alpha = 0.85f),
                                    Color(0xFF990033).copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFFF3366), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Event Warning",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = engine.activeEventName.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // LIVE SCOREBOARD & NETWORK TELEMETRY PANEL
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(130.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Live Network Telemetry
                val connStatus by viewModel.multiplayerManager.connectionStatus.collectAsState()
                val pingVal by viewModel.multiplayerManager.pingMs.collectAsState()
                val packetLossVal by viewModel.multiplayerManager.packetLoss.collectAsState()

                val statusLabel: String
                val statusColor: Color
                when (connStatus) {
                    ConnectionStatus.OFFLINE -> {
                        statusLabel = "LOCAL SYNCS"
                        statusColor = Color.Gray
                    }
                    ConnectionStatus.CONNECTING -> {
                        statusLabel = "CONNECTING"
                        statusColor = Color(0xFFFF9900)
                    }
                    ConnectionStatus.HANDSHARING -> {
                        statusLabel = "HANDSHAKE"
                        statusColor = Color(0xFFCC00FF)
                    }
                    ConnectionStatus.CONNECTED -> {
                        statusLabel = "LIVE NET"
                        statusColor = Color(0xFF00FFCC)
                    }
                    ConnectionStatus.DISCONNECTED -> {
                        statusLabel = "DISCONNECTED"
                        statusColor = Color(0xFFFF3366)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = "REGION: ${viewModel.multiplayerManager.selectedRegion.name.take(6)}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        if (connStatus == ConnectionStatus.CONNECTED) {
                            Text(
                                text = "LATENCY: ${pingVal}ms",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (packetLossVal > 0f) {
                                Text(
                                    text = "LOSS: ${String.format("%.1f%%", packetLossVal * 100f)}",
                                    color = Color(0xFFFF3366),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // GRID LEADERS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "GRID LEADERS",
                            color = Color(0xFFFF9900),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        engine.rankingList.take(5).forEachIndexed { i, record ->
                            val label = if (record.first.contains("You")) "YOU" else record.first.take(8)
                            val col = if (record.first.contains("You")) Color(0xFF00FFCC) else Color.LightGray
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${i + 1}. $label", color = col, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("${record.second}", color = Color.White, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            // DANGER COLLAPSE WARNING (Immersive full-screen pulsing vignette + clean top pill HUD)
            if (engine.gameMode == "Battle Royale" && player?.isAlive == true) {
                val dist = player.position.distance(engine.safeZoneCenter)
                if (dist > engine.safeZoneRadius) {
                    // Soft pulsing glow vignette across entire viewport boundaries (never blocks middle-screen)
                    val pulseAlpha by rememberInfiniteTransition(label = "danger_pulse").animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.65f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "danger_pulse_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 4.dp, color = Color(0xFFFF1744).copy(alpha = pulseAlpha))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFF1744).copy(alpha = pulseAlpha * 0.35f)
                                    ),
                                    radius = 1800f
                                )
                            )
                    )

                    // Sits perfectly at the top, cleanly integrated below the server info counters
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 54.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE67F1D1D)) // rich warm safety red
                            .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Zone Collapse warning",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "RADIATION DETECTED - RE-ENTER SAFE ZONE!",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // UPGRADED ARCADE SCORE / LENGTH HUD CAPSULE (Cleanly centered on the bottom bezel, leaving gameplay 100% visible)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-12).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF0070B13))
                    .border(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Length stat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Text(
                            text = "LENGTH",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${player?.length ?: 0}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(12.dp)
                            .background(Color(0xFF334155))
                    )

                    // Score stat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FFCC))
                        )
                        Text(
                            text = "SCORE",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${player?.score ?: 0}",
                            color = Color(0xFF00FFCC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ACTIVE POWER-UP HUD BANNER OVERLAY (Cleanly floats just above the bottom HUD capsule)
            if (player?.activePowerUpType != null) {
                val powerUpType = player.activePowerUpType!!
                val label = when (powerUpType) {
                    PowerUpType.MAGNET -> "MAGNET FORCEFIELD"
                    PowerUpType.DOUBLE_POINTS -> "2X SCORE MULTIPLIER"
                    PowerUpType.SHIELD -> "DEFENSE FORCE FIELD"
                    PowerUpType.GROWTH -> "GROWTH POTION"
                    PowerUpType.GHOST -> "GHOST CHRONO-PHASE"
                    PowerUpType.SPEED_BOOST -> "LIGHTSPEED PROPULSION"
                }
                val color = when (powerUpType) {
                    PowerUpType.MAGNET -> Color(0xFFE040FB)
                    PowerUpType.DOUBLE_POINTS -> Color(0xFFFFEB3B)
                    PowerUpType.SHIELD -> Color(0xFF00E5FF)
                    PowerUpType.GROWTH -> Color(0xFF66BB6A)
                    PowerUpType.GHOST -> Color(0xFFB0BEC5)
                    PowerUpType.SPEED_BOOST -> Color(0xFFFF5722)
                }
                val secondsLeft = (player.powerUpTimer / 60f).coerceAtLeast(0f)
                val formattedTime = String.format("%.1f s", secondsLeft)
                
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-58).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.5.dp, color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = "$label: $formattedTime".uppercase(),
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            // VIRTUAL FLOATING JOYSTICK TOUCH SURFACE
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .fillMaxWidth(0.55f)
                    .align(if (joystickOnRightSide) Alignment.BottomEnd else Alignment.BottomStart)
                    .pointerInput(joystickOnRightSide) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val initialPos = down.position
                                joystickCenter = initialPos
                                joystickTouch = initialPos
                                
                                drag(down.id) { change ->
                                    change.consume()
                                    joystickTouch = change.position
                                    val diffX = change.position.x - initialPos.x
                                    val diffY = change.position.y - initialPos.y
                                    if (diffX != 0f || diffY != 0f) {
                                        joystickAngle = atan2(diffY, diffX)
                                    }
                                }
                                
                                joystickCenter = null
                                joystickTouch = null
                            }
                        }
                    }
            )

            // RENDER THE FLOATING JOYSTICK VISUALS
            joystickCenter?.let { center ->
                val touch = joystickTouch ?: center
                val diff = touch - center
                val dist = sqrt(diff.x * diff.x + diff.y * diff.y)
                val maxRadiusPx = with(LocalDensity.current) { 55.dp.toPx() }
                val clampedOffset = if (dist > maxRadiusPx) {
                    val angle = atan2(diff.y, diff.x)
                    Offset(cos(angle) * maxRadiusPx, sin(angle) * maxRadiusPx)
                } else {
                    diff
                }
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(LocalDensity.current) { (center.x - 65.dp.toPx()).toDp() },
                            y = with(LocalDensity.current) { (center.y - 65.dp.toPx()).toDp() }
                        )
                        .size(130.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cX = size.width / 2f
                        val cY = size.height / 2f
                        drawCircle(
                            color = Color(0x2200FFCC),
                            radius = cX
                        )
                        drawCircle(
                            color = Color(0x6600FFCC),
                            radius = cX,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        for (i in 0 until 8) {
                            val a = i * (6.28f / 8f)
                            val rOuter = cX
                            val rInner = rOuter - 8.dp.toPx()
                            drawLine(
                                color = Color(0x5500FFCC),
                                start = Offset(cX + rInner * cos(a), cY + rInner * sin(a)),
                                end = Offset(cX + rOuter * cos(a), cY + rOuter * sin(a)),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { (clampedOffset.x + 65.dp.toPx() - 22.dp.toPx()).toDp() },
                                y = with(LocalDensity.current) { (clampedOffset.y + 65.dp.toPx() - 22.dp.toPx()).toDp() }
                            )
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF00FFCC), Color(0xFF0099FF))
                                )
                            )
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            // BOOST / ACTION BUTTONS PANEL
            Row(
                modifier = Modifier
                    .align(if (joystickOnRightSide) Alignment.BottomStart else Alignment.BottomEnd)
                    .offset(
                        x = if (joystickOnRightSide) 10.dp else (-10).dp,
                        y = (-20).dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SPECIAL SELECTABLE TACTICAL COMBAT ABILITY BUTTON
                val abilityType = player?.activeAbilityType ?: "SHIELD"
                val cdMax = when(abilityType) {
                    "SHIELD" -> 540f
                    "FREEZE_PULSE" -> 600f
                    "EMP_BLAST" -> 660f
                    "SPEED_BURST" -> 420f
                    "GHOST_PHASE" -> 540f
                    else -> 1f
                }
                val actMax = when(abilityType) {
                    "SHIELD" -> 180f
                    "SPEED_BURST" -> 100f
                    "GHOST_PHASE" -> 180f
                    else -> 1f
                }
                val cooldownFraction = if (player != null) {
                    if (player.specialAbilityActive) {
                        if (abilityType == "FREEZE_PULSE" || abilityType == "EMP_BLAST") 0f 
                        else player.abilityActiveDuration.toFloat() / actMax
                    } else if (player.abilityCooldownRemaining > 0) {
                        player.abilityCooldownRemaining.toFloat() / cdMax
                    } else {
                        0f
                    }
                } else {
                    0f
                }
                
                val abilityIcon = when(abilityType) {
                    "SHIELD" -> Icons.Default.Shield
                    "FREEZE_PULSE" -> Icons.Default.AcUnit
                    "EMP_BLAST" -> Icons.Default.FlashOn
                    "SPEED_BURST" -> Icons.Default.Bolt
                    "GHOST_PHASE" -> Icons.Default.Widgets
                    else -> Icons.Default.Star
                }
                val abColor = when(abilityType) {
                    "SHIELD" -> Color(0xFF00E5FF)
                    "FREEZE_PULSE" -> Color(0xFF80D8FF)
                    "EMP_BLAST" -> Color(0xFFFFEE55)
                    "SPEED_BURST" -> Color(0xFFFF5722)
                    "GHOST_PHASE" -> Color(0xFFB0BEC5)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("ability_shield_button")
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(
                            2.dp,
                            if (player?.specialAbilityActive == true) abColor else abColor.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .pointerInteropFilter { ev ->
                            when (ev.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if (player != null && player.abilityCooldownRemaining <= 0 && !player.specialAbilityActive) {
                                        triggerAbility = true
                                    }
                                }
                            }
                            true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (cooldownFraction > 0f) {
                        CircularProgressIndicator(
                            progress = { cooldownFraction },
                            color = abColor,
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Icon(
                        imageVector = abilityIcon,
                        contentDescription = "Activate Tactical Special Ability",
                        tint = if (player?.specialAbilityActive == true) abColor else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // DYNAMIC BOOST / SPEED ACTION BUTTON
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = boostButtonScale, scaleY = boostButtonScale)
                        .size(76.dp)
                        .testTag("boost_dash_button")
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isBoosting) Color(0xFFFACC15) else Color(0xFFFF3366),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            width = if (isBoosting) 4.dp else 3.dp,
                            color = if (isBoosting) Color(0xFFFACC15) else Color(0xFFFF3366),
                            shape = CircleShape
                        )
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> isBoosting = true
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isBoosting = false
                            }
                            true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "DASH BOOST ACTIVE",
                        tint = if (isBoosting) Color.Black else Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 3. GAME OVER OVERLAY SCREEN (Full modal overlay card)
        AnimatedVisibility(
            visible = engine.isGameOver,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    borderColor = Color(0xFFFF3366).copy(alpha = 0.5f),
                    backgroundColor = Color(0xFA0F1426),
                    glowColor = Color(0xFFFF3366)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (engine.rankingPlacement == 1) Icons.Default.EmojiEvents else Icons.Default.Cancel,
                            contentDescription = "Completed",
                            tint = if (engine.rankingPlacement == 1) Color(0xFFFFFF33) else Color(0xFFFF3366),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (engine.rankingPlacement == 1) "VICTORY ROYALE" else "SLITHER CRASH!",
                            color = if (engine.rankingPlacement == 1) Color(0xFFFFFF33) else Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = if (engine.rankingPlacement == 1) "YOU ARE THE LAST SURVIVOR" else "GAME OVER",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Match stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatSummaryBadge("PLACEMENT", "${engine.rankingPlacement} / ${engine.rankingPlacement + engine.alivePlayersCount}", Color.LightGray)
                            StatSummaryBadge("SCORE", "${player?.score ?: 0}", Color(0xFF00FFCC))
                            StatSummaryBadge("KILLS", "${engine.totalKills}", Color(0xFFFF5252))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Rewards earned
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("BOUNTIES RECOVERED", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("+${engine.totalXpEarned} XP", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("+${engine.totalCoinsEarned} Coins", color = Color(0xFFFFFF33), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action button
                        Button(
                            onClick = {
                                viewModel.finishActiveGameAndSave()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_rewards_exit")
                        ) {
                            Text("CLAIM BOUNTIES & SECURE", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        // Casual Mode Free quick Respawn option
                        if (engine.gameMode == "Casual") {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    engine.resetEngine()
                                    engine.isGameOver = false
                                },
                                modifier = Modifier.testTag("casual_quick_respawn")
                            ) {
                                Text("QUICK RESPAWN NOW (FREE)", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 4. GAME PAUSED OVERLAY SCREEN (Full modal overlay)
        AnimatedVisibility(
            visible = isPaused && !engine.isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true, onClick = {}) // block touch pass-through
                    .testTag("game_pause_overlay"),
                contentAlignment = Alignment.Center
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    borderColor = Color(0xFF00FFCC).copy(alpha = 0.5f),
                    backgroundColor = Color(0xFA0F1426),
                    glowColor = Color(0xFF00FFCC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Box with pause emblem
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x0C00FFCC))
                                .border(1.dp, Color(0x1A00FFCC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ARENA SYSTEM PAUSED",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (showSecurityTerminalInPause) {
                            // SENTINEL ANTI-CHEAT SECURITY PORTAL
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showSecurityTerminalInPause = false }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FFCC))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "SENTINEL HEURISTICS CORE",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Status overview card
                                val scoreColor = when {
                                    engine.antiCheat.securityScore >= 85 -> Color(0xFF22C55E)
                                    engine.antiCheat.securityScore >= 60 -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (engine.antiCheat.isSentinelOnline) Color(0xFF22C55E) else Color.Red)
                                                )
                                                Text(
                                                    "SENTINEL ONLINE CHECKER",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                "SECURITY RATING: ${engine.antiCheat.securityScore}%",
                                                color = scoreColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("CHECKS", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text("${engine.antiCheat.integrityChecksProcessed}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("TELEPORTS", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text("${engine.antiCheat.teleportViolationsDetected}", color = if (engine.antiCheat.teleportViolationsDetected > 0) Color(0xFFEF4444) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("SPEEDS", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text("${engine.antiCheat.speedViolationsDetected}", color = if (engine.antiCheat.speedViolationsDetected > 0) Color(0xFFFF9900) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("SCORES", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text("${engine.antiCheat.scoreViolationsDetected}", color = if (engine.antiCheat.scoreViolationsDetected > 0) Color(0xFFEF4444) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }

                                // Interactive Cheat injection simulation header
                                Text(
                                    "CHEAT INJECTION SIMULATION PLATFORM",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                // Sandboxed cheat action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            engine.playerSnake?.let { p ->
                                                p.speed = 45f // Speedhack Injection
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("SPEED HACK", color = Color(0xFFEF4444), fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }

                                    Button(
                                        onClick = {
                                            engine.playerSnake?.let { p ->
                                                p.position = p.position + Vector2D(1200f, 1200f) // Teleport Injection
                                                p.body.clear()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("TP HACK", color = Color(0xFFEF4444), fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }

                                    Button(
                                        onClick = {
                                            engine.playerSnake?.let { p ->
                                                p.score += 800 // Points Injection
                                                p.length = 4 + (p.score / 25)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("SCORE LOBBY", color = Color(0xFFEF4444), fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // Security toggle (can disable/enable Sentinel core)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("SENTINEL CORE ENGAGEMENT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Disable validation bounds (Sandbox Mode)", color = Color.Gray, fontSize = 8.sp)
                                    }
                                    Switch(
                                        checked = engine.antiCheat.isSentinelOnline,
                                        onCheckedChange = { engine.antiCheat.isSentinelOnline = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FFCC),
                                            checkedTrackColor = Color(0x3300FFCC),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.Black
                                        )
                                    )
                                }

                                // Interactive Monospace Logs Console block
                                Text(
                                    "REAL-TIME LOGS INTEGRITY CONSOLE",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF04060C)),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                    ) {
                                        val revLogs = engine.antiCheat.integrityLogs.reversed()
                                        items(revLogs) { log ->
                                            Text(
                                                text = log,
                                                color = if (log.contains("ALERT")) Color(0xFFF87171) else Color(0xFF4ADE80),
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { showSecurityTerminalInPause = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .padding(top = 4.dp)
                                ) {
                                    Text("CONFIRM DEPLOY", color = Color.Black, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        } else if (!isSettingsOpenInPause) {
                            // MAIN PAUSE CONTROLS
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Resume Match
                                Button(
                                    onClick = { isPaused = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FFCC)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_resume_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Text(
                                            "RESUME CHRONOLOGY",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // 2. Restart Match
                                Button(
                                    onClick = {
                                        engine.resetEngine()
                                        isPaused = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x3300FFCC),
                                        contentColor = Color(0xFF00FFCC)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_restart_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00FFCC))
                                        Text(
                                            "REPLAY ARENA MATCH",
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                // 3. Settings Configurator
                                Button(
                                    onClick = { isSettingsOpenInPause = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x11FFFFFF),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_settings_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                                        Text(
                                            "CALIBRATE SYSTEM SETTINGS",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 3b. Anti-Cheat Security Core Status button
                                Button(
                                    onClick = { showSecurityTerminalInPause = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x2210B981),
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_sentinel_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981))
                                        Text(
                                            "SENTINEL ANTI-CHEAT CORE",
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // 4. Return to Main Menu
                                TextButton(
                                    onClick = {
                                        isPaused = false
                                        viewModel.finishActiveGameAndSave()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("pause_exit_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF3366))
                                        Text(
                                            "RETURN TO TERMINAL LOBBY",
                                            color = Color(0xFFFF3366),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // SETTINGS INTERFACE PANEL
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { isSettingsOpenInPause = false }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "SYSTEM CALIBRATIONS",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // 1. Haptic Preference Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("CYBER-HAPTICS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Tactile shockwaves on collisions/kills", color = Color.Gray, fontSize = 8.5.sp)
                                    }
                                    Switch(
                                        checked = viewModel.hapticsEnabled,
                                        onCheckedChange = { viewModel.hapticsEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FFCC),
                                            checkedTrackColor = Color(0x3300FFCC),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("setting_toggle_haptics")
                                    )
                                }

                                // 2. Right-Handed Joystick Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("CONTROLLER REVERSAL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Relocate Joystick to right side", color = Color.Gray, fontSize = 8.5.sp)
                                    }
                                    Switch(
                                        checked = joystickOnRightSide,
                                        onCheckedChange = { joystickOnRightSide = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FFCC),
                                            checkedTrackColor = Color(0x3300FFCC),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("setting_toggle_controls")
                                    )
                                }

                                // 3. Graphics Mode Optimization Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("LOW GRAPHICS MODE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Optimize FPS for low power devices", color = Color.Gray, fontSize = 8.5.sp)
                                    }
                                    Switch(
                                        checked = lowGraphicsMode,
                                        onCheckedChange = { lowGraphicsMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FFCC),
                                            checkedTrackColor = Color(0x3300FFCC),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("setting_toggle_graphics")
                                    )
                                }

                                // 3b. Tablet View Dynamic Scale Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("PANORAMIC TABLET MODE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("Spacious camera zoom (Snake.io style)", color = Color.Gray, fontSize = 8.5.sp)
                                    }
                                    Switch(
                                        checked = isWideViewportMode,
                                        onCheckedChange = { isWideViewportMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FFCC),
                                            checkedTrackColor = Color(0x3300FFCC),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("setting_toggle_tablet_view")
                                    )
                                }

                                // 4. Mock Volume slider for extra fidelity setting
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("AUDIO AMPLIFICATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("${(soundVolumeFraction * 100).toInt()}%", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = soundVolumeFraction,
                                        onValueChange = { soundVolumeFraction = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF00FFCC),
                                            activeTrackColor = Color(0xFF00FFCC),
                                            inactiveTrackColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                            .testTag("setting_slider_volume")
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { isSettingsOpenInPause = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FFCC)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(45.dp)
                                        .testTag("save_settings_back")
                                ) {
                                    Text("CONFIRM DEPLOY", color = Color.Black, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                TextButton(
                                    onClick = {
                                        isPaused = false
                                        isSettingsOpenInPause = false
                                        viewModel.finishActiveGameAndSave()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(45.dp)
                                        .testTag("settings_quit_game_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF3366))
                                        Text(
                                            "QUIT ACTIVE MATCH",
                                            color = Color(0xFFFF3366),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatSummaryBadge(label: String, valStr: String, AccentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(valStr, color = AccentColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

private fun fontSpacingAlign(p: Float, bounds: Float, spacing: Float): Float {
    return (p - bounds) - (p - bounds) % spacing
}
