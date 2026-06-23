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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MatchRecord
import com.example.data.UserProfile
import com.example.game.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random
// Additional imports for Live Snake Canvas Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.hypot

// Color palette
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showPrivateRoomDialog by remember { mutableStateOf(false) }
    var showMultiplayerSettings by remember { mutableStateOf(false) }

    val rankTier by derivedStateOf { getRankTier(userProfile?.rankedScore ?: 0) }
    val xpProgress by derivedStateOf {
        val level = userProfile?.level ?: 1
        val xp = userProfile?.xp ?: 0
        val reqXp = level * 1000
        (xp.toFloat() / reqXp.toFloat()).coerceIn(0f, 1f)
    }

    // Mock data for new sections
    var friendsOnline by remember { mutableStateOf(listOf("Alex", "Mehir", "pari", "Kalu", "Jade", "Leo", "Riya")) }
    var dailyRewardDay by remember { mutableStateOf(5) }
    var dailyRewardClaimed by remember { mutableStateOf(false) }
    var playersOnlineCount by remember { mutableStateOf(2431) }
    var queueTimeSec by remember { mutableStateOf(3) }
    var pointsEaten by remember { mutableStateOf(0) }
    var skinCycle by remember { mutableStateOf(0) }

    // Simulate player count updates
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

    // Countdown for season end (12 days from a fixed date)
    val seasonEndDate = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 12) }.time }
    var daysLeft by remember { mutableStateOf(12L) }
    LaunchedEffect(seasonEndDate) {
        while (true) {
            daysLeft = ((seasonEndDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            delay(3600000) // update every hour
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
                        "HOME" -> { /* Default active view */ }
                        "SNAKES" -> {
                            editedName = userProfile?.username ?: ""
                            showEditNameDialog = true
                        }
                        "EVENTS" -> {
                            showMultiplayerSettings = true
                        }
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
            // 1. DYNAMIC CUSTOM GAME HEADER HUD (Profile, Title, Coins, Gems, Quick Actions)
            GameHeaderHUD(
                username = userProfile?.username ?: "SNAKE_KING",
                level = userProfile?.level ?: 24,
                goldPoints = userProfile?.coins ?: 5400,
                gemPoints = (userProfile?.coins ?: 5400) / 20 + 120, // Plus bonus gems for the reference style
                rank = rankTier,
                xpProgress = xpProgress,
                onEditNameClick = {
                    editedName = userProfile?.username ?: ""
                    showEditNameDialog = true
                },
                onSettingsClick = { showMultiplayerSettings = true },
                onMailClick = {
                    // Quick simulation: allow user to toggle settings or reset coins
                    viewModel.earnFreeCoins(500)
                },
                onAlertClick = {
                    // Quick simulation: claim points
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLandscape) {
                // Multi-Column Dashboard layout for Widescreen scales
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Social Feed Panel
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

                    // Center Column: Live Hexagonal Grid Preview Board with gold crown snake simulation
                    Column(
                        modifier = Modifier.weight(1.8f)
                    ) {
                        HexGridLiveSnakePreview(
                            pointsEaten = pointsEaten,
                            onPointsEatenChange = { pointsEaten = it },
                            skinCycle = skinCycle,
                            onSkinCycleChange = { skinCycle = it }
                        )
                    }

                    // Right Column: PLAY NOW deck, MULTIPLAYER selectors, and ROYAL PASS track
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
                            onSettingsClick = { showMultiplayerSettings = true }
                        )

                        RoyalPassCard(
                            level = ((userProfile?.level ?: 24)),
                            xpProgress = xpProgress,
                            onViewRewardsClick = {
                                // Claim battle pass rewards
                                viewModel.earnFreeCoins(300)
                            }
                        )
                    }
                }
            } else {
                // Adaptive Cascading Scroll Column for portrait smartphone scales
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Center Stage simulation
                    item(key = "hex_grid_preview") {
                        HexGridLiveSnakePreview(
                            pointsEaten = pointsEaten,
                            onPointsEatenChange = { pointsEaten = it },
                            skinCycle = skinCycle,
                            onSkinCycleChange = { skinCycle = it }
                        )
                    }

                    // Main Action Cards
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
                            onSettingsClick = { showMultiplayerSettings = true }
                        )
                    }

                    item(key = "portrait_royal_pass") {
                        RoyalPassCard(
                            level = ((userProfile?.level ?: 24)),
                            xpProgress = xpProgress,
                            onViewRewardsClick = {
                                viewModel.earnFreeCoins(300)
                            }
                        )
                    }

                    // Secondary info cards
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

                    item(key = "portrait_themes_row") {
                        SectionTitle("ARENA THEMES")
                        ArenaThemeRow(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it }
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

    // Dialogs (unchanged)
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
            mpManager = mpManager,
            onDismiss = { showMultiplayerSettings = false }
        )
    }
}

