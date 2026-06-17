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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.roundToInt
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

// Extensions for days/hours
private val Int.days: Long get() = this * 24 * 60 * 60 * 1000L
private val Int.hours: Long get() = this * 60 * 60 * 1000L

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

    // Countdown for season end (12 days from now)
    val seasonEndDate = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 12) }.time }
    var daysLeft by remember { mutableStateOf(12L) }
    LaunchedEffect(seasonEndDate) {
        while (true) {
            daysLeft = ((seasonEndDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            delay(3600000) // update every hour
        }
    }

    val bpLevel = ((userProfile?.level ?: 1) - 1).coerceAtLeast(1)
    val bpProgress = (bpLevel / 50f).coerceIn(0f, 1f)

    val liveActivities = listOf(
        "CyberAce reached Diamond Rank" to (Color(0xFFE040FB) to Icons.Default.Star),
        "Cyber Tournament in 2h" to (PremiumGold to Icons.Default.EmojiEvents),
        "Double XP Weekend Active!" to (Success to Icons.Default.Bolt),
        "Clan Wars Open" to (EpicPurple to Icons.Default.Groups)
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            // Sticky START MATCH CTA V3
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Background,
                shadowElevation = 20.dp
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulseShimmer")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                val shimmerOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(88.dp)
                        .scale(pulseScale)
                        .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Primary, spotColor = Primary)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Primary, Secondary),
                                startX = shimmerOffset * 0.5f,
                                endX = 1f + shimmerOffset * 0.5f
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
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "START MATCH",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$playersOnlineCount ONLINE  ·  Queue ~${queueTimeSec}s",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Profile Header V3
            item(key = "profile_header") {
                ProfileHeaderV3(
                    userProfile = userProfile,
                    rankTier = rankTier,
                    xpProgress = xpProgress,
                    onEditNameClick = {
                        editedName = userProfile?.username ?: ""
                        showEditNameDialog = true
                    }
                )
            }

            // 2. Hero Season Banner V3
            item(key = "hero_season") {
                HeroSeasonBannerV3(
                    daysLeft = daysLeft,
                    bpProgress = bpProgress,
                    bpLevel = bpLevel,
                    onViewRewards = { /* navigate to battle pass or rewards */ }
                )
            }

            // 3. Live Activity Strip
            item(key = "live_activity") {
                LiveActivityStrip(activities = liveActivities)
            }

            // 4. Featured Event
            item(key = "featured_event") {
                FeaturedEventCard(onJoinEvent = { /* Join event logic */ })
            }

            // 5. Daily Login Reward
            item(key = "daily_reward") {
                DailyLoginRewardV3(
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

            // 6. Play Modes
            item(key = "play_now_title") { SectionTitle("PLAY NOW") }
            item(key = "game_modes") {
                GameModeRow(selectedMode = selectedMode, onModeSelected = { selectedMode = it })
            }
            item(key = "arena_themes") {
                ArenaThemeRow(selectedTheme = selectedTheme, onThemeSelected = { selectedTheme = it })
            }
            item(key = "tactical_class") {
                TacticalClassRow(selectedClass = selectedClass, onClassSelected = { viewModel.selectedAbility.value = it })
            }

            item(key = "multiplayer_settings") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showMultiplayerSettings = true }) {
                        Icon(Icons.Default.Settings, null, tint = TextGray, modifier = Modifier.size(18.dp))
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

            // 7. Battle Pass V2
            item(key = "battle_pass_v2") {
                BattlePassV2(
                    bpLevel = bpLevel,
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

            // 8. Missions
            item(key = "missions_title") { SectionTitle("MISSIONS") }
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

            // 9. Social Redesign
            item(key = "social_title") { SectionTitle("SOCIAL") }
            item(key = "social_grid") {
                SocialRedesign(onShop = onNavigateToShop, onClan = onNavigateToClans, onLeaderboard = onNavigateToLeaderboard)
            }

            // 10. Friends Online Widget
            item(key = "friends_online") {
                FriendsOnlineWidget(friends = friendsOnline, onInvite = {})
            }

            // 11. Match History V3
            item(key = "history_title") { SectionTitle("MATCH HISTORY") }
            item(key = "match_history") {
                MatchHistoryV3(matchRecords = matchRecords)
            }
        }
    }

    // Dialogs
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
        MultiplayerSettingsSheet(mpManager = mpManager, onDismiss = { showMultiplayerSettings = false })
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

// ========== Profile Header V3 ==========
@Composable
fun ProfileHeaderV3(
    userProfile: UserProfile?,
    rankTier: String,
    xpProgress: Float,
    onEditNameClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = xpProgress, animationSpec = tween(600), label = "xpProgress")
    val rankColor = getRankColor(rankTier)
    val avatarGlow = rememberInfiniteTransition(label = "avatarGlow")
    val glowAlpha by avatarGlow.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glowAlpha")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated avatar frame
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    drawCircle(
                        color = rankColor.copy(alpha = glowAlpha),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 4f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Secondary)))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(rankColor)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getRankIcon(rankTier), "Rank", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userProfile?.username ?: "Player",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEditNameClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = "$rankTier · Level ${userProfile?.level ?: 1}",
                    color = rankColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Gold, modifier = Modifier.size(22.dp))
                    Text(
                        text = "${userProfile?.coins ?: 0}",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, null, tint = PremiumGold, modifier = Modifier.size(20.dp))
                    Text(
                        text = "${(userProfile?.coins ?: 0) / 20}",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== Hero Season Banner V3 ==========
@Composable
fun HeroSeasonBannerV3(
    daysLeft: Long,
    bpProgress: Float,
    bpLevel: Int,
    onViewRewards: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = bpProgress, animationSpec = tween(800), label = "bpProgress")
    val percent = (animatedProgress * 100).roundToInt()

    val particleTransition = rememberInfiniteTransition(label = "particles")
    val particles = remember {
        List(12) { Random.nextFloat() * 2 - 1f to Random.nextFloat() * 2 - 1f }
    }
    val animatedParticles = particles.map { (startX, startY) ->
        val x = particleTransition.animateFloat(
            initialValue = startX,
            targetValue = startX + 0.2f,
            animationSpec = infiniteRepeatable(tween((2000..4000).random()), RepeatMode.Reverse),
            label = "px"
        )
        val y = particleTransition.animateFloat(
            initialValue = startY,
            targetValue = startY - 0.15f,
            animationSpec = infiniteRepeatable(tween((2500..3500).random()), RepeatMode.Reverse),
            label = "py"
        )
        x to y
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D47A1), Color(0xFF1A237E), Color(0xFF000051))
                )
            )
    ) {
        // Cyber snake silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.8f)
                cubicTo(
                    size.width * 0.6f, size.height * 0.1f,
                    size.width * 0.8f, size.height * 0.5f,
                    size.width * 0.9f, size.height * 0.3f
                )
            }
            drawPath(
                path = path,
                color = Primary.copy(alpha = 0.2f),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            drawCircle(Color.White.copy(alpha = 0.4f), 6f, center = Offset(size.width * 0.3f, size.height * 0.7f))
            drawCircle(Color.Cyan.copy(alpha = 0.5f), 4f, center = Offset(size.width * 0.55f, size.height * 0.4f))
            drawCircle(Color.Magenta.copy(alpha = 0.4f), 5f, center = Offset(size.width * 0.75f, size.height * 0.25f))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            animatedParticles.forEachIndexed { index, (xState, yState) ->
                val x = size.width * (0.1f + 0.8f * (xState.value + 1f) / 2f)
                val y = size.height * (0.2f + 0.6f * (yState.value + 1f) / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = 3f + (index % 3) * 1.5f,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "SEASON 7",
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "CYBER REBIRTH",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Battle Pass Progress: $percent%",
                    color = TextGray,
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Secondary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$daysLeft Days Remaining", color = TextGray, fontSize = 12.sp)
                    Text("Next: Legendary Neon Skin", color = LegendaryOrange, fontSize = 12.sp)
                }
            }

            Button(
                onClick = onViewRewards,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("VIEW REWARDS", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========== Live Activity Strip ==========
@Composable
fun LiveActivityStrip(activities: List<Pair<String, Pair<Color, ImageVector>>>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(activities) { (text, colors) ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInHorizontally(tween(400))
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Surface,
                    border = BorderStroke(1.dp, colors.first.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(colors.second, null, tint = colors.first, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text, color = TextWhite, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ========== Featured Event Card ==========
@Composable
fun FeaturedEventCard(onJoinEvent: () -> Unit) {
    val endTime = remember { System.currentTimeMillis() + 2.days + 14.hours }
    var timeLeft by remember { mutableStateOf(endTime - System.currentTimeMillis()) }
    LaunchedEffect(endTime) {
        while (timeLeft > 0) {
            timeLeft = endTime - System.currentTimeMillis()
            delay(1000)
        }
    }
    val hours = (timeLeft / 3600000) % 24
    val minutes = (timeLeft / 60000) % 60
    val seconds = (timeLeft / 1000) % 60

    val borderAnimation = rememberInfiniteTransition(label = "eventBorder")
    val borderAlpha by borderAnimation.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "alpha")
    val borderColor = Color(0xFFFFD700).copy(alpha = borderAlpha)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFF1A1A2E))))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Gold.copy(alpha = 0.3f),
                            radius = size.minDimension / 2f,
                            center = center
                        )
                    }
                    Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("FEATURED EVENT", color = TextGray, fontSize = 12.sp, letterSpacing = 1.sp)
                    Text("Cyber Championship", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Prize: 50,000 Coins", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ends in ${"%02d".format(hours)}:${"%02d".format(minutes)}:${"%02d".format(seconds)}",
                        color = Danger,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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

// ========== Daily Login Reward V3 ==========
@Composable
fun DailyLoginRewardV3(day: Int, claimed: Boolean, onClaim: () -> Unit) {
    val chestScale by animateFloatAsState(
        targetValue = if (!claimed) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "chestAnim"
    )
    val totalDays = 7
    val streakProgress = day / totalDays.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(chestScale)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Gold, PremiumGold))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("DAILY REWARD", color = TextGray, fontSize = 12.sp, letterSpacing = 1.sp)
                    Text("Day $day of $totalDays", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("300 Coins", color = Gold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { streakProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Secondary,
                        trackColor = Color.White.copy(alpha = 0.1f)
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
}

// ========== Game Mode Row (V2) ==========
@Composable
fun GameModeRow(selectedMode: String, onModeSelected: (String) -> Unit) {
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
fun ArenaThemeRow(selectedTheme: ArenaTheme, onThemeSelected: (ArenaTheme) -> Unit) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Landscape,
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
                Text("Night · Urban", color = TextLight, fontSize = 10.sp)
            }
        }
    }
}

