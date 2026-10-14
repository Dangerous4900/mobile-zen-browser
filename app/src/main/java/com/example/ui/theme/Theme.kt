package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ZenAccentTheme(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val bgTop: Color,
    val bgBottom: Color
) {
    MONO_CARBON("carbon", "Carbon Monochrome", MonoWhite, MonoSilver, Color(0xFF000000), Color(0xFF000000)),
    MONO_GRAPHITE("graphite", "Graphite Ash", MonoSilver, MonoZinc, Color(0xFF0A0A0D), Color(0xFF000000)),
    MONO_OBSIDIAN("obsidian", "Pure Obsidian", MonoWhite, MonoSlate, Color(0xFF050507), Color(0xFF000000)),
    MONO_SLATE("slate", "Muted Zinc", MonoZinc, MonoSlate, Color(0xFF0D0D10), Color(0xFF000000));

    companion object {
        fun fromId(id: String): ZenAccentTheme = entries.find { it.id == id } ?: MONO_CARBON
    }
}

val ZenShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun ZenBrowserTheme(
    accentTheme: ZenAccentTheme = ZenAccentTheme.MONO_CARBON,
    content: @Composable () -> Unit
) {
    // Always Pitch Black AMOLED by default
    val background = Color(0xFF000000)
    val surface = Color(0xFF08080A)
    val surfaceVariant = Color(0xFF121216)

    val colorScheme = darkColorScheme(
        primary = accentTheme.primary,
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF1F1F23),
        onPrimaryContainer = MonoWhite,
        secondary = accentTheme.secondary,
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF1A1A1E),
        onSecondaryContainer = MonoSilver,
        tertiary = MonoSilver,
        background = background,
        onBackground = ZenTextPrimary,
        surface = surface,
        onSurface = ZenTextPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = ZenTextSecondary,
        outline = ZenBorderSubtle,
        error = Color(0xFFD4D4D8)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ZenShapes,
        content = content
    )
}
