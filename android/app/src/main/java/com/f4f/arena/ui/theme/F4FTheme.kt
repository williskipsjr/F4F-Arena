package com.f4f.arena.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object F4FColors {
    // Forest-to-Sage header gradient stops
    val ForestDeep = Color(0xFF132A1E)
    val ForestMid = Color(0xFF244934)
    val SageMid = Color(0xFF4E7A5B)
    val SageSoft = Color(0xFF95B59D)

    // Surfaces & Canvas
    val CanvasOffWhite = Color(0xFFF5F7F5)
    val CardWhite = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF1F4F1)
    val HairlineBorder = Color(0xFFE3EAE4)

    // Accents
    val PitchLime = Color(0xFF6EE748)
    val PitchGreen = Color(0xFF22C55E)
    val PitchLimeDarkText = Color(0xFF0E2416)

    // Navigation & Text
    val NavCharcoal = Color(0xFF111613)
    val TextPrimary = Color(0xFF111613)
    val TextSecondary = Color(0xFF536358)
    val TextMuted = Color(0xFF7B8B80)

    // Semantic Position & Alerts
    val WarningBg = Color(0xFFFFF7ED)
    val WarningText = Color(0xFFC2410C)
    val DangerRed = Color(0xFFDC2626)

    val HeroVerticalBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to ForestDeep,
            0.32f to ForestMid,
            0.68f to SageMid,
            0.88f to SageSoft,
            1.00f to CanvasOffWhite
        )
    )

    val LimeCtaBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7EF04E), Color(0xFF2CC84E))
    )
}

private val F4FLightColorScheme = lightColorScheme(
    primary = F4FColors.PitchGreen,
    onPrimary = F4FColors.PitchLimeDarkText,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = F4FColors.ForestDeep,
    secondary = F4FColors.ForestDeep,
    onSecondary = Color.White,
    background = F4FColors.CanvasOffWhite,
    onBackground = F4FColors.TextPrimary,
    surface = F4FColors.CardWhite,
    onSurface = F4FColors.TextPrimary,
    surfaceVariant = F4FColors.SurfaceMuted,
    onSurfaceVariant = F4FColors.TextSecondary,
    outline = F4FColors.HairlineBorder,
    error = F4FColors.DangerRed
)

val DisplayFontFamily: FontFamily = FontFamily.SansSerif
val BodyFontFamily: FontFamily = FontFamily.SansSerif

val F4FTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val F4FShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun F4FMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = F4FLightColorScheme,
        typography = F4FTypography,
        shapes = F4FShapes,
        content = content
    )
}
