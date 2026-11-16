package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FallbackDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = SonicCoral,
    onSecondary = OnSonicCoral,
    secondaryContainer = SonicCoralContainer,
    onSecondaryContainer = OnSonicCoralContainer,
    tertiary = StudioAmber,
    onTertiary = OnStudioAmber,
    tertiaryContainer = StudioAmberContainer,
    onTertiaryContainer = OnStudioAmberContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = OutlineDark
)

private val FallbackLightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = Color(0xFF001F24),
    secondary = DeepCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF3F0011),
    tertiary = DeepAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF281800),
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColorScheme
        else -> FallbackLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
