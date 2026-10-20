package com.sift.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sift.R

// Tokens are normative in DESIGN.md — values here must match it exactly.
object SiftColors {
    val Surface = Color(0xFFF6F8F4)
    val Ink = Color(0xFF161A17)
    val InkSoft = Color(0xFF5E6660)
    val Rule = Color(0xFFE0E4DC)
    val Pine = Color(0xFF0E4F34)
    val PineWash = Color(0xFFEAF0EB)
    val Shimmer = Color(0xFFC9CFC6)
}

val SchibstedGrotesk = FontFamily(
    Font(R.font.schibsted_grotesk, FontWeight.Normal),
    Font(R.font.schibsted_grotesk, FontWeight.Medium),
    Font(R.font.schibsted_grotesk, FontWeight.SemiBold),
    Font(R.font.schibsted_grotesk, FontWeight.Bold),
)

object SiftType {
    val wordmark = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.8.sp,
        letterSpacing = (-0.01).em,
    )
    val status = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.2.sp,
    )
    val drawerLabel = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 14.4.sp,
        letterSpacing = 0.14.em,
        fontFeatureSettings = "tnum",
    )
    val count = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.4.sp,
        fontFeatureSettings = "tnum",
    )
    val snippetTitle = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.8.sp,
        letterSpacing = (-0.005).em,
    )
    val snippetText = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.85.sp,
        letterSpacing = 0.005.em,
    )
    val button = TextStyle(
        fontFamily = SchibstedGrotesk,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 15.6.sp,
        letterSpacing = 0.10.em,
    )
}

@Composable
fun SiftTheme(content: @Composable () -> Unit) {
    // Light-only v1 by decision (DESIGN.md Do's and Don'ts); the scene is daylight.
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = SiftColors.Surface,
            surface = SiftColors.Surface,
            onBackground = SiftColors.Ink,
            onSurface = SiftColors.Ink,
            primary = SiftColors.Pine,
            outline = SiftColors.Rule,
        ),
        typography = Typography(),
        content = content,
    )
}
