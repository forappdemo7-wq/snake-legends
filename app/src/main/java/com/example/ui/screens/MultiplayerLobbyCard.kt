package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserProfile
import com.example.game.ConnectionStatus
import com.example.game.MultiplayerManager

@Composable
fun MultiplayerLobbyCard(
    mpStatus: ConnectionStatus,
    mpManager: MultiplayerManager,
    userProfile: UserProfile?,
    privateRoomCode: String,
    chatTextInput: String,
    onChatTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val participants by mpManager.activeParticipants.collectAsStateWithLifecycle()
    val chatMessages by mpManager.chatMessages.collectAsStateWithLifecycle()
    val pingMs by mpManager.pingMs.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (mpStatus) {
                            ConnectionStatus.CONNECTED -> Success
                            ConnectionStatus.CONNECTING, ConnectionStatus.HANDSHARING -> Color(0xFFFFCC00)
                            else -> Color.Gray
                        }
                    )
            )
            Text("Status: ${mpStatus.name}", color = TextWhite, fontSize = 14.sp)
            if (mpStatus == ConnectionStatus.CONNECTED) {
                Text("Ping: ${pingMs}ms", color = Primary, fontSize = 12.sp)
            }
        }

        when (mpStatus) {
            ConnectionStatus.OFFLINE, ConnectionStatus.DISCONNECTED -> {
                Button(
                    onClick = {
                        val finalCode = privateRoomCode.ifBlank { "LOBBY-VIPER" }
                        mpManager.connectToRoomWebSocket(finalCode, userProfile?.username ?: "Player")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Connect to Room", color = Color.White)
                }
            }
            ConnectionStatus.CONNECTING, ConnectionStatus.HANDSHARING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...", color = TextGray)
                }
            }
            ConnectionStatus.CONNECTED -> {
                Text("Participants (${participants.size})", color = TextGray, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(participants) { user ->
                        val isSelf = user == (userProfile?.username ?: "Player")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelf) Primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSelf) Primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(user, color = TextWhite, fontSize = 12.sp)
                        }
                    }
                }

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            items(chatMessages.reversed()) { msg ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "[${msg.sender}]:",
                                        color = if (msg.sender == userProfile?.username) Primary else Danger,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(msg.text, color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatTextInput,
                            onValueChange = onChatTextChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type message...", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(onClick = onSendMessage, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Send")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { mpManager.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}
