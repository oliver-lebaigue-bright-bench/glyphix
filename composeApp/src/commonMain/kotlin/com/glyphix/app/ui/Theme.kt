package com.glyphix.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun GlyphixTheme(
    themeName: String = "Default",
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val colorScheme = if (isDark) {
        darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0D0D0E),
            primary = Color(0xFFD5FC2D),
            secondary = Color(0xFFD5FC2D),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            background = Color.White,
            surface = Color(0xFFF5F5F5),
            primary = Color(0xFFD5FC2D),
            secondary = Color(0xFF626262),
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    val typography = Typography(
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    )

    val shapes = Shapes(
        extraLarge = RoundedCornerShape(32.dp),
        large = RoundedCornerShape(28.dp),
        medium = RoundedCornerShape(20.dp),
        small = RoundedCornerShape(14.dp),
    )

    val appSpacing = AppSpacing()

    CompositionLocalProvider(
        LocalAppSpacing provides appSpacing,
        LocalIsGlassTheme provides (themeName == "Glass"),
        LocalBananaMode provides false,
        LocalPenisMode provides false
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
            ) {
                content()
            }
        }
    }
}

@Immutable
class AppSpacing(
    edge: Dp = 6.dp,
    val between: Dp = 12.dp,
    val inner: Dp = 20.dp
) {
    var edge by mutableStateOf(edge)
        internal set
}

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalIsGlassTheme = compositionLocalOf { false }
val LocalBananaMode = compositionLocalOf { false }
val LocalPenisMode = compositionLocalOf { false }
