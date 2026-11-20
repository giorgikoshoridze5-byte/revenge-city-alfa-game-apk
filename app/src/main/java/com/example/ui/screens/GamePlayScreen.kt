package com.example.ui.screens

import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.WeaponType
import com.example.engine.HudSnapshot
import com.example.engine.RevengeGlRenderer
import com.example.engine.WorldSimulation
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianAsphalt
import com.example.ui.theme.PoliceBlue
import com.example.ui.theme.RevengeGold
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    hud: HudSnapshot
) {
    val sim = viewModel.simulation

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Real-Time 3D OpenGL ES Viewport
        RevengeGlViewport(
            simulation = sim,
            isPaused = uiState.isPaused || uiState.activeModal != ActiveModal.NONE,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Camera Touch Orbit Overlay (does not block HUD/Joystick)
        if (!uiState.isPaused && uiState.cutscene == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            sim.orbitCamera(dragAmount.x, dragAmount.y)
                        }
                    }
            )
        }

        // 3. Active Gameplay HUD (hidden during cutscenes)
        if (uiState.cutscene == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP HUD ROW: Mini-map on left, Objective in center, Stats/Cash/Stars on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: Touchable GPS Mini-Map Radar + District + Active GPS Distance
                    Column(horizontalAlignment = Alignment.Start) {
                        MiniMapRadar(
                            hud = hud,
                            onOpenFullMap = { viewModel.openFullMap() }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = hud.districtName,
                            style = MaterialTheme.typography.labelSmall,
                            color = BoneWhite,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (hud.gpsLabel != null && hud.gpsDistanceMeters != null) {
                            Spacer(Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0A1929).copy(alpha = 0.85f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.openFullMap() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "GPS Route",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "${hud.gpsDistanceMeters}m • ${hud.gpsLabel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    // Right: Cash, Health, Armor, Wanted Stars, Time, FPS, Pause
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (uiState.settings.showFpsCounter) {
                                Text(
                                    text = "${hud.fps} FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = formatGameClock(hud.timeOfDayHours),
                                style = MaterialTheme.typography.labelSmall,
                                color = BoneWhite,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            IconButton(
                                onClick = { viewModel.togglePauseMenu() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .testTag("pause_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = BoneWhite, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Cash & Wanted Stars
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hud.wantedStars > 0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    repeat(3) { idx ->
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Wanted",
                                            tint = if (idx < hud.wantedStars) PoliceBlue else MutedSteel.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$${hud.cash}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = CashEmerald,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }

                        // Health & Armor bars
                        Column(
                            modifier = Modifier
                                .width(122.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatBar(progress = hud.health / 100f, color = CashEmerald)
                            StatBar(progress = hud.armor / 100f, color = NeonCyan)
                        }

                        // Equipped Weapon Chip (tap to cycle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(1.dp, RevengeGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.cycleEquippedWeapon() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("weapon_cycle_chip")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Weapon", tint = RevengeGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(hud.equippedWeapon.displayName, style = MaterialTheme.typography.labelSmall, color = BoneWhite)
                        }
                    }
                }

                // CENTER MISSION OBJECTIVE OR TOAST BANNER
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hud.activeObjectiveText != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.76f))
                                .border(1.dp, RevengeGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${hud.activeMissionTitle ?: ""} • ${hud.activeStageTitle ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RevengeGold
                            )
                            Text(
                                text = hud.activeObjectiveText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BoneWhite
                            )
                            hud.missionTimerSec?.let { t ->
                                Text(
                                    text = "TIME LEFT: ${t.toInt()}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (t < 15f) CrimsonAlert else CashEmerald
                                )
                            }
                        }
                    }

                    uiState.toastMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelLarge,
                            color = ObsidianAsphalt,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RevengeGold)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // BOTTOM SECTION: Speedometer/Prompt stacked ABOVE Portrait-Safe Control Clusters
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vehicle Speedometer & Nearby Interactable Prompt (stacked above so left/right controls never clip in portrait)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hud.inVehicle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${hud.speedKmh} KM/H", style = MaterialTheme.typography.labelLarge, color = BoneWhite)
                            }
                        }

                        hud.nearbyInteractable?.let { near ->
                            if (hud.inVehicle) Spacer(Modifier.width(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RevengeGold)
                                    .clickable { viewModel.triggerNearbyInteraction() }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                                    .testTag("interact_button")
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = near.actionLabel, tint = ObsidianAsphalt, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(near.actionLabel.uppercase(), style = MaterialTheme.typography.labelLarge, color = ObsidianAsphalt)
                                    Text(near.label, style = MaterialTheme.typography.labelSmall, color = ObsidianAsphalt.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    // BOTTOM CONTROLS ROW: Portrait-fit Car Pedals (LEFT/RIGHT + BRAKE/GAS) vs On-Foot (Joystick + Toggle Weapon/Run/Attack)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left Control Cluster
                        if (hud.inVehicle) {
                            // CAR MODE: Left / Right Steering Arrows
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                TouchHoldButton(
                                    label = "LEFT",
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    color = NeonCyan,
                                    sizeDp = 60,
                                    onPressChanged = { sim.steerLeft = it }
                                )
                                TouchHoldButton(
                                    label = "RIGHT",
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    color = NeonCyan,
                                    sizeDp = 60,
                                    onPressChanged = { sim.steerRight = it }
                                )
                            }
                        } else {
                            // ON-FOOT MODE: 360° Touch Movement Controller
                            VirtualJoystick(
                                onMove = { x, y ->
                                    sim.joyX = x
                                    sim.joyY = y
                                }
                            )
                        }

                        // Right Control Cluster: Car Forward/Brake Pedals vs On-Foot Holster/Run/Attack
                        if (hud.inVehicle) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TouchHoldButton(
                                        label = "HORN",
                                        icon = Icons.Default.Campaign,
                                        color = RevengeGold,
                                        sizeDp = 48,
                                        onPressChanged = { pressed -> if (pressed) sim.soundHorn() }
                                    )
                                    TouchHoldButton(
                                        label = "DRIFT",
                                        icon = Icons.Default.DirectionsCar,
                                        color = NeonCyan,
                                        sizeDp = 48,
                                        onPressChanged = { sim.isHandbrake = it }
                                    )
                                    TouchHoldButton(
                                        label = "EXIT",
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        color = CrimsonAlert,
                                        sizeDp = 48,
                                        onPressChanged = { pressed -> if (pressed) sim.toggleVehicleEntry() }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TouchHoldButton(
                                        label = "BRAKE",
                                        icon = Icons.Default.KeyboardArrowDown,
                                        color = CrimsonAlert,
                                        sizeDp = 64,
                                        onPressChanged = { sim.isBrake = it }
                                    )
                                    TouchHoldButton(
                                        label = "GAS",
                                        icon = Icons.Default.KeyboardArrowUp,
                                        color = CashEmerald,
                                        sizeDp = 68,
                                        onPressChanged = { sim.isGas = it }
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (hud.canEnterVehicle) {
                                        TouchHoldButton(
                                            label = "CAR",
                                            icon = Icons.Default.DirectionsCar,
                                            color = NeonCyan,
                                            sizeDp = 48,
                                            onPressChanged = { pressed -> if (pressed) sim.toggleVehicleEntry() }
                                        )
                                    }
                                    TouchHoldButton(
                                        label = if (hud.equippedWeapon == WeaponType.FISTS) "DRAW" else "HOLSTER",
                                        icon = Icons.Default.SwapHoriz,
                                        color = NeonCyan,
                                        sizeDp = 50,
                                        onPressChanged = { pressed -> if (pressed) viewModel.toggleWeaponHolster() }
                                    )
                                    TouchHoldButton(
                                        label = "RUN",
                                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                        color = RevengeGold,
                                        sizeDp = 52,
                                        onPressChanged = { sim.isSprinting = it }
                                    )
                                }
                                TouchHoldButton(
                                    label = if (hud.equippedWeapon == WeaponType.FISTS) "PUNCH" else "ATTACK",
                                    icon = Icons.Default.GpsFixed,
                                    color = CrimsonAlert,
                                    sizeDp = 72,
                                    onPressChanged = { pressed ->
                                        sim.isFiring = pressed
                                        if (pressed) sim.triggerFireOnce()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Auto-Playing Movie Cutscene Overlay
        uiState.cutscene?.let { cs ->
            CutsceneOverlay(
                cutscene = cs,
                isSpeaking = uiState.isSpeaking,
                onSkip = { viewModel.skipCutscene() }
            )
        }

        // 5. Interactive Modals
        when (uiState.activeModal) {
            ActiveModal.WEAPON_SHOP -> WeaponShopModal(
                progress = uiState.progress,
                onBuyOrEquip = { viewModel.buyOrEquipWeapon(it) },
                onBuyArmor = { viewModel.buyArmorVest() },
                onClose = { viewModel.closeActiveModal() }
            )
            ActiveModal.CAR_STATION -> CarStationModal(
                progress = uiState.progress,
                onBuyOrSpawn = { viewModel.buyOrSpawnVehicle(it) },
                onRepair = { viewModel.repairVehicleAtStation() },
                onClose = { viewModel.closeActiveModal() }
            )
            ActiveModal.BUY_HOUSE -> uiState.selectedHouse?.let { h ->
                BuyHouseModal(
                    house = h,
                    progress = uiState.progress,
                    onBuyOrRest = { viewModel.buyOrRestAtHouse(it) },
                    onClose = { viewModel.closeActiveModal() }
                )
            }
            ActiveModal.NPC_DIALOGUE -> uiState.npcChat?.let { chat ->
                NpcDialogueModal(
                    chat = chat,
                    isSpeaking = uiState.isSpeaking,
                    onClose = { viewModel.closeActiveModal() }
                )
            }
            ActiveModal.FULL_MAP -> FullGpsMapModal(
                hud = hud,
                onSetGpsWaypoint = { x, z, label -> viewModel.setGpsWaypoint(x, z, label) },
                onClearGps = { viewModel.clearGpsWaypoint() },
                onClose = { viewModel.closeActiveModal() }
            )
            ActiveModal.PAUSE_MENU -> PauseMenuModal(
                progress = uiState.progress,
                streetLightsEnabled = uiState.settings.streetLightsEnabled,
                onToggleStreetLights = { viewModel.setStreetLightsEnabled(it) },
                onResume = { viewModel.closeActiveModal() },
                onStartMission = { viewModel.startMissionFromMenu(it) },
                onOpenSettings = { viewModel.openSettings() },
                onReturnMainMenu = { viewModel.returnToMainMenu() }
            )
            ActiveModal.MISSION_PASSED -> MissionOutcomeOverlay(
                isSuccess = true,
                isWasted = false,
                title = uiState.lastMissionTitle,
                detail = "REWARD EARNED: +$300",
                onContinue = { viewModel.closeActiveModal() }
            )
            ActiveModal.MISSION_FAILED -> MissionOutcomeOverlay(
                isSuccess = false,
                isWasted = false,
                title = "Objective Failed",
                detail = uiState.lastMissionFailReason,
                onContinue = { viewModel.closeActiveModal() }
            )
            ActiveModal.WASTED -> MissionOutcomeOverlay(
                isSuccess = false,
                isWasted = true,
                title = "George Nikoladze was downed",
                detail = "Hospital recovery completed. Back on the streets.",
                onContinue = { viewModel.handleWastedRespawn() }
            )
            ActiveModal.NONE -> {}
        }
    }
}

@Composable
private fun RevengeGlViewport(
    simulation: WorldSimulation,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    var wasPaused by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                setRenderer(RevengeGlRenderer(simulation))
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        update = { view ->
            if (isPaused && !wasPaused) {
                view.onPause()
                wasPaused = true
            } else if (!isPaused && wasPaused) {
                view.onResume()
                wasPaused = false
            }
        },
        onRelease = { view ->
            view.onPause()
        }
    )
}

@Composable
private fun MiniMapRadar(
    hud: HudSnapshot,
    onOpenFullMap: () -> Unit
) {
    val radarSize = 106.dp
    Box(
        modifier = Modifier
            .size(radarSize)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A101A).copy(alpha = 0.88f))
            .border(2.dp, if (hud.gpsTargetX != null) NeonCyan else RevengeGold.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .clickable { onOpenFullMap() }
            .testTag("minimap_button")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val scale = size.width / 195f // shows +/- ~97m radius around George

            fun toScreen(wx: Float, wz: Float): Offset {
                val dx = (wx - hud.playerX) * scale
                val dz = (wz - hud.playerZ) * scale
                return Offset(cx + dx, cy + dz)
            }

            // Green park districts hint
            drawRect(
                color = Color(0xFF173828).copy(alpha = 0.45f),
                topLeft = Offset(0f, 0f),
                size = size
            )

            // Mtkvari River ribbon
            val riverX = cx + (0f - hud.playerX) * scale
            drawRect(
                color = Color(0xFF1B4D78).copy(alpha = 0.82f),
                topLeft = Offset(riverX - 6f * scale, 0f),
                size = Size(12f * scale, size.height)
            )

            // Main Avenues grid
            val avenues = listOf(-120f, -60f, 60f, 120f)
            for (ax in avenues) {
                val sx = cx + (ax - hud.playerX) * scale
                drawLine(Color(0xFF364559), Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 3f)
            }
            for (az in listOf(-120f, -60f, 0f, 60f, 120f)) {
                val sy = cy + (az - hud.playerZ) * scale
                drawLine(Color(0xFF364559), Offset(0f, sy), Offset(size.width, sy), strokeWidth = 3f)
            }

            // Subtle radar range ring
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = size.width * 0.36f,
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )

            // Active GPS Route Path (glowing cyan street-grid dashed path)
            val gx = hud.gpsTargetX
            val gz = hud.gpsTargetZ
            if (gx != null && gz != null) {
                val rawTarget = toScreen(gx, gz)
                val targetClamped = Offset(
                    rawTarget.x.coerceIn(7f, size.width - 7f),
                    rawTarget.y.coerceIn(7f, size.height - 7f)
                )
                val corner = Offset(targetClamped.x, cy)
                val gpsPath = Path().apply {
                    moveTo(cx, cy)
                    lineTo(corner.x, corner.y)
                    lineTo(targetClamped.x, targetClamped.y)
                }
                drawPath(
                    path = gpsPath,
                    color = NeonCyan.copy(alpha = 0.28f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = gpsPath,
                    color = NeonCyan,
                    style = Stroke(
                        width = 2.2f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                )
                drawCircle(NeonCyan.copy(alpha = 0.32f), radius = 8f, center = targetClamped)
                drawCircle(NeonCyan, radius = 4f, center = targetClamped)
            }

            // World Dots
            for (dot in hud.minimapDots) {
                val pt = toScreen(dot.x, dot.z)
                val clamped = Offset(
                    pt.x.coerceIn(6f, size.width - 6f),
                    pt.y.coerceIn(6f, size.height - 6f)
                )
                val col = when (dot.kind) {
                    "MISSION" -> RevengeGold
                    "MISSION_DONE" -> CashEmerald.copy(alpha = 0.55f)
                    "WEAPON" -> Color(0xFFFF9800)
                    "CAR" -> NeonCyan
                    "OWNED_HOUSE" -> CashEmerald
                    "HOUSE" -> Color(0xFFA7F3D0)
                    "ENEMY" -> CrimsonAlert
                    "POLICE" -> PoliceBlue
                    else -> BoneWhite.copy(alpha = 0.75f)
                }
                val r = if (dot.kind == "MISSION") 5.5f else 3.8f
                drawCircle(col, radius = r, center = clamped)
            }

            // Player directional chevron at center
            val deg = (hud.playerYaw * 180f / PI.toFloat())
            rotate(degrees = 180f - deg, pivot = Offset(cx, cy)) {
                val path = Path().apply {
                    moveTo(cx, cy - 8f)
                    lineTo(cx + 6f, cy + 6f)
                    lineTo(cx, cy + 2f)
                    lineTo(cx - 6f, cy + 6f)
                    close()
                }
                drawPath(path, color = BoneWhite)
            }
        }
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = RevengeGold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(Icons.Default.Map, contentDescription = "Open GPS Map", tint = NeonCyan, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                text = "GPS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = NeonCyan
            )
        }
    }
}

@Composable
private fun StatBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit
) {
    val outerDp = 110.dp
    val knobDp = 44.dp
    val maxRadiusPx = with(LocalDensity.current) { ((outerDp - knobDp) / 2).toPx() }
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose { onMove(0f, 0f) }
    }

    Box(
        modifier = Modifier
            .size(outerDp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    try {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        fun updateFromPos(pos: Offset) {
                            val dx = pos.x - center.x
                            val dy = pos.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val clamped = dist.coerceAtMost(maxRadiusPx)
                            val nx = if (dist > 0.001f) (dx / dist) * clamped else 0f
                            val ny = if (dist > 0.001f) (dy / dist) * clamped else 0f
                            knobX = nx
                            knobY = ny
                            onMove((nx / maxRadiusPx).coerceIn(-1f, 1f), (ny / maxRadiusPx).coerceIn(-1f, 1f))
                        }
                        updateFromPos(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            updateFromPos(change.position)
                        }
                    } finally {
                        knobX = 0f
                        knobY = 0f
                        onMove(0f, 0f)
                    }
                }
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size(knobDp)
                .clip(CircleShape)
                .background(RevengeGold.copy(alpha = 0.85f))
                .border(2.dp, BoneWhite, CircleShape)
        )
    }
}

@Composable
private fun TouchHoldButton(
    label: String,
    icon: ImageVector,
    color: Color,
    sizeDp: Int,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    DisposableEffect(label) {
        onDispose {
            if (pressed) {
                pressed = false
                onPressChanged(false)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (pressed) color.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.58f))
            .border(2.dp, color, CircleShape)
            .pointerInput(label) {
                awaitEachGesture {
                    try {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        pressed = true
                        onPressChanged(true)
                        while (true) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            ch.consume()
                        }
                    } finally {
                        pressed = false
                        onPressChanged(false)
                    }
                }
            }
            .testTag("btn_${label.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = if (pressed) ObsidianAsphalt else color, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (pressed) ObsidianAsphalt else BoneWhite
            )
        }
    }
}

private fun formatGameClock(hours: Float): String {
    val totalMin = ((hours % 24f) * 60f).toInt().coerceIn(0, 1439)
    val h = totalMin / 60
    val m = totalMin % 60
    return "%02d:%02d".format(h, m)
}
