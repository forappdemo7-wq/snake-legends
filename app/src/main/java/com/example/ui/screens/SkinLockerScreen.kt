package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

data class LockerSkin(
    val name: String,
    val description: String,
    val price: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val rarity: String, // "EPIC", "LEGENDARY", "STEALTH", "MYSTICAL"
    val rarityColor: Color,
    val particlesType: String // "lava", "cosmic", "stealth", "ghost", "cyber"
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
    var isEditingName by remember { mutableStateOf(false) }
    var renameStatusMessage by remember { mutableStateOf<String?>(null) }
    var activeTabSelection by remember { mutableStateOf("skins") } // skins, profiles

    val unlockedNames = remember(unlockedList) { unlockedList.map { it.name }.toSet() }

    val skins = remember {
        listOf(
            LockerSkin(
                name = "Neon Cyber",
                description = "Futuristic standard laser-carved casing with cyan-blue matrix trails",
                price = 0,
                primaryColor = Color(0xFF00FFCC),
                secondaryColor = Color(0xFF0099FF),
                rarity = "CYBER RARE",
                rarityColor = Color(0xFF00E5FF),
                particlesType = "cyber"
            ),
            LockerSkin(
                name = "Volcanic Lava",
                description = "Chalcogenide magma plate casing that drips volatile glowing slag",
                price = 200,
                primaryColor = Color(0xFFFF3300),
                secondaryColor = Color(0xFFFFBB00),
                rarity = "LEGENDARY",
                rarityColor = Color(0xFFFF5722),
                particlesType = "lava"
            ),
            LockerSkin(
                name = "Phantom Ghost",
                description = "Translucent poltergeist scales that float between spectral dimensions",
                price = 350,
                primaryColor = Color(0xFFD4E6F1),
                secondaryColor = Color(0xFF90A4AE),
                rarity = "SPECTRAL EPIC",
                rarityColor = Color(0xFF90A4AE),
                particlesType = "ghost"
            ),
            LockerSkin(
                name = "Galactic Cosmic",
                description = "Pulsing dense quantum stardust drawn from collapsing stellar rifts",
                price = 500,
                primaryColor = Color(0xFF9933FF),
                secondaryColor = Color(0xFFFF5252),
                rarity = "MYSTICAL",
                rarityColor = Color(0xFFE040FB),
                particlesType = "cosmic"
            ),
            LockerSkin(
                name = "Stealth Cyber",
                description = "Low-observability dark titanium plates optimized for quiet hunting",
                price = 600,
                primaryColor = Color(0xFF00E676),
                secondaryColor = Color(0xFF37474F),
                rarity = "STEALTH OPS",
                rarityColor = Color(0xFF00E676),
                particlesType = "stealth"
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp || configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NEURAL HANGER DECK",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("locker_back")) {
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
                    containerColor = Color(0xFF0A0E1A),
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
                    // Draw sci-fi background hanger grid lines
                    val width = size.width
                    val height = size.height
                    val gridSpacing = 45f
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 15f), 0f)
                    
                    // Draw vertical dash lines
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.35f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )
                        x += gridSpacing
                    }
                    
                    // Draw horizontal dash lines
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )
                        y += gridSpacing
                    }

                    // Bottom ambient glow gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.8f)),
                            startY = height * 0.6f,
                            endY = height
                        ),
                        size = size
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Tabs: Skin Showcase vs Callsign Registry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { activeTabSelection = "skins" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTabSelection == "skins") Color(0xFF1E293B) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COSMETIC SHIELDS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { activeTabSelection = "profile" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTabSelection == "profile") Color(0xFF1E293B) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AGENT CALLSIGN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (activeTabSelection == "skins") {
                    // Subtitle for scientific re-fitting deck
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "CYBERNETIC COIL CHAMBERS",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            "Preview active skins, particle radiation outputs, and refit body coils.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }

                    // Sliding carousel of skins mimicking sci-fi "garage chamber"
                    val lazyListState = rememberLazyListState()
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        items(skins.size) { index ->
                            val skin = skins[index]
                            val isUnlocked = unlockedNames.contains(skin.name) || skin.price == 0
                            val isCurrentlyEquipped = userProfile?.currentSkin == skin.name

                            SkinHangerChamberCard(
                                skin = skin,
                                isUnlocked = isUnlocked,
                                isEquipped = isCurrentlyEquipped,
                                tickState = tickState,
                                onEquipClick = {
                                    viewModel.selectCosmetic(skin.name, "skin")
                                },
                                onShopRedirect = onNavigateToShop,
                                isLandscape = isLandscape
                            )
                        }
                    }
                } else {
                    // CALLSIGN REGISTRY / PROFILE UPDATES
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "NEURAL CALLSIGN RE-REGISTRY",
                                color = Color(0xFF00FFCC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )

                            Text(
                                "Update your central network identity. Your callsign is synchronized across competitive ranked sectors and global clan databanks.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("AGENT CALLSIGN", color = Color.Gray) },
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
                                        renameStatusMessage = "CALLSIGN SYNCHRONIZED SUCCESSFULLY!"
                                    } else {
                                        renameStatusMessage = "ERROR: CALLSIGN CANNOT BE EMPTY."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_callsign_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WRITE TO COGNITIVE CORE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            }
        }
    }
}

