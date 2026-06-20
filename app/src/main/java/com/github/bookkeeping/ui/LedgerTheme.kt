package com.github.bookkeeping.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2E7D74),
            onPrimary = Color.White,
            secondary = Color(0xFFE07A5F),
            tertiary = Color(0xFF3D5A80),
            background = Color(0xFFF7F8FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFEAF1F0),
            outlineVariant = Color(0xFFE5E8EC)
        ),
        content = content
    )
}
