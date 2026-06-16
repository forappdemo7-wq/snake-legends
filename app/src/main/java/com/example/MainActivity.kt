// MainActivity.kt
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

    // Immersive mode helpers
    private fun configureImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    // Re-apply immersive mode when window focus changes (e.g., after dialog or notification)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveMode()
        }
    }

    override fun onBackPressed() {
        // If we are in a game, let the game handle back press (or we can prompt)
        // Otherwise, super will handle it.
        // We can customize this to prevent accidental exits from game.
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                // ViewModel is retained across configuration changes
                val gameViewModel: GameViewModel = viewModel()
                val isGameActive by gameViewModel.isGameActive.collectAsStateWithLifecycle()

                // Navigation state using a sealed class for type safety
                var currentRoute by remember { mutableStateOf<ScreenRoute>(ScreenRoute.AppLoading) }
                var isMatchmakingCompleted by remember { mutableStateOf(false) }

                // When game becomes inactive, reset matchmaking flag
                LaunchedEffect(isGameActive) {
                    if (!isGameActive) {
                        isMatchmakingCompleted = false
                    }
                }

                // Handle system back press while in game – cancel game if active
                BackHandler(
                    enabled = isGameActive && !gameViewModel.gameEngine.isGameOver
                ) {
                    gameViewModel.cancelActiveGame()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    when {
                        // Active game flow
                        isGameActive -> {
                            if (isMatchmakingCompleted) {
                                GameScreen(
                                    viewModel = gameViewModel,
                                    onNavigateBack = {
                                        gameViewModel.cancelActiveGame()
                                        currentRoute = ScreenRoute.Lobby
                                    }
                                )
                            } else {
                                GameLoadingScreen(
                                    gameMode = gameViewModel.gameEngine.gameMode,
                                    arenaTheme = gameViewModel.gameEngine.arenaTheme,
                                    privateRoomCode = gameViewModel.privateRoomCode,
                                    onLoadingFinished = { isMatchmakingCompleted = true }
                                )
                            }
                        }
                        // Main navigation
                        else -> {
                            when (currentRoute) {
                                ScreenRoute.AppLoading -> {
                                    AppLoadingScreen(
                                        onLoadingFinished = { currentRoute = ScreenRoute.Lobby }
                                    )
                                }
                                ScreenRoute.Lobby -> {
                                    LobbyScreen(
                                        viewModel = gameViewModel,
                                        onNavigateToShop = { currentRoute = ScreenRoute.Shop },
                                        onNavigateToClans = { currentRoute = ScreenRoute.Clans },
                                        onNavigateToLeaderboard = { currentRoute = ScreenRoute.Leaderboard }
                                    )
                                }
                                ScreenRoute.Shop -> {
                                    ShopScreen(
                                        viewModel = gameViewModel,
                                        onBack = { currentRoute = ScreenRoute.Lobby }
                                    )
                                }
                                ScreenRoute.Clans -> {
                                    ClansScreen(
                                        viewModel = gameViewModel,
                                        onBack = { currentRoute = ScreenRoute.Lobby }
                                    )
                                }
                                ScreenRoute.Leaderboard -> {
                                    LeaderboardScreen(
                                        viewModel = gameViewModel,
                                        onBack = { currentRoute = ScreenRoute.Lobby }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Define navigation routes as a sealed class for type safety and extensibility
    sealed class ScreenRoute {
        object AppLoading : ScreenRoute()
        object Lobby : ScreenRoute()
        object Shop : ScreenRoute()
        object Clans : ScreenRoute()
        object Leaderboard : ScreenRoute()
    }
}

// Composable to handle back press in a specific part of the UI
@Composable
private fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    // Use androidx.activity.compose.BackHandler if available (requires dependency)
    // For now, we'll rely on the Activity's onBackPressed override.
    // However, we can also use the system's BackHandler composable.
    // If you add the dependency: implementation "androidx.activity:activity-compose:1.8.0"
    // Then you can use: androidx.activity.compose.BackHandler(enabled, onBack)
    // Since it's not imported, we'll just leave a comment.
    // The Activity's onBackPressed will handle it for now.
}