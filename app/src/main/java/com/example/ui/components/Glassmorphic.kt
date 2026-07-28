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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x33FFFFFF),
    backgroundColor: Color = Color(0x12000000),
    glowColor: Color? = null,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
            }
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun GlowButton(
    text: String,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF00FFCC),
    textColor: Color = Color.White,
    tag: String = "glow_button",
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

    Box(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.8f),
                        glowColor.copy(alpha = 0.4f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = glowColor.copy(alpha = pulseAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = glowColor)
            ) {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onChange: (angle: Float?, offset: Vector2D) -> Unit
) {
    val radiusPx = with(LocalDensity.current) { 80.dp.toPx() }
    val handleRadiusPx = with(LocalDensity.current) { 30.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }

    val resolvedPosition = if (isDragging) touchPos else Offset.Zero

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
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = true
                        val localX = event.x - radiusPx
                        val localY = event.y - radiusPx
                        val distance = sqrt(localX * localX + localY * localY)
                        if (distance <= radiusPx) {
                            touchPos = Offset(localX, localY)
                        } else {
                            val angle = atan2(localY, localX)
                            touchPos = Offset(cos(angle) * radiusPx, sin(angle) * radiusPx)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val localX = event.x - radiusPx
                        val localY = event.y - radiusPx
                        val distance = sqrt(localX * localX + localY * localY)
                        if (distance <= radiusPx) {
                            touchPos = Offset(localX, localY)
                        } else {
                            val angle = atan2(localY, localX)
                            touchPos = Offset(cos(angle) * radiusPx, sin(angle) * radiusPx)
                        }
                        
                        // Output angle and ratio
                        val angleOut = atan2(touchPos.y, touchPos.x)
                        onChange(angleOut, Vector2D(touchPos.x, touchPos.y))
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        touchPos = Offset.Zero
                        onChange(null, Vector2D(0f, 0f))
                    }
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Pad markings
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x3300FFCC),
                radius = radiusPx,
                style = Stroke(width = 1.dp.toPx())
            )
            // Tiny inside ticks
            for (i in 0 until 8) {
                val a = i * (6.28f / 8f)
                val start = Offset(
                    (radiusPx - 10.dp.toPx()) * cos(a) + center.x,
                    (radiusPx - 10.dp.toPx()) * sin(a) + center.y
                )
                val end = Offset(
                    radiusPx * cos(a) + center.x,
                    radiusPx * sin(a) + center.y
                )
                drawLine(
                    color = Color(0x4400FFCC),
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Inner stick handle knob
        Box(
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { resolvedPosition.x.toDp() },
                    y = with(LocalDensity.current) { resolvedPosition.y.toDp() }
                )
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