// ========== Section Title ==========
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// ========== Profile Header V2 ==========
@Composable
fun ProfileHeaderV2(
    userProfile: UserProfile?,
    rankTier: String,
    xpProgress: Float,
    onEditNameClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = xpProgress, animationSpec = tween(600), label = "xpProgress")
    val rankColor = getRankColor(rankTier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with rank badge
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Primary, Secondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(rankColor)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getRankIcon(rankTier),
                        contentDescription = "Rank",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userProfile?.username ?: "Player",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEditNameClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = "$rankTier · Level ${userProfile?.level ?: 1}",
                    color = rankColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // XP progress
                Column {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Primary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${userProfile?.xp ?: 0} / ${(userProfile?.level ?: 1) * 1000} XP",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Currencies
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                    Text(
                        text = "${userProfile?.coins ?: 0}",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${(userProfile?.coins ?: 0) / 20}",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== Hero Season Banner ==========
@Composable
fun HeroSeasonBanner(
    daysLeft: Long,
    onViewRewards: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1), Color(0xFF01579B))
                    )
                )
        ) {
            // Animated particles (simplified)
            val infiniteTransition = rememberInfiniteTransition(label = "particles")
            val p0 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse), label = "p0")
            val p1 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3100, easing = LinearEasing), RepeatMode.Reverse), label = "p1")
            val p2 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse), label = "p2")
            val p3 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse), label = "p3")
            val p4 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "p4")
            val p5 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2900, easing = LinearEasing), RepeatMode.Reverse), label = "p5")
            val p6 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3300, easing = LinearEasing), RepeatMode.Reverse), label = "p6")
            val p7 = infiniteTransition.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "p7")
            val particlePositions = listOf(p0, p1, p2, p3, p4, p5, p6, p7)

            Canvas(modifier = Modifier.fillMaxSize()) {
                particlePositions.forEachIndexed { index, offset ->
                    val x = size.width * (0.1f + ((index * 0.12f) + offset.value * 0.05f).coerceIn(0f, 1f))
                    val y = size.height * (0.2f + (index % 3) * 0.25f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = 4f + (index % 3) * 2f,
                        center = Offset(x, y)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "SEASON 7",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "CYBER REBIRTH",
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Battle Pass Ends:",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        "$daysLeft Days",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Level 5 / 50",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onViewRewards,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VIEW REWARDS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Progress bar
                LinearProgressIndicator(
                    progress = { 0.1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Secondary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

// ========== Featured Event Card ==========
@Composable
fun FeaturedEventCard(
    onJoinEvent: () -> Unit
) {
    val borderAnimation = rememberInfiniteTransition(label = "border")
    val borderProgress by borderAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "borderAnim"
    )
    val borderColor = lerp(Color(0xFFFFD700), Color(0xFFFFA000), borderProgress)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF2E1A47), Color(0xFF1A1A2E))
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("FEATURED EVENT", color = TextGray, fontSize = 12.sp, letterSpacing = 1.sp)
                    Text(
                        "Cyber Arena Championship",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Prize Pool: 50,000 Coins", color = Gold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ends in 2d 14h", color = TextLight, fontSize = 12.sp)
                }
                Button(
                    onClick = onJoinEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join Event", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ========== Game Mode Row (redesigned) ==========
@Composable
fun GameModeRow(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf(
        "Casual" to "Quick Match",
        "Ranked" to "Competitive +Rank",
        "Battle Royale" to "Last Snake Standing",
        "Private Room" to "Friends Only"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(modes) { (mode, desc) ->
            val isSelected = selectedMode == mode
            ModeCardV2(
                mode = mode,
                description = desc,
                isSelected = isSelected,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
fun ModeCardV2(
    mode: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "modeScale"
    )
    val glowColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.Transparent,
        animationSpec = tween(300),
        label = "glow"
    )

    Card(
        modifier = modifier
            .height(110.dp)
            .scale(scale)
            .shadow(if (isSelected) 8.dp else 0.dp, RoundedCornerShape(20.dp), ambientColor = glowColor, spotColor = glowColor)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.15f) else Surface
        ),
        border = BorderStroke(1.5.dp, if (isSelected) Primary else Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = when (mode) {
                    "Casual" -> Icons.Default.SportsEsports
                    "Ranked" -> Icons.Default.MilitaryTech
                    "Battle Royale" -> Icons.Default.EmojiEvents
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (isSelected) Primary else TextGray,
                modifier = Modifier.size(30.dp)
            )
            Column {
                Text(
                    text = mode,
                    color = if (isSelected) Primary else TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextLight,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ========== Arena Theme Redesign ==========
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
            containerColor = if (isSelected) Secondary.copy(alpha = 0.2f) else Surface
        ),
        border = BorderStroke(2.dp, borderAnim)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Thumbnail placeholder (could use actual images later)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Landscape, // Replace with themed icon
                    contentDescription = null,
                    tint = if (isSelected) Secondary else TextGray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    theme.displayName,
                    color = if (isSelected) Secondary else TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Night · Urban",
                    color = TextLight,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ========== Tactical Class Redesign ==========
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

// Extended ClassInfo with cooldown
data class ClassInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val cooldown: Int
)

// ========== Daily Login Reward ==========
@Composable
fun DailyLoginReward(
    day: Int,
    claimed: Boolean,
    onClaim: () -> Unit
) {
    val chestScale by animateFloatAsState(
        targetValue = if (!claimed) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse) ,
        label = "chestAnim"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(chestScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Gold, PremiumGold))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DAILY REWARD", color = TextGray, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(
                    "Day $day",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Reward: 300 Coins",
                    color = Gold,
                    fontSize = 14.sp
                )
            }
            Button(
                onClick = onClaim,
                enabled = !claimed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (claimed) TextLight else Primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (claimed) "CLAIMED" else "CLAIM", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========== Friends Online Widget ==========
@Composable
fun FriendsOnlineWidget(
    friends: List<String>,
    onInvite: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ONLINE FRIENDS",
                    color = TextGray,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    "${friends.size} Online",
                    color = Success,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                friends.take(4).forEach { friend ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Secondary, modifier = Modifier.size(28.dp))
                        }
                        Text(friend, color = TextWhite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onInvite(friends.firstOrNull() ?: "") },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Invite", fontSize = 12.sp)
                }
            }
        }
    }
}

