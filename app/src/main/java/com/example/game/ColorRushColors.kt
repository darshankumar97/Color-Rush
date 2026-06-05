package com.example.game

import androidx.compose.ui.graphics.Color

enum class BlockColor(val displayName: String, val color: Color) {
    RED("Red Hot", Color(0xFFFF453A)),
    BLUE("Electric Blue", Color(0xFF0A84FF)),
    GREEN("Neon Green", Color(0xFF30D158)),
    YELLOW("Golden Rush", Color(0xFFFFD60A)),
    PURPLE("Psychedelic Purple", Color(0xFFBF5AF2));

    companion object {
        fun random(exclude: BlockColor? = null): BlockColor {
            val list = values().filter { it != exclude }
            return list.random()
        }
    }
}
