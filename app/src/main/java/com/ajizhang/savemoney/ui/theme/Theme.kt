package com.ajizhang.savemoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = Persimmon,
    onPrimary = SoftSurface,
    secondary = Teal,
    tertiary = Ocean,
    surface = SoftSurface,
    surfaceVariant = SurfaceMuted,
    background = Sand,
    onBackground = Ink,
    onSurface = Ink,
)

@Composable
fun SaveMoneyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography(),
        content = content,
    )
}
