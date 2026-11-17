package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val SlateNavy = Color(0xFF131B28)
val SlateNight = Color(0xFF0E131B)
val SlateCardDark = Color(0xFF161E2B)
val SlateSurfaceVariantDark = Color(0xFF202B3D)

val ChalkBackground = Color(0xFFF6F7F4)
val PureWhite = Color(0xFFFFFFFF)
val GymMatGrey = Color(0xFFEAECE5)

val ElectricLime = Color(0xFFD7F957)
val LimeDeep = Color(0xFF263414)
val LimeDarkText = Color(0xFF131B28)

val CoralEnergy = Color(0xFFFF5A36)
val CoralSoft = Color(0xFFFFE6DF)
val CoralDeep = Color(0xFF4A1104)
val CoralDark = Color(0xFFFF7A59)

val CobaltCoach = Color(0xFF2E6BFF)
val CobaltSoft = Color(0xFFE2EBFF)
val CobaltDeep = Color(0xFF0A225C)
val CobaltLight = Color(0xFF78A2FF)

val InkPrimary = Color(0xFF121822)
val InkSecondary = Color(0xFF545E6E)
val InkMuted = Color(0xFF8A94A6)
val LightOutline = Color(0xFFD6DBD0)
val DarkOutline = Color(0xFF2C384C)

val SuccessGreen = Color(0xFF1B9E5A)
val SuccessGreenSoft = Color(0xFFDFF6E9)

// Kinetic Athletic & Home Workout Color Tokens for Visual Impact
val CyberLime = Color(0xFFCCFF00)
val NeonCyan = Color(0xFF00E5FF)
val NeonEmber = Color(0xFFFF5252)
val HyperPurple = Color(0xFFA855F7)
val AmberGold = Color(0xFFFFB800)
val ObsidianBlack = Color(0xFF080C12)
val DarkGlassCard = Color(0xFF141D2B)
val DarkGlassBorder = Color(0xFF26354B)
val DarkGlassGlow = Color(0xFF1C2738)

// Slick Home Workout Design System (Restrained Monochrome + Electric Volt)
val HomeWorkoutBg = Color(0xFF0D0E12)
val HomeWorkoutCard = Color(0xFF151821)
val HomeWorkoutCardBorder = Color(0xFF232838)
val HomeWorkoutCardSelected = Color(0xFF1D2436)
val HomeWorkoutVolt = Color(0xFFD4FF00)
val HomeWorkoutCyan = Color(0xFF00E5FF)
val HomeWorkoutTextPrimary = Color(0xFFFFFFFF)
val HomeWorkoutTextSecondary = Color(0xFF8E95A5)
val HomeWorkoutTextMuted = Color(0xFF555D70)

// Tri-Macro High-Contrast Energy Gauges
val ProteinCyan = Color(0xFF00D2FF)
val CarbsLime = Color(0xFFB8F924)
val FatAmber = Color(0xFFFFAB00)

// Muscle Group Kinetic Tag Colors
fun getMuscleAccentColor(muscleGroup: String): Color {
    val lower = muscleGroup.lowercase()
    return when {
        lower.contains("chest") || lower.contains("push") || lower.contains("pec") -> NeonEmber
        lower.contains("back") || lower.contains("pull") || lower.contains("lat") -> NeonCyan
        lower.contains("leg") || lower.contains("quad") || lower.contains("glute") || lower.contains("ham") || lower.contains("squat") -> CyberLime
        lower.contains("shoulder") || lower.contains("delt") || lower.contains("trap") -> HyperPurple
        lower.contains("arm") || lower.contains("bicep") || lower.contains("tricep") -> AmberGold
        lower.contains("core") || lower.contains("ab") -> Color(0xFF06B6D4)
        else -> ElectricLime
    }
}
