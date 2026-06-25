package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private fun configureImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()
        
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val gameViewModel: GameViewModel = viewModel()
                val isGameActive by gameViewModel.isGameActive.collectAsStateWithLifecycle()

                var currentRoute by remember { mutableStateOf("app_loading") }
                var isMatchmakingCompleted by remember { mutableStateOf(false) }

                LaunchedEffect(isGameActive) {
                    if (!isGameActive) {
                        isMatchmakingCompleted = false
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    if (isGameActive) {
                        if (isMatchmakingCompleted) {
                            GameScreen(
                                viewModel = gameViewModel,
                                onNavigateBack = { gameViewModel.cancelActiveGame() }
                            )
                        } else {
                            GameLoadingScreen(
                                gameMode = gameViewModel.gameEngine.gameMode,
                                arenaTheme = gameViewModel.gameEngine.arenaTheme,
                                privateRoomCode = gameViewModel.privateRoomCode,
                                onLoadingFinished = { isMatchmakingCompleted = true }
                            )
                        }
                    } else {
                        when (currentRoute) {
                            "app_loading" -> {
                                AppLoadingScreen(
                                    onLoadingFinished = { currentRoute = "lobby" }
                                )
                            }
                            "lobby" -> {
                                LobbyScreen(
                                    viewModel = gameViewModel,
                                    onNavigateToShop = { currentRoute = "shop" },
                                    onNavigateToClans = { currentRoute = "clans" },
                                    onNavigateToLeaderboard = { currentRoute = "leaderboard" },
                                    onNavigateToSkinLocker = { currentRoute = "skin_locker" }
                                )
                            }
                            "shop" -> {
                                ShopScreen(
                                    viewModel = gameViewModel,
                                    onBack = { currentRoute = "lobby" }
                                )
                            }
                            "clans" -> {
                                ClansScreen(
                                    viewModel = gameViewModel,
                                    onBack = { currentRoute = "lobby" }
                                )
                            }
                            "leaderboard" -> {
                                LeaderboardScreen(
                                    viewModel = gameViewModel,
                                    onBack = { currentRoute = "lobby" }
                                )
                            }
                            "skin_locker" -> {
                                SkinLockerScreen(
                                    viewModel = gameViewModel,
                                    onBack = { currentRoute = "lobby" },
                                    onNavigateToShop = { currentRoute = "shop" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
