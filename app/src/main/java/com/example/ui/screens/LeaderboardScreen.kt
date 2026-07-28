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

    // Mock high score leaders
    val worldLeaders = listOf(
        Pair("KryptonSlayer (CYBER)", 8400),
        Pair("OuroborosKing (VIPER)", 7200),
        Pair("AlphaConstrictor", 6100),
        Pair("NeonTitan (APEX)", 4950),
        Pair("CobaltFangs", 3800),
        Pair("StealthGlitch", 2900),
        Pair("ViperGlow", 1850)
    )

    // Merge User profile dynamic high score
    val fullLeaders = remember(userProfile?.highestScore) {
        val userScore = userProfile?.highestScore ?: 0
        val userName = "${userProfile?.username ?: "You"} [YOU]"
        val list = worldLeaders.toMutableList()
        list.add(Pair(userName, userScore))
        list.sortByDescending { it.second }
        list
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
            // High and clean Stealth Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color(0xFF0C0E17), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) Color(0xFF1E293B) else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color(0xFFFFFF33) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "MILESTONES",
                            color = if (selectedTab == 0) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) Color(0xFF1E293B) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color(0xFF00FFCC) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "WORLD RANKS",
                            color = if (selectedTab == 1) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tab Content Display Space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (selectedTab == 0) {
                    // Milestones List
                    AchievementsTab(achievements)
                } else {
                    // World Leaderboards ranks list
                    LeaderboardsTab(fullLeaders)
                }
            }
        }
    }
}

@Composable
fun AchievementsTab(achievements: List<Achievement>) {
    if (achievements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00FFCC))
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(achievements.size) { index ->
                val ach = achievements[index]
                val progRatio = (ach.currentValue.toFloat() / ach.targetValue.toFloat()).coerceIn(0f, 1f)

                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (ach.completed) Color(0x3300FFCC) else Color(0x11FFFFFF),
                    backgroundColor = if (ach.completed) Color(0x0C00FFCC) else Color(0x09FFFFFF)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    color = if (ach.completed) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ach.description,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            // Show tick completed indicator
                            if (ach.completed) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FFCC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = "Coins Reward", tint = Color(0xFFFFFF33), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "+${ach.rewardCoins}",
                                        color = Color(0xFFFFFF33),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress slider visual
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progRatio },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (ach.completed) Color(0xFF00FFCC) else Color(0xFF06B6D4),
                                trackColor = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${ach.currentValue}/${ach.targetValue}",
                                color = Color.LightGray,
                                fontSize = 10.sp,
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
fun LeaderboardsTab(leaders: List<Pair<String, Int>>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(leaders.size) { index ->
            val ranking = leaders[index]
            val place = index + 1
            val isUser = ranking.first.contains("[YOU]")

            val placeColor = when (place) {
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
                        // Place indicator bubble
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(placeColor.copy(alpha = 0.15f))
                                .border(1.dp, placeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = place.toString(),
                                color = if (place <= 3) placeColor else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = if (isUser) ranking.first.replace(" [YOU]", "") else ranking.first,
                            color = if (isUser) Color(0xFF00FFCC) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }

                    // Score Length indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ranking.second}",
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
    }
}
