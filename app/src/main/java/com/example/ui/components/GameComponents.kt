package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

// ---------- Glassmorphic Card ----------
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x33FFFFFF),
    backgroundColor: Color = Color(0x12000000),
    glowColor: Color? = null,
    cornerRadius: Dp = 16.dp,
    shadowElevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = borderColor.alpha * 1.5f),
                        borderColor.copy(alpha = borderColor.alpha * 0.3f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                if (glowColor != null) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.05f),
                        cornerRadius = CornerRadius(
                            with(density) { cornerRadius.toPx() },
                            with(density) { cornerRadius.toPx() }
                        )
                    )
                }
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.08f),
                    topLeft = Offset(
                        with(density) { shadowElevation.toPx() },
                        with(density) { shadowElevation.toPx() }
                    ),
                    cornerRadius = CornerRadius(
                        with(density) { cornerRadius.toPx() },
                        with(density) { cornerRadius.toPx() }
                    )
                )
            }
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

// ---------- Glow Button ----------
@Composable
fun GlowButton(
    text: String,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF00FFCC),
    textColor: Color = Color.White,
    tag: String = "glow_button",
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                        glowColor.copy(alpha = if (enabled) 0.4f else 0.15f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = if (enabled) glowColor.copy(alpha = pulseAlpha) else glowColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = glowColor),
                enabled = enabled && !loading
            ) {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, textColor, CircleShape)
            )
        } else {
            Text(
                text = text,
                color = textColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ---------- Virtual Joystick ----------
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onChange: (angle: Float?, normalizedOffset: Vector2D) -> Unit
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { 80.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedOffset by animateOffsetAsState(
        targetValue = if (isDragging) dragOffset else Offset.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "joystick_spring"
    )

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(160.dp)
            .testTag("joystick_pad")
            .clip(CircleShape)
            .background(Color(0xE0C0800))
            .border(
                2.dp,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x8800FFCC), Color(0x221A1A24))
                ),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val local = offset - center
                        dragOffset = clampOffset(local, radiusPx)
                        notifyChange(dragOffset, onChange)
                        haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val local = change.position - center
                        dragOffset = clampOffset(local, radiusPx)
                        notifyChange(dragOffset, onChange)
                    },
                    onDragEnd = {
                        isDragging = false
                        onChange(null, Vector2D(0f, 0f))
                        haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                    },
                    onDragCancel = {
                        isDragging = false
                        onChange(null, Vector2D(0f, 0f))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x3300FFCC),
                radius = radiusPx,
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )
            for (i in 0 until 8) {
                val a = i * (2 * Math.PI / 8).toFloat()
                val start = Offset(
                    (radiusPx - with(density) { 10.dp.toPx() }) * cos(a) + center.x,
                    (radiusPx - with(density) { 10.dp.toPx() }) * sin(a) + center.y
                )
                val end = Offset(
                    radiusPx * cos(a) + center.x,
                    radiusPx * sin(a) + center.y
                )
                drawLine(
                    color = Color(0x4400FFCC),
                    start = start,
                    end = end,
                    strokeWidth = with(density) { 2.dp.toPx() }
                )
            }
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedOffset.x.roundToInt(),
                        y = animatedOffset.y.roundToInt()
                    )
                }
                .size(60.dp)
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

private fun clampOffset(offset: Offset, radius: Float): Offset {
    val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
    return if (distance <= radius) offset else Offset(
        offset.x / distance * radius,
        offset.y / distance * radius
    )
}

private fun notifyChange(offset: Offset, onChange: (Float?, Vector2D) -> Unit) {
    val density = LocalDensity.current
    val maxRadius = with(density) { 80.dp.toPx() }
    val normalized = Vector2D(
        x = offset.x / maxRadius,
        y = offset.y / maxRadius
    )
    val angle = atan2(offset.y, offset.x)
    onChange(angle, normalized)
}