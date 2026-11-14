package com.marsguide.vzd.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MarsOrangeBright,
    onPrimary = SpaceBlack,
    primaryContainer = MarsTerracotta.copy(alpha = 0.3f),
    onPrimaryContainer = MarsOrangeBright,
    secondary = ScienceTeal,
    onSecondary = SpaceBlack,
    secondaryContainer = ScienceTeal.copy(alpha = 0.2f),
    onSecondaryContainer = ScienceTeal,
    tertiary = MarsDuneGold,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = Color(0xFFE2E8F0),
    surface = SpaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SpaceCardDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SpaceBorderDark,
    outlineVariant = Color(0xFF1E2232),
    error = ScienceCritical,
    onError = SpaceBlack
)

@Composable
fun MarsGuideTheme(
    darkTheme: Boolean = true, // Force dark spacecraft interface
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
