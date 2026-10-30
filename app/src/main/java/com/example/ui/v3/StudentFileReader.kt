@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.v3

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.PageBookmarkEntity
import kotlinx.coroutines.CancellationException

@Composable
fun StudentFileReader(
    vm: StudentV3ViewModel,
    file: AttachmentEntity,
    bookmarks: List<PageBookmarkEntity>,
    onDismiss: () -> Unit,
) {
    var page by rememberSaveable(file.id) { mutableIntStateOf(file.lastPage) }
    var pageInput by rememberSaveable(file.id) { mutableStateOf("") }
    var note by
        rememberSaveable(file.id, page) {
            mutableStateOf(bookmarks.find { it.page == page }?.note.orEmpty())
        }
    var noteDirty by rememberSaveable(file.id, page) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var zoom by remember(page) { mutableFloatStateOf(1f) }
    var panX by remember(page) { mutableFloatStateOf(0f) }
    var panY by remember(page) { mutableFloatStateOf(0f) }
    val bitmap by
        produceState<Bitmap?>(null, file.id, page, reload) {
            value = null
            error = null
            try {
                value = vm.files.render(file, page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.localizedMessage ?: "صفحه باز نشد."
            }
        }
    val busy by vm.busy.collectAsStateCompat()
    fun go(target: Int) {
        if (!busy && target in 0 until file.pageCount && target != page) {
            vm.perform({
                if (noteDirty) bookmark(file.id, page, note)
                savePage(file.id, target)
            }) {
                page = target
            }
        }
    }
    StudentV3Dialog(
        file.title,
        onDismiss,
        tag = "v3_reader",
        requestClose = { close ->
            if (!busy) {
                if (noteDirty)
                    vm.perform({ bookmark(file.id, page, note) }) {
                        noteDirty = false
                        close()
                    }
                else close()
            }
        },
    ) {
        V3Status(vm)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton({ go(page - 1) }, enabled = page > 0 && !busy) { Text("صفحهٔ قبل") }
            Text("${page+1} / ${file.pageCount}", Modifier.padding(12.dp))
            TextButton({ go(page + 1) }, enabled = page < file.pageCount - 1 && !busy) {
                Text("صفحهٔ بعد")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                pageInput,
                { pageInput = it },
                modifier = Modifier.weight(1f),
                enabled = !busy,
                label = { Text("شمارهٔ صفحه") },
                singleLine = true,
            )
            TextButton(
                {
                    val number =
                        com.example.data.local.util.DateTimeNormalizer.normalizeDigits(pageInput)
                            .toIntOrNull()
                    if (number != null && number in 1..file.pageCount) go(number - 1)
                    else error = "شمارهٔ صفحه معتبر نیست."
                },
                enabled = !busy,
            ) {
                Text("برو")
            }
        }
        error?.let { text ->
            V3Card("صفحه نمایش داده نشد", text) { TextButton({ reload++ }) { Text("تلاش دوباره") } }
        }
        val image = bitmap
        if (image == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        image?.let { loaded ->
            val transform = rememberTransformableState { scale, pan, _ ->
                zoom = (zoom * scale).coerceIn(1f, 3f)
                panX = (panX + pan.x).coerceIn(-800f, 800f)
                panY = (panY + pan.y).coerceIn(-1000f, 1000f)
            }
            Box(Modifier.fillMaxWidth().clipToBounds().transformable(transform)) {
                Image(
                    loaded.asImageBitmap(),
                    "صفحهٔ ${page+1} از ${file.title}",
                    Modifier.fillMaxWidth()
                        .aspectRatio(loaded.width.toFloat() / loaded.height)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = if (zoom > 1) panX else 0f
                            translationY = if (zoom > 1) panY else 0f
                        },
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton({ zoom = (zoom - 0.5f).coerceAtLeast(1f) }) { Text("کوچک‌نمایی") }
                TextButton({ zoom = (zoom + 0.5f).coerceAtMost(3f) }) { Text("بزرگ‌نمایی") }
                TextButton({
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                }) {
                    Text("اندازهٔ اصلی")
                }
            }
        }
        V3Card("یادداشت این صفحه", "یادداشت و نشانک همراه جزوه ذخیره می‌شوند.") {
            OutlinedTextField(
                note,
                {
                    note = it
                    noteDirty = true
                },
                modifier = Modifier.fillMaxWidth().testTag("v3_page_note"),
                enabled = !busy,
                label = { Text("نکتهٔ مهم این صفحه") },
                minLines = 2,
            )
            FilledTonalButton(
                { vm.perform({ bookmark(file.id, page, note) }) { noteDirty = false } },
                enabled = !busy,
                modifier = Modifier.testTag("v3_save_bookmark"),
            ) {
                Text("ذخیرهٔ نشانک و یادداشت")
            }
        }
        if (bookmarks.isNotEmpty())
            V3Card("صفحه‌های نشان‌شده") {
                V3Paged(bookmarks.sortedBy { it.page }, "bookmarks:${file.id}") { b ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        TextButton({ go(b.page) }, Modifier.weight(1f), enabled = !busy) {
                            Text(
                                "صفحهٔ ${b.page+1}" +
                                    if (b.note.isBlank()) "" else " · ${b.note.take(80)}"
                            )
                        }
                        TextButton(
                            {
                                vm.perform({ removeBookmark(b) }) {
                                    if (b.page == page) {
                                        note = ""
                                        noteDirty = false
                                    }
                                }
                            },
                            enabled = !busy,
                        ) {
                            Text("حذف نشانک")
                        }
                    }
                }
            }
    }
}

@Composable
internal fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): State<T> =
    this.collectAsStateWithLifecycle()
