package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserProfile
import com.example.game.ArenaTheme
import com.example.game.ConnectionStatus
import com.example.game.GameViewModel
import com.example.game.MultiplayerManager

@Composable
fun MatchSettingsDialog(
    viewModel: GameViewModel,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    selectedTheme: ArenaTheme,
    onThemeSelected: (ArenaTheme) -> Unit,
    selectedClass: String,
    onClassSelected: (String) -> Unit,
    privateRoomCode: String,
    onCodeChange: (String) -> Unit,
    mpManager: MultiplayerManager,
    mpStatus: ConnectionStatus,
    userProfile: UserProfile?,
    onStartBattle: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth(if (isLandscape) 0.82f else 0.95f)
            .padding(vertical = if (isLandscape) 4.dp else 12.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MATCH CONFIGURATION",
                        color = Color(0xFF22D3EE),
                        fontSize = if (isLandscape) 15.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            shadow = Shadow(color = Color(0xFF1D4ED8), offset = Offset(1f, 1f), blurRadius = 2f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Adjust battle size scale, bots count & match options",
                        color = Color(0xFF64748B),
                        fontSize = if (isLandscape) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(if (isLandscape) 28.dp else 32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
            ) {
                // Match Scale
                Column {
                    Text(
                        text = "MATCH SCALE (SNAKE DENSITY)",
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))

                    val scaleOptions = listOf(
                        Triple(16, "16 Snakes", "Compact Map"),
                        Triple(50, "50 Snakes", "Spacious Arena"),
                        Triple(100, "100 Snakes", "Gigantic Megamap")
                    )

                    val activeScale by viewModel.maxMatchSnakes.collectAsStateWithLifecycle()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scaleOptions.forEach { (count, label, desc) ->
                            val isSelected = count == activeScale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFF151C30))
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.maxMatchSnakes.value = count }
                                    .padding(vertical = if (isLandscape) 8.dp else 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF00FFCC) else Color.White,
                                        fontSize = if (isLandscape) 11.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = Color(0xFF94A3B8),
                                        fontSize = if (isLandscape) 8.sp else 9.sp,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Game Modes
                Column {
                    Text(
                        text = "GAME MODE TYPE",
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
                    GameModeRow(
                        selectedMode = selectedMode,
                        onModeSelected = onModeSelected
                    )
                }

                // Arena Themes
                Column {
                    Text(
                        text = "ARENA MAP THEMES",
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
                    ArenaThemeRow(
                        selectedTheme = selectedTheme,
                        onThemeSelected = onThemeSelected
                    )
                }

                // Tactical Class
                Column {
                    Text(
                        text = "TACTICAL CLASS ABILITY",
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
                    TacticalClassRow(
                        selectedClass = selectedClass,
                        onClassSelected = onClassSelected
                    )
                }

                // Haptics & Vibration
                Column {
                    Text(
                        text = "TACTILE FEEDBACK & HAPTICS",
                        color = Color(0xFFE2E8F0),
                        fontSize = if (isLandscape) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151C30))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = if (isLandscape) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "Tactile Rumble Effects",
                                color = Color.White,
                                fontSize = if (isLandscape) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vibrate device on food consumption, score updates, and collisions",
                                color = Color(0xFF94A3B8),
                                fontSize = if (isLandscape) 8.5.sp else 9.5.sp,
                                lineHeight = if (isLandscape) 10.sp else 12.sp
                            )
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

                // Private Room
                if (selectedMode == "Private Room") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(if (isLandscape) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (mpStatus == ConnectionStatus.CONNECTED) Color.Green else Color.Red)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "MULTIPLAYER PORTAL (Private)",
                                    color = Color(0xFF00FFCC),
                                    fontSize = if (isLandscape) 9.sp else 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                                    val generated = "${prefixes.random()}-${(100..999).random()}"
                                    onCodeChange(generated)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("GENERATE CODE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = privateRoomCode,
                            onValueChange = { onCodeChange(it.uppercase()) },
                            placeholder = { Text("No active code. Click GENERATE or enter code...", color = Color(0xFF64748B), fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        var chatTextLocal by remember { mutableStateOf("") }
                        MultiplayerLobbyCard(
                            mpStatus = mpStatus,
                            mpManager = mpManager,
                            userProfile = userProfile,
                            privateRoomCode = privateRoomCode,
                            chatTextInput = chatTextLocal,
                            onChatTextChange = { chatTextLocal = it },
                            onSendMessage = {
                                if (chatTextLocal.isNotBlank()) {
                                    mpManager.broadcastChatMessage(chatTextLocal)
                                    chatTextLocal = ""
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF22D3EE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "SAVE",
                            color = Color(0xFF22D3EE),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isLandscape) 12.sp else 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onStartBattle()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9E00)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "BATTLE NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isLandscape) 12.sp else 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                TextButton(
                    onClick = {
                        (context as? android.app.Activity)?.finish()
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
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
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    )
}
