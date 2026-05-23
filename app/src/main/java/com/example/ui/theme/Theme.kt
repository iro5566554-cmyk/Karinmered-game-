package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = WuxiaGleamingAmber,
    secondary = WuxiaTextGolden,
    tertiary = WuxiaVibrantEmerald,
    background = WuxiaMahoganyBg,
    surface = WuxiaParchmentCard,
    onPrimary = WuxiaMahoganyBg,
    onSecondary = WuxiaParchmentCard,
    onBackground = WuxiaTextGolden,
    onSurface = WuxiaTextGolden
  )

private val LightColorScheme = DarkColorScheme // Always use premium mahogany dark theme for authentic experience

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled to preserve cohesive atmosphere
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
