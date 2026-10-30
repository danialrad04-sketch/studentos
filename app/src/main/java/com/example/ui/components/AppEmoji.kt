package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.min
import kotlin.math.roundToInt

/** Stable illustration identifiers, independent of enum reordering. */
enum class AppEmojiType(val cell: Int) {
    HOME(0), BOOK(1), CALENDAR(2), CHECK(3), MORE(4), COPILOT(5),
    GRAD_CAP(6), BOLT(7), PLUS(8), POMODORO(9), SPARKLE(10), FIRE(11),
    TARGET(12), TROPHY(13), MEDAL(14), WARNING(15), BELL(16), CHART(17),
    SETTINGS(18), PROFILE(19), HELP(20), NOTES(21), HISTORY(22), CURRICULUM(23)
}

// The theme decodes the atlas once and shares it across every sticker instance.
val LocalStudentStickerAtlas = staticCompositionLocalOf<ImageBitmap?> { null }
val LocalStudentCompanionAtlas = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
@Suppress("UNUSED_PARAMETER") // Retain existing call sites without reintroducing colored emoji tiles.
fun AppEmoji(type: AppEmojiType, modifier: Modifier = Modifier, size: Dp = 32.dp,
    shapeRadiusRatio: Float = 0.28f, elevation: Dp = 0.dp) {
    val atlas = if (type.cell >= 18) LocalStudentCompanionAtlas.current ?: ImageBitmap.imageResource(R.drawable.student_sticker_companions)
        else LocalStudentStickerAtlas.current ?: ImageBitmap.imageResource(R.drawable.student_sticker_atlas)
    val painter = remember(atlas, type) { StudentStickerPainter(atlas, type.cell) }
    Image(painter, contentDescription = null, modifier = modifier.size(size), contentScale = ContentScale.Fit)
}

/** Measured artwork bounds: the generated sheet has slightly staggered rows. No pixel editing. */
private val StudentStickerBounds = arrayOf(
    IntRect(44,67,284,287), IntRect(313,96,576,291), IntRect(617,77,833,297),
    IntRect(41,340,289,585), IntRect(335,352,557,571), IntRect(604,357,849,575),
    IntRect(34,630,299,840), IntRect(369,616,534,849), IntRect(616,626,840,842),
    IntRect(46,869,290,1146), IntRect(347,895,540,1107), IntRect(632,884,828,1129),
    IntRect(43,1178,298,1426), IntRect(325,1166,566,1429), IntRect(647,1173,819,1422),
    IntRect(39,1475,293,1702), IntRect(322,1471,568,1701), IntRect(613,1459,844,1692)
)

private val StudentCompanionBounds = arrayOf(
    IntRect(66,64,490,486), IntRect(572,73,964,466), IntRect(1064,82,1479,466),
    IntRect(63,554,503,943), IntRect(589,549,973,946), IntRect(1047,544,1488,952)
)

/** Display one illustration with transparent breathing room and its original aspect ratio. */
private class StudentStickerPainter(private val atlas: ImageBitmap, cell: Int) : Painter() {
    private val region = if (cell >= 18) StudentCompanionBounds[cell - 18] else StudentStickerBounds[cell]
    private val source = IntSize(region.width, region.height)
    override val intrinsicSize = Size(source.width.toFloat(), source.height.toFloat())
    override fun DrawScope.onDraw() {
        val scale = 0.9f * min(size.width / source.width, size.height / source.height)
        val destination = IntSize((source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1))
        drawImage(atlas, srcOffset = IntOffset(region.left, region.top), srcSize = source,
            dstOffset = IntOffset(((size.width-destination.width)/2).roundToInt(), ((size.height-destination.height)/2).roundToInt()),
            dstSize = destination, filterQuality = FilterQuality.High)
    }
}

/** Illustrations are decorative; the destination title remains the accessible label. */
internal fun studentDestinationSticker(tab: com.example.ui.models.AppTab): AppEmojiType = when(tab) {
    com.example.ui.models.AppTab.DASHBOARD -> AppEmojiType.HOME
    com.example.ui.models.AppTab.COPILOT -> AppEmojiType.COPILOT
    com.example.ui.models.AppTab.ACADEMIC_INTELLIGENCE -> AppEmojiType.CHART
    com.example.ui.models.AppTab.PASSPORT -> AppEmojiType.GRAD_CAP
    com.example.ui.models.AppTab.AGENDA, com.example.ui.models.AppTab.SCHEDULE -> AppEmojiType.CALENDAR
    com.example.ui.models.AppTab.LIBRARY -> AppEmojiType.HISTORY
    com.example.ui.models.AppTab.NOTES -> AppEmojiType.NOTES
    com.example.ui.models.AppTab.HISTORY -> AppEmojiType.HISTORY
    com.example.ui.models.AppTab.ATTENDANCE, com.example.ui.models.AppTab.TASKS -> AppEmojiType.CHECK
    com.example.ui.models.AppTab.EXAMS -> AppEmojiType.TARGET
    com.example.ui.models.AppTab.GRADES -> AppEmojiType.CHART
    com.example.ui.models.AppTab.SEMESTER_PLANNER -> AppEmojiType.GRAD_CAP
    com.example.ui.models.AppTab.CURRICULUM -> AppEmojiType.CURRICULUM
    com.example.ui.models.AppTab.POMODORO -> AppEmojiType.POMODORO
    com.example.ui.models.AppTab.GAMIFICATION -> AppEmojiType.TROPHY
    com.example.ui.models.AppTab.ACCOUNT -> AppEmojiType.PROFILE
}

/**
 * Region decoding reuses the original PNGs without allocating the full atlas for a widget.
 * Resources.openRawResource explicitly supports file-backed drawables (API documentation),
 * despite its RawRes lint annotation. Both IDs below are packaged PNGs, verified on-device.
 * https://developer.android.com/reference/android/content/res/Resources#openRawResource(int)
 */
@android.annotation.SuppressLint("ResourceType")
internal fun studentWidgetSticker(context: android.content.Context, type: AppEmojiType): android.graphics.Bitmap {
    val resource=if(type.cell>=18) R.drawable.student_sticker_companions else R.drawable.student_sticker_atlas
    val rect=if(type.cell>=18) StudentCompanionBounds[type.cell-18] else StudentStickerBounds[type.cell]
    context.resources.openRawResource(resource).use { input ->
        @Suppress("DEPRECATION")
        val decoder=checkNotNull(android.graphics.BitmapRegionDecoder.newInstance(input,false))
        try {
            val region=checkNotNull(decoder.decodeRegion(android.graphics.Rect(rect.left,rect.top,rect.right,rect.bottom),android.graphics.BitmapFactory.Options()))
            val scale=96f/maxOf(region.width,region.height)
            return android.graphics.Bitmap.createScaledBitmap(region,(region.width*scale).roundToInt().coerceAtLeast(1),(region.height*scale).roundToInt().coerceAtLeast(1),true).also { if(it!==region) region.recycle() }
        } finally { decoder.recycle() }
    }
}
