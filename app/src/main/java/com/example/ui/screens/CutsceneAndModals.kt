package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import com.example.data.BuyableHouse
import com.example.data.DialogueEmotion
import com.example.data.GameCatalog
import com.example.data.MissionDefinition
import com.example.data.PlayerProgressEntity
import com.example.data.VehicleModel
import com.example.data.WeaponType
import com.example.engine.HudSnapshot
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianAsphalt
import com.example.ui.theme.RevengeGold
import com.example.viewmodel.CutsceneState
import com.example.viewmodel.NpcChatState

@Composable
fun TalkingCharacterFace(
    speakerName: String,
    isFemale: Boolean,
    isSpeaking: Boolean,
    currentText: String,
    emotion: DialogueEmotion = DialogueEmotion.NEUTRAL,
    modifier: Modifier = Modifier
) {
    var targetOpen by remember { mutableFloatStateOf(0f) }
    var targetWide by remember { mutableFloatStateOf(0.5f) }
    var isBlinking by remember { mutableStateOf(false) }

    // Synchronize lip movement to characters/vowels in currentText while voice is speaking
    LaunchedEffect(isSpeaking, currentText) {
        if (!isSpeaking || currentText.isBlank()) {
            targetOpen = 0f
            targetWide = 0.5f
            return@LaunchedEffect
        }
        var idx = 0
        while (isActive && isSpeaking) {
            val ch = currentText[idx % currentText.length].lowercaseChar()
            when (ch) {
                'a', 'e', 'i', 'o', 'u', 'y' -> {
                    targetOpen = when (ch) {
                        'a' -> 0.95f
                        'o' -> 0.85f
                        'e' -> 0.70f
                        'i' -> 0.52f
                        else -> 0.62f
                    }
                    targetWide = when (ch) {
                        'o', 'u' -> 0.25f
                        'e', 'i' -> 0.92f
                        else -> 0.65f
                    }
                }
                ' ', ',', '.', '!', '?', '-', ';' -> {
                    targetOpen = 0.06f
                    targetWide = 0.45f
                }
                else -> {
                    targetOpen = 0.34f
                    targetWide = 0.58f
                }
            }
            idx++
            delay(75L)
        }
        targetOpen = 0f
    }

    // Subtle natural eye blinking
    LaunchedEffect(speakerName) {
        while (isActive) {
            delay(2600L + (abs(speakerName.hashCode()) % 1400).toLong())
            isBlinking = true
            delay(130L)
            isBlinking = false
        }
    }

    val mouthOpen by animateFloatAsState(targetValue = targetOpen, animationSpec = tween(65), label = "mouthOpen")
    val mouthWide by animateFloatAsState(targetValue = targetWide, animationSpec = tween(75), label = "mouthWide")
    val eyeScaleY by animateFloatAsState(targetValue = if (isBlinking) 0.12f else 1f, animationSpec = tween(80), label = "blink")

    val isGeorge = speakerName.contains("George", ignoreCase = true)
    val isVillain = speakerName.contains("Vakhtang", ignoreCase = true) || speakerName.contains("Boss", ignoreCase = true)
    val seed = abs(speakerName.hashCode())

    val hairColor = when {
        isGeorge -> Color(0xFF1E1A18)
        isVillain -> Color(0xFF2B221D)
        isFemale && seed % 3 == 0 -> Color(0xFF4A2511)
        isFemale && seed % 3 == 1 -> Color(0xFF1A1517)
        isFemale -> Color(0xFF6B3E26)
        seed % 2 == 0 -> Color(0xFF201C1A)
        else -> Color(0xFF3E2B1E)
    }
    val skinBase = if (isFemale) Color(0xFFF3C6A5) else Color(0xFFE5B18C)
    val skinShadow = if (isFemale) Color(0xFFD99E7B) else Color(0xFFC58C68)
    val jacketColor = when {
        isGeorge -> Color(0xFF5A3921)
        isVillain -> Color(0xFF3A1A1A)
        isFemale -> Color(0xFF9C2C58)
        else -> Color(0xFF283747)
    }
    val borderColor = if (isGeorge) RevengeGold else if (isFemale) Color(0xFFF472B6) else NeonCyan

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(borderColor.copy(alpha = 0.28f), ObsidianAsphalt)
                )
            )
            .border(2.dp, if (isSpeaking) borderColor else borderColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // Shoulders & Jacket Collar
            drawRoundRect(
                color = jacketColor,
                topLeft = Offset(w * 0.12f, h * 0.74f),
                size = Size(w * 0.76f, h * 0.30f),
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
            )
            // Neck
            drawRoundRect(
                color = skinShadow,
                topLeft = Offset(cx - w * 0.11f, h * 0.62f),
                size = Size(w * 0.22f, h * 0.18f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Back long hair for female characters
            if (isFemale) {
                drawRoundRect(
                    color = hairColor,
                    topLeft = Offset(w * 0.16f, h * 0.15f),
                    size = Size(w * 0.68f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
                )
            }

            // Ears
            drawOval(
                color = skinShadow,
                topLeft = Offset(w * 0.20f, h * 0.40f),
                size = Size(w * 0.08f, h * 0.14f)
            )
            drawOval(
                color = skinShadow,
                topLeft = Offset(w * 0.72f, h * 0.40f),
                size = Size(w * 0.08f, h * 0.14f)
            )

            // Face / Jaw contour
            val facePath = Path().apply {
                moveTo(w * 0.25f, h * 0.28f)
                quadraticTo(cx, h * 0.14f, w * 0.75f, h * 0.28f)
                lineTo(w * 0.74f, h * 0.50f)
                quadraticTo(w * 0.70f, h * 0.66f, cx, h * (if (isFemale) 0.69f else 0.71f))
                quadraticTo(w * 0.30f, h * 0.66f, w * 0.26f, h * 0.50f)
                close()
            }
            drawPath(facePath, color = skinBase)

            // Cheek shading
            drawCircle(
                color = if (isFemale) Color(0x33EC4899) else skinShadow.copy(alpha = 0.25f),
                radius = w * 0.075f,
                center = Offset(w * 0.33f, h * 0.49f)
            )
            drawCircle(
                color = if (isFemale) Color(0x33EC4899) else skinShadow.copy(alpha = 0.25f),
                radius = w * 0.075f,
                center = Offset(w * 0.67f, h * 0.49f)
            )

            // Top & Side Hair
            val topHair = Path().apply {
                moveTo(w * 0.22f, h * 0.35f)
                quadraticTo(w * 0.24f, h * 0.10f, cx, h * 0.11f)
                quadraticTo(w * 0.76f, h * 0.10f, w * 0.78f, h * 0.35f)
                quadraticTo(w * 0.64f, h * (if (isFemale) 0.22f else 0.26f), cx, h * 0.23f)
                quadraticTo(w * 0.36f, h * (if (isFemale) 0.22f else 0.26f), w * 0.22f, h * 0.35f)
                close()
            }
            drawPath(topHair, color = hairColor)

            // Eyebrows (express emotion + lift slightly while speaking)
            val browLift = (if (isSpeaking) -h * 0.015f else 0f) + when (emotion) {
                DialogueEmotion.HAPPY -> -h * 0.018f
                DialogueEmotion.SURPRISED -> -h * 0.030f
                else -> 0f
            }
            val browTilt = when (emotion) {
                DialogueEmotion.ANGRY, DialogueEmotion.SERIOUS -> h * 0.022f
                DialogueEmotion.HAPPY -> -h * 0.008f
                else -> 0f
            }
            drawLine(
                color = hairColor,
                start = Offset(w * 0.31f, h * 0.35f + browLift - browTilt),
                end = Offset(w * 0.44f, h * 0.34f + browLift + browTilt),
                strokeWidth = w * 0.032f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = hairColor,
                start = Offset(w * 0.56f, h * 0.34f + browLift + browTilt),
                end = Offset(w * 0.69f, h * 0.35f + browLift - browTilt),
                strokeWidth = w * 0.032f,
                cap = StrokeCap.Round
            )

            // Expressive Eyes with blinking & highlights
            val eyeEmotionScale = if (emotion == DialogueEmotion.SURPRISED) 1.22f else 1f
            val eyeH = (h * 0.068f * eyeScaleY * eyeEmotionScale).coerceAtLeast(2f)
            val eyeY = h * 0.415f - eyeH * 0.5f
            val eyeW = w * 0.105f
            val leftEyeX = w * 0.335f
            val rightEyeX = w * 0.560f

            // Sclera
            drawOval(color = Color.White, topLeft = Offset(leftEyeX, eyeY), size = Size(eyeW, eyeH))
            drawOval(color = Color.White, topLeft = Offset(rightEyeX, eyeY), size = Size(eyeW, eyeH))
            if (!isBlinking) {
                val irisColor = if (isGeorge) Color(0xFF2B6CB0) else if (isFemale) Color(0xFF3F6212) else Color(0xFF3B2F2F)
                drawCircle(color = irisColor, radius = w * 0.030f, center = Offset(leftEyeX + eyeW * 0.5f, h * 0.415f))
                drawCircle(color = irisColor, radius = w * 0.030f, center = Offset(rightEyeX + eyeW * 0.5f, h * 0.415f))
                drawCircle(color = Color(0xFF0A0A0A), radius = w * 0.016f, center = Offset(leftEyeX + eyeW * 0.5f, h * 0.415f))
                drawCircle(color = Color(0xFF0A0A0A), radius = w * 0.016f, center = Offset(rightEyeX + eyeW * 0.5f, h * 0.415f))
                drawCircle(color = Color.White, radius = w * 0.009f, center = Offset(leftEyeX + eyeW * 0.38f, h * 0.405f))
                drawCircle(color = Color.White, radius = w * 0.009f, center = Offset(rightEyeX + eyeW * 0.38f, h * 0.405f))
            }
            if (isFemale) {
                drawLine(Color(0xFF18181B), Offset(leftEyeX - 2f, h * 0.39f), Offset(leftEyeX + eyeW + 2f, h * 0.39f), strokeWidth = 2.5f)
                drawLine(Color(0xFF18181B), Offset(rightEyeX - 2f, h * 0.39f), Offset(rightEyeX + eyeW + 2f, h * 0.39f), strokeWidth = 2.5f)
            }

            // Nose
            val nosePath = Path().apply {
                moveTo(cx, h * 0.42f)
                lineTo(cx - w * 0.032f, h * 0.515f)
                lineTo(cx + w * 0.032f, h * 0.515f)
            }
            drawPath(nosePath, color = skinShadow, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Beard / Goatee (George & Villain)
            if (isGeorge) {
                val goatee = Path().apply {
                    moveTo(cx - w * 0.04f, h * 0.635f)
                    lineTo(cx + w * 0.04f, h * 0.635f)
                    lineTo(cx, h * 0.69f)
                    close()
                }
                drawPath(goatee, color = hairColor)
            } else if (isVillain) {
                drawOval(
                    color = hairColor.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.32f, h * 0.55f),
                    size = Size(w * 0.36f, h * 0.16f)
                )
            }

            // Animated Mouth synced to spoken text & emotion!
            val mw = w * (0.11f + mouthWide * 0.10f)
            val mh = (h * (0.015f + mouthOpen * 0.085f)).coerceAtLeast(3f)
            val mouthTop = h * 0.585f - mh * 0.45f
            val lipColor = if (isFemale) Color(0xFFB91C4A) else Color(0xFF8C3B3B)

            if (mouthOpen > 0.15f) {
                drawOval(
                    color = Color(0xFF2B0A0D),
                    topLeft = Offset(cx - mw * 0.5f, mouthTop),
                    size = Size(mw, mh)
                )
                drawRoundRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset(cx - mw * 0.34f, mouthTop + mh * 0.10f),
                    size = Size(mw * 0.68f, mh * 0.24f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawOval(
                    color = Color(0xFFBE4B5C),
                    topLeft = Offset(cx - mw * 0.25f, mouthTop + mh * 0.58f),
                    size = Size(mw * 0.50f, mh * 0.34f)
                )
                drawOval(
                    color = lipColor,
                    topLeft = Offset(cx - mw * 0.5f, mouthTop),
                    size = Size(mw, mh),
                    style = Stroke(width = 3.2f)
                )
            } else if (emotion == DialogueEmotion.HAPPY) {
                // Warm smiling expression
                val smilePath = Path().apply {
                    moveTo(cx - mw * 0.58f, h * 0.578f)
                    quadraticTo(cx, h * 0.625f, cx + mw * 0.58f, h * 0.578f)
                }
                drawPath(smilePath, color = lipColor, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
            } else {
                drawLine(
                    color = lipColor,
                    start = Offset(cx - mw * 0.5f, h * 0.585f),
                    end = Offset(cx + mw * 0.5f, h * 0.585f),
                    strokeWidth = 4.2f,
                    cap = StrokeCap.Round
                )
            }
        }
        if (emotion == DialogueEmotion.PHONE_CALL) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(CashEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Phone Call", tint = ObsidianAsphalt, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
fun CutsceneOverlay(
    cutscene: CutsceneState,
    isSpeaking: Boolean,
    onSkip: () -> Unit
) {
    val line = cutscene.lines.getOrNull(cutscene.currentLineIndex) ?: return
    val infinite = rememberInfiniteTransition(label = "voice_wave")
    val wave by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Top cinematic letterbox bar (adapts properly below status bar/cutout on vertical screens)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.Black.copy(alpha = 0.94f))
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = if (cutscene.isOutro) "MISSION COMPLETE CUTSCENE" else "MISSION ${cutscene.mission.number}: ${cutscene.mission.title.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = RevengeGold
                    )
                    Text(
                        text = cutscene.mission.district,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteel
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("skip_cutscene_button")
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = BoneWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Skip", color = BoneWhite)
                }
            }
        }

        // Bottom cinematic letterbox bar + Animated Talking Portrait + Movie Subtitles
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.95f))
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TalkingCharacterFace(
                speakerName = line.speaker,
                isFemale = line.voiceProfile.isFemale,
                isSpeaking = isSpeaking,
                currentText = line.text,
                emotion = line.emotion,
                modifier = Modifier.size(84.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (line.emotion == DialogueEmotion.PHONE_CALL) Icons.Default.Call else Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice",
                        tint = if (isSpeaking) CashEmerald else MutedSteel,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (line.emotion == DialogueEmotion.PHONE_CALL) "${line.speaker.uppercase()} (PHONE)" else line.speaker.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (line.speaker.contains("George")) RevengeGold else if (line.voiceProfile.isFemale) Color(0xFFF472B6) else NeonCyan
                    )
                    Spacer(Modifier.width(10.dp))
                    if (isSpeaking) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                            listOf(0.5f, 1.0f, 0.7f).forEachIndexed { i, h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((12 * h * (if (i % 2 == 0) wave else 1.3f - wave * 0.5f)).dp)
                                        .background(CashEmerald, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\"${line.text}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BoneWhite
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scene ${cutscene.currentLineIndex + 1} / ${cutscene.lines.size} • ${line.emotion.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteel
                    )
                    Text(
                        text = if (isSpeaking) "SPEAKING • MOVIE AUTO" else "NEXT IN 2s...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSpeaking) CashEmerald else RevengeGold
                    )
                }
            }
        }
    }
}

