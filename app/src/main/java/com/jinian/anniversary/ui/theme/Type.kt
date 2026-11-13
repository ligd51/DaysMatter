package com.jinian.anniversary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun numericStyle(base: TextStyle): TextStyle {
    return base.copy(
        fontFeatureSettings = "tnum",
        fontWeight = if (base.fontWeight == FontWeight.W400) FontWeight(360) else base.fontWeight,
    )
}

@Composable
fun MaterialTheme.numericTitle(): TextStyle = numericStyle(typography.headlineMedium)