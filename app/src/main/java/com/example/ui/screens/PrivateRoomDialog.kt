package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.game.ConnectionStatus
import com.example.game.MultiplayerManager

@Composable
fun PrivateRoomDialog(
    initialCode: String,
    mpManager: MultiplayerManager,
    mpStatus: ConnectionStatus,
    userProfile: UserProfile?,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var roomCode by remember { mutableStateOf(initialCode) }
    var chatText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Private Room Lobby", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = {
                        roomCode = it.take(10).uppercase()
                        onCodeChange(roomCode)
                    },
                    label = { Text("Room Code") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                MultiplayerLobbyCard(
                    mpStatus = mpStatus,
                    mpManager = mpManager,
                    userProfile = userProfile,
                    privateRoomCode = roomCode,
                    chatTextInput = chatText,
                    onChatTextChange = { chatText = it },
                    onSendMessage = {
                        if (chatText.isNotBlank()) {
                            mpManager.broadcastChatMessage(chatText)
                            chatText = ""
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextGray)
            }
        }
    )
}
