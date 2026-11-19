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

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimaryFocus,
    onPrimary = LightOnPrimaryFocus,
    primaryContainer = LightPrimaryFocusContainer,
    onPrimaryContainer = LightOnPrimaryFocusContainer,
    secondary = LightSecondaryBreak,
    onSecondary = LightOnSecondaryBreak,
    secondaryContainer = LightSecondaryBreakContainer,
    onSecondaryContainer = LightOnSecondaryBreakContainer,
    tertiary = LightTertiaryWater,
    onTertiary = LightOnTertiaryWater,
    tertiaryContainer = LightTertiaryWaterContainer,
    onTertiaryContainer = LightOnTertiaryWaterContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimaryFocus,
    onPrimary = DarkOnPrimaryFocus,
    primaryContainer = DarkPrimaryFocusContainer,
    onPrimaryContainer = DarkOnPrimaryFocusContainer,
    secondary = DarkSecondaryBreak,
    onSecondary = DarkOnSecondaryBreak,
    secondaryContainer = DarkSecondaryBreakContainer,
    onSecondaryContainer = DarkOnSecondaryBreakContainer,
    tertiary = DarkTertiaryWater,
    onTertiary = DarkOnTertiaryWater,
    tertiaryContainer = DarkTertiaryWaterContainer,
    onTertiaryContainer = DarkOnTertiaryWaterContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

@Composable
fun CadenceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = CadenceTypography,
    content = content,
  )
}