@Composable
fun SkinHangerChamberCard(
    skin: LockerSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    tickState: Int,
    onEquipClick: () -> Unit,
    onShopRedirect: () -> Unit,
    isLandscape: Boolean
) {
    val cardWidth = if (isLandscape) 280.dp else 250.dp
    
    // Manage local particles list inside each skin card to showcase Custom Particle Trails
    val particles = remember { mutableStateListOf<ShowcaseParticle>() }

    // Tick-based Particle State Machine inside each card
    LaunchedEffect(tickState) {
        // 1. Progress active particles
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

        // 2. Spawn particles tailored to each specific skin theme
        if (particles.size < 18 && Random.nextInt(2) == 0) {
            val centerX = 125f // center coordinates relative to card preview field width/height
            val centerY = 100f
            val radAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val offsetDist = Random.nextFloat() * 30f

            // Customized physical vectors based on casing skin theme
            when (skin.particlesType) {
                "lava" -> {
                    // Magma glowing orange embers rising upwards
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * offsetDist,
                            y = centerY + sin(radAngle) * offsetDist + 10f,
                            vx = (Random.nextFloat() * 0.8f - 0.4f),
                            vy = -Random.nextFloat() * 1.5f - 0.3f, // floats upwards
                            size = Random.nextFloat() * 6f + 2f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 30f + 25f,
                            color = if (Random.nextBoolean()) Color(0xFFFF5722) else Color(0xFFFFC107),
                            shapeType = 0 // embers
                        )
                    )
                }
                "cosmic" -> {
                    // Purple stardust floating symmetrically and orbiting
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * (offsetDist + 15f),
                            y = centerY + sin(radAngle) * (offsetDist + 15f),
                            vx = (cos(radAngle + Math.PI / 2).toFloat() * 0.6f) + (Random.nextFloat() * 0.4f - 0.2f),
                            vy = (sin(radAngle + Math.PI / 2).toFloat() * 0.6f) - 0.3f,
                            size = Random.nextFloat() * 5f + 1.5f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 40f + 20f,
                            color = if (Random.nextBoolean()) Color(0xFFE040FB) else Color(0xFF00FFFF),
                            shapeType = 2 // sparkling stars
                        )
                    )
                }
                "stealth" -> {
                    // Quiet low-observability green digital blocks
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + (Random.nextFloat() * 80f - 40f),
                            y = centerY + (Random.nextFloat() * 60f - 30f),
                            vx = 0f,
                            vy = -Random.nextFloat() * 0.6f - 0.2f, // static cyber drift
                            size = Random.nextFloat() * 4f + 2f,
                            alpha = 0.9f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 25f + 15f,
                            color = Color(0xFF00E676),
                            shapeType = 1 // cyber blocks
                        )
                    )
                }
                "ghost" -> {
                    // Spectral poltergeist expanding soft vapor circles
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + cos(radAngle) * offsetDist,
                            y = centerY + sin(radAngle) * offsetDist,
                            vx = (Random.nextFloat() * 0.5f - 0.25f),
                            vy = (Random.nextFloat() * 0.5f - 0.25f),
                            size = Random.nextFloat() * 12f + 4f, // larger, soft misty cells
                            alpha = 0.7f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 40f + 20f,
                            color = Color(0xFFD4E6F1).copy(alpha = 0.15f),
                            shapeType = 0 // soft vapor circle
                        )
                    )
                }
                "cyber" -> {
                    // Cyan-blue laser vertical data spark columns
                    particles.add(
                        ShowcaseParticle(
                            x = centerX + (Random.nextFloat() * 90f - 45f),
                            y = centerY + (Random.nextFloat() * 50f - 25f),
                            vx = (Random.nextFloat() * 0.3f - 0.15f),
                            vy = -Random.nextFloat() * 2.2f - 0.8f, // fast vertical streams
                            size = Random.nextFloat() * 5f + 1f,
                            alpha = 1.0f,
                            life = 0f,
                            maxLife = Random.nextFloat() * 20f + 15f,
                            color = if (Random.nextBoolean()) Color(0xFF00FFCC) else Color(0xFF0099FF),
                            shapeType = 1 // square data sparks
                        )
                    )
                }
            }
        }
    }

    GlassmorphicCard(
        modifier = Modifier
            .width(cardWidth)
            .fillMaxHeight()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                brush = if (isEquipped) {
                    Brush.verticalGradient(listOf(skin.primaryColor, Color.Transparent))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                },
                shape = RoundedCornerShape(20.dp)
            ),
        borderColor = if (isEquipped) skin.primaryColor.copy(alpha = 0.4f) else Color(0x221E293B),
        backgroundColor = Color(0x0F090D1C)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Interactive glowing neon-drop-shadow rarity badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .drawBehind {
                        // Soft neon drop-shadow behind the badge
                        drawRoundRect(
                            color = skin.rarityColor.copy(alpha = 0.45f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 6f)
                        )
                    }
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .border(1.dp, skin.rarityColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = skin.rarity,
                    color = skin.rarityColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top section spacer for badge alignment
                Spacer(modifier = Modifier.height(24.dp))

                // Core Orbiting Preview + Particles showcase Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // A. Render custom particle trails first (behind the orbiting snake coils)
                        particles.forEach { p ->
                            val alphaColor = p.color.copy(alpha = p.alpha)
                            when (p.shapeType) {
                                1 -> { // Cyber pixel block / square
                                    drawRect(
                                        color = alphaColor,
                                        topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                                        size = Size(p.size, p.size)
                                    )
                                }
                                2 -> { // Sparkling star/cross
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
                                else -> { // Circular particle
                                    drawCircle(
                                        color = alphaColor,
                                        radius = p.size,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                            }
                        }

                        // B. Draw Orbit path
                        drawCircle(
                            color = skin.primaryColor.copy(alpha = 0.08f),
                            radius = 60f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), tickState * 0.5f))
                        )

                        // C. Interactive Orbiting Previews of snake body coils floating
                        val segments = 12
                        val time = tickState * 0.045f

                        for (i in 0 until segments) {
                            val segmentAngle = time - i * 0.38f
                            
                            // 3D Orbiting Projection effect
                            val z = sin(segmentAngle) // virtual depth
                            val scale = 0.82f + (z * 0.22f)
                            val alpha = 0.35f + ((z + 1f) / 2f) * 0.65f
                            
                            val x = centerX + cos(segmentAngle) * 62f
                            val y = centerY + sin(segmentAngle * 1.5f) * 22f

                            val baseRadius = (12f - i * 0.5f).coerceAtLeast(4.5f)
                            val rad = baseRadius * scale

                            // Draw glowing shadow circle
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(skin.primaryColor.copy(alpha = alpha * 0.6f), Color.Transparent),
                                    center = Offset(x, y),
                                    radius = rad * 1.8f
                                ),
                                center = Offset(x, y),
                                radius = rad * 1.8f
                            )

                            // Draw segment core
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = alpha),
                                        skin.primaryColor.copy(alpha = alpha),
                                        skin.secondaryColor.copy(alpha = alpha * 0.2f)
                                    ),
                                    center = Offset(x, y),
                                    radius = rad
                                ),
                                center = Offset(x, y),
                                radius = rad
                            )
                        }
                    }
                }

                // Title and description section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = skin.name.uppercase(),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = skin.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Refit action button deck
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isEquipped -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(skin.primaryColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, skin.primaryColor, RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = skin.primaryColor, modifier = Modifier.size(16.dp))
                                    Text("COIL ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                }
                            }
                        }
                        isUnlocked -> {
                            Button(
                                onClick = onEquipClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("locker_equip_${skin.name}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("REFIT VESSEL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onShopRedirect,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("locker_buy_${skin.name}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Text("ACQUIRE AT SHOP", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