@Composable
fun WeaponShopModal(
    progress: PlayerProgressEntity,
    onBuyOrEquip: (WeaponType) -> Unit,
    onBuyArmor: () -> Unit,
    onClose: () -> Unit
) {
    val owned = progress.ownedWeaponsCsv.split(",").toSet()
    ModalContainer(title = "RUSTAVELI ARMORY", subtitle = "Cash: $${progress.cash} • Armor: ${progress.armor}%", onClose = onClose) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianAsphalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NeonCyan)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Kevlar Tactical Vest", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                                Text("Restores 100% ballistic armor", style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                            }
                        }
                        Button(
                            onClick = onBuyArmor,
                            colors = ButtonDefaults.buttonColors(containerColor = CashEmerald, contentColor = ObsidianAsphalt)
                        ) {
                            Text("$200", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(WeaponType.entries) { w ->
                val isOwned = owned.contains(w.id)
                val isEquipped = progress.equippedWeapon == w.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianAsphalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isEquipped) RevengeGold else HairlineBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(w.displayName, style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                            Text(w.description, style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                            Text("DMG ${w.damage * w.pellets} • RNG ${w.range.toInt()}m", style = MaterialTheme.typography.labelSmall, color = RevengeGold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onBuyOrEquip(w) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEquipped) CashEmerald else if (isOwned) NeonCyan else RevengeGold,
                                contentColor = ObsidianAsphalt
                            ),
                            modifier = Modifier.testTag("weapon_btn_${w.id}")
                        ) {
                            Text(
                                text = when {
                                    isEquipped -> "EQUIPPED"
                                    isOwned -> "EQUIP"
                                    else -> "$${w.price}"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarStationModal(
    progress: PlayerProgressEntity,
    onBuyOrSpawn: (VehicleModel) -> Unit,
    onRepair: () -> Unit,
    onClose: () -> Unit
) {
    val owned = progress.ownedVehiclesCsv.split(",").toSet()
    ModalContainer(title = "TBILISI MOTORS STATION", subtitle = "Cash: $${progress.cash} • Buy & Spawn Outside", onClose = onClose) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianAsphalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Express Chassis Repair & Tune", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                            Text("Restores nearby car to 100% condition", style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                        }
                        Button(
                            onClick = onRepair,
                            colors = ButtonDefaults.buttonColors(containerColor = CashEmerald, contentColor = ObsidianAsphalt)
                        ) {
                            Text("$50", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(VehicleModel.entries) { m ->
                val isOwned = owned.contains(m.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianAsphalt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isOwned) CashEmerald.copy(alpha = 0.6f) else HairlineBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(m.r, m.g, m.b), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.displayName, style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                            Text(m.description, style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                            Text("TOP ${(m.maxSpeed * 4.5f).toInt()} KM/H", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onBuyOrSpawn(m) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOwned) NeonCyan else RevengeGold,
                                contentColor = ObsidianAsphalt
                            ),
                            modifier = Modifier.testTag("car_btn_${m.id}")
                        ) {
                            Text(if (isOwned) "SPAWN" else "$${m.price}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuyHouseModal(
    house: BuyableHouse,
    progress: PlayerProgressEntity,
    onBuyOrRest: (BuyableHouse) -> Unit,
    onClose: () -> Unit
) {
    val owned = progress.ownedHousesCsv.split(",").contains(house.id)
    ModalContainer(title = "REVENGE CITY REAL ESTATE", subtitle = house.district, onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, contentDescription = null, tint = if (owned) CashEmerald else RevengeGold, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(house.name, style = MaterialTheme.typography.titleLarge, color = BoneWhite)
                    Text(if (owned) "OWNED SAFEHOUSE" else "Market Price: $${house.price}", style = MaterialTheme.typography.labelLarge, color = if (owned) CashEmerald else RevengeGold)
                }
            }
            Text(house.description, style = MaterialTheme.typography.bodyLarge, color = MutedSteel)
            Text("Your Cash: $${progress.cash}", style = MaterialTheme.typography.labelLarge, color = CashEmerald)
            Button(
                onClick = { onBuyOrRest(house) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("house_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (owned) CashEmerald else RevengeGold,
                    contentColor = ObsidianAsphalt
                )
            ) {
                Text(
                    text = if (owned) "REST, HEAL 100% & SUMMON CAR" else "PURCHASE PROPERTY ($${house.price})",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NpcDialogueModal(
    chat: NpcChatState,
    isSpeaking: Boolean,
    onClose: () -> Unit
) {
    val line = chat.npc.lines.getOrNull(chat.lineIndex) ?: ""
    val emotion = when {
        line.contains("How are you", ignoreCase = true) ||
            line.contains("I am good", ignoreCase = true) ||
            line.contains("Welcome", ignoreCase = true) ||
            line.contains("pretty", ignoreCase = true) -> DialogueEmotion.HAPPY
        line.contains("Watch", ignoreCase = true) ||
            line.contains("Careful", ignoreCase = true) -> DialogueEmotion.SERIOUS
        else -> DialogueEmotion.NEUTRAL
    }
    ModalContainer(title = chat.npc.name, subtitle = chat.npc.role, onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TalkingCharacterFace(
                    speakerName = chat.npc.name,
                    isFemale = chat.npc.voiceProfile.isFemale,
                    isSpeaking = isSpeaking,
                    currentText = line,
                    emotion = emotion,
                    modifier = Modifier.size(92.dp)
                )
                Text(
                    text = "\"$line\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BoneWhite,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSpeaking) "Speaking • Line ${chat.lineIndex + 1}/${chat.npc.lines.size}" else "Next line in 2s...",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSpeaking) CashEmerald else RevengeGold
                )
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("npc_dialogue_close")
                ) {
                    Text("Close", color = BoneWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FullGpsMapModal(
    hud: HudSnapshot,
    onSetGpsWaypoint: (Float, Float, String) -> Unit,
    onClearGps: () -> Unit,
    onClose: () -> Unit
) {
    ModalContainer(
        title = "REVENGE CITY GPS TRACKER",
        subtitle = hud.gpsLabel?.let { "$it • ${hud.gpsDistanceMeters ?: 0}m away" } ?: "Tap anywhere on map to drop GPS pin",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A101A))
                    .border(2.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val wx = ((offset.x / size.width) - 0.5f) * 390f
                            val wz = ((offset.y / size.height) - 0.5f) * 390f
                            onSetGpsWaypoint(wx, wz, "Custom Pin")
                        }
                    }
                    .testTag("full_gps_map_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun worldToMap(wx: Float, wz: Float): Offset {
                        val sx = ((wx / 390f) + 0.5f) * size.width
                        val sy = ((wz / 390f) + 0.5f) * size.height
                        return Offset(sx, sy)
                    }

                    // Parks
                    val green = Color(0xFF153E2B)
                    drawRect(green, topLeft = worldToMap(-110f, 15f), size = Size(size.width * 0.1f, size.height * 0.1f))
                    drawRect(green, topLeft = worldToMap(75f, -50f), size = Size(size.width * 0.1f, size.height * 0.1f))

                    // Mtkvari River
                    val riverLeft = worldToMap(-11f, -195f)
                    drawRect(Color(0xFF164E78), topLeft = riverLeft, size = Size(size.width * (22f / 390f), size.height))

                    // Highways & Avenues
                    for (ax in listOf(-164f, -120f, -60f, 60f, 120f, 164f)) {
                        val p0 = worldToMap(ax, -184f)
                        val p1 = worldToMap(ax, 184f)
                        drawLine(Color(0xFF334155), p0, p1, strokeWidth = if (abs(ax) > 150f) 7f else 4f)
                    }
                    for (az in listOf(-164f, -120f, -60f, 0f, 60f, 120f, 164f)) {
                        val p0 = worldToMap(-184f, az)
                        val p1 = worldToMap(184f, az)
                        drawLine(Color(0xFF334155), p0, p1, strokeWidth = if (abs(az) > 150f || az == 0f) 7f else 4f)
                    }

                    // Active GPS Route Path (snapped street-grid leg + final approach)
                    val gx = hud.gpsTargetX
                    val gz = hud.gpsTargetZ
                    if (gx != null && gz != null) {
                        val pStart = worldToMap(hud.playerX, hud.playerZ)
                        val pCorner = worldToMap(gx, hud.playerZ)
                        val pEnd = worldToMap(gx, gz)
                        val route = Path().apply {
                            moveTo(pStart.x, pStart.y)
                            lineTo(pCorner.x, pCorner.y)
                            lineTo(pEnd.x, pEnd.y)
                        }
                        drawPath(
                            path = route,
                            color = NeonCyan.copy(alpha = 0.35f),
                            style = Stroke(width = 11f, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = route,
                            color = NeonCyan,
                            style = Stroke(
                                width = 4.5f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                        drawCircle(NeonCyan.copy(alpha = 0.3f), radius = 16f, center = pEnd)
                        drawCircle(RevengeGold, radius = 7f, center = pEnd)
                    }

                    // Landmarks / Mission Dots
                    for (dot in hud.minimapDots) {
                        val pt = worldToMap(dot.x, dot.z)
                        val col = when (dot.kind) {
                            "MISSION" -> RevengeGold
                            "MISSION_DONE" -> CashEmerald.copy(alpha = 0.65f)
                            "WEAPON" -> Color(0xFFFF9800)
                            "CAR" -> NeonCyan
                            "OWNED_HOUSE", "HOUSE" -> CashEmerald
                            "ENEMY" -> CrimsonAlert
                            "POLICE" -> Color(0xFF3B82F6)
                            else -> BoneWhite.copy(alpha = 0.8f)
                        }
                        drawCircle(col, radius = if (dot.kind == "MISSION") 7f else 5f, center = pt)
                    }

                    // Player George Nikoladze Arrow
                    val pPos = worldToMap(hud.playerX, hud.playerZ)
                    val deg = hud.playerYaw * 180f / PI.toFloat()
                    rotate(degrees = 180f - deg, pivot = pPos) {
                        val arrow = Path().apply {
                            moveTo(pPos.x, pPos.y - 10f)
                            lineTo(pPos.x + 7f, pPos.y + 8f)
                            lineTo(pPos.x, pPos.y + 3f)
                            lineTo(pPos.x - 7f, pPos.y + 8f)
                            close()
                        }
                        drawPath(arrow, color = BoneWhite)
                    }
                }
            }

            // Quick GPS Destination Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { onSetGpsWaypoint(GameCatalog.WEAPON_SHOP_X, GameCatalog.WEAPON_SHOP_Z, "Weapon Shop") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Armory", color = BoneWhite, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { onSetGpsWaypoint(GameCatalog.CAR_STATION_X, GameCatalog.CAR_STATION_Z, "Car Station") }) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Car Station", color = BoneWhite, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { onSetGpsWaypoint(-16f, 10f, "Peace Bridge") }) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = RevengeGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Peace Bridge", color = BoneWhite, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { onSetGpsWaypoint(126f, 164f, "Tbilisi Airport") }) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = CashEmerald, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Airport", color = BoneWhite, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onClearGps) {
                    Text("Clear Pin", color = CrimsonAlert, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PauseMenuModal(
    progress: PlayerProgressEntity,
    streetLightsEnabled: Boolean,
    onToggleStreetLights: (Boolean) -> Unit,
    onResume: () -> Unit,
    onStartMission: (MissionDefinition) -> Unit,
    onOpenSettings: () -> Unit,
    onReturnMainMenu: () -> Unit
) {
    val completed = progress.completedMissionsCsv.split(",").toSet()
    ModalContainer(title = "PAUSED • REVENGE CITY", subtitle = "George Nikoladze • $${progress.cash}", onClose = onResume) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RevengeGold, contentColor = ObsidianAsphalt)
                ) { Text("Resume", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                    Text("Settings", color = BoneWhite)
                }
                OutlinedButton(onClick = onReturnMainMenu, modifier = Modifier.weight(1f)) {
                    Text("Menu", color = CrimsonAlert)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianAsphalt)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Street Lights (Saved)", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                    Text(
                        if (streetLightsEnabled) "ON • Full night glow" else "OFF • Power & FPS saving",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (streetLightsEnabled) CashEmerald else MutedSteel
                    )
                }
                Switch(
                    checked = streetLightsEnabled,
                    onCheckedChange = onToggleStreetLights,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianAsphalt,
                        checkedTrackColor = RevengeGold
                    )
                )
            }
            Text("QUICK-START MISSION ($300 EACH)", style = MaterialTheme.typography.labelLarge, color = RevengeGold)
            LazyColumn(
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GameCatalog.missions) { m ->
                    val done = completed.contains(m.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianAsphalt)
                            .border(1.dp, if (done) CashEmerald.copy(alpha = 0.5f) else HairlineBorder, RoundedCornerShape(10.dp))
                            .clickable { onStartMission(m) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("M${m.number}: ${m.title}", style = MaterialTheme.typography.titleMedium, color = BoneWhite)
                            Text(m.district, style = MaterialTheme.typography.bodyMedium, color = MutedSteel)
                        }
                        if (done) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = CashEmerald)
                        } else {
                            Text("+$300", style = MaterialTheme.typography.labelLarge, color = CashEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionOutcomeOverlay(
    isSuccess: Boolean,
    isWasted: Boolean,
    title: String,
    detail: String,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .border(
                    2.dp,
                    if (isSuccess) CashEmerald else CrimsonAlert,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        isWasted -> "WASTED"
                        isSuccess -> "MISSION PASSED!"
                        else -> "MISSION FAILED"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = when {
                        isWasted || !isSuccess -> CrimsonAlert
                        else -> RevengeGold
                    }
                )
                Text(title, style = MaterialTheme.typography.titleLarge, color = BoneWhite)
                Text(detail, style = MaterialTheme.typography.bodyLarge, color = if (isSuccess) CashEmerald else MutedSteel)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("outcome_continue_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) CashEmerald else RevengeGold,
                        contentColor = ObsidianAsphalt
                    )
                ) {
                    Text(if (isWasted) "RESPAWN IN CITY" else "CONTINUE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModalContainer(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .heightIn(max = 640.dp)
                .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = RevengeGold)
                        Text(subtitle, style = MaterialTheme.typography.labelLarge, color = CashEmerald)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("modal_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = BoneWhite)
                    }
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
