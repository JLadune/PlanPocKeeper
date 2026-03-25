package com.example.planpockeeper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Vieux_Rose_Dark,
    secondary = Cyan_Pastel_Dark,
    tertiary = Blanc_Cassé_Dark,
    background = Fond_Dark,
    surface = Fond_Dark,
    surfaceVariant = Blanc_Cassé_Dark,
    onPrimary = Fond_Dark,
    onSecondary = Fond_Dark,
    onBackground = Blanc_Cassé_Dark,
    onSurface = Blanc_Cassé_Dark,
)

private val LightColorScheme = lightColorScheme(
    primary = Vieux_Rose,
    secondary = Cyan_Pastel,
    tertiary = Blanc_Cassé,
    background = Fond,
    surface = Fond,
    surfaceVariant = Blanc_Cassé,
    onPrimary = Fond,
    onSecondary = Fond,
    onBackground = Vieux_Rose,
    onSurface = Vieux_Rose,
)

@Composable
fun PlanPocKeeperTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}