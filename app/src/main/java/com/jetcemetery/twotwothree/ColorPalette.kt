package com.jetcemetery.twotwothree

import androidx.compose.ui.graphics.Color

data class ColorOption(val name: String, val hex: String)

object ColorPalette {
    val options = listOf(
        ColorOption("Grey", "#9E9E9E"),
        ColorOption("Brown", "#795548"),
        ColorOption("Blue", "#2196F3"),
        ColorOption("Deep Blue", "#1A237E"),
        ColorOption("Light Blue", "#E3F2FD"),
        ColorOption("Green", "#4CAF50"),
        ColorOption("Dark Green", "#1B5E20"),
        ColorOption("Light Green", "#F1F8E9"),
        ColorOption("Red", "#F44336"),
        ColorOption("Maroon", "#800000"),
        ColorOption("Light Red", "#FFEBEE"),
        ColorOption("Yellow", "#FFEB3B"),
        ColorOption("Amber", "#FFC107"),
        ColorOption("Light Yellow", "#FFFDE7"),
        ColorOption("Orange", "#FF9800"),
        ColorOption("Deep Orange", "#FF5722"),
        ColorOption("Light Orange", "#FFF3E0"),
        ColorOption("Purple", "#9C27B0"),
        ColorOption("Deep Purple", "#673AB7"),
        ColorOption("Light Purple", "#F3E5F5"),
        ColorOption("Teal", "#009688"),
        ColorOption("Cyan", "#00BCD4"),
        ColorOption("Light Teal", "#E0F2F1"),
        ColorOption("Light Grey", "#F5F5F5")
    )

    fun fromHex(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.LightGray
        }
    }
    
    /**
     * Determines a suitable text color (Black or White) based on background luminance.
     */
    fun getContrastColor(backgroundColor: Color): Color {
        // Luminance calculation
        val r = backgroundColor.red
        val g = backgroundColor.green
        val b = backgroundColor.blue
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return if (luminance > 0.5) Color.Black else Color.White
    }
}
