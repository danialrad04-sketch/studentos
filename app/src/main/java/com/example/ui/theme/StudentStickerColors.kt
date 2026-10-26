package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.AppEmojiType

/** Paper colors sampled from the illustration family. Text always uses the paired ink. */
@Immutable
data class StudentStickerColors(val container: Color, val ink: Color)

@Composable
fun studentStickerColors(type: AppEmojiType): StudentStickerColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return stickerPalette(type, dark)
}

internal fun stickerPalette(type: AppEmojiType, dark: Boolean): StudentStickerColors {
    val family = when (type) {
        AppEmojiType.HOME, AppEmojiType.CHECK, AppEmojiType.PLUS, AppEmojiType.MORE, AppEmojiType.SETTINGS, AppEmojiType.CURRICULUM -> 0
        AppEmojiType.BOOK, AppEmojiType.COPILOT, AppEmojiType.GRAD_CAP, AppEmojiType.CHART, AppEmojiType.PROFILE, AppEmojiType.HELP, AppEmojiType.HISTORY -> 1
        AppEmojiType.FIRE, AppEmojiType.WARNING, AppEmojiType.TARGET, AppEmojiType.NOTES -> 2
        else -> 3
    }
    return if (dark) when (family) {
        0 -> StudentStickerColors(Color(0xFF303D27), Color(0xFFDCE9BA))
        1 -> StudentStickerColors(Color(0xFF203F4A), Color(0xFFC7E7EF))
        2 -> StudentStickerColors(Color(0xFF50312A), Color(0xFFFFD5C4))
        else -> StudentStickerColors(Color(0xFF493D23), Color(0xFFFFE4A8))
    } else when (family) {
        0 -> StudentStickerColors(Color(0xFFE9EED7), Color(0xFF344420))
        1 -> StudentStickerColors(Color(0xFFDEEEF0), Color(0xFF234854))
        2 -> StudentStickerColors(Color(0xFFFBE5DA), Color(0xFF773D29))
        else -> StudentStickerColors(Color(0xFFFFEEC8), Color(0xFF654914))
    }
}
