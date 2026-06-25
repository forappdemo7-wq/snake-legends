package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.MultiplayerManager
import com.example.game.ServerRegion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerSettingsSheet(
    viewModel: GameViewModel,
    mpManager: MultiplayerManager,
    onDismiss: () -> Unit
) {
    var selectedSettingsTab by remember { mutableStateOf("NETWORK") }
    var controlScheme by remember { mutableStateOf("Joystick") }
    var joystickSensitivity by remember { mutableStateOf(1.2f) }
    var soundMusicVolume by remember { mutableStateOf(0.75f) }
    var soundSfxVolume by remember { mutableStateOf(0.85f) }
    var renderQualityMode by remember { mutableStateOf("Turbo (120FPS)") }
    var customParticleMultiplier by remember { mutableStateOf(0.8f) }
    var interfaceThemeMode by remember { mutableStateOf("Neo Cyber") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ARCADE SYSTEM CONFIG",
                        color = Color(0xFF22D3EE),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            shadow = Shadow(color = Color(0xFF1D4ED8), offset = Offset(1f, 1f), blurRadius = 2f)
                        )
                    )
                    Text(
                        text = "Optimize matchmaking feeds, input layout & sensory styling",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "NETWORK" to "Online",
                    "CONTROLS" to "Input",
                    "AESTHETICS" to "Sensory"
                ).forEach { (tabId, label) ->
                    val isTabActive = selectedSettingsTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isTabActive) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isTabActive) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedSettingsTab = tabId }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.uppercase(),
                            color = if (isTabActive) Color(0xFF00FFCC) else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedSettingsTab) {
                "NETWORK" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "MULTIPLAYER MATCHMAKING CORES",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Global Server Zone", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Estimated latency: ${
                                            when(mpManager.selectedRegion.regionName) {
                                                "US East" -> "~18ms (Optimal)"
                                                "EU West" -> "~74ms"
                                                "Asia Pac" -> "~138ms"
                                                else -> "~112ms"
                                            }
                                        }",
                                        color = Color(0xFF22C55E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val regions = ServerRegion.values()
                                            val next = (mpManager.selectedRegion.ordinal + 1) % regions.size
                                            mpManager.selectedRegion = regions[next]
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = mpManager.selectedRegion.regionName.uppercase(),
                                        color = Color(0xFF22D3EE),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("Delay Wave Compensation", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Interpolates remote peer positions dynamically to prevent game jitter", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                                }
                                Switch(
                                    checked = mpManager.isLagCompensationEnabled,
                                    onCheckedChange = { mpManager.isLagCompensationEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00FFCC),
                                        checkedTrackColor = Color(0xFF00FFCC).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color(0xFF475569),
                                        uncheckedTrackColor = Color(0xFF1E293B)
                                    )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text("Engine Simulation Frequency", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Higher frequencies yield accurate snake tracking but increase bandwidth usage", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(20, 30, 60).forEach { rate ->
                                    val isActive = mpManager.tickRateHz == rate
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) Color(0xFF1E3A8A).copy(alpha = 0.8f) else Color(0xFF1E293B))
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isActive) Color(0xFF3B82F6) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { mpManager.tickRateHz = rate }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${rate} HZ",
                                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "CONTROLS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = Color(0xFFFF9E00), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TACTILE TENSE SCHEMES",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text("Tactile Action Layout", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Choose your preferred slither handling input layout", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Joystick", "Swipe", "D-Pad").forEach { mode ->
                                    val isActive = controlScheme == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) Color(0xFF7C2D12).copy(alpha = 0.5f) else Color(0xFF1E293B))
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isActive) Color(0xFFEA580C) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { controlScheme = mode }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.uppercase(),
                                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Steering Sensitivity", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format("%.1fx", joystickSensitivity),
                                    color = Color(0xFFFF9E00),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text("Adjusts input response acceleration when making fast coils", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Slider(
                                value = joystickSensitivity,
                                onValueChange = { joystickSensitivity = it },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF9E00),
                                    activeTrackColor = Color(0xFFFF9E00),
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("Rumble Haptic Resonance", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Fires tactile feedback rumbles upon feeding or hitting walls", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                                }
                                Switch(
                                    checked = viewModel.hapticsEnabled,
                                    onCheckedChange = { viewModel.hapticsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFF9E00),
                                        checkedTrackColor = Color(0xFFFF9E00).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color(0xFF475569),
                                        uncheckedTrackColor = Color(0xFF1E293B)
                                    )
                                )
                            }
                        }
                    }
                }

                "AESTHETICS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "GRAPHICAL & SONIC RENDERING",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Synth Background Music (BGM)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${(soundMusicVolume * 100).toInt()}%",
                                    color = Color(0xFFEC4899),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = soundMusicVolume,
                                onValueChange = { soundMusicVolume = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEC4899),
                                    activeTrackColor = Color(0xFFEC4899),
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Arcade Feed Sound Effects (SFX)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${(soundSfxVolume * 100).toInt()}%",
                                    color = Color(0xFFEC4899),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = soundSfxVolume,
                                onValueChange = { soundSfxVolume = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEC4899),
                                    activeTrackColor = Color(0xFFEC4899),
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text("Sensory Frame-Rate Limit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Smoother frame updates require active screen-reign graphics adapters", color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Eco (30FPS)", "Pro (60FPS)", "Turbo (120FPS)").forEach { qOpt ->
                                    val isActive = renderQualityMode == qOpt
                                    Box(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) Color(0xFF4C1D95).copy(alpha = 0.5f) else Color(0xFF1E293B))
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isActive) Color(0xFFA78BFA) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { renderQualityMode = qOpt }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = qOpt.uppercase(),
                                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF070B13))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text("Aesthetic Color Theme", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Neo Cyber", "Prismatic", "Solar Flare").forEach { thm ->
                                    val isActive = interfaceThemeMode == thm
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) Color(0xFF831843).copy(alpha = 0.5f) else Color(0xFF1E293B))
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isActive) Color(0xFFF43F5E) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { interfaceThemeMode = thm }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = thm.uppercase(),
                                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "APPLY CONFIGURATION",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            TextButton(
                onClick = {
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFFF3366),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "QUIT GAME / EXIT APP",
                        color = Color(0xFFFF3366),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
