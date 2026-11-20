package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RevengeCityTheme
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RevengeCityTheme {
                RevengeCityApp()
            }
        }
    }
}

@Composable
fun RevengeCityApp(viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.screen != AppScreen.LOADING && uiState.screen != AppScreen.MAIN_MENU) {
        when (uiState.screen) {
            AppScreen.SETTINGS -> viewModel.closeSettingsToMenu()
            AppScreen.PLAYING -> {
                if (uiState.activeModal != ActiveModal.NONE) {
                    viewModel.closeActiveModal()
                } else {
                    viewModel.togglePauseMenu()
                }
            }
            else -> {}
        }
    }

    Crossfade(targetState = uiState.screen, label = "screen_transition") { screen ->
        when (screen) {
            AppScreen.LOADING -> LoadingScreen(
                progress = uiState.loadingProgress,
                loadingComplete = uiState.loadingComplete,
                musicVolume = uiState.settings.musicVolume,
                onToggleMute = {
                    val next = if (uiState.settings.musicVolume > 0.05f) 0f else 0.75f
                    viewModel.setMusicVolume(next)
                },
                onContinueToMenu = { viewModel.enterMainMenuFromLoading() }
            )

            AppScreen.MAIN_MENU -> MainMenuScreen(
                progress = uiState.progress,
                streetLightsEnabled = uiState.settings.streetLightsEnabled,
                onToggleStreetLights = { viewModel.setStreetLightsEnabled(it) },
                onEnterOpenWorld = { viewModel.enterOpenWorld() },
                onStartMission = { mission -> viewModel.startMissionFromMenu(mission) },
                onOpenSettings = { viewModel.openSettings() }
            )

            AppScreen.SETTINGS -> SettingsScreen(
                settings = uiState.settings,
                onBack = { viewModel.closeSettingsToMenu() },
                onGraphicsChange = { viewModel.setGraphicsQuality(it) },
                onMusicVolumeChange = { viewModel.setMusicVolume(it) },
                onSfxVolumeChange = { viewModel.setSfxVolume(it) },
                onVoiceToggle = { viewModel.setVoiceEnabled(it) },
                onSensitivityChange = { viewModel.setTouchSensitivity(it) },
                onTimeSpeedChange = { viewModel.setTimeSpeed(it) },
                onShowFpsToggle = { viewModel.setShowFps(it) },
                onDynamicCamToggle = { viewModel.setDynamicCam(it) },
                onStreetLightsToggle = { viewModel.setStreetLightsEnabled(it) },
                onResetProgress = { viewModel.resetProgress() }
            )

            AppScreen.PLAYING -> GamePlayScreen(
                viewModel = viewModel,
                uiState = uiState,
                hud = hudState
            )
        }
    }
}
