package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GameViewModel

@Composable
fun RoyalPassDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val unlockedCosmetics by viewModel.unlockedCosmetics.collectAsStateWithLifecycle()

    val currentLevel = userProfile?.level ?: 1
    val prefs = remember { context.getSharedPreferences("royal_pass_prefs", Context.MODE_PRIVATE) }
    
    var isEliteActive by remember { mutableStateOf(prefs.getBoolean("is_elite_active", false)) }
    var claimedStatusMap by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    LaunchedEffect(Unit) {
        val newMap = mutableMapOf<Int, Boolean>()
        listOf(1, 5, 10, 15, 20, 25, 30).forEach { lvl ->
            newMap[lvl] = prefs.getBoolean("claimed_level_$lvl", false)
        }
        claimedStatusMap = newMap
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

    data class PassReward(
        val levelRequired: Int,
        val isPremium: Boolean,
        val displayName: String,
        val icon: ImageVector,
        val tint: Color,
        val claimAction: () -> Unit,
        val rewardDescription: String,
        val checkOwned: () -> Boolean
    )

    val rewards = remember(unlockedCosmetics, claimedStatusMap) {
        listOf(
            PassReward(
                levelRequired = 1,
                isPremium = false,
                displayName = "150 Coins Starter Pack",
                icon = Icons.Default.MonetizationOn,
                tint = Gold,
                rewardDescription = "Free startup coins for customization option",
                checkOwned = { claimedStatusMap[1] == true },
                claimAction = {
                    viewModel.earnFreeCoins(150)
                    prefs.edit().putBoolean("claimed_level_1", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(1, true) }
                }
            ),
            PassReward(
                levelRequired = 5,
                isPremium = true,
                displayName = "Volcanic Lava Skin",
                icon = Icons.Default.Brush,
                tint = LegendaryOrange,
                rewardDescription = "Molten lava scaling matrix plating tier",
                checkOwned = { unlockedCosmetics.any { it.name == "Volcanic Lava" } },
                claimAction = {
                    viewModel.unlockCosmeticDirect("Volcanic Lava", "skin")
                    prefs.edit().putBoolean("claimed_level_5", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(5, true) }
                }
            ),
            PassReward(
                levelRequired = 10,
                isPremium = false,
                displayName = "300 Coins Booster",
                icon = Icons.Default.MonetizationOn,
                tint = Gold,
                rewardDescription = "Double multiplier bonus currency pack",
                checkOwned = { claimedStatusMap[10] == true },
                claimAction = {
                    viewModel.earnFreeCoins(300)
                    prefs.edit().putBoolean("claimed_level_10", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(10, true) }
                }
            ),
            PassReward(
                levelRequired = 15,
                isPremium = true,
                displayName = "Phantom Ghost Skin",
                icon = Icons.Default.Brush,
                tint = RareBlue,
                rewardDescription = "Spectral glowing scale plating matrix tier",
                checkOwned = { unlockedCosmetics.any { it.name == "Phantom Ghost" } },
                claimAction = {
                    viewModel.unlockCosmeticDirect("Phantom Ghost", "skin")
                    prefs.edit().putBoolean("claimed_level_15", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(15, true) }
                }
            ),
            PassReward(
                levelRequired = 20,
                isPremium = false,
                displayName = "500 Coins Super Package",
                icon = Icons.Default.MonetizationOn,
                tint = Gold,
                rewardDescription = "Massive payload premium currency payout",
                checkOwned = { claimedStatusMap[20] == true },
                claimAction = {
                    viewModel.earnFreeCoins(500)
                    prefs.edit().putBoolean("claimed_level_20", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(20, true) }
                }
            ),
            PassReward(
                levelRequired = 25,
                isPremium = true,
                displayName = "Galactic Cosmic Skin",
                icon = Icons.Default.Brush,
                tint = EpicPurple,
                rewardDescription = "Nebulous quantum stardust scale cosmos cosmic skin",
                checkOwned = { unlockedCosmetics.any { it.name == "Galactic Cosmic" } },
                claimAction = {
                    viewModel.unlockCosmeticDirect("Galactic Cosmic", "skin")
                    prefs.edit().putBoolean("claimed_level_25", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(25, true) }
                }
            ),
            PassReward(
                levelRequired = 30,
                isPremium = true,
                displayName = "Stealth Cyber Skin",
                icon = Icons.Default.Brush,
                tint = Success,
                rewardDescription = "Quiet hunt low-observability dark matrix casing option",
                checkOwned = { unlockedCosmetics.any { it.name == "Stealth Cyber" } },
                claimAction = {
                    viewModel.unlockCosmeticDirect("Stealth Cyber", "skin")
                    prefs.edit().putBoolean("claimed_level_30", true).apply()
                    claimedStatusMap = claimedStatusMap.toMutableMap().apply { put(30, true) }
                }
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0B1E),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth(if (isLandscape) 0.88f else 0.95f)
            .padding(vertical = if (isLandscape) 6.dp else 12.dp)
            .border(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ROYAL PASS REWARDS",
                        color = Color(0xFFFFD700),
                        fontSize = if (isLandscape) 16.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            shadow = Shadow(color = Color(0xFF7C3AED), offset = Offset(1f, 1f), blurRadius = 3f)
                        )
                    )
                    Text(
                        text = "Season 5: Slither Reign • Level $currentLevel",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = if (isLandscape) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1530))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Royal Pass",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEliteActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF7E22CE), Color(0xFFD97706))
                                )
                            )
                            .clickable {
                                isEliteActive = true
                                prefs.edit().putBoolean("is_elite_active", true).apply()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UPGRADE TO ELITE PREMIUM",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Instantly unlock premium skins & benefits!",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "FREE ACTIVATE",
                                    color = Color(0xFF7E22CE),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1F22C55E))
                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                            Text(
                                "ELITE ROYAL PASS ACTIVATED • RETRO BENEFITS INJECTED",
                                color = Color(0xFF22C55E),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 140.dp else 280.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rewards) { reward ->
                            val isClaimed = reward.checkOwned()
                            val isLevelMet = currentLevel >= reward.levelRequired
                            val isPremiumLocked = reward.isPremium && !isEliteActive

                            val itemBackground = if (reward.isPremium) {
                                if (isEliteActive) Color(0xFF251642) else Color(0x40251642)
                            } else {
                                Color(0xFF130E26)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBackground)
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isClaimed -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                            reward.isPremium -> Color(0xFFD97706).copy(alpha = 0.3f)
                                            else -> Color(0xFF7C3AED).copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isLevelMet) Color(0xFF7C3AED) else Color(0xFF251A40)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "L${reward.levelRequired}",
                                            color = if (isLevelMet) Color.White else Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = reward.displayName,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (reward.isPremium) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFFF9E00).copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "ELITE",
                                                        color = Color(0xFFFF9E00),
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = reward.rewardDescription,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                when {
                                    isClaimed -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1F22C55E))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "CLAIMED",
                                                color = Color(0xFF22C55E),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    isPremiumLocked -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1F94A3B8))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Default.Lock, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(8.dp))
                                                Text(
                                                    text = "ELITE REQ",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    !isLevelMet -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF221A3B))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "LOCKED",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { reward.claimAction() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text(
                                                text = "CLAIM",
                                                color = Color.Black,
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
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(
                    text = "CLOSE INTERFACES",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    )
}
