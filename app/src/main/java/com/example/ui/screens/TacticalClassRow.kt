package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TacticalClassRow(
    selectedClass: String,
    onClassSelected: (String) -> Unit
) {
    val classData = listOf(
        ClassInfo("SHIELD", "Aegis Shield", "Invulnerability", Icons.Default.Shield, Color(0xFF00E5FF), 10),
        ClassInfo("FREEZE_PULSE", "Sub-Zero Blast", "Freeze pulse", Icons.Default.AcUnit, Color(0xFF80D8FF), 15),
        ClassInfo("EMP_BLAST", "Disruptor EMP", "Stall enemy boost", Icons.Default.FlashOn, Color(0xFFFFEE55), 12),
        ClassInfo("SPEED_BURST", "Drive Burst", "Speed boost", Icons.Default.Bolt, Color(0xFFFF5722), 8),
        ClassInfo("GHOST_PHASE", "Quantum Ghost", "Phase through enemies", Icons.Default.Widgets, Color(0xFFB0BEC5), 15)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(classData) { classInfo ->
            val isSelected = selectedClass == classInfo.id
            HeroClassCard(
                classInfo = classInfo,
                isSelected = isSelected,
                onClick = { onClassSelected(classInfo.id) },
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
fun HeroClassCard(
    classInfo: ClassInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(300),
        label = "elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(),
        label = "classScale"
    )

    Card(
        modifier = modifier
            .height(130.dp)
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) classInfo.color.copy(alpha = 0.15f) else Surface
        ),
        border = BorderStroke(1.5.dp, if (isSelected) classInfo.color else Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                classInfo.icon,
                contentDescription = null,
                tint = classInfo.color,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = classInfo.name,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cooldown: ${classInfo.cooldown}s",
                    color = TextGray,
                    fontSize = 11.sp
                )
                Text(
                    text = classInfo.description,
                    color = TextLight,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class ClassInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val cooldown: Int
)
