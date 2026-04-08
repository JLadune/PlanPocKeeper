package com.example.planpockeeper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Vieux_Rose_Dark,
    secondary = Cyan_Pastel_Dark,
    tertiary = Vieux_Rose_Dark,
    background = Fond_Dark,
    surface = Fond_Dark,
    surfaceVariant = Blanc_Cassé_Dark,
    onPrimary = Vieux_Rose_Dark,
    onSecondary = Vieux_Rose_Dark,
    onBackground = Vieux_Rose_Dark,
    onSurface = Vieux_Rose_Dark,
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
    error = Depassement,
)

@Composable
fun PlanPocKeeperTheme(
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = if (darkMode or systemDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}