// ========== Tactical Class Redesign ==========
@Composable
fun TacticalClassRow(selectedClass: String, onClassSelected: (String) -> Unit) {
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

data class ClassInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val cooldown: Int
)

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

// ========== Battle Pass V2 ==========
@Composable
fun BattlePassV2(bpLevel: Int, onClaimReward: (Int, String) -> Unit) {
    val tiers = listOf(
        "200 Coins" to false,
        "Glow Cyber (Skin)" to true,
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                onClaimReward(index + 1, reward)
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
            modifier = Modifier.fillMaxSize().padding(10.dp),
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
                Icon(Icons.Default.Diamond, "Premium", tint = PremiumGold, modifier = Modifier.size(16.dp))
                Text("PREMIUM", color = PremiumGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (unlocked && !claimed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("CLAIM", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========== Mission List ==========
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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

// ========== Social Redesign ==========
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
                SocialPremiumCard(icon = Icons.Default.Store, title = "Shop", subtitle = "Gear Up", onClick = onShop)
            }
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(icon = Icons.Default.Groups, title = "Clan", subtitle = "2 Online", onClick = onClan)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(icon = Icons.Default.Leaderboard, title = "Leaderboard", subtitle = "Top 100", onClick = onLeaderboard)
            }
            Box(modifier = Modifier.weight(1f)) {
                SocialPremiumCard(icon = Icons.Default.CardGiftcard, title = "Rewards", subtitle = "5 New", onClick = { /* Rewards screen */ })
            }
        }
    }
}

