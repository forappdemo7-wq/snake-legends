package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ArenaTheme

fun getThemePreviewDrawable(theme: ArenaTheme): Int {
    return when (theme) {
        ArenaTheme.CYBER_CITY -> R.drawable.img_sand_pit_1782378375870
        ArenaTheme.LAVA_WORLD -> R.drawable.img_volcanic_wasteland_1782378977998
        ArenaTheme.FROZEN_ARENA -> R.drawable.img_frost_bite_1782378402694
        ArenaTheme.JUNGLE_TEMPLE -> R.drawable.img_forest_ruins_1782378417866
        ArenaTheme.SPACE_STATION -> R.drawable.img_deep_space_nebula_1782378992376
        ArenaTheme.NEON_GRID -> R.drawable.img_cyber_neon_city_1782378963453
    }
}

@Composable
fun ArenaThemeRow(
    selectedTheme: ArenaTheme,
    onThemeSelected: (ArenaTheme) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(ArenaTheme.values()) { theme ->
            val isSelected = selectedTheme == theme
            ThemeCardV2(
                theme = theme,
                isSelected = isSelected,
                onClick = { onThemeSelected(theme) },
                modifier = Modifier.width(140.dp)
            )
        }
    }
}

@Composable
fun ThemeCardV2(
    theme: ArenaTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderAnim by animateColorAsState(
        targetValue = if (isSelected) Secondary else Color.Transparent,
        animationSpec = tween(300),
        label = "themeBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(),
        label = "themeScale"
    )

    Card(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        border = BorderStroke(2.dp, borderAnim)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // High fidelity generated biome background image
            Image(
                painter = painterResource(id = getThemePreviewDrawable(theme)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark tint overlay to maintain readable high-contrast typography
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) 
                            Color.Black.copy(alpha = 0.45f) 
                        else 
                            Color.Black.copy(alpha = 0.65f)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    theme.displayName,
                    color = if (isSelected) Secondary else TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                val subtitle = when (theme) {
                    ArenaTheme.CYBER_CITY -> "Desert · Dunes"
                    ArenaTheme.LAVA_WORLD -> "Magma · Volcanic"
                    ArenaTheme.FROZEN_ARENA -> "Glacier · Icy"
                    ArenaTheme.JUNGLE_TEMPLE -> "Ruins · Overgrown"
                    ArenaTheme.SPACE_STATION -> "Galactic · Void"
                    ArenaTheme.NEON_GRID -> "Synthwave · Grid"
                }
                Text(
                    subtitle,
                    color = if (isSelected) TextWhite.copy(alpha = 0.9f) else TextLight,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