// ========== Battle Pass V2 ==========
@Composable
fun BattlePassV2(
    bpLevel: Int,
    onClaimReward: (Int, String) -> Unit
) {
    // Simplified reward tiers with free/premium tracks
    val tiers = listOf(
        "200 Coins" to false, // free
        "Glow Cyber (Skin)" to true, // premium
        "600 Coins" to false,
        "Space Wraith (Skin)" to true,
        "Meteor Trail" to true
    )
    val claimedStates = remember { mutableStateListOf(*Array(tiers.size) { false }) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "BATTLE PASS · Season 7",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Progression line
            LinearProgressIndicator(
                progress = { (bpLevel.toFloat() / tiers.size.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Secondary,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tiers.size) { index ->
                    val (reward, isPremium) = tiers[index]
                    val unlocked = bpLevel > index
                    val claimed = claimedStates[index]
                    TierCardV2(
                        reward = reward,
                        isPremium = isPremium,
                        unlocked = unlocked,
                        claimed = claimed,
                        onClaim = {
                            if (unlocked && !claimed) {
                                onClaimReward(index + 1, reward) // tierNum 1-based
                                claimedStates[index] = true
                            }
                        },
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TierCardV2(
    reward: String,
    isPremium: Boolean,
    unlocked: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        claimed -> Success
        isPremium -> PremiumGold
        else -> if (unlocked) Primary else TextLight
    }
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(enabled = unlocked && !claimed, onClick = onClaim),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked && !claimed) Primary.copy(alpha = 0.1f) else Surface
        ),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (claimed) Icons.Default.CheckCircle
                else if (unlocked) Icons.Default.LockOpen
                else Icons.Default.Lock,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                reward,
                color = if (claimed) TextGray else TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (isPremium) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(16.dp)
                )
                Text("PREMIUM", color = PremiumGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (unlocked && !claimed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("CLAIM", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========== Social Redesign (rows of cards) ==========
@Composable
fun SocialRedesign(
    onShop: () -> Unit,
    onClan: () -> Unit,
    onLeaderboard: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(
                    icon = Icons.Default.Store,
                    title = "Shop",
                    subtitle = "Gear Up",
                    onClick = onShop
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(
                    icon = Icons.Default.Groups,
                    title = "Clan",
                    subtitle = "2 Online",
                    onClick = onClan
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(
                    icon = Icons.Default.Leaderboard,
                    title = "Leaderboard",
                    subtitle = "Top 100",
                    onClick = onLeaderboard
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(
                    icon = Icons.Default.CardGiftcard,
                    title = "Rewards",
                    subtitle = "5 New",
                    onClick = { /* Rewards screen */ }
                )
            }
        }
    }
}

@Composable
fun SocialPremiumCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextGray, fontSize = 12.sp)
        }
    }
}

// ========== Match History V2 ==========
@Composable
fun MatchHistoryV2(matchRecords: List<MatchRecord>) {
    if (matchRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = TextLight, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No matches yet", color = TextGray, fontSize = 16.sp)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            matchRecords.take(5).forEach { record ->
                MatchCardV2(record)
            }
        }
    }
}

@Composable
fun MatchCardV2(record: MatchRecord) {
    val dateStr = remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }
    val won = record.score > 0 // example logic
    val backgroundColor = if (won) Success.copy(alpha = 0.05f) else Danger.copy(alpha = 0.05f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (won) Icons.Default.EmojiEvents else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (won) Success else Danger,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${if (won) "🏆 Victory" else "Defeat"} · ${record.mode}",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(dateStr, color = TextGray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Score ${record.score}",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+${record.xpEarned} XP", color = Primary, fontSize = 12.sp)
                    Text("+${record.coinsEarned} coins", color = Gold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ========== Private Room Quick Card (same as original, minor tweaks) ==========
@Composable
fun PrivateRoomQuickCard(
    privateRoomCode: String,
    mpStatus: ConnectionStatus,
    onOpenRoom: () -> Unit,
    onGenerateCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Private Room", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (privateRoomCode.isNotBlank()) "Code: $privateRoomCode" else "No code generated",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Status: ${mpStatus.name}",
                    color = if (mpStatus == ConnectionStatus.CONNECTED) Success else TextLight,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onOpenRoom, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Lobby", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGenerateCode,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Secondary),
                    border = BorderStroke(1.dp, Secondary)
                ) {
                    Text("Generate Code")
                }
            }
        }
    }
}

