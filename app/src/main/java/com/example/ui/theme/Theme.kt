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

private val DarkColorScheme = darkColorScheme(
  primary = WorkdayDarkPrimary,
  onPrimary = WorkdayDarkOnPrimary,
  primaryContainer = WorkdayDarkPrimaryContainer,
  onPrimaryContainer = WorkdayDarkOnPrimaryContainer,
  secondary = WorkdayDarkSecondary,
  onSecondary = WorkdayDarkOnSecondary,
  secondaryContainer = WorkdayDarkSecondaryContainer,
  onSecondaryContainer = WorkdayDarkOnSecondaryContainer,
  tertiary = WorkdayDarkTertiary,
  onTertiary = WorkdayDarkOnTertiary,
  tertiaryContainer = WorkdayDarkTertiaryContainer,
  onTertiaryContainer = WorkdayDarkOnTertiaryContainer,
  background = WorkdayDarkBackground,
  onBackground = WorkdayDarkOnBackground,
  surface = WorkdayDarkSurface,
  onSurface = WorkdayDarkOnSurface,
  surfaceVariant = WorkdayDarkSurfaceVariant,
  onSurfaceVariant = WorkdayDarkOnSurfaceVariant,
  outline = WorkdayDarkOutline,
)

private val LightColorScheme = lightColorScheme(
  primary = WorkdayPrimary,
  onPrimary = WorkdayOnPrimary,
  primaryContainer = WorkdayPrimaryContainer,
  onPrimaryContainer = WorkdayOnPrimaryContainer,
  secondary = WorkdaySecondary,
  onSecondary = WorkdayOnSecondary,
  secondaryContainer = WorkdaySecondaryContainer,
  onSecondaryContainer = WorkdayOnSecondaryContainer,
  tertiary = WorkdayTertiary,
  onTertiary = WorkdayOnTertiary,
  tertiaryContainer = WorkdayTertiaryContainer,
  onTertiaryContainer = WorkdayOnTertiaryContainer,
  background = WorkdayBackground,
  onBackground = WorkdayOnBackground,
  surface = WorkdaySurface,
  onSurface = WorkdayOnSurface,
  surfaceVariant = WorkdaySurfaceVariant,
  onSurfaceVariant = WorkdayOnSurfaceVariant,
  outline = WorkdayOutline,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent curated brand theme
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
