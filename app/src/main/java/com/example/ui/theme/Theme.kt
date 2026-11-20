package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val RevengeDarkColorScheme = darkColorScheme(
    primary = RevengeGold,
    onPrimary = ObsidianAsphalt,
    primaryContainer = RevengeGoldDeep,
    onPrimaryContainer = BoneWhite,
    secondary = CashEmerald,
    onSecondary = ObsidianAsphalt,
    secondaryContainer = CashEmeraldDark,
    onSecondaryContainer = BoneWhite,
    tertiary = NeonCyan,
    onTertiary = ObsidianAsphalt,
    background = ObsidianAsphalt,
    onBackground = BoneWhite,
    surface = SlateElevated,
    onSurface = BoneWhite,
    surfaceVariant = CardSurface,
    onSurfaceVariant = MutedSteel,
    outline = HairlineBorder,
    error = CrimsonAlert,
    onError = BoneWhite
)

private val RevengeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun RevengeCityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RevengeDarkColorScheme,
        typography = RevengeTypography,
        shapes = RevengeShapes,
        content = content
    )
}
