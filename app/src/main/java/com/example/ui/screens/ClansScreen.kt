package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.Clan
import com.example.game.GameViewModel
import com.example.ui.components.GlassmorphicCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClansScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val clansList by viewModel.clans.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var clanNameInput by remember { mutableStateOf("") }
    var clanTagInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Show snackbar when a message is set
    LaunchedEffect(pendingMessage) {
        pendingMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            pendingMessage = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "CLANS ALLIANCE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("clans_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Current Clan section
                userProfile?.let { profile ->
                    if (profile.clanName != null) {
                        // User in a Clan
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            borderColor = Color(0xFF00FFCC).copy(alpha = 0.5f),
                            backgroundColor = Color(0x1F0F172A),
                            glowColor = Color(0xFF00FFCC)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "YOUR ALLIANCE TEAM",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        profile.clanName,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Share room codes with your crew to practice together",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            coroutineScope.launch {
                                                viewModel.leaveCurrentClan()
                                                isProcessing = false
                                                pendingMessage = "You left the clan."
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("leave_clan"),
                                    enabled = !isProcessing
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = "Leave", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("LEAVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // User NOT in a Clan
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            borderColor = Color(0x33FFFFFF)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "JOIN A CLAN & DOMINATE",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Compete side-by-side with fellow snakes, share achievements, and unlock top ELO league points.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("show_create_clan"),
                                    enabled = !isProcessing
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                                        Text("MAKE NEW CLAN (300 COINS)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "LADDER RANKINGS",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                // List of Clans – FIX: key now uses index, not id.
                if (clansList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Searching active clans on server...", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = clansList.size,
                            key = { index -> index }  // FIXED: Use index as stable key
                        ) { index ->
                            ClanRow(
                                clan = clansList[index],
                                isUserInAnyClan = userProfile?.clanName != null,
                                isCurrentlyMember = userProfile?.clanName == clansList[index].name,
                                isProcessing = isProcessing,
                                onJoin = {
                                    if (!isProcessing) {
                                        isProcessing = true
                                        coroutineScope.launch {
                                            viewModel.joinOrCreateClan(
                                                isCreate = false,
                                                name = clansList[index].name,
                                                tag = clansList[index].tag,
                                                onCompleted = { msg ->
                                                    pendingMessage = msg
                                                    isProcessing = false
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Create Clan Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (clanNameInput.isNotBlank() && clanTagInput.length in 2..4) {
                                    if (!isProcessing) {
                                        isProcessing = true
                                        showCreateDialog = false
                                        coroutineScope.launch {
                                            viewModel.joinOrCreateClan(
                                                isCreate = true,
                                                name = clanNameInput,
                                                tag = clanTagInput.uppercase(),
                                                onCompleted = { msg ->
                                                    pendingMessage = msg
                                                    clanNameInput = ""
                                                    clanTagInput = ""
                                                    isProcessing = false
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    pendingMessage = "Please insert unique name & 2-4 tag size"
                                }
                            }
                        ) {
                            Text("CREATE CLAN (300 C)", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    },
                    title = { Text("ESTABLISH NEW CLAN", color = Color.White, fontWeight = FontWeight.Black) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Declare your crew name and standard 4-letter tag.", color = Color.LightGray, fontSize = 12.sp)

                            OutlinedTextField(
                                value = clanNameInput,
                                onValueChange = { clanNameInput = it.take(20) },
                                label = { Text("CREW NAME") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing
                            )

                            OutlinedTextField(
                                value = clanTagInput,
                                onValueChange = { clanTagInput = it.take(4).uppercase() },
                                label = { Text("CLAN TAG (2-4 LETTERS)") },
                                placeholder = { Text("E.g. APEX") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing
                            )

                            if (isProcessing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF00FFCC)
                                )
                            }
                        }
                    },
                    containerColor = Color(0xFF0C101F),
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun ClanRow(
    clan: Clan,
    isUserInAnyClan: Boolean,
    isCurrentlyMember: Boolean,
    isProcessing: Boolean,
    onJoin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .border(
                1.dp,
                if (isCurrentlyMember) Color(0xFF00FFCC) else Color(0x0AFFFFFF),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF9900).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(clan.tag, color = Color(0xFFFF9900), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Text(clan.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text("${clan.memberCount} MEMBERS", color = Color.Gray, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(12.dp))
                        Text("${clan.totalScore} POINTS", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Join Button
            when {
                isCurrentlyMember -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FFCC).copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("MEMBER", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                !isUserInAnyClan -> {
                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("join_${clan.name}"),
                        enabled = !isProcessing
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Text("JOIN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                else -> {
                    // User is in a different clan – no action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("OTHER", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}