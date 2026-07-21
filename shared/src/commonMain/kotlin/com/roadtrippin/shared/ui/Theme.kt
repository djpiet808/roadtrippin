package com.roadtrippin.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.roadtrippin.shared.domain.AppSettings

val RoadNavy = Color(0xFF17324D)
val RoadCream = Color(0xFFFFF8EC)
val RoadOrange = Color(0xFFF28E2B)
val RoadRed = Color(0xFFE15759)
val RoadGreen = Color(0xFF2A9D8F)

private val LightColors = lightColorScheme(
    primary = RoadNavy,
    onPrimary = Color.White,
    secondary = RoadOrange,
    onSecondary = Color(0xFF241400),
    tertiary = RoadGreen,
    background = RoadCream,
    onBackground = Color(0xFF17212B),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFF1E8D8),
    onSurfaceVariant = Color(0xFF4E473E),
    error = RoadRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FC9EE),
    onPrimary = Color(0xFF002F4E),
    secondary = Color(0xFFFFB867),
    onSecondary = Color(0xFF482800),
    tertiary = Color(0xFF84D5C8),
    background = Color(0xFF101820),
    onBackground = Color(0xFFE4EDF5),
    surface = Color(0xFF16222D),
    onSurface = Color(0xFFE4EDF5),
    surfaceVariant = Color(0xFF273541),
    onSurfaceVariant = Color(0xFFC5D0D9),
    error = Color(0xFFFFB4AB),
)

@Composable
fun RoadtrippinTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val useDark = settings.forceDarkMode ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}

