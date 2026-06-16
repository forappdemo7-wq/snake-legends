package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.*
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onNavigateToShop: () -> Unit,
    onNavigateToClans: () -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val matchRecords by viewModel.matchRecords.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedAbility.collectAsStateWithLifecycle()

    val mpManager = viewModel.multiplayerManager
    val mpStatus by mpManager.connectionStatus.collectAsStateWithLifecycle()
    val lobbyStartTrigger by mpManager.incomingGameStartTrigger.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf("Casual") }
    var selectedTheme by remember { mutableStateOf(ArenaTheme.CYBER_CITY) }
    var privateRoomCode by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var activeLobbyTab by remember { mutableStateOf("LOBBY") }
    var chatTextInput by remember { mutableStateOf("") }

    // Derived states for performance
    val rankTier by derivedStateOf { getRankTier(userProfile?.rankedScore ?: 0) }
    val xpProgress by derivedStateOf {
        val level = userProfile?.level ?: 1
        val xp = userProfile?.xp ?: 0
        val reqXp = level * 1000
        (xp.toFloat() / reqXp.toFloat()).coerceIn(0f, 1f)
    }

    LaunchedEffect(lobbyStartTrigger) {
        if (lobbyStartTrigger) {
            viewModel.startNewGame(selectedMode, selectedTheme, privateRoomCode)
            mpManager.disconnect()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mpManager.disconnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0x0E00FFCC), radius = 300f, center = Offset(0f, 300f))
            drawCircle(Color(0x0E9933FF), radius = 400f, center = Offset(size.width, size.height - 200f))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            LobbyHeader(userProfile, viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            // Player Profile Card
            userProfile?.let { profile ->
                PlayerProfileCard(
                    profile = profile,
                    rankTier = rankTier,
                    xpProgress = xpProgress,
                    onEditNameClick = {
                        editedName = profile.username
                        showEditNameDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Links
            NavigationLinks(
                onNavigateToShop = onNavigateToShop,
                onNavigateToClans = onNavigateToClans,
                onNavigateToLeaderboard = onNavigateToLeaderboard
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs: Play Grid / Missions
            LobbyTabs(
                activeTab = activeLobbyTab,
                onTabSelected = { activeLobbyTab = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (activeLobbyTab) {
                "LOBBY" -> {
                    // Class Selector
                    TacticalClassSelector(
                        selectedClass = selectedClass,
                        onClassSelected = { viewModel.selectedAbility.value = it }
                    )

                    // Arena Theme Selector
                    ArenaThemeSelector(
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it }
                    )

                    // Multiplayer Settings
                    MultiplayerSettingsCard(viewModel)

                    // Game Mode Selector
                    GameModeSelector(
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Private Room Extra Controls
                    if (selectedMode == "Private Room") {
                        PrivateRoomControls(
                            privateRoomCode = privateRoomCode,
                            onCodeChange = { privateRoomCode = it },
                            onGenerateCode = {
                                val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                                privateRoomCode = "${prefixes.random()}-${(100..999).random()}"
                            },
                            mpManager = mpManager,
                            mpStatus = mpStatus,
                            userProfile = userProfile,
                            chatTextInput = chatTextInput,
                            onChatTextChange = { chatTextInput = it },
                            onSendMessage = {
                                if (chatTextInput.isNotBlank()) {
                                    mpManager.broadcastChatMessage(chatTextInput)
                                    chatTextInput = ""
                                }
                            }
                        )
                    }

                    // Start Game Button
                    GlowButton(
                        text = "SLITHER NOW",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        glowColor = when (selectedMode) {
                            "Ranked" -> Color(0xFFFF9900)
                            "Battle Royale" -> Color(0xFFFF3366)
                            else -> Color(0xFF00FFCC)
                        },
                        tag = "start_game_launcher"
                    ) {
                        val finalCode = if (selectedMode == "Private Room") {
                            if (privateRoomCode.isBlank()) {
                                val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                                val code = "${prefixes.random()}-${(100..999).random()}"
                                privateRoomCode = code
                                code
                            } else privateRoomCode
                        } else ""
                        if (selectedMode == "Private Room" && mpStatus == ConnectionStatus.CONNECTED) {
                            mpManager.broadcastStartMatchTrigger()
                        }
                        viewModel.startNewGame(selectedMode, selectedTheme, finalCode)
                        if (selectedMode == "Private Room") {
                            mpManager.disconnect()
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Match History
                    MatchHistory(matchRecords = matchRecords)
                }

                "MISSIONS" -> {
                    MissionsAndBattlePass(userProfile, viewModel)
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        EditNameDialog(
            currentName = editedName,
            onNameChange = { editedName = it },
            onConfirm = {
                val trimmed = editedName.trim()
                if (trimmed.isNotEmpty()) {
                    viewModel.updateUsername(trimmed)
                    showEditNameDialog = false
                }
            },
            onDismiss = { showEditNameDialog = false }
        )
    }
}

// ========== Header ==========
@Composable
private fun LobbyHeader(
    userProfile: com.example.data.UserProfile?,
    viewModel: GameViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SNAKE",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Text(
                text = "LEGENDS",
                color = Color(0xFF00FFCC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp,
                modifier = Modifier.offset(y = (-6).dp)
            )
        }

        // Coins
        userProfile?.let { profile ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1F1E293B))
                    .border(1.dp, Color(0xFFFFFF33).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { viewModel.earnFreeCoins(200) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFFF33),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${profile.coins} COINS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ========== Player Profile Card ==========
@Composable
private fun PlayerProfileCard(
    profile: com.example.data.UserProfile,
    rankTier: String,
    xpProgress: Float,
    onEditNameClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = Color(0x2200FFCC),
        backgroundColor = Color(0x120F172A),
        glowColor = Color(0xFF00FFCC)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LEVEL ${profile.level}",
                    color = Color(0xFF00FFCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable { onEditNameClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = profile.username,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Username",
                        tint = Color(0xFF00FFCC).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.xp} / ${profile.level * 1000} XP",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Rank
            RankBadge(rankTier = rankTier, rankScore = profile.rankedScore)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoStatItem("TOP LENGTH", profile.highestScore.toString(), Icons.Default.TrendingUp)
            InfoStatItem("MATCHES", profile.matchesPlayed.toString(), Icons.Default.SportsEsports)
            InfoStatItem("CLAN", profile.clanName ?: "NONE", Icons.Default.Group)
        }
    }
}

@Composable
private fun RankBadge(rankTier: String, rankScore: Int) {
    val rankColor = getRankColor(rankTier)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.15f))
                .border(2.dp, rankColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getRankIcon(rankTier),
                contentDescription = rankTier,
                tint = rankColor,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = rankTier.uppercase(Locale.getDefault()),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$rankScore PTS",
            color = Color.LightGray,
            fontSize = 10.sp
        )
    }
}

// ========== Navigation Links ==========
@Composable
private fun NavigationLinks(
    onNavigateToShop: () -> Unit,
    onNavigateToClans: () -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavLinkItem(
            icon = Icons.Default.Palette,
            label = "SHOP",
            iconColor = Color(0xFF9933FF),
            onClick = onNavigateToShop
        )
        NavLinkItem(
            icon = Icons.Default.Group,
            label = "CLANS",
            iconColor = Color(0xFF00FFCC),
            onClick = onNavigateToClans
        )
        NavLinkItem(
            icon = Icons.Default.Leaderboard,
            label = "RECORDS",
            iconColor = Color(0xFFFF9900),
            onClick = onNavigateToLeaderboard
        )
    }
}

@Composable
private fun NavLinkItem(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ========== Lobby Tabs ==========
@Composable
private fun LobbyTabs(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("PLAY GRID" to "LOBBY", "MISSIONS & BATTLE PASS" to "MISSIONS")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0E17), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0x1F22D3EE)), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { (label, key) ->
            val isSelected = activeTab == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0x1A00FFCC) else Color.Transparent)
                    .clickable { onTabSelected(key) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (key == "LOBBY") Icons.Default.SportsEsports else Icons.Default.WorkspacePremium,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF00FFCC) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ========== Tactical Class Selector ==========
@Composable
private fun TacticalClassSelector(
    selectedClass: String,
    onClassSelected: (String) -> Unit
) {
    Text(
        text = "SELECT TACTICAL CLASS",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    val classTypes = listOf("SHIELD", "FREEZE_PULSE", "EMP_BLAST", "SPEED_BURST", "GHOST_PHASE")
    val classLabels = listOf("AEGIS SHIELD", "SUB-ZERO BLAST", "DISRUPTOR EMP", "DRIVE BURST", "QUANTUM GHOST")
    val classDesc = listOf("Generates 3s shield", "Slowing freeze pulse", "Stalls boost; converts food", "Aggressive speed boost", "Phase through of body segment")
    val classIcons = listOf(Icons.Default.Shield, Icons.Default.AcUnit, Icons.Default.FlashOn, Icons.Default.Bolt, Icons.Default.Widgets)
    val classColors = listOf(Color(0xFF00E5FF), Color(0xFF80D8FF), Color(0xFFFFEE55), Color(0xFFFF5722), Color(0xFFB0BEC5))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        classTypes.forEachIndexed { idx, type ->
            val isSelected = selectedClass == type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) classColors[idx].copy(alpha = 0.08f) else Color(0x08FFFFFF))
                    .border(
                        1.dp,
                        if (isSelected) classColors[idx] else Color(0x1AFFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onClassSelected(type) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(classColors[idx].copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = classIcons[idx],
                            contentDescription = type,
                            tint = classColors[idx],
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = classLabels[idx],
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = classDesc[idx],
                            color = Color.Gray,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(classColors[idx].copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "DEPLOY",
                                color = classColors[idx],
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== Arena Theme Selector ==========
@Composable
private fun ArenaThemeSelector(
    selectedTheme: ArenaTheme,
    onThemeSelected: (ArenaTheme) -> Unit
) {
    Text(
        text = "SELECT ARENA PARAMETERS",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArenaTheme.values().forEach { theme ->
            val isSelected = selectedTheme == theme
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0x1A00FFCC) else Color(0x08FFFFFF))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF00FFCC) else Color(0x1AFFFFFF),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onThemeSelected(theme) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme.displayName.uppercase(Locale.getDefault()).replace(" ", "\n"),
                    color = if (isSelected) Color(0xFF00FFCC) else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ========== Multiplayer Settings ==========
@Composable
private fun MultiplayerSettingsCard(viewModel: GameViewModel) {
    Text(
        text = "MULTIPLAYER NETWORK TUNING",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )

    val mpManager = viewModel.multiplayerManager

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        borderColor = Color(0x19FFFFFF),
        backgroundColor = Color(0x0CFFFFFF),
        glowColor = Color(0x1100FFCC)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Region Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVER GEOLOCATION",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable {
                            val regions = ServerRegion.values()
                            val nextIndex = (mpManager.selectedRegion.ordinal + 1) % regions.size
                            mpManager.selectedRegion = regions[nextIndex]
                        }
                ) {
                    Text(
                        text = mpManager.selectedRegion.regionName.uppercase(),
                        color = Color(0xFF00FFCC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Region",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Lag Compensation
            SettingsSwitchRow(
                title = "LAG COMPENSATION (LERP)",
                subtitle = "Interpolates position packets at 20Hz",
                checked = mpManager.isLagCompensationEnabled,
                onCheckedChange = { mpManager.isLagCompensationEnabled = it }
            )

            // Tick Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PACKET DATA RATE",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simulated tick syncing frequency",
                        color = Color.Gray,
                        fontSize = 8.5.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(20, 30, 60).forEach { rate ->
                        val active = mpManager.tickRateHz == rate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF00FFCC) else Color(0xFF1E293B))
                                .clickable { mpManager.tickRateHz = rate }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${rate}Hz",
                                color = if (active) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 8.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFCC),
                checkedTrackColor = Color(0x4D00FFCC)
            )
        )
    }
}

// ========== Game Mode Selector ==========
@Composable
private fun GameModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf("Casual", "Ranked", "Battle Royale", "Private Room")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0E17), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        modes.forEach { mode ->
            val isSelected = selectedMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) Color(0x33FFFFFF) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.uppercase(Locale.getDefault()),
                    color = if (isSelected) Color(0xFF00FFCC) else Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ========== Private Room Controls ==========
@Composable
private fun PrivateRoomControls(
    privateRoomCode: String,
    onCodeChange: (String) -> Unit,
    onGenerateCode: () -> Unit,
    mpManager: MultiplayerManager,
    mpStatus: ConnectionStatus,
    userProfile: com.example.data.UserProfile?,
    chatTextInput: String,
    onChatTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Room Code Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = privateRoomCode,
                    onValueChange = { onCodeChange(it.take(10).uppercase()) },
                    label = { Text("ROOM CODE") },
                    placeholder = { Text("E.g. COBRA-919") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFF00FFCC)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Button(
                    onClick = onGenerateCode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1F00FFCC),
                        contentColor = Color(0xFF00FFCC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        "GENERATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multiplayer Lobby Card
            MultiplayerLobbyCard(
                mpStatus = mpStatus,
                mpManager = mpManager,
                userProfile = userProfile,
                privateRoomCode = privateRoomCode,
                chatTextInput = chatTextInput,
                onChatTextChange = onChatTextChange,
                onSendMessage = onSendMessage
            )
        }
    }
}

// ========== Multiplayer Lobby Card ==========
@Composable
private fun MultiplayerLobbyCard(
    mpStatus: ConnectionStatus,
    mpManager: MultiplayerManager,
    userProfile: com.example.data.UserProfile?,
    privateRoomCode: String,
    chatTextInput: String,
    onChatTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val participants by mpManager.activeParticipants.collectAsStateWithLifecycle()
    val chatMessages by mpManager.chatMessages.collectAsStateWithLifecycle()
    val pingMs by mpManager.pingMs.collectAsStateWithLifecycle()

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        borderColor = when (mpStatus) {
            ConnectionStatus.CONNECTED -> Color(0xFF00FFCC).copy(alpha = 0.5f)
            ConnectionStatus.CONNECTING, ConnectionStatus.HANDSHARING -> Color(0xFFFFCC00).copy(alpha = 0.5f)
            else -> Color(0x33FFFFFF)
        },
        backgroundColor = Color(0x1F0A0E1A)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (mpStatus) {
                                    ConnectionStatus.CONNECTED -> Color(0xFF00FFCC)
                                    ConnectionStatus.CONNECTING, ConnectionStatus.HANDSHARING -> Color(0xFFFFCC00)
                                    else -> Color.DarkGray
                                }
                            )
                    )
                    Text(
                        text = "WS NETWORK LOBBY: ${mpStatus.name}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                if (mpStatus == ConnectionStatus.CONNECTED) {
                    Text(
                        text = "PING: ${pingMs}MS",
                        color = Color(0xFF00FFCC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            when (mpStatus) {
                ConnectionStatus.OFFLINE, ConnectionStatus.DISCONNECTED -> {
                    Text(
                        text = "Ready to deploy real-time low-latency cross-device room sync? Hook up to our global Socket.IO lobby network to sync multiplayer game matches, room chats, list participants, and deploy instantly with other live players on the same Room Code.",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Button(
                        onClick = {
                            val finalCode = privateRoomCode.ifBlank { "LOBBY-VIPER" }
                            mpManager.connectToRoomWebSocket(finalCode, userProfile?.username ?: "Player")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SOCKET.IO MULTIPLAYER CONNECT",
                            color = Color.Black,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                ConnectionStatus.CONNECTING, ConnectionStatus.HANDSHARING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00FFCC),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NEGOTIATING STUN HANDSHAKE RELAY...",
                            color = Color(0xFFFFCC00),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ConnectionStatus.CONNECTED -> {
                    // Participants
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ACTIVE ROOM PARTICIPANTS (${participants.size}):",
                            color = Color.Gray,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = participants,
                                key = { it }
                            ) { user ->
                                val isSelf = user == (userProfile?.username ?: "Player")
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelf) Color(0x3300FFCC) else Color(0x11FFFFFF))
                                        .border(1.dp, if (isSelf) Color(0x6600FFCC) else Color(0x1Fffffff), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelf) Color(0xFF00FFCC) else Color.LightGray,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = user.uppercase(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Chat
                    ChatBox(
                        chatMessages = chatMessages,
                        currentUsername = userProfile?.username ?: "Player",
                        chatTextInput = chatTextInput,
                        onChatTextChange = onChatTextChange,
                        onSendMessage = onSendMessage
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { mpManager.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF3366)),
                        border = BorderStroke(1.dp, Color(0xFFFF3366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DISCONNECT FROM LOBBY NETWORK",
                            color = Color(0xFFFF3366),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ========== Chat Box ==========
@Composable
private fun ChatBox(
    chatMessages: List<com.example.game.LobbyChatMessage>,
    currentUsername: String,
    chatTextInput: String,
    onChatTextChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "LOBBY SECURED CHAT TRANSMISSIONS:",
            color = Color.Gray,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO INCOMING MESSAGES YET. START TYPING BELOW!",
                        color = Color.DarkGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = chatMessages.reversed(),
                        key = { it.id }
                    ) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "[${msg.sender.uppercase()}]:",
                                color = if (msg.sender == currentUsername) Color(0xFF00FFCC) else Color(0xFFFF3366),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = msg.text,
                                color = Color.LightGray,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatTextInput,
                onValueChange = onChatTextChange,
                placeholder = { Text("Signal text...", fontSize = 9.sp, color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFCC),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
            )

            Button(
                onClick = onSendMessage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300FFCC)),
                border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "SEND",
                    color = Color(0xFF00FFCC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ========== Match History ==========
@Composable
private fun MatchHistory(matchRecords: List<com.example.data.MatchRecord>) {
    Text(
        text = "RECENT MATCH HISTORY",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    if (matchRecords.isEmpty()) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0x11FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Empty Matches",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No games recorded yet!\nChoose a mode above and jump into the grid.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            matchRecords.take(5).forEach { record ->
                HistoricalRecordRow(record)
            }
        }
    }
}

// ========== Missions & Battle Pass ==========
@Composable
private fun MissionsAndBattlePass(
    userProfile: com.example.data.UserProfile?,
    viewModel: GameViewModel
) {
    Text(
        text = "NEON BATTLE PASS - SEASON 1",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Advance your slithering achievements to unlock elite tier reward cells",
        color = Color.Gray,
        fontSize = 10.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )

    val bpLevel = ((userProfile?.level ?: 1) - 1).coerceAtLeast(1)
    val totalTiers = 5

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        borderColor = Color(0x33FFFF00),
        backgroundColor = Color(0x1F110B29),
        glowColor = Color(0x22FFFF00)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BATTLE PASS LEVEL $bpLevel", color = Color(0xFFFFFF33), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("TIERS: $bpLevel / $totalTiers", color = Color.Gray, fontSize = 9.sp)
            }

            val progressFraction = (bpLevel.toFloat() / totalTiers.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFFF33),
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Battle Pass Tiers
            BattlePassTiers(
                bpLevel = bpLevel,
                onClaimReward = { tierNum, rewardDesc ->
                    when (tierNum) {
                        1 -> viewModel.earnFreeCoins(200)
                        2 -> viewModel.buyCosmetic("Glow Cyber", "Skin", 0, {}, {}) // In real app, handle properly
                        3 -> viewModel.earnFreeCoins(600)
                        4 -> viewModel.buyCosmetic("Space Wraith", "Skin", 0, {}, {})
                        5 -> viewModel.buyCosmetic("Meteor Trail", "Trail", 0, {}, {})
                    }
                }
            )
        }
    }

    // Daily & Weekly Missions
    Text(
        text = "DAILY & WEEKLY MISSIONS",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
    Text(
        text = "Execute target assignments in active grids to harvest progression multipliers",
        color = Color.Gray,
        fontSize = 10.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )

    val dailyMissions = listOf(
        "Inhale 12 Super Orbs in any arena theme" to true,
        "Activate Tactical Ability 5 times" to false,
        "Engage in 1 Ranked multiplayer slither match" to true
    )
    val weeklyMissions = listOf(
        "Accumulate 1500 XP in player progression levels" to false,
        "Defeat 5 opponent snakes in the Battle Royale grid" to true,
        "Reach Silver ranked multiplayer score threshold" to false
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Text("DAILY TARGETS", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        dailyMissions.forEach { (desc, done) ->
            MissionItemRow(desc, done)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("WEEKLY DIRECTIVES", color = Color(0xFF9933FF), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        weeklyMissions.forEach { (desc, done) ->
            MissionItemRow(desc, done)
        }
    }
}

// ========== Battle Pass Tiers ==========
@Composable
private fun BattlePassTiers(
    bpLevel: Int,
    onClaimReward: (Int, String) -> Unit
) {
    val rewards = listOf(
        Triple("T1: 200 COINS", "Free", 1),
        Triple("T2: GLOW CYBER", "Free", 2),
        Triple("T3: 600 COINS", "Premium", 3),
        Triple("T4: SPACE WRAITH", "Premium", 4),
        Triple("T5: METEOR TRAIL", "Premium", 5)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rewards.forEach { (label, tierType, tierNum) ->
            val unlocked = bpLevel >= tierNum
            val borderCol = if (unlocked) Color(0xFFFFFF33) else Color(0x11FFFFFF)
            val bgCol = if (unlocked) Color(0x1F22C55E) else Color(0x0AFFFFFF)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgCol)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable(enabled = unlocked) {
                        if (unlocked) onClaimReward(tierNum, label)
                    }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (unlocked) Color(0xFF22C55E) else Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(label, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(tierType.uppercase(), color = if (tierType == "Free") Color(0xFF00FFCC) else Color(0xFFFF3366), fontSize = 6.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ========== Edit Name Dialog ==========
@Composable
private fun EditNameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                text = "EDIT PROFILE ENTRY CODE",
                color = Color(0xFF00FFCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set your visual identity within the game arena. You can enter any combination of alpha, numeric, or underscore characters (e.g., 1_ffg). Max 15 chars.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { onNameChange(it.take(15)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFF00FFCC)
                    ),
                    placeholder = { Text("E.g. 1_ffg", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFCC),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "APPLY IDENTIFIER",
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "ABORT RESET",
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        },
        modifier = Modifier
            .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            .padding(2.dp)
    )
}

// ========== Info Stat Item ==========
@Composable
fun InfoStatItem(label: String, value: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x06FFFFFF))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ========== Historical Record Row ==========
@Composable
fun HistoricalRecordRow(record: com.example.data.MatchRecord) {
    val dateStr = remember(record.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }

    val modeColor = when (record.mode) {
        "Ranked" -> Color(0xFFFF9900)
        "Battle Royale" -> Color(0xFFFF3366)
        else -> Color(0xFF00FFCC)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x09FFFFFF))
            .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.mode.uppercase(Locale.getDefault()),
                    color = modeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SCORE: ${record.score}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+${record.xpEarned} XP",
                        color = Color(0xFF00FFCC),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "+${record.coinsEarned} C",
                        color = Color(0xFFFFFF33),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ========== Mission Item Row ==========
@Composable
fun MissionItemRow(desc: String, done: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, if (done) Color(0x3322C55E) else Color(0x0AFFFFFF), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = desc,
                color = if (done) Color.Gray else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (done) Color(0xFF22C55E) else Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ========== Rank Helpers ==========
fun getRankTier(points: Int): String {
    return when {
        points < 1200 -> "Bronze"
        points < 1400 -> "Silver"
        points < 1600 -> "Gold"
        points < 1800 -> "Platinum"
        points < 2000 -> "Diamond"
        else -> "Legend"
    }
}

fun getRankColor(tier: String): Color {
    return when (tier) {
        "Bronze" -> Color(0xFFCD7F32)
        "Silver" -> Color(0xFFC0C0C0)
        "Gold" -> Color(0xFFFFD700)
        "Platinum" -> Color(0xFFE5E4E2)
        "Diamond" -> Color(0xFF33CCFF)
        else -> Color(0xFF00FFCC) // Legend teal glow
    }
}

fun getRankIcon(tier: String): ImageVector {
    return when (tier) {
        "Bronze" -> Icons.Default.Terrain
        "Silver" -> Icons.Default.Shield
        "Gold" -> Icons.Default.MilitaryTech
        "Platinum" -> Icons.Default.WorkspacePremium
        "Diamond" -> Icons.Default.Diamond
        else -> Icons.Default.Star
    }
}