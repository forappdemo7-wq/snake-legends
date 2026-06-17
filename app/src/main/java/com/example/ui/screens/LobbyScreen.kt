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

    Scaffold(
        containerColor = Background,
        bottomBar = {
            // Sticky START MATCH button V2
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Background,
                shadowElevation = 16.dp
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(80.dp)
                        .scale(pulseScale)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Primary, spotColor = Primary)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Primary, Secondary)
                            )
                        )
                        .clickable {
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
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "START MATCH",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Players Online: $playersOnlineCount · Queue ~${queueTimeSec}s",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. PROFILE HEADER V2
            item(key = "profile_header") {
                ProfileHeaderV2(
                    userProfile = userProfile,
                    rankTier = rankTier,
                    xpProgress = xpProgress,
                    onEditNameClick = {
                        editedName = userProfile?.username ?: ""
                        showEditNameDialog = true
                    }
                )
            }

            // 2. HERO SEASON BANNER
            item(key = "hero_banner") {
                HeroSeasonBanner(
                    daysLeft = daysLeft,
                    onViewRewards = { /* navigate to battle pass or rewards */ }
                )
            }

            // 3. FEATURED EVENT CARD
            item(key = "featured_event") {
                FeaturedEventCard(onJoinEvent = { /* Join event logic */ })
            }

            // 4. GAME MODES
            item(key = "play_now_title") {
                SectionTitle("PLAY NOW")
            }

            item(key = "game_modes") {
                GameModeRow(
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it }
                )
            }

            item(key = "arena_themes") {
                ArenaThemeRow(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it }
                )
            }

            item(key = "tactical_class") {
                TacticalClassRow(
                    selectedClass = selectedClass,
                    onClassSelected = { viewModel.selectedAbility.value = it }
                )
            }

            // Multiplayer settings trigger
            item(key = "multiplayer_settings") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showMultiplayerSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Network Settings",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Network Settings", color = TextGray, fontSize = 14.sp)
                    }
                }
            }

            if (selectedMode == "Private Room") {
                item(key = "private_room") {
                    PrivateRoomQuickCard(
                        privateRoomCode = privateRoomCode,
                        mpStatus = mpStatus,
                        onOpenRoom = { showPrivateRoomDialog = true },
                        onGenerateCode = {
                            val prefixes = listOf("SNAKE", "CYBER", "VIPER", "COBRA", "ARENA", "NEON", "KODEX", "SLITHR")
                            privateRoomCode = "${prefixes.random()}-${(100..999).random()}"
                        }
                    )
                }
            }

            // 5. DAILY LOGIN REWARD
            item(key = "daily_reward") {
                DailyLoginReward(
                    day = dailyRewardDay,
                    claimed = dailyRewardClaimed,
                    onClaim = {
                        if (!dailyRewardClaimed) {
                            viewModel.earnFreeCoins(300)
                            dailyRewardClaimed = true
                        }
                    }
                )
            }

            // 6. FRIENDS ONLINE WIDGET
            item(key = "friends_online") {
                FriendsOnlineWidget(
                    friends = friendsOnline,
                    onInvite = { friendName -> /* invite friend to game */ }
                )
            }

            // 7. BATTLE PASS V2
            item(key = "battle_pass_v2") {
                BattlePassV2(
                    bpLevel = ((userProfile?.level ?: 1) - 1).coerceAtLeast(1),
                    onClaimReward = { tierNum, rewardDesc ->
                        when (tierNum) {
                            1 -> viewModel.earnFreeCoins(200)
                            2 -> viewModel.buyCosmetic("Glow Cyber", "Skin", 0, {}, {})
                            3 -> viewModel.earnFreeCoins(600)
                            4 -> viewModel.buyCosmetic("Space Wraith", "Skin", 0, {}, {})
                            5 -> viewModel.buyCosmetic("Meteor Trail", "Trail", 0, {}, {})
                        }
                    }
                )
            }

            // 8. MISSIONS (daily/weekly) - kept from original but redesigned cards
            item(key = "missions_title") {
                SectionTitle("MISSIONS")
            }
            item(key = "daily_missions") {
                MissionList(
                    title = "Daily Missions",
                    missions = listOf(
                        "Inhale 12 Super Orbs" to true,
                        "Activate Ability 5 times" to false,
                        "Play 1 Ranked match" to true
                    )
                )
            }
            item(key = "weekly_missions") {
                MissionList(
                    title = "Weekly Missions",
                    missions = listOf(
                        "Earn 1500 XP" to false,
                        "Defeat 5 opponents in Royale" to true,
                        "Reach Silver rank" to false
                    )
                )
            }

            // 9. SOCIAL REDESIGN (Row layout)
            item(key = "social_title") {
                SectionTitle("SOCIAL")
            }
            item(key = "social_grid") {
                SocialRedesign(
                    onShop = onNavigateToShop,
                    onClan = onNavigateToClans,
                    onLeaderboard = onNavigateToLeaderboard
                )
            }

            // 10. MATCH HISTORY V2
            item(key = "history_title") {
                SectionTitle("MATCH HISTORY")
            }
            item(key = "match_history") {
                MatchHistoryV2(matchRecords = matchRecords)
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
    val participants = mpManager.activeParticipants
    val chatMessages = mpManager.chatMessages
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