package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Achievement
import com.example.game.GameViewModel
import com.example.ui.components.GlassmorphicCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Achievements, 1 = Leaderboards

    // Mock high score leaders – in production, fetch from repository
    val worldLeaders = listOf(
        "KryptonSlayer (CYBER)" to 8400,
        "OuroborosKing (VIPER)" to 7200,
        "AlphaConstrictor" to 6100,
        "NeonTitan (APEX)" to 4950,
        "CobaltFangs" to 3800,
        "StealthGlitch" to 2900,
        "ViperGlow" to 1850
    )

    // Merge user's high score and sort
    val fullLeaders = remember(userProfile?.highestScore) {
        val userScore = userProfile?.highestScore ?: 0
        val userName = "${userProfile?.username ?: "You"} [YOU]"
        val list = worldLeaders.toMutableList()
        list.add(userName to userScore)
        list.sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "HALL OF LEGENDS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0C101F),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF030712)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF030712))
        ) {
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color(0xFF0C0E17), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(
                    selected = selectedTab == 0,
                    icon = Icons.Default.Star,
                    label = "MILESTONES",
                    iconTint = if (selectedTab == 0) Color(0xFFFFFF33) else Color.Gray,
                    textColor = if (selectedTab == 0) Color.White else Color.Gray,
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    selected = selectedTab == 1,
                    icon = Icons.Default.Leaderboard,
                    label = "WORLD RANKS",
                    iconTint = if (selectedTab == 1) Color(0xFF00FFCC) else Color.Gray,
                    textColor = if (selectedTab == 1) Color.White else Color.Gray,
                    onClick = { selectedTab = 1 }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> AchievementsTab(achievements = achievements)
                    else -> LeaderboardsTab(leaders = fullLeaders)
                }
            }
        }
    }
}

// ---------- Tab Button Component ----------
@Composable
private fun RowScope.TabButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)   // Now correctly references RowScope.weight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF1E293B) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Achievements Tab ----------
@Composable
fun AchievementsTab(achievements: List<Achievement>) {
    if (achievements.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00FFCC))
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(
                count = achievements.size,
                key = { index -> achievements[index].id }
            ) { index ->
                AchievementItem(achievement = achievements[index])
            }
        }
    }
}

// ---------- Achievement Item ----------
@Composable
private fun AchievementItem(achievement: Achievement) {
    val progressRatio = (achievement.currentValue.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (achievement.completed) Color(0x3300FFCC) else Color(0x11FFFFFF),
        backgroundColor = if (achievement.completed) Color(0x0C00FFCC) else Color(0x09FFFFFF)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title,
                        color = if (achievement.completed) Color(0xFF00FFCC) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = achievement.description,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                // Status badge
                if (achievement.completed) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFCC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = "Coins Reward",
                            tint = Color(0xFFFFFF33),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+${achievement.rewardCoins}",
                            color = Color(0xFFFFFF33),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (achievement.completed) Color(0xFF00FFCC) else Color(0xFF06B6D4),
                    trackColor = Color(0xFF1E293B)
                )
                Text(
                    text = "${achievement.currentValue}/${achievement.targetValue}",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------- Leaderboards Tab ----------
@Composable
fun LeaderboardsTab(leaders: List<Pair<String, Int>>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(
            count = leaders.size,
            key = { index -> leaders[index].first }
        ) { index ->
            LeaderboardItem(
                name = leaders[index].first,
                score = leaders[index].second,
                rank = index + 1
            )
        }
    }
}

// ---------- Leaderboard Item ----------
@Composable
private fun LeaderboardItem(name: String, score: Int, rank: Int) {
    val isUser = name.contains("[YOU]")
    val placeColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isUser) Color(0x2200FFCC) else Color(0x06FFFFFF))
            .border(
                1.dp,
                if (isUser) Color(0xFF00FFCC) else Color(0x08FFFFFF),
                RoundedCornerShape(12.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(placeColor.copy(alpha = 0.15f))
                        .border(1.dp, placeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        color = if (rank <= 3) placeColor else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = if (isUser) name.replace(" [YOU]", "") else name,
                    color = if (isUser) Color(0xFF00FFCC) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Bold
                )
            }

            // Score
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = score.toString(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "LGT",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}