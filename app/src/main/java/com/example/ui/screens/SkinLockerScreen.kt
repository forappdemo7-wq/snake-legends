package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GameViewModel
import com.example.ui.components.GlassmorphicCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// High fidelity unified inventory item model representing everything in the user's promo image!
data class InventoryItem(
    val name: String,
    val category: String, // "SNAKES", "SKINS", "TRAILS", "EMOTES", "BOOSTS"
    val description: String,
    val price: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val rarity: String, // "COMMON", "RARE", "EPIC", "LEGENDARY", "STEALTH", "MYSTICAL"
    val rarityColor: Color,
    val particlesType: String, // "lava", "cosmic", "stealth", "cyber", "ghost", "none"
    val iconText: String? = null
)

data class ShowcaseParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val shapeType: Int // 0: circle, 1: square, 2: star
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinLockerScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val unlockedList by viewModel.unlockedCosmetics.collectAsStateWithLifecycle()

    var usernameInput by remember(userProfile) { mutableStateOf(userProfile?.username ?: "SNAKE_KING") }
    var renameStatusMessage by remember { mutableStateOf<String?>(null) }
    
    // Active left sidebar tab matching "INVENTORY" sidebar in uploaded image
    var activeCategory by remember { mutableStateOf("SNAKES") } // SNAKES, SKINS, TRAILS, EMOTES, BOOSTS, AGENT

    val unlockedNames = remember(unlockedList) { unlockedList.map { it.name }.toSet() }

    // Fully detailed catalog corresponding to the awesome item list of the updated inventory system
    val inventoryItems = remember {
        listOf(
            // --- SNAKES category ---
            InventoryItem(
                name = "Emerald Serpent",
                category = "SNAKES",
                description = "Deep emerald cyber scale matrix that increases visual dominance and target focus",
                price = 0,
                primaryColor = Color(0xFF00FF88),
                secondaryColor = Color(0xFF004D26),
                rarity = "EPIC",
                rarityColor = Color(0xFF00FF88),
                particlesType = "cyber"
            ),
            InventoryItem(
                name = "Fire Fang",
                category = "SNAKES",
                description = "Volatile molten magma plates that leave a dangerous heat imprint during acceleration",
                price = 150,
                primaryColor = Color(0xFFFF4500),
                secondaryColor = Color(0xFF550000),
                rarity = "EPIC",
                rarityColor = Color(0xFFFF5722),
                particlesType = "lava"
            ),
            InventoryItem(
                name = "Shadow Venom",
                category = "SNAKES",
                description = "Low-frequency spectral scale mesh drawn from deep cosmic space rifts",
                price = 250,
                primaryColor = Color(0xFF9933FF),
                secondaryColor = Color(0xFF1A0033),
                rarity = "EPIC",
                rarityColor = Color(0xFFE040FB),
                particlesType = "cosmic"
            ),

            // --- SKINS category ---
            InventoryItem(
                name = "Neon Cyber",
                category = "SKINS",
                description = "Futuristic standard laser-carved casing with cyan-blue matrix trails",
                price = 0,
                primaryColor = Color(0xFF00FFCC),
                secondaryColor = Color(0xFF0099FF),
                rarity = "RARE",
                rarityColor = Color(0xFF00E5FF),
                particlesType = "cyber"
            ),
            InventoryItem(
                name = "Volcanic Lava",
                category = "SKINS",
                description = "Chalcogenide magma plate casing that drips volatile glowing slag",
                price = 200,
                primaryColor = Color(0xFFFF3300),
                secondaryColor = Color(0xFFFFBB00),
                rarity = "LEGENDARY",
                rarityColor = Color(0xFFFF5722),
                particlesType = "lava"
            ),
            InventoryItem(
                name = "Phantom Ghost",
                category = "SKINS",
                description = "Translucent poltergeist scales that float between spectral dimensions",
                price = 350,
                primaryColor = Color(0xFFD4E6F1),
                secondaryColor = Color(0xFF90A4AE),
                rarity = "EPIC",
                rarityColor = Color(0xFF90A4AE),
                particlesType = "ghost"
            ),
            InventoryItem(
                name = "Galactic Cosmic",
                category = "SKINS",
                description = "Pulsing dense quantum stardust drawn from collapsing stellar rifts",
                price = 400,
                primaryColor = Color(0xFF9933FF),
                secondaryColor = Color(0xFFFF5252),
                rarity = "MYSTICAL",
                rarityColor = Color(0xFFE040FB),
                particlesType = "cosmic"
            ),
            InventoryItem(
                name = "Stealth Cyber",
                category = "SKINS",
                description = "Low-observability dark titanium plates optimized for quiet hunting",
                price = 500,
                primaryColor = Color(0xFF00E676),
                secondaryColor = Color(0xFF37474F),
                rarity = "STEALTH",
                rarityColor = Color(0xFF00E676),
                particlesType = "stealth"
            ),

            // --- TRAILS category ---
            InventoryItem(
                name = "Neon Trail",
                category = "TRAILS",
                description = "High-voltage azure trace particles that illuminate the dark arena grid",
                price = 0,
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFF00838F),
                rarity = "RARE",
                rarityColor = Color(0xFF00E5FF),
                particlesType = "cyber"
            ),
            InventoryItem(
                name = "Crown Aura",
                category = "TRAILS",
                description = "Exclusive golden crown royalty spark that hovers above your viper",
                price = 150,
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFFF8F00),
                rarity = "EPIC",
                rarityColor = Color(0xFFFFD700),
                particlesType = "cosmic"
            ),
            InventoryItem(
                name = "Volcanic Heat",
                category = "TRAILS",
                description = "Continuous stream of glowing red hot soot rising behind your head",
                price = 200,
                primaryColor = Color(0xFFFF5722),
                secondaryColor = Color(0xFFFF8A65),
                rarity = "EPIC",
                rarityColor = Color(0xFFFF5722),
                particlesType = "lava"
            ),

            // --- EMOTES category ---
            InventoryItem(
                name = "GG Easy",
                category = "EMOTES",
                description = "Display a clean retro cybernetic 'GG EASY' text bubble in active chat logs",
                price = 50,
                primaryColor = Color(0xFF00FF88),
                secondaryColor = Color(0xFF004D26),
                rarity = "COMMON",
                rarityColor = Color(0xFF94A3B8),
                particlesType = "cyber",
                iconText = "GG"
            ),
            InventoryItem(
                name = "Rage Slither",
                category = "EMOTES",
                description = "Ignite intense angry flame face on your snake avatar when devouring foes",
                price = 100,
                primaryColor = Color(0xFFEF4444),
                secondaryColor = Color(0xFF7F1D1D),
                rarity = "EPIC",
                rarityColor = Color(0xFFEF4444),
                particlesType = "lava",
                iconText = "RAGE"
            ),
            InventoryItem(
                name = "Victory Crown",
                category = "EMOTES",
                description = "Display a brilliant gold champion crown badge next to your scoreboard name",
                price = 180,
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFB45309),
                rarity = "LEGENDARY",
                rarityColor = Color(0xFFFFD700),
                particlesType = "cosmic",
                iconText = "👑"
            ),

            // --- BOOSTS category ---
            InventoryItem(
                name = "XP Boost",
                category = "BOOSTS",
                description = "Boost standard round level gains by 1.5x to unlock Royal Pass paths quicker",
                price = 10,
                primaryColor = Color(0xFF00FFCC),
                secondaryColor = Color(0xFF0369A1),
                rarity = "COMMON",
                rarityColor = Color(0xFF94A3B8),
                particlesType = "cyber",
                iconText = "XP+"
            ),
            InventoryItem(
                name = "Coin Magnet",
                category = "BOOSTS",
                description = "Deploy interactive electromagnetic node that doubles normal coin collection radius",
                price = 15,
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFFF8F00),
                rarity = "EPIC",
                rarityColor = Color(0xFFFFD700),
                particlesType = "cosmic",
                iconText = "MAG"
            ),
            InventoryItem(
                name = "Shield Cell",
                category = "BOOSTS",
                description = "Start the next survival round with single-impact energy barrier protection",
                price = 30,
                primaryColor = Color(0xFF60A5FA),
                secondaryColor = Color(0xFF1E3A8A),
                rarity = "STEALTH",
                rarityColor = Color(0xFF60A5FA),
                particlesType = "stealth",
                iconText = "SHLD"
            )
        )
    }

    var tickState by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(16)
            tickState++
        }
    }

    // Interactive Transaction Confirmation dialog state
    var selectedBuyItem by remember { mutableStateOf<InventoryItem?>(null) }
    var buyErrorMessage by remember { mutableStateOf<String?>(null) }
    var buySuccessMessage by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NEURAL HANGER DECK",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "SYSTEM UPGRADE SECTOR • INVENTORY",
                            color = Color(0xFF00FFCC),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("locker_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFFF33),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${userProfile?.coins ?: 0}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF020617),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF020617))
                .drawBehind {
                    // Modern premium grid backing
                    val width = size.width
                    val height = size.height
                    val gridSpacing = 50f
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 15f), 0f)
                    
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.2f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )
                        x += gridSpacing
                    }
                    
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )
                        y += gridSpacing
                    }

                    // Cyber green top border ambient sweep line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF00FFCC).copy(alpha = 0.4f), Color.Transparent)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(width, 0f),
                        strokeWidth = 3f
                    )
                }
        ) {
            if (isLandscape) {
                // Wide landscape layout side-by-side matching the premium graphic!
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left sidebar matching "INVENTORY" sidebar card
                    InventorySidebarCard(
                        activeCategory = activeCategory,
                        onCategorySelect = { activeCategory = it },
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )

                    // Right grid showing current inventory items
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (activeCategory == "AGENT") {
                            AgentProfileSectorCard(
                                usernameInput = usernameInput,
                                onUsernameChange = { usernameInput = it },
                                viewModel = viewModel,
                                renameStatusMessage = renameStatusMessage,
                                onStatusMessageChange = { renameStatusMessage = it }
                            )
                        } else {
                            val filteredItems = remember(activeCategory, inventoryItems) {
                                inventoryItems.filter { it.category == activeCategory }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 210.dp),
                                state = rememberLazyGridState(),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredItems.size) { index ->
                                    val item = filteredItems[index]
                                    val isUnlocked = unlockedNames.contains(item.name) || item.price == 0
                                    
                                    val isCurrentlyEquipped = if (item.category == "TRAILS") {
                                        userProfile?.currentTrail == item.name
                                    } else {
                                        userProfile?.currentSkin == item.name
                                    }

                                    InventoryShowcaseCard(
                                        item = item,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isCurrentlyEquipped,
                                        tickState = tickState,
                                        onActionClick = {
                                            if (isUnlocked) {
                                                val type = if (item.category == "TRAILS") "trail" else "skin"
                                                viewModel.selectCosmetic(item.name, type)
                                            } else {
                                                selectedBuyItem = item
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Vertical portrait/compact layout: Tabs at the top, Grid below
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top horizontal scrollable tab row for Category Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SNAKES", "SKINS", "TRAILS", "EMOTES", "BOOSTS", "AGENT").forEach { cat ->
                            val isActive = activeCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isActive) Color(0xFF00FFCC) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activeCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isActive) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Screen content
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeCategory == "AGENT") {
                            AgentProfileSectorCard(
                                usernameInput = usernameInput,
                                onUsernameChange = { usernameInput = it },
                                viewModel = viewModel,
                                renameStatusMessage = renameStatusMessage,
                                onStatusMessageChange = { renameStatusMessage = it }
                            )
                        } else {
                            val filteredItems = remember(activeCategory, inventoryItems) {
                                inventoryItems.filter { it.category == activeCategory }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                state = rememberLazyGridState(),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems.size) { index ->
                                    val item = filteredItems[index]
                                    val isUnlocked = unlockedNames.contains(item.name) || item.price == 0
                                    
                                    val isCurrentlyEquipped = if (item.category == "TRAILS") {
                                        userProfile?.currentTrail == item.name
                                    } else {
                                        userProfile?.currentSkin == item.name
                                    }

                                    InventoryShowcaseCard(
                                        item = item,
                                        isUnlocked = isUnlocked,
                                        isEquipped = isCurrentlyEquipped,
                                        tickState = tickState,
                                        onActionClick = {
                                            if (isUnlocked) {
                                                val type = if (item.category == "TRAILS") "trail" else "skin"
                                                viewModel.selectCosmetic(item.name, type)
                                            } else {
                                                selectedBuyItem = item
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secure Purchase Authorization Modal
            selectedBuyItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { selectedBuyItem = null },
                    containerColor = Color(0xFF0F172A),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.5.dp, Color(0xFF00FFCC), RoundedCornerShape(20.dp)),
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUTHORIZE TRANSACTION",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(top = 8.dp))
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Acquire the custom item \"${item.name}\" to refit your viper matrix.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("COST DETECTED:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFFF33), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${item.price} Coins", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }

                            buyErrorMessage?.let {
                                Text(it, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            buySuccessMessage?.let {
                                Text(it, color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val type = if (item.category == "TRAILS") "trail" else "skin"
                                viewModel.buyCosmetic(
                                    name = item.name,
                                    type = type,
                                    price = item.price,
                                    onSuccess = {
                                        buySuccessMessage = "ACQUISITION AUTHORIZED!"
                                        buyErrorMessage = null
                                        viewModel.selectCosmetic(item.name, type)
                                        // Auto close after brief interval
                                        selectedBuyItem = null
                                        buySuccessMessage = null
                                    },
                                    onError = { err ->
                                        buyErrorMessage = "ERROR: $err"
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CONFIRM ACQUISITION", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            selectedBuyItem = null
                            buyErrorMessage = null
                            buySuccessMessage = null
                        }) {
                            Text("ABORT", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                )
            }
        }
    }
}

// Sidebar panel designed to look exactly like the INVENTORY sidebar from the user graphic!
@Composable
fun InventorySidebarCard(
    activeCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: "INVENTORY" in tech design
            Text(
                text = "INVENTORY",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .padding(bottom = 6.dp, start = 4.dp)
                    .drawBehind {
                        // Small tech accent bar
                        drawLine(
                            color = Color(0xFF00FFCC),
                            start = Offset(0f, size.height + 4.dp.toPx()),
                            end = Offset(30f, size.height + 4.dp.toPx()),
                            strokeWidth = 3f
                        )
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sidebar Buttons corresponding to image
            val buttons = listOf(
                "SNAKES" to Icons.Default.SportsEsports,
                "SKINS" to Icons.Default.Brush,
                "TRAILS" to Icons.Default.Star,
                "EMOTES" to Icons.Default.Favorite,
                "BOOSTS" to Icons.Default.OfflineBolt,
                "AGENT" to Icons.Default.Badge
            )

            buttons.forEach { (name, icon) ->
                val isActive = activeCategory == name
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isActive) Color(0xFF00FFCC).copy(alpha = 0.12f) else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) Color(0xFF00FFCC) else Color(0xFF1E293B).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onCategorySelect(name) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) Color(0xFF00FFCC) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = name,
                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// Showcase Card on the right panel
@Composable
fun InventoryShowcaseCard(
    item: InventoryItem,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    tickState: Int,
    onActionClick: () -> Unit
) {
    // Local particles pool inside each card to achieve Custom Particle Trails!
    val particles = remember { mutableStateListOf<ShowcaseParticle>() }

    LaunchedEffect(tickState) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            p.life += 1f
            if (p.life >= p.maxLife) {
                iterator.remove()
            }
        }

        // Spawn custom particles matching the theme!
        if (particles.size < 12 && Random.nextInt(3) == 0) {
            val centerX = 100f
            val centerY = 75f
            val radAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val offsetDist = Random.nextFloat() * 20f

            when (item.particlesType) {
                "lava" -> {
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * offsetDist,
                            y = centerY + sin(radAngle) * offsetDist + 5f,
                            vx = (Random.nextFloat() * 0.6f - 0.3f),
                            vy = -Random.nextFloat() * 1.2f - 0.2f, // upward lava embers
                            size = Random.nextFloat() * 5f + 1.5f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 25f + 20f,
                            color = if (Random.nextBoolean()) Color(0xFFFF4500) else Color(0xFFFFD700),
                            shapeType = 0
                        )
                    )
                }
                "cosmic" -> {
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * (offsetDist + 10f),
                            y = centerY + sin(radAngle) * (offsetDist + 10f),
                            vx = cos(radAngle + Math.PI / 2).toFloat() * 0.5f,
                            vy = sin(radAngle + Math.PI / 2).toFloat() * 0.5f - 0.2f,
                            size = Random.nextFloat() * 4f + 1f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 30f + 15f,
                            color = if (Random.nextBoolean()) Color(0xFFE040FB) else Color(0xFF00FFFF),
                            shapeType = 2
                        )
                    )
                }
                "stealth" -> {
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + (Random.nextFloat() * 60f - 30f),
                            y = centerY + (Random.nextFloat() * 40f - 20f),
                            vx = 0f,
                            vy = -Random.nextFloat() * 0.4f - 0.1f,
                            size = Random.nextFloat() * 3f + 1.5f,
                            alpha = 0.8f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 20f + 10f,
                            color = Color(0xFF00E676),
                            shapeType = 1
                        )
                    )
                }
                "ghost" -> {
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * offsetDist,
                            y = centerY + sin(radAngle) * offsetDist,
                            vx = (Random.nextFloat() * 0.4f - 0.2f),
                            vy = (Random.nextFloat() * 0.4f - 0.2f),
                            size = Random.nextFloat() * 8f + 3f,
                            alpha = 0.6f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 35f + 15f,
                            color = Color(0xFFD4E6F1).copy(alpha = 0.15f),
                            shapeType = 0
                        )
                    )
                }
                "cyber" -> {
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + (Random.nextFloat() * 70f - 35f),
                            y = centerY + (Random.nextFloat() * 35f - 17f),
                            vx = 0f,
                            vy = -Random.nextFloat() * 1.8f - 0.4f,
                            size = Random.nextFloat() * 4f + 1f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 18f + 12f,
                            color = if (Random.nextBoolean()) Color(0xFF00FFCC) else Color(0xFF0099FF),
                            shapeType = 1
                        )
                    )
                }
            }
        }
    }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                brush = if (isEquipped) {
                    Brush.verticalGradient(listOf(item.primaryColor, Color.Transparent))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                },
                shape = RoundedCornerShape(16.dp)
            ),
        borderColor = if (isEquipped) item.primaryColor.copy(alpha = 0.45f) else Color(0x1F1E293B),
        backgroundColor = Color(0x15090D24)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Interactive glowing neon-drop-shadow rarity badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = item.rarityColor.copy(alpha = 0.45f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 4f)
                        )
                    }
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, item.rarityColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.rarity,
                    color = item.rarityColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Preview area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        // A. Render custom particle trails
                        particles.forEach { p ->
                            val alphaColor = p.color.copy(alpha = p.alpha)
                            when (p.shapeType) {
                                1 -> {
                                    drawRect(
                                        color = alphaColor,
                                        topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                                        size = Size(p.size, p.size)
                                    )
                                }
                                2 -> {
                                    val sizeHalf = p.size / 2f
                                    drawLine(
                                        color = alphaColor,
                                        start = Offset(p.x - sizeHalf, p.y),
                                        end = Offset(p.x + sizeHalf, p.y),
                                        strokeWidth = 2f
                                    )
                                    drawLine(
                                        color = alphaColor,
                                        start = Offset(p.x, p.y - sizeHalf),
                                        end = Offset(p.x, p.y + sizeHalf),
                                        strokeWidth = 2f
                                    )
                                }
                                else -> {
                                    drawCircle(
                                        color = alphaColor,
                                        radius = p.size,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                            }
                        }

                        // B. Render different visuals depending on category
                        if (item.category == "SNAKES" || item.category == "SKINS") {
                            // Beautiful 3D Orbiting preview snake coils
                            drawCircle(
                                color = item.primaryColor.copy(alpha = 0.08f),
                                radius = 42f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), tickState * 0.4f))
                            )

                            val segments = 10
                            val time = tickState * 0.04f

                            for (i in 0 until segments) {
                                val segmentAngle = time - i * 0.42f
                                val z = sin(segmentAngle)
                                val scale = 0.8f + (z * 0.2f)
                                val alpha = 0.4f + ((z + 1f) / 2f) * 0.6f
                                val x = cx + cos(segmentAngle) * 44f
                                val y = cy + sin(segmentAngle * 1.5f) * 16f

                                val rad = (9.5f - i * 0.5f).coerceAtLeast(3.5f) * scale

                                // Shadow aura
                                drawCircle(
                                    color = item.primaryColor.copy(alpha = alpha * 0.4f),
                                    radius = rad * 1.6f,
                                    center = Offset(x, y)
                                )

                                // Core segment
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = alpha), item.primaryColor.copy(alpha = alpha), item.secondaryColor.copy(alpha = alpha * 0.15f)),
                                        center = Offset(x, y),
                                        radius = rad
                                    ),
                                    center = Offset(x, y),
                                    radius = rad
                                )
                            }
                        } else if (item.category == "TRAILS") {
                            // Draw glowing trail flow stream
                            val segmentCount = 14
                            for (i in 0 until segmentCount) {
                                val x = cx - 50f + (i * 8f)
                                val waveOffset = sin((tickState * 0.1f) + (i * 0.4f)) * 14f
                                val y = cy + waveOffset
                                val sizeScale = (1.2f - (i.toFloat() / segmentCount)).coerceAtLeast(0.2f)
                                val rad = 10f * sizeScale

                                drawCircle(
                                    color = item.primaryColor.copy(alpha = (1f - (i.toFloat() / segmentCount)) * 0.8f),
                                    radius = rad,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - (i.toFloat() / segmentCount)) * 0.9f),
                                    radius = rad * 0.4f,
                                    center = Offset(x, y)
                                )
                            }
                        } else {
                            // Emotes & Boosts: large neon custom graphic indicator
                            drawCircle(
                                color = item.primaryColor.copy(alpha = 0.08f),
                                radius = 32f,
                                center = Offset(cx, cy)
                            )
                        }
                    }

                    // Render plain overlay text/graphic for EMOTES/BOOSTS
                    if (item.iconText != null) {
                        Text(
                            text = item.iconText,
                            color = Color.White,
                            fontSize = if (item.iconText.length > 2) 20.sp else 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(item.primaryColor.copy(alpha = 0.35f), Color.Transparent)
                                    ),
                                    radius = 45f
                                )
                            }
                        )
                    }
                }

                // Title and brief description
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name.uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        color = Color.Gray,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                // Button deck
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isEquipped -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(12.dp))
                                    Text("EQUIPPED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                        isUnlocked -> {
                            Button(
                                onClick = onActionClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .testTag("equip_item_${item.name}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("EQUIP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onActionClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .testTag("buy_item_${item.name}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFFFF33),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text("${item.price}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Callsign registry panel inside our inventory screen
@Composable
fun AgentProfileSectorCard(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    viewModel: GameViewModel,
    renameStatusMessage: String?,
    onStatusMessageChange: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "AGENT CALLSIGN REGISTRY",
                color = Color(0xFF00FFCC),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Text(
                "Modify your central network identity. Your callsign is broadcasted to all multiplayer match segments, clans directories, and world leaderboard sectors.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                label = { Text("AGENT CALLSIGN", color = Color.Gray, fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("locker_username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FFCC),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = {
                    if (usernameInput.isNotBlank()) {
                        viewModel.updateUsername(usernameInput.trim())
                        onStatusMessageChange("CALLSIGN SYNCHRONIZED SUCCESSFULLY!")
                    } else {
                        onStatusMessageChange("ERROR: CALLSIGN CANNOT BE EMPTY.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("save_callsign_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WRITE TO COGNITIVE CORE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            AnimatedVisibility(visible = renameStatusMessage != null) {
                Text(
                    text = renameStatusMessage ?: "",
                    color = if (renameStatusMessage?.startsWith("ERROR") == true) Color(0xFFEF4444) else Color(0xFF22C55E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
