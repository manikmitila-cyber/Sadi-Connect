package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SadiDarkColorScheme =
  darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlueBright,
    onSecondary = DeepBlackBg,
    secondaryContainer = CardNavyElevated,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CyanGlow,
    onTertiary = DeepBlackBg,
    background = DeepBlackBg,
    onBackground = TextPrimaryDark,
    surface = DeepNavyBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardNavy,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderNavy,
    error = DisconnectRed,
    onError = Color.White,
    errorContainer = DisconnectRedBg,
    onErrorContainer = DisconnectRed,
  )

private val SadiLightColorScheme =
  lightColorScheme(
    primary = ElectricBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = CyanGlow,
    onTertiary = TextPrimaryLight,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder,
    error = DisconnectRedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = DisconnectRedDark,
  )

val SadiShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun SadiConnectTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SadiDarkColorScheme else SadiLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = SadiTypography,
    shapes = SadiShapes,
    content = content,
  )
}
