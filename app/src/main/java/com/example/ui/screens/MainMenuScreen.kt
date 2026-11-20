package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.data.MissionDefinition
import com.example.data.PlayerProgressEntity
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianAsphalt
import com.example.ui.theme.RevengeGold
import kotlinx.coroutines.delay

@Composable
fun MainMenuScreen(
    progress: PlayerProgressEntity,
    streetLightsEnabled: Boolean,
    onToggleStreetLights: (Boolean) -> Unit,
    onEnterOpenWorld: () -> Unit,
    onStartMission: (MissionDefinition) -> Unit,
    onOpenSettings: () -> Unit
) {
    val slides = GameCatalog.loadingSlides
    var bgIndex by remember { mutableIntStateOf(0) }
    var showMissionSelect by remember { mutableStateOf(false) }
    var showControlsGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            bgIndex = (bgIndex + 1) % slides.size
        }
    }

    val completedSet = remember(progress.completedMissionsCsv) {
        progress.completedMissionsCsv.split(",").filter { it.isNotBlank() }.toSet()
    }
    val ownedHousesCount = remember(progress.ownedHousesCsv) {
        progress.ownedHousesCsv.split(",").count { it.isNotBlank() }
    }
    val ownedWeaponsCount = remember(progress.ownedWeaponsCsv) {
        progress.ownedWeaponsCsv.split(",").count { it.isNotBlank() }
    }
    val ownedCarsCount = remember(progress.ownedVehiclesCsv) {
        progress.ownedVehiclesCsv.split(",").count { it.isNotBlank() }
    }

    val pulseTransition = rememberInfiniteTransition(label = "menu_glow")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianAsphalt)
    ) {
        Image(
            painter = painterResource(id = slides[bgIndex].imageRes),
            contentDescription = "Revenge City Artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.42f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF07090F).copy(alpha = 0.90f),
                        0.45f to Color(0xFF0A0F1A).copy(alpha = 0.65f),
                        1.0f to ObsidianAsphalt.copy(alpha = 0.98f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header + Live Hero Dossier Banner
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REVENGE CITY",
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = BoneWhite,
                                letterSpacing = 2.2.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CashEmerald.copy(alpha = glowAlpha))
                            )
                            Text(
                                text = "OPEN WORLD • GEORGE NIKOLADZE",
                                style = MaterialTheme.typography.labelSmall.copy(color = RevengeGold, letterSpacing = 1.2.sp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(CashEmerald.copy(alpha = 0.25f), Color(0xFF064E3B).copy(alpha = 0.55f))))
                            .border(1.5.dp, CashEmerald.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "$${progress.cash}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CashEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Cooler Cinematic Hero Banner Card with Artwork Backdrop + Stats
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, RevengeGold.copy(alpha = glowAlpha * 0.75f), RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = slides[bgIndex].imageRes),
                        contentDescription = slides[bgIndex].title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.20f),
                                    0.55f to Color.Black.copy(alpha = 0.58f),
                                    1.0f to ObsidianAsphalt.copy(alpha = 0.96f)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = slides[bgIndex].title,
                                style = MaterialTheme.typography.labelLarge.copy(color = RevengeGold),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "${completedSet.size}/6 MISSIONS",
                                style = MaterialTheme.typography.labelSmall.copy(color = CashEmerald),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GEORGE NIKOLADZE",
                                style = MaterialTheme.typography.titleLarge.copy(color = BoneWhite, fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatPill(Icons.Default.Home, "$ownedHousesCount/4 Houses", CashEmerald)
                                StatPill(Icons.Default.DirectionsCar, "$ownedCarsCount Cars", NeonCyan)
                                StatPill(Icons.Default.Star, "$ownedWeaponsCount Guns", RevengeGold)
                            }
                        }
                    }
                }
            }

            // Center Dynamic Panel (either Mission Select, Controls Guide, or Main Actions)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showMissionSelect -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STORY MISSIONS ($300 EACH)",
                                    style = MaterialTheme.typography.headlineMedium.copy(color = RevengeGold)
                                )
                                OutlinedButton(onClick = { showMissionSelect = false }) {
                                    Text("Back", color = BoneWhite)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(GameCatalog.missions) { m ->
                                    val done = completedSet.contains(m.id)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (done) CashEmerald.copy(alpha = 0.5f) else HairlineBorder,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onStartMission(m) }
                                            .testTag("mission_card_${m.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (done) CashEmerald.copy(alpha = 0.2f) else RevengeGold.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (done) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = CashEmerald)
                                                } else {
                                                    Text("M${m.number}", color = RevengeGold, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(m.title, style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(m.district, style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("+$${m.rewardCash}", style = MaterialTheme.typography.labelLarge, color = CashEmerald)
                                                Text("PLAY", style = MaterialTheme.typography.labelSmall, color = RevengeGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    showControlsGuide -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CONTROLS & CITY GUIDE", style = MaterialTheme.typography.headlineMedium, color = RevengeGold)
                                    OutlinedButton(onClick = { showControlsGuide = false }) {
                                        Text("Close", color = BoneWhite)
                                    }
                                }
                                GuideRow("Left Joystick", "Move George Nikoladze or steer vehicles")
                                GuideRow("Swipe Screen", "Orbit 3D camera around George or car")
                                GuideRow("Car / Exit Button", "Steal or enter any parked/traffic car")
                                GuideRow("Gold Markers (M)", "Start voiced missions in 6 districts ($300 each)")
                                GuideRow("Amber / Blue Bays", "Rustaveli Armory (Guns) & Tbilisi Motors (Cars)")
                                GuideRow("Green House Pads", "Buy 4 city houses, heal 100%, & spawn cars")
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onEnterOpenWorld,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("play_open_world_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RevengeGold,
                                    contentColor = ObsidianAsphalt
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (completedSet.isEmpty()) "START STORY & OPEN WORLD" else "RESUME REVENGE CITY",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = { showMissionSelect = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("mission_select_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CardSurface,
                                    contentColor = BoneWhite
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = CashEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text("MISSIONS & DISTRICTS (6 × $300)", style = MaterialTheme.typography.titleMedium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("main_menu_settings_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = RevengeGold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Settings", color = BoneWhite)
                                }

                                OutlinedButton(
                                    onClick = { showControlsGuide = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("controls_guide_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonCyan)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Controls", color = BoneWhite)
                                }
                            }

                            // Saved Street Lights Power-Saving / Visual Toggle in Menu
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.90f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = "Street Lights",
                                            tint = if (streetLightsEnabled) RevengeGold else MutedSteel
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Street Lights",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BoneWhite
                                            )
                                            Text(
                                                text = if (streetLightsEnabled) "Saved: ON (Full night glow)" else "Saved: OFF (Power & FPS saving)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (streetLightsEnabled) CashEmerald else MutedSteel
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = streetLightsEnabled,
                                        onCheckedChange = onToggleStreetLights,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianAsphalt,
                                            checkedTrackColor = RevengeGold
                                        ),
                                        modifier = Modifier.testTag("menu_street_lights_switch")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !showMissionSelect && !showControlsGuide) {
                Text(
                    text = "Day-Night Cycle • Physical Vehicles & Props • Voiced Cutscenes",
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSteel),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianAsphalt.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BoneWhite,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun GuideRow(key: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.labelLarge, color = CashEmerald, modifier = Modifier.weight(0.38f))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = BoneWhite, modifier = Modifier.weight(0.62f))
    }
}