// ========== Mission List (redesigned card style) ==========
@Composable
fun MissionList(title: String, missions: List<Pair<String, Boolean>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            missions.forEach { (desc, done) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        desc,
                        color = if (done) TextGray else TextWhite,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (done) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (done) Success else TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ========== Private Room Dialog (unchanged logic, updated styling) ==========
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

// ========== Multiplayer Settings Sheet (unchanged, but restyled) ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerSettingsSheet(
    mpManager: MultiplayerManager,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Network Settings", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            // Region
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Server Region", color = TextGray, fontSize = 16.sp)
                Button(
                    onClick = {
                        val regions = ServerRegion.values()
                        val next = (mpManager.selectedRegion.ordinal + 1) % regions.size
                        mpManager.selectedRegion = regions[next]
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Primary),
                    border = BorderStroke(1.dp, Primary)
                ) {
                    Text(mpManager.selectedRegion.regionName)
                }
            }
            // Lag Compensation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Lag Compensation", color = TextWhite, fontSize = 16.sp)
                    Text("Interpolates packets", color = TextLight, fontSize = 12.sp)
                }
                Switch(
                    checked = mpManager.isLagCompensationEnabled,
                    onCheckedChange = { mpManager.isLagCompensationEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary)
                )
            }
            // Tick Rate
            Text("Tick Rate", color = TextWhite, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(20, 30, 60).forEach { rate ->
                    FilterChip(
                        selected = mpManager.tickRateHz == rate,
                        onClick = { mpManager.tickRateHz = rate },
                        label = { Text("${rate}Hz") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

// ========== Edit Name Dialog (unchanged) ==========
@Composable
fun EditNameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Change Username", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter a new display name (max 15 characters)",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { onNameChange(it.take(15)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = Primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

// ========== Rank Helpers (unchanged) ==========
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
        else -> Primary
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

// ========== Multiplayer Lobby Card (from original) ==========
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
                // This now works perfectly because participants is a unwrapped List
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
                
                // Chat Section
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

// ========== Live 3D/Canvas Snake Preview Pane ==========

data class PreviewParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var life: Float,
    var size: Float
)

@Composable
fun LiveSnakePreviewPane() {
    var width by remember { mutableStateOf(300f) }
    var height by remember { mutableStateOf(180f) }

    // Snake parts
    val snakeSegments = remember { mutableStateListOf<Offset>() }
    var food by remember { mutableStateOf(Offset(150f, 90f)) }
    val particles = remember { mutableStateListOf<PreviewParticle>() }
    var pointsEaten by remember { mutableStateOf(0) }
    var skinCycle by remember { mutableStateOf(0) } // Cycles skins: Neon Green, Cyber Violet, Meteor Orange

    var targetOverride by remember { mutableStateOf<Offset?>(null) }
    var frameTick by remember { mutableStateOf(0) }

    // Standard properties for the selected skin
    val skinColors = listOf(
        listOf(Color(0xFF00FFCC), Color(0xFF008B8B), Color(0xFFE2FDF5)), // Neon Green/Cyan/Bright Mint
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFDE8EF)), // Violet/Pink/White Rose
        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFFFFFDE7))  // Orange/Yellow/Warm light
    )
    val curSkin = skinColors[skinCycle % skinColors.size]

    // Initialize snake segments if empty
    LaunchedEffect(width, height) {
        if (snakeSegments.isEmpty() && width > 0f && height > 0f) {
            val cx = width / 2
            val cy = height / 2
            repeat(12) { i ->
                snakeSegments.add(Offset(cx - i * 11f, cy))
            }
            food = Offset(
                Random.nextFloat() * (width - 40f) + 20f,
                Random.nextFloat() * (height - 40f) + 20f
            )
        }
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        val random = Random(System.currentTimeMillis())
        while (true) {
            delay(16) // ~60fps
            frameTick++

            if (snakeSegments.isEmpty() || width <= 0f || height <= 0f) continue

            val head = snakeSegments.first()
            val target = targetOverride ?: food

            // Move head towards target with smooth steering & sine-wave slither
            val dx = target.x - head.x
            val dy = target.y - head.y
            val dist = hypot(dx, dy)

            val speed = 3.8f
            var vx = 0f
            var vy = 0f

            if (dist > 2f) {
                val baseVx = (dx / dist) * speed
                val baseVy = (dy / dist) * speed

                // Slither movement (add sine-wave fluctuation perpendicular to motion vector)
                val slitherFreq = 0.22f
                val slitherAmp = 1.2f
                val perpX = -baseVy
                val perpY = baseVx
                val slitherOffset = sin(frameTick * slitherFreq) * slitherAmp

                vx = baseVx + (perpX / speed) * slitherOffset
                vy = baseVy + (perpY / speed) * slitherOffset
            } else {
                if (targetOverride != null) {
                    targetOverride = null
                }
            }

            // Update Head position
            val newHead = Offset(
                (head.x + vx).coerceIn(10f, width - 10f),
                (head.y + vy).coerceIn(10f, height - 10f)
            )
            if (newHead.x == 10f || newHead.x == width - 10f || newHead.y == 10f || newHead.y == height - 10f) {
                targetOverride = null
            }

            // Propagate joints with standard distance decay
            val updatedSegments = ArrayList<Offset>(snakeSegments.size)
            updatedSegments.add(newHead)

            var prev = newHead
            val targetDist = 9.5f
            for (i in 1 until snakeSegments.size) {
                val curr = snakeSegments[i]
                val sDx = curr.x - prev.x
                val sDy = curr.y - prev.y
                val sDist = hypot(sDx, sDy)
                if (sDist > targetDist) {
                    val ratio = targetDist / sDist
                    updatedSegments.add(Offset(prev.x + sDx * ratio, prev.y + sDy * ratio))
                } else {
                    updatedSegments.add(curr)
                }
                prev = updatedSegments.last()
            }

            snakeSegments.clear()
            snakeSegments.addAll(updatedSegments)

            // Food collision check
            val fDx = food.x - newHead.x
            val fDy = food.y - newHead.y
            val fDist = hypot(fDx, fDy)
            if (fDist < 15f) {
                // Burst sparkling particles
                repeat(18) {
                    val ang = random.nextFloat() * 2f * Math.PI.toFloat()
                    val pSpeed = random.nextFloat() * 4.5f + 1.5f
                    particles.add(
                        PreviewParticle(
                            x = food.x,
                            y = food.y,
                            vx = cos(ang) * pSpeed,
                            vy = sin(ang) * pSpeed,
                            color = curSkin.random(),
                            life = 1f,
                            size = random.nextFloat() * 5f + 2f
                        )
                    )
                }

                // Grow snake body segments
                val tail = snakeSegments.lastOrNull() ?: newHead
                snakeSegments.add(tail)

                pointsEaten++
                if (pointsEaten % 3 == 0) {
                    skinCycle++
                }

                // Relocate food inside secure padding
                food = Offset(
                    random.nextFloat() * (width - 50f) + 25f,
                    random.nextFloat() * (height - 50f) + 25f
                )
            }

            // Update Particle alpha/positions
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.95f
                p.vy *= 0.95f
                p.life -= 0.025f
                if (p.life <= 0f) {
                    iterator.remove()
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFCC))
                    )
                    Text(
                        text = "LIVE SNAKE PREVIEW",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Points eaten counter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SCORE: $pointsEaten",
                            color = Color(0xFF00FFCC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Active Cosmetic Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(curSkin[0].copy(alpha = 0.15f))
                            .border(1.dp, curSkin[0].copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (skinCycle % 3) {
                                0 -> "NEON VIPER"
                                1 -> "CYBER GLOW"
                                else -> "SOLAR FLARE"
                            },
                            color = curSkin[0],
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Canvas viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val gridAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gridAlpha"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            width = size.width.toFloat()
                            height = size.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                targetOverride = offset
                                // Interactive sparkling burst on tap
                                repeat(12) {
                                    val r = Random(System.nanoTime())
                                    val ang = r.nextFloat() * 2f * Math.PI.toFloat()
                                    val spd = r.nextFloat() * 4f + 1f
                                    particles.add(
                                        PreviewParticle(
                                            x = offset.x,
                                            y = offset.y,
                                            vx = cos(ang) * spd,
                                            vy = sin(ang) * spd,
                                            color = curSkin.random(),
                                            life = 1f,
                                            size = r.nextFloat() * 4f + 2f
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    val gridSize = 25f

                    // Draw vertical grid lines
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = gridAlpha),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.0f
                        )
                        x += gridSize
                    }

                    // Draw horizontal grid lines
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = gridAlpha),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.0f
                        )
                        y += gridSize
                    }

                    // Draw steering target assist reticle
                    targetOverride?.let { tgt ->
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.25f),
                            radius = 12f + sin(frameTick * 0.12f).absoluteValue * 3f,
                            center = tgt,
                            style = Stroke(width = 1.5f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(tgt.x, 0f),
                            end = Offset(tgt.x, size.height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(0f, tgt.y),
                            end = Offset(size.width, tgt.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }

                    // Drawing Particles
                    particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.life),
                            radius = p.size * p.life,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // Goal point (Food Orb)
                    val pulseScale = 1.0f + sin(frameTick * 0.16f).absoluteValue * 0.2f
                    val outerGlowSize = 8f * pulseScale
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.15f),
                        radius = outerGlowSize * 2.2f,
                        center = food
                    )
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.4f),
                        radius = outerGlowSize * 1.5f,
                        center = food,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = curSkin[1],
                        radius = 4.5f,
                        center = food
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = food
                    )

                    // Draw Snake layers tail -> head
                    for (i in snakeSegments.indices.reversed()) {
                        val pos = snakeSegments[i]
                        val rPercent = 1.0f - (i.toFloat() / snakeSegments.size.toFloat()) * 0.55f
                        val radius = (7.5f * rPercent).coerceAtLeast(3.0f)

                        // Outer segment ring glow
                        drawCircle(
                            color = curSkin[i % curSkin.size].copy(alpha = 0.10f),
                            radius = radius * 2.5f,
                            center = pos
                        )

                        // Main core segment
                        drawCircle(
                            color = curSkin[i % curSkin.size],
                            radius = radius,
                            center = pos
                        )

                        // Highlight glossy reflection
                        if (i == 0) {
                            drawCircle(
                                color = Color.White,
                                radius = radius * 0.4f,
                                center = Offset(pos.x - radius * 0.2f, pos.y - radius * 0.2f)
                            )
                        }
                    }
                }

                // Cyber HUD panel indicators
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PREVIEW_SIM_SYS_v2.0",
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (targetOverride != null) "MODE: USER_MANUAL_STEER" else "MODE: AUTO_HUNTING_AI",
                            color = if (targetOverride != null) Color(0xFF00FFCC) else Color(0xFF3B82F6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "SNAKE_LEN: ${snakeSegments.size}",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SYS_FPS: 60FPS",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "TAP TO STEER",
                            color = Color(0xFF64748B).copy(alpha = 0.8f),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

// ========== Custom Arcade Helper Components ==========

@Composable
fun GameHeaderHUD(
    username: String,
    level: Int,
    goldPoints: Int,
    gemPoints: Int,
    rank: String,
    xpProgress: Float,
    onEditNameClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMailClick: () -> Unit,
    onAlertClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Profile Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                .clickable { onEditNameClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                // Crown Icon above avatar
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                )
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                        .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Level label badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700))
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level",
                        fontSize = 9.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = username.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RANK: ",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rank,
                        color = Color(0xFFFFC107),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Tiny XP slider
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpProgress)
                            .background(Color(0xFF00FFCC))
                    )
                }
            }
        }

        // Center Arcade Game Logo (Dynamic 3D glowing SNAKE LEGENDS)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "SNAKE",
                    color = Color(0xFFFFD700),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(color = Color(0xFFEA580C), offset = Offset(1.5f, 1.5f), blurRadius = 2f)
                    )
                )
                Text(
                    text = "LEGENDS",
                    color = Color(0xFF22D3EE),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(color = Color(0xFF1D4ED8), offset = Offset(1.5f, 1.5f), blurRadius = 2f)
                    )
                )
            }
        }

        // Right Coin Indicator + Gems + Action suite
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Coins capsules
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format("%,d", goldPoints),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Gems capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Star, // Star representing diamond gem visually
                    contentDescription = null,
                    tint = Color(0xFFF472B6),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$gemPoints",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Command panel
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Icons.Default.Settings to onSettingsClick,
                    Icons.Default.Mail to onMailClick,
                    Icons.Default.Notifications to onAlertClick
                ).forEach { (icon, clickAction) ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            .clickable { clickAction() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialFeedPanel(
    friendsOnline: List<String>,
    onPartyInvitesClick: () -> Unit,
    onRecentNewsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Steel blue panel header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E3A8A).copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOCIAL FEED",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Friends Online: ${friendsOnline.size}",
                color = Color(0xFF22C55E),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Dynamic links
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onPartyInvitesClick() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Party Invites",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onRecentNewsClick() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star, // Star fallback
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recent News",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bulletin lists
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Recent News", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Friend: ${friendsOnline.getOrNull(0) ?: "Alex"} and ${friendsOnline.getOrNull(1) ?: "Mehir"} just entered a Ranked match!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            lineHeight = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "System Update", color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Weekly tournaments are live! Join the Classic Arena event for double XP multipliers!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayNowButtonCard(
    playersCount: Int,
    queueTime: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBtn"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clickable { onClick() }
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFFF9E00), spotColor = Color(0xFFFF9E00)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFEA79), Color(0xFFFF9E00))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star, // standard cute mascot stand-in
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "PLAY NOW",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(2f, 2f), blurRadius = 3f)
                        )
                    )
                    Text(
                        text = "Auto-Queue · Lobby Live ($playersCount players online)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MultiplayerSelectorV2(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, Color(0xFF0284C7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-6).dp)
                    ) {
                        listOf(Color(0xFF00FFCC), Color(0xFFEC4899), Color(0xFFFF9800)).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Circle, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(10.dp))
                            }
                        }
                    }

                    Text(
                        text = "MULTIPLAYER",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(1.5f, 1.5f), blurRadius = 2f)
                        )
                    )
                }

                IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blue capsule sub-mode options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Classic" to "Casual", "Teams" to "Royale", "Rush" to "Rush").forEach { (label, rawMode) ->
                    val isActive = selectedMode == rawMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) Color.White else Color.Black.copy(alpha = 0.25f))
                            .clickable { onModeSelected(rawMode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Color(0xFF0284C7) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoyalPassCard(
    level: Int,
    xpProgress: Float,
    onViewRewardsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0).copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF5B21B6), Color(0xFF311062))
                    )
                )
                .padding(14.dp)
        ) {
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFC107))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ROYAL PASS",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "SEASON 5: SLITHER REIGN",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = "Level $level",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(xpProgress)
                        .background(Color(0xFFE0F2FE))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4C1D95)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star, // Premium Star fallback
                            contentDescription = null,
                            tint = Color(0xFFE9D5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Snake Skin",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"Neon Fury\"",
                            color = Color(0xFFA5B4FC),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // View Pass yellow button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFD97706))
                            )
                        )
                        .clickable { onViewRewardsClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Pass",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveGameBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F172A),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "HOME" to Icons.Default.Home,
                "SNAKES" to Icons.Default.Brush,
                "EVENTS" to Icons.Default.Star,
                "LEADERBOARDS" to Icons.Default.BarChart,
                "SHOP" to Icons.Default.ShoppingCart
            )

            tabs.forEach { (tabId, icon) ->
                val isActive = activeTab == tabId
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) Color(0xFF131F3F) else Color.Transparent)
                        .clickable { onTabSelected(tabId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tabId,
                        tint = if (isActive) Color(0xFF00FFCC) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when(tabId) {
                            "SNAKES" -> "CUSTOMIZE"
                            "EVENTS" -> "TOURNAMENTS"
                            "LEADERBOARDS" -> "GLOBAL"
                            "SHOP" -> "PRODUCTS"
                            else -> tabId
                        },
                        color = if (isActive) Color.White else Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HexGridLiveSnakePreview(
    pointsEaten: Int,
    onPointsEatenChange: (Int) -> Unit,
    skinCycle: Int,
    onSkinCycleChange: (Int) -> Unit
) {
    var width by remember { mutableStateOf(300f) }
    var height by remember { mutableStateOf(200f) }

    val snakeSegments = remember { mutableStateListOf<Offset>() }
    var food by remember { mutableStateOf(Offset(150f, 100f)) }
    val particles = remember { mutableStateListOf<PreviewParticle>() }
    var targetOverride by remember { mutableStateOf<Offset?>(null) }
    var frameTick by remember { mutableStateOf(0) }

    val skinColors = listOf(
        listOf(Color(0xFF00FFCC), Color(0xFF008B8B), Color(0xFFEDFDF9)), // Neon Green
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFFF1F2)), // Cyber Violet/Pink
        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFFFFFDE7))  // Solar Flare
    )
    val curSkin = skinColors[skinCycle % skinColors.size]

    LaunchedEffect(width, height) {
        if (snakeSegments.isEmpty() && width > 0f && height > 0f) {
            val cx = width / 2
            val cy = height / 2
            repeat(14) { i ->
                snakeSegments.add(Offset(cx - i * 10f, cy))
            }
            food = Offset(
                Random.nextFloat() * (width - 40f) + 20f,
                Random.nextFloat() * (height - 40f) + 20f
            )
        }
    }

    LaunchedEffect(Unit) {
        val random = Random(System.currentTimeMillis())
        while (true) {
            delay(16)
            frameTick++

            if (snakeSegments.isEmpty() || width <= 0f || height <= 0f) continue

            val head = snakeSegments.first()
            val target = targetOverride ?: food

            val dx = target.x - head.x
            val dy = target.y - head.y
            val dist = hypot(dx, dy)

            val speed = 3.6f
            var vx = 0f
            var vy = 0f

            if (dist > 2f) {
                val baseVx = (dx / dist) * speed
                val baseVy = (dy / dist) * speed

                val slitherFreq = 0.2f
                val slitherAmp = 1.0f
                val perpX = -baseVy
                val perpY = baseVx
                val slitherOffset = sin(frameTick * slitherFreq) * slitherAmp

                vx = baseVx + (perpX / speed) * slitherOffset
                vy = baseVy + (perpY / speed) * slitherOffset
            } else {
                if (targetOverride != null) {
                    targetOverride = null
                }
            }

            val newHead = Offset(
                (head.x + vx).coerceIn(12f, width - 12f),
                (head.y + vy).coerceIn(12f, height - 12f)
            )
            if (newHead.x == 12f || newHead.x == width - 12f || newHead.y == 12f || newHead.y == height - 12f) {
                targetOverride = null
            }

            val updatedSegments = ArrayList<Offset>(snakeSegments.size)
            updatedSegments.add(newHead)

            var prev = newHead
            val targetDist = 9.0f
            for (i in 1 until snakeSegments.size) {
                val curr = snakeSegments[i]
                val sDx = curr.x - prev.x
                val sDy = curr.y - prev.y
                val sDist = hypot(sDx, sDy)
                if (sDist > targetDist) {
                    val ratio = targetDist / sDist
                    updatedSegments.add(Offset(prev.x + sDx * ratio, prev.y + sDy * ratio))
                } else {
                    updatedSegments.add(curr)
                }
                prev = updatedSegments.last()
            }

            snakeSegments.clear()
            snakeSegments.addAll(updatedSegments)

            val fDist = hypot(food.x - newHead.x, food.y - newHead.y)
            if (fDist < 16f) {
                repeat(16) {
                    val ang = random.nextFloat() * 2f * Math.PI.toFloat()
                    val pSpeed = random.nextFloat() * 4.0f + 1.2f
                    particles.add(
                        PreviewParticle(
                            x = food.x,
                            y = food.y,
                            vx = cos(ang) * pSpeed,
                            vy = sin(ang) * pSpeed,
                            color = curSkin.random(),
                            life = 1f,
                            size = random.nextFloat() * 4.5f + 1.5f
                        )
                    )
                }

                val tail = snakeSegments.lastOrNull() ?: newHead
                snakeSegments.add(tail)

                onPointsEatenChange(pointsEaten + 1)
                if ((pointsEaten + 1) % 3 == 0) {
                    onSkinCycleChange(skinCycle + 1)
                }

                food = Offset(
                    random.nextFloat() * (width - 60f) + 30f,
                    random.nextFloat() * (height - 60f) + 30f
                )
            }

            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vx *= 0.94f
                p.vy *= 0.94f
                p.life -= 0.02f
                if (p.life <= 0f) {
                    iterator.remove()
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
        border = BorderStroke(1.2.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFCC))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SNAKE PREVIEW",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(curSkin[0].copy(alpha = 0.15f))
                        .border(1.dp, curSkin[0].copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (skinCycle % 3) {
                            0 -> "VIPER: NEON VIPER"
                            1 -> "VIPER: CYBER GLOW"
                            else -> "VIPER: SOLAR FLARE"
                        },
                        color = curSkin[0],
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B13))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "hex")
                val gridAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.08f,
                    targetValue = 0.18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alphaGrid"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            width = size.width.toFloat()
                            height = size.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                targetOverride = offset
                                repeat(10) {
                                    val r = Random(System.nanoTime())
                                    val ang = r.nextFloat() * 2f * Math.PI.toFloat()
                                    val spd = r.nextFloat() * 3.5f + 1.2f
                                    particles.add(
                                        PreviewParticle(
                                            x = offset.x,
                                            y = offset.y,
                                            vx = cos(ang) * spd,
                                            vy = sin(ang) * spd,
                                            color = curSkin.random(),
                                            life = 1f,
                                            size = r.nextFloat() * 4f + 2f
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    // Geometric Hexagonal Matrix
                    val hexRadius = 18f
                    val dx = hexRadius * 1.5f
                    val dy = hexRadius * kotlin.math.sqrt(3f)

                    for (i in 0..(size.width / dx).toInt() + 1) {
                        for (j in 0..(size.height / dy).toInt() + 1) {
                            val cx = if (j % 2 == 0) i * dx * 2f else i * dx * 2f + dx
                            val cy = j * dy

                            val hexPath = Path().apply {
                                for (corner in corner_indices) {
                                    val rad = Math.toRadians(corner * 60.0)
                                    val px = cx + hexRadius * cos(rad).toFloat()
                                    val py = cy + hexRadius * sin(rad).toFloat()
                                    if (corner == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(
                                path = hexPath,
                                color = Color(0xFF1E293B).copy(alpha = gridAlpha),
                                style = Stroke(width = 0.8f)
                            )
                        }
                    }

                    targetOverride?.let { tgt ->
                        drawCircle(
                            color = Color(0xFF00FFCC).copy(alpha = 0.35f),
                            radius = 11f + sin(frameTick * 0.15f).absoluteValue * 3f,
                            center = tgt,
                            style = Stroke(width = 1.2f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(tgt.x, 0f),
                            end = Offset(tgt.x, size.height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            start = Offset(0f, tgt.y),
                            end = Offset(size.width, tgt.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    particles.forEach { p ->
                        drawCircle(
                            color = p.color.copy(alpha = p.life),
                            radius = p.size * p.life,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // Food Orb
                    val pulseScale = 1.0f + sin(frameTick * 0.18f).absoluteValue * 0.25f
                    val outerGlow = 7.5f * pulseScale
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.15f),
                        radius = outerGlow * 2.2f,
                        center = food
                    )
                    drawCircle(
                        color = curSkin[0].copy(alpha = 0.4f),
                        radius = outerGlow * 1.5f,
                        center = food,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = curSkin[1],
                        radius = 4.2f,
                        center = food
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.8f,
                        center = food
                    )

                    // Snake layers template
                    for (i in snakeSegments.indices.reversed()) {
                        val pos = snakeSegments[i]
                        val rPercent = 1.0f - (i.toFloat() / snakeSegments.size.toFloat()) * 0.5f
                        val radius = (7.0f * rPercent).coerceAtLeast(3.2f)

                        drawCircle(
                            color = curSkin[i % curSkin.size].copy(alpha = 0.12f),
                            radius = radius * 2.5f,
                            center = pos
                        )

                        drawCircle(
                            color = curSkin[i % curSkin.size],
                            radius = radius,
                            center = pos
                        )

                        if (i == 0) {
                            drawCircle(
                                color = Color.White,
                                radius = radius * 0.45f,
                                center = Offset(pos.x - radius * 0.2f, pos.y - radius * 0.2f)
                            )

                            // Vector gold crown representation!
                            val crownPath = Path().apply {
                                moveTo(pos.x - 7f, pos.y - 6f)
                                lineTo(pos.x - 9f, pos.y - 12f)
                                lineTo(pos.x - 3f, pos.y - 9f)
                                lineTo(pos.x + 0f, pos.y - 15f)
                                lineTo(pos.x + 3f, pos.y - 9f)
                                lineTo(pos.x + 9f, pos.y - 12f)
                                lineTo(pos.x + 7f, pos.y - 6f)
                                close()
                            }
                            drawPath(crownPath, color = Color(0xFFFFD700))
                            drawPath(crownPath, color = Color(0xFFFFC107), style = Stroke(width = 0.8f))
                        }
                    }
                }

                // Interactive dashboards overlay text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PREVIEW_SYSTEM: v3.2",
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VIPER",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "SNAKE_LENGTH: ${snakeSegments.size}",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SYS_FPS: 60FPS · STABLE",
                                color = Color(0xFF22C55E),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "TAP TO STEER VIPER",
                            color = Color(0xFF64748B).copy(alpha = 0.8f),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

private val corner_indices = listOf(0, 1, 2, 3, 4, 5)