@Composable
fun SocialPremiumCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

// ========== Friends Online Widget ==========
@Composable
fun FriendsOnlineWidget(friends: List<String>, onInvite: (String) -> Unit) {
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
                Text("ONLINE FRIENDS", color = TextGray, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("${friends.size} Online", color = Success, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

// ========== Match History V3 ==========
@Composable
fun MatchHistoryV3(matchRecords: List<MatchRecord>) {
    if (matchRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
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
                MatchCardV3(record)
            }
        }
    }
}

@Composable
fun MatchCardV3(record: MatchRecord) {
    val won = record.score > 0
    val backgroundColor = if (won) Success.copy(alpha = 0.05f) else Danger.copy(alpha = 0.05f)
    val relativeTime = getRelativeTime(record.timestamp)

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
                null,
                tint = if (won) Success else Danger,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${if (won) "🏆 Victory" else "💔 Defeat"} · ${record.mode}",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(relativeTime, color = TextGray, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Score ${record.score}", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("+${record.xpEarned} XP", color = Primary, fontSize = 12.sp)
                    Text("+${record.coinsEarned} coins", color = Gold, fontSize = 12.sp)
                }
                Text("Rank Unchanged", color = TextLight, fontSize = 11.sp)
            }
        }
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

// ========== Private Room Quick Card ==========
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

// ========== Dialogs (unchanged from previous) ==========
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
        title = { Text("Private Room Lobby", color = TextWhite, fontWeight = FontWeight.Bold) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerSettingsSheet(mpManager: MultiplayerManager, onDismiss: () -> Unit) {
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
                Text("Enter a new display name (max 15 characters)", color = TextGray, fontSize = 14.sp)
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary)
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