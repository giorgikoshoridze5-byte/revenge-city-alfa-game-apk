package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameSettings
import com.example.data.GraphicsQuality
import com.example.data.TimeCycleSpeed
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianAsphalt
import com.example.ui.theme.RevengeGold

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onGraphicsChange: (GraphicsQuality) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceToggle: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onTimeSpeedChange: (TimeCycleSpeed) -> Unit,
    onShowFpsToggle: (Boolean) -> Unit,
    onDynamicCamToggle: (Boolean) -> Unit,
    onStreetLightsToggle: (Boolean) -> Unit,
    onResetProgress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianAsphalt)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BoneWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("GAME & ENGINE SETTINGS", style = MaterialTheme.typography.headlineMedium, color = RevengeGold)
        }

        // Graphics Rendering Optimization
        SettingsCard(title = "3D RENDERING & PERFORMANCE") {
            Text(
                "Optimize draw distance, traffic density, and frame pacing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSteel
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GraphicsQuality.entries.forEach { q ->
                    FilterChip(
                        selected = settings.graphicsQuality == q,
                        onClick = { onGraphicsChange(q) },
                        label = { Text(q.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RevengeGold,
                            selectedLabelColor = ObsidianAsphalt,
                            containerColor = ObsidianAsphalt,
                            labelColor = BoneWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow("Show Real-Time FPS Counter", settings.showFpsCounter, onShowFpsToggle)
            ToggleRow("Dynamic Driving Camera Follow", settings.dynamicCameraTilt, onDynamicCamToggle)
            ToggleRow("Street Lights (Off = Save Power & Boost FPS)", settings.streetLightsEnabled, onStreetLightsToggle)
        }

        // Audio & Voicing
        SettingsCard(title = "AUDIO, MUSIC & VOICE ACTING") {
            SliderRow("Music & Loading Score", settings.musicVolume, onMusicVolumeChange)
            SliderRow("Weapons, Cars & SFX", settings.sfxVolume, onSfxVolumeChange)
            ToggleRow("Character Speech Voicing (TTS)", settings.voiceActingEnabled, onVoiceToggle)
        }

        // Touch & Day-Night Cycle
        SettingsCard(title = "CONTROLS & OPEN WORLD") {
            SliderRow("Camera Touch Orbit Sensitivity", (settings.touchSensitivity - 0.4f) / 1.4f) { norm ->
                onSensitivityChange(0.4f + norm * 1.4f)
            }
            Spacer(Modifier.height(6.dp))
            Text("Day-Night Cycle Speed", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeCycleSpeed.entries.forEach { spd ->
                    FilterChip(
                        selected = settings.timeCycleSpeed == spd,
                        onClick = { onTimeSpeedChange(spd) },
                        label = { Text(spd.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CashEmerald,
                            selectedLabelColor = ObsidianAsphalt,
                            containerColor = ObsidianAsphalt,
                            labelColor = BoneWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Management
        SettingsCard(title = "SAVE DATA") {
            Button(
                onClick = onResetProgress,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = BoneWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_progress_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reset Story Missions & Assets", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = RevengeGold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = BoneWhite)
            Text("${(value.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = CashEmerald)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = RevengeGold,
                activeTrackColor = CashEmerald
            )
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = BoneWhite, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianAsphalt,
                checkedTrackColor = CashEmerald
            )
        )
    }
}
