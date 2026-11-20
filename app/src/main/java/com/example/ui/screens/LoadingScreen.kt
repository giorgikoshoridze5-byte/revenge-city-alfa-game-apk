package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CashEmerald
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianAsphalt
import com.example.ui.theme.RevengeGold
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    progress: Float,
    loadingComplete: Boolean,
    musicVolume: Float,
    onToggleMute: () -> Unit,
    onContinueToMenu: () -> Unit
) {
    val slides = GameCatalog.loadingSlides
    var slideIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3100L)
            slideIndex = (slideIndex + 1) % slides.size
        }
    }

    val infinite = rememberInfiniteTransition(label = "ken_burns")
    val zoom by infinite.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )
    val panX by infinite.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pan"
    )
    val eqPulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq"
    )

    val currentSlide = slides[slideIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianAsphalt)
    ) {
        // Ken Burns full-bleed artwork carousel using the 5 Revenge City photos
        AnimatedContent(
            targetState = slideIndex,
            transitionSpec = {
                fadeIn(tween(700)) togetherWith fadeOut(tween(700))
            },
            label = "loading_photo_fade"
        ) { idx ->
            val slide = slides[idx]
            Image(
                painter = painterResource(id = slide.imageRes),
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX
                    }
            )
        }

        // Dramatic top & bottom cinematic vignettes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.72f),
                        0.32f to Color.Transparent,
                        0.60f to Color.Black.copy(alpha = 0.25f),
                        1.0f to ObsidianAsphalt.copy(alpha = 0.96f)
                    )
                )
        )

        // Top Bar: Title + Music indicator + Mute toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REVENGE CITY",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = BoneWhite,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "GEORGE NIKOLADZE CHRONICLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = RevengeGold,
                        letterSpacing = 1.6.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Loading Soundtrack",
                    tint = CashEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                // Mini audio equalizer bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(14.dp)
                ) {
                    val heights = listOf(0.5f, 1.0f, 0.7f, 0.9f)
                    heights.forEachIndexed { i, h ->
                        val barH = if (musicVolume > 0.02f) (h * (if (i % 2 == 0) eqPulse else 1.25f - eqPulse * 0.4f)).coerceIn(0.2f, 1f) else 0.15f
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((14 * barH).dp)
                                .background(CashEmerald, RoundedCornerShape(2.dp))
                        )
                    }
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("loading_mute_button")
                ) {
                    Icon(
                        imageVector = if (musicVolume > 0.02f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle Music",
                        tint = BoneWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Storytelling Caption + Loading Bar + Enter CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            // Slide dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                slides.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (idx == slideIndex) 26.dp else 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (idx == slideIndex) RevengeGold else Color.White.copy(alpha = 0.28f))
                    )
                }
            }

            Text(
                text = currentSlide.title,
                style = MaterialTheme.typography.headlineMedium.copy(color = RevengeGold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentSlide.subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(color = BoneWhite)
            )
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stageText = when {
                    loadingComplete -> "REVENGE CITY READY"
                    progress < 0.22f -> "LANDING AT TBILISI AIRPORT..."
                    progress < 0.45f -> "STREAMING HIGHWAYS & SKYSCRAPERS..."
                    progress < 0.70f -> "RIGGING DETAILED NPCS & TRAFFIC..."
                    progress < 0.90f -> "SYNCING GPS RADAR & CUTSCENES..."
                    else -> "FINALIZING WORLD LIGHTING..."
                }
                Text(
                    text = stageText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (loadingComplete) CashEmerald else MutedSteel
                    )
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(color = BoneWhite)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = CashEmerald,
                trackColor = Color.White.copy(alpha = 0.14f)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onContinueToMenu,
                enabled = loadingComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("enter_revenge_city_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RevengeGold,
                    contentColor = ObsidianAsphalt,
                    disabledContainerColor = Color.White.copy(alpha = 0.14f),
                    disabledContentColor = MutedSteel
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (loadingComplete) "ENTER MAIN MENU" else "LOADING ASSETS...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
