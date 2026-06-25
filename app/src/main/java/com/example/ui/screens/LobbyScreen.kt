package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.*
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

// ========== Color Palette ==========
val Background = Color(0xFF0B1020)
val Surface = Color(0xFF151C30)
val Primary = Color(0xFF3B82F6)
val Secondary = Color(0xFF8B5CF6)
val Success = Color(0xFF22C55E)
val Danger = Color(0xFFEF4444)
val TextWhite = Color.White
val TextGray = Color(0xFFB0B8C1)
val TextLight = Color(0xFF6B7280)
val Gold = Color(0xFFFFD700)
val PremiumGold = Color(0xFFFFC107)
val RareBlue = Color(0xFF00BFFF)
val EpicPurple = Color(0xFF9C27B0)
val LegendaryOrange = Color(0xFFFF9800)

// ========== Main Lobby Screen ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onNavigateToShop: () -> Unit,
    onNavigateToClans: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToSkinLocker: () -> Unit
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
    var showPrivateRoomDialog by remember { mutableStateOf(false) }
    var showMultiplayerSettings by remember { mutableStateOf(false) }
    var showMatchSettingsDialog by remember { mutableStateOf(false) }
    var showRoyalPassDialog by remember { mutableStateOf(false) }

    val rankTier by derivedStateOf { getRankTier(userProfile?.rankedScore ?: 0) }
    val xpProgress by derivedStateOf {
        val level = userProfile?.level ?: 1
        val xp = userProfile?.xp ?: 0
        val reqXp = level * 1000
        (xp.toFloat() / reqXp.toFloat()).coerceIn(0f, 1f)
    }

    var friendsOnline by remember { mutableStateOf(listOf("Alex", "Mehir", "pari", "Kalu", "Jade", "Leo", "Riya")) }
    var playersOnlineCount by remember { mutableStateOf(2431) }
    val queueTimeSec by remember { mutableStateOf(3) }
    var pointsEaten by remember { mutableStateOf(0) }
    var skinCycle by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            playersOnlineCount += Random.nextInt(-50, 50)
            if (playersOnlineCount < 1800) playersOnlineCount = 1800
        }
    }

    LaunchedEffect(lobbyStartTrigger) {
        if (lobbyStartTrigger) {
            viewModel.startNewGame(selectedMode, selectedTheme, privateRoomCode)
            mpManager.disconnect()
        }
    }

    DisposableEffect(Unit) {
        onDispose { mpManager.disconnect() }
    }

    LaunchedEffect(selectedMode) {
        if (selectedMode != "Private Room" && mpStatus == ConnectionStatus.CONNECTED) {
            mpManager.disconnect()
        }
    }

    val seasonEndDate = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 12) }.time }
    var daysLeft by remember { mutableStateOf(12L) }
    LaunchedEffect(seasonEndDate) {
        while (true) {
            daysLeft = ((seasonEndDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            delay(3600000)
        }
    }

    var activeTab by remember { mutableStateOf("HOME") }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            InteractiveGameBottomBar(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    activeTab = tab
                    when (tab) {
                        "HOME" -> { /* Default */ }
                        "SNAKES" -> onNavigateToSkinLocker()
                        "EVENTS" -> showMultiplayerSettings = true
                        "LEADERBOARDS" -> onNavigateToLeaderboard()
                        "SHOP" -> onNavigateToShop()
                    }
                }
            )
        }
    ) { innerPadding ->
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GameHeaderHUD(
                username = userProfile?.username ?: "SNAKE_KING",
                level = userProfile?.level ?: 24,
                goldPoints = userProfile?.coins ?: 5400,
                gemPoints = (userProfile?.coins ?: 5400) / 20 + 120,
                rank = rankTier,
                xpProgress = xpProgress,
                onEditNameClick = {
                    editedName = userProfile?.username ?: ""
                    showEditNameDialog = true
                },
                onSettingsClick = { showMultiplayerSettings = true },
                onMailClick = { viewModel.earnFreeCoins(500) },
                onAlertClick = { /* claim points */ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SocialFeedPanel(
                            friendsOnline = friendsOnline,
                            onPartyInvitesClick = { showPrivateRoomDialog = true },
                            onRecentNewsClick = { showMultiplayerSettings = true }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitle("HISTORY CHRONICLES")
                        MatchHistoryV2(matchRecords = matchRecords)
                    }

                    Column(modifier = Modifier.weight(1.8f)) {
                        HexGridLiveSnakePreview(
                            pointsEaten = pointsEaten,
                            onPointsEatenChange = { pointsEaten = it },
                            skinCycle = skinCycle,
                            onSkinCycleChange = { skinCycle = it }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PlayNowButtonCard(
                            playersCount = playersOnlineCount,
                            queueTime = queueTimeSec,
                            onClick = {
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
                                if (selectedMode == "Private Room") mpManager.disconnect()
                            }
                        )

                        MultiplayerSelectorV2(
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it },
                            onSettingsClick = { showMatchSettingsDialog = true }
                        )

                        RoyalPassCard(
                            level = (userProfile?.level ?: 24),
                            xpProgress = xpProgress,
                            onViewRewardsClick = { showRoyalPassDialog = true }
                        )

                        if (selectedMode == "Private Room") {
                            SectionTitle("PRIVATE LOBBY PORTAL")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.5.dp, Primary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lobby Code Setup", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                                                privateRoomCode = "${prefixes.random()}-${(100..999).random()}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("GENERATE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = privateRoomCode,
                                        onValueChange = { privateRoomCode = it.uppercase() },
                                        placeholder = { Text("Enter room code...", color = TextLight, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = TextLight.copy(alpha = 0.4f)
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

                        SectionTitle("GAME MODES")
                        GameModeRow(
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it }
                        )

                        SectionTitle("ARENA THEMES")
                        ArenaThemeRow(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it }
                        )

                        val maxSnakesState by viewModel.maxMatchSnakes.collectAsStateWithLifecycle()
                        SectionTitle("MATCH SCALE & DENSITY")
                        MatchScaleRow(
                            selectedScale = maxSnakesState,
                            onScaleSelected = { viewModel.maxMatchSnakes.value = it }
                        )

                        SectionTitle("TACTICAL CLASS")
                        TacticalClassRow(
                            selectedClass = selectedClass,
                            onClassSelected = { viewModel.selectedAbility.value = it }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "hex_grid_preview") {
                        HexGridLiveSnakePreview(
                            pointsEaten = pointsEaten,
                            onPointsEatenChange = { pointsEaten = it },
                            skinCycle = skinCycle,
                            onSkinCycleChange = { skinCycle = it }
                        )
                    }

                    item(key = "portrait_play_now") {
                        PlayNowButtonCard(
                            playersCount = playersOnlineCount,
                            queueTime = queueTimeSec,
                            onClick = {
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
                                if (selectedMode == "Private Room") mpManager.disconnect()
                            }
                        )
                    }

                    item(key = "portrait_multiplayer") {
                        MultiplayerSelectorV2(
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it },
                            onSettingsClick = { showMatchSettingsDialog = true }
                        )
                    }

                    item(key = "portrait_royal_pass") {
                        RoyalPassCard(
                            level = (userProfile?.level ?: 24),
                            xpProgress = xpProgress,
                            onViewRewardsClick = { showRoyalPassDialog = true }
                        )
                    }

                    item(key = "portrait_social_feed") {
                        SocialFeedPanel(
                            friendsOnline = friendsOnline,
                            onPartyInvitesClick = { showPrivateRoomDialog = true },
                            onRecentNewsClick = { showMultiplayerSettings = true }
                        )
                    }

                    item(key = "portrait_modes_row") {
                        SectionTitle("GAME MODES")
                        GameModeRow(
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it }
                        )
                    }

                    if (selectedMode == "Private Room") {
                        item(key = "portrait_private_lobby_portal") {
                            SectionTitle("PRIVATE LOBBY PORTAL")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.5.dp, Primary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lobby Code Setup", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                                                privateRoomCode = "${prefixes.random()}-${(100..999).random()}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("GENERATE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = privateRoomCode,
                                        onValueChange = { privateRoomCode = it.uppercase() },
                                        placeholder = { Text("Enter room code...", color = TextLight, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = TextLight.copy(alpha = 0.4f)
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
                    }

                    item(key = "portrait_themes_row") {
                        SectionTitle("ARENA THEMES")
                        ArenaThemeRow(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it }
                        )
                    }

                    item(key = "portrait_match_scale") {
                        val maxSnakesState by viewModel.maxMatchSnakes.collectAsStateWithLifecycle()
                        SectionTitle("MATCH SCALE & DENSITY")
                        MatchScaleRow(
                            selectedScale = maxSnakesState,
                            onScaleSelected = { viewModel.maxMatchSnakes.value = it }
                        )
                    }

                    item(key = "portrait_tactical_row") {
                        SectionTitle("TACTICAL CLASS")
                        TacticalClassRow(
                            selectedClass = selectedClass,
                            onClassSelected = { viewModel.selectedAbility.value = it }
                        )
                    }

                    item(key = "portrait_match_history") {
                        SectionTitle("MATCH CHRONICLES")
                        MatchHistoryV2(matchRecords = matchRecords)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditNameDialog) {
        EditNameDialog(
            currentName = editedName,
            onSave = { trimmed ->
                viewModel.updateUsername(trimmed)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }

    if (showPrivateRoomDialog) {
        PrivateRoomDialog(
            initialCode = privateRoomCode,
            mpManager = mpManager,
            mpStatus = mpStatus,
            userProfile = userProfile,
            onCodeChange = { privateRoomCode = it },
            onDismiss = { showPrivateRoomDialog = false }
        )
    }

    if (showMultiplayerSettings) {
        MultiplayerSettingsSheet(
            viewModel = viewModel,
            mpManager = mpManager,
            onDismiss = { showMultiplayerSettings = false }
        )
    }

    if (showMatchSettingsDialog) {
        MatchSettingsDialog(
            viewModel = viewModel,
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            selectedTheme = selectedTheme,
            onThemeSelected = { selectedTheme = it },
            selectedClass = selectedClass,
            onClassSelected = { viewModel.selectedAbility.value = it },
            privateRoomCode = privateRoomCode,
            onCodeChange = { privateRoomCode = it },
            mpManager = mpManager,
            mpStatus = mpStatus,
            userProfile = userProfile,
            onStartBattle = {
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
                if (selectedMode == "Private Room") mpManager.disconnect()
            },
            onDismiss = { showMatchSettingsDialog = false }
        )
    }

    if (showRoyalPassDialog) {
        RoyalPassDialog(
            viewModel = viewModel,
            onDismiss = { showRoyalPassDialog = false }
        )
    }
}
