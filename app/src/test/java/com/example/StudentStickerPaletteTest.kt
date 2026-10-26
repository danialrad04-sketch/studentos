package com.example

import androidx.compose.ui.graphics.luminance
import com.example.ui.components.AppEmojiType
import com.example.ui.theme.stickerPalette
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentStickerPaletteTest {
    @Test fun allStickerSurfacesKeepNormalTextAboveWcagContrastInBothThemes() {
        for (dark in listOf(false, true)) for (type in AppEmojiType.entries) {
            val pair = stickerPalette(type, dark)
            val foreground = pair.ink.luminance()
            val background = pair.container.luminance()
            val contrast = (maxOf(foreground, background) + 0.05) / (minOf(foreground, background) + 0.05)
            assertTrue("$type dark=$dark contrast=$contrast", contrast >= 4.5)
        }
    }
}
