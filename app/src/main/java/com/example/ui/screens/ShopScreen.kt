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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GameViewModel
import com.example.ui.components.GlassmorphicCard
import kotlinx.coroutines.launch

// ---------- Data Class ----------
data class ShopItem(
    val name: String,
    val type: String, // "skin"
    val description: String,
    val price: Int,
    val primaryColor: Color,
    val secondaryColor: Color
)

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val unlockedList by viewModel.unlockedCosmetics.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Store a pending message to show in snackbar
    var pendingMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for successful unlock
    var unlockedItemName by remember { mutableStateOf<String?>(null) }

    // Shop items
    val shopSkins = remember {
        listOf(
            ShopItem("Neon Cyber", "skin", "Futuristic standard laser-carved casing with cyan-blue matrix trails", 0, Color(0xFF00FFCC), Color(0xFF0099FF)),
            ShopItem("Volcanic Lava", "skin", "Chalcogenide magma plate casing that drips volatile glowing slag", 200, Color(0xFFFF3300), Color(0xFFFFBB00)),
            ShopItem("Phantom Ghost", "skin", "Translucent poltergeist scales that float between spectral dimensions", 350, Color(0xFFD4E6F1), Color(0xFF90A4AE)),
            ShopItem("Galactic Cosmic", "skin", "Pulsing dense quantum stardust drawn from collapsing stellar rifts", 500, Color(0xFF9933FF), Color(0xFFFF5252)),
            ShopItem("Stealth Cyber", "skin", "Low-observability dark titanium plates optimized for quiet hunting", 600, Color(0xFF00E676), Color(0xFF37474F))
        )
    }

    // Set of unlocked names for fast lookup
    val unlockedNames by remember(unlockedList) {
        derivedStateOf { unlockedList.map { it.name }.toSet() }
    }

    // Show snackbar when pendingMessage changes
    LaunchedEffect(pendingMessage) {
        pendingMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            pendingMessage = null
        }
    }

    // DisposableEffect to clear any pending dialogs on leave (optional)
    DisposableEffect(Unit) {
        onDispose {
            // Clean up if needed
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "LEGENDS SHOP",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("shop_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFFF33), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${userProfile?.coins ?: 0}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0C101F),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF030712),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF030712))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Promotional banner
                item {
                    PromotionalBanner()
                }

                item {
                    Text(
                        text = "CARBON CASING SKINS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                // Items with stable keys
                items(
                    count = shopSkins.size,
                    key = { index -> shopSkins[index].name } // Use name as unique key
                ) { index ->
                    val skin = shopSkins[index]
                    val isUnlocked = unlockedNames.contains(skin.name) || skin.price == 0
                    val isCurrentlyEquipped = userProfile?.currentSkin == skin.name

                    ShopItemCard(
                        skin = skin,
                        isUnlocked = isUnlocked,
                        isCurrentlyEquipped = isCurrentlyEquipped,
                        onEquip = { viewModel.selectCosmetic(skin.name, "skin") },
                        onBuy = {
                            viewModel.buyCosmetic(
                                name = skin.name,
                                type = "skin",
                                price = skin.price,
                                onSuccess = {
                                    unlockedItemName = skin.name
                                    viewModel.selectCosmetic(skin.name, "skin")
                                },
                                onError = { err ->
                                    pendingMessage = err
                                }
                            )
                        }
                    )
                }
            }

            // Unlocked Dialog
            if (unlockedItemName != null) {
                AlertDialog(
                    onDismissRequest = { unlockedItemName = null },
                    confirmButton = {
                        TextButton(onClick = { unlockedItemName = null }) {
                            Text("EQUIP NOW", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                        }
                    },
                    title = { Text("COSMETIC UNLOCKED!", color = Color.White, fontWeight = FontWeight.Black) },
                    text = {
                        Text(
                            "Congratulations! The dynamic body shell [$unlockedItemName] has been purchased and loaded into your command hanger. Enjoy slithering with pride!",
                            color = Color.LightGray
                        )
                    },
                    containerColor = Color(0xFF0C101F),
                    titleContentColor = Color.White
                )
            }
        }
    }
}

// ---------- Promotional Banner ----------
@Composable
private fun PromotionalBanner() {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        borderColor = Color(0x339933FF),
        backgroundColor = Color(0x0C9933FF),
        glowColor = Color(0xFF9933FF)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SEASON 1 BATTLE PASS",
                color = Color(0xFFFF9900),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "UNLEASH THE CYBER VIPER",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Collect special achievements and win Premium Coins",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------- Shop Item Card ----------
@Composable
private fun ShopItemCard(
    skin: ShopItem,
    isUnlocked: Boolean,
    isCurrentlyEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isCurrentlyEquipped) skin.primaryColor.copy(alpha = 0.5f) else Color(0x1FFFFFFF),
        backgroundColor = Color(0x0E0A0F24)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Skin Preview Circle
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                skin.primaryColor,
                                skin.secondaryColor,
                                skin.primaryColor
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.Black.copy(alpha = 0.2f), radius = size.width / 3f)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skin.name.uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = skin.description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Button
            ShopActionButton(
                isCurrentlyEquipped = isCurrentlyEquipped,
                isUnlocked = isUnlocked,
                skin = skin,
                onEquip = onEquip,
                onBuy = onBuy
            )
        }
    }
}

// ---------- Shop Action Button ----------
@Composable
private fun ShopActionButton(
    isCurrentlyEquipped: Boolean,
    isUnlocked: Boolean,
    skin: ShopItem,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    when {
        isCurrentlyEquipped -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.primaryColor.copy(alpha = 0.15f))
                    .border(1.dp, skin.primaryColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = skin.primaryColor, modifier = Modifier.size(14.dp))
                    Text("ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        isUnlocked -> {
            Button(
                onClick = onEquip,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("equip_${skin.name}")
            ) {
                Text("EQUIP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFF33)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("buy_${skin.name}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.Black, modifier = Modifier.size(12.dp))
                    Text("${skin.price}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}