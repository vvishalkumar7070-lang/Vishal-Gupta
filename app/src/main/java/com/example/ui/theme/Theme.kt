package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpotMeLightColorScheme = lightColorScheme(
    primary = SlateNavy,
    onPrimary = PureWhite,
    primaryContainer = ElectricLime,
    onPrimaryContainer = LimeDarkText,
    secondary = CoralEnergy,
    onSecondary = PureWhite,
    secondaryContainer = CoralSoft,
    onSecondaryContainer = CoralDeep,
    tertiary = CobaltCoach,
    onTertiary = PureWhite,
    tertiaryContainer = CobaltSoft,
    onTertiaryContainer = CobaltDeep,
    background = ChalkBackground,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = GymMatGrey,
    onSurfaceVariant = InkSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE5E8DF)
)

private val SpotMeDarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = SlateNavy,
    primaryContainer = LimeDeep,
    onPrimaryContainer = ElectricLime,
    secondary = CoralDark,
    onSecondary = SlateNight,
    secondaryContainer = Color(0xFF3D1C14),
    onSecondaryContainer = Color(0xFFFFDAD1),
    tertiary = CobaltLight,
    onTertiary = SlateNight,
    tertiaryContainer = Color(0xFF172B57),
    onTertiaryContainer = CobaltSoft,
    background = SlateNight,
    onBackground = PureWhite,
    surface = SlateCardDark,
    onSurface = PureWhite,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB4BED0),
    outline = DarkOutline,
    outlineVariant = Color(0xFF232F43)
)

val SpotMeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SpotMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpotMeDarkColorScheme else SpotMeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpotMeTypography,
        shapes = SpotMeShapes,
        content = content
    )
}
