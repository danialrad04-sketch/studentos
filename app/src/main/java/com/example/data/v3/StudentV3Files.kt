package com.example.data.v3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.local.entity.AttachmentEntity
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Immutable content-addressed blobs keep file/database restore safe across failures. */
class StudentV3Files(val context: Context) {
    val mutex = Mutex()
    private val rendering = Mutex()
    private val root = File(context.filesDir, "student-v3/blobs")

    fun blob(key: String): File {
        require(key.matches(Regex("[a-f0-9]{64}")))
        return File(root, key)
    }

    fun exists(key: String) = blob(key).isFile

    suspend fun import(
        uri: Uri,
        courseId: String,
        projectId: String?,
        canWrite: () -> Boolean,
        commit: suspend (AttachmentEntity) -> Unit,
    ): AttachmentEntity =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                check(canWrite())
                root.mkdirs()
                val stage = File.createTempFile("library-", ".part", context.cacheDir)
                try {
                    var name = "جزوه"
                    context.contentResolver
                        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (i >= 0) name = cursor.getString(i) ?: name
                            }
                        }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        stage.outputStream().use { output ->
                            copyLimited(input, output, MAX_FILE_BYTES)
                        }
                    } ?: error("دسترسی به فایل برقرار نشد.")
                    require(stage.length() > 0) { "فایل خالی است." }
                    val (mime, pages) = inspect(stage)
                    val key = digest(stage)
                    check(canWrite()) { "نشست تغییر کرده؛ فایل دوباره انتخاب شود." }
                    val destination = blob(key)
                    promote(stage, destination)
                    val row =
                        AttachmentEntity(
                            UUID.randomUUID().toString(),
                            courseId,
                            projectId,
                            name.take(200),
                            mime,
                            key,
                            destination.length(),
                            pages,
                        )
                    commit(row)
                    row
                } finally {
                    stage.delete()
                }
            }
        }

    suspend fun promote(stage: File, destination: File) {
        if (
            destination.isFile &&
                destination.length() == stage.length() &&
                digest(destination) == destination.name
        )
            return
        destination.parentFile?.mkdirs()
        val atomic = android.util.AtomicFile(destination)
        val output = atomic.startWrite()
        try {
            stage.inputStream().use { copyLimited(it, output, MAX_FILE_BYTES) }
            atomic.finishWrite(output)
        } catch (e: Throwable) {
            atomic.failWrite(output)
            throw e
        }
    }

    suspend fun render(row: AttachmentEntity, page: Int): Bitmap =
        withContext(Dispatchers.IO) {
            rendering.withLock {
                require(!row.isDeleted && page in 0 until row.pageCount)
                val file = blob(row.blobKey)
                check(file.isFile) {
                    "این فایل روی دستگاه نیست؛ پشتیبان کامل شامل فایل‌ها را بازیابی کنید."
                }
                if (row.mimeType == "application/pdf") {
                    PdfRenderer(
                            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        )
                        .use { renderer ->
                            renderer.openPage(page).use { p ->
                                val scale = minOf(1200f / p.width, 1800f / p.height, 2f)
                                val bitmap =
                                    Bitmap.createBitmap(
                                        (p.width * scale).toInt().coerceAtLeast(1),
                                        (p.height * scale).toInt().coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888,
                                    )
                                bitmap.eraseColor(Color.WHITE)
                                p.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                )
                                bitmap
                            }
                        }
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    var sample = 1
                    while (
                        bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1800
                    ) sample *= 2
                    checkNotNull(
                        BitmapFactory.decodeFile(
                            file.path,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        )
                    ) {
                        "تصویر خوانده نشد."
                    }
                }
            }
        }

    internal fun validateArchiveFile(file: File, row: AttachmentEntity) {
        val actual = inspect(file)
        require(actual.first == row.mimeType && actual.second == row.pageCount) {
            "نوع یا تعداد صفحه‌های فایل با پشتیبان سازگار نیست."
        }
    }

    private fun inspect(file: File): Pair<String, Int> {
        val header =
            file
                .inputStream()
                .use { input -> ByteArray(5).also { input.read(it) } }
                .toString(Charsets.US_ASCII)
        return if (header == "%PDF-") {
            try {
                PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
                    .use {
                        require(it.pageCount > 0)
                        "application/pdf" to it.pageCount
                    }
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "PDF خوانده نشد؛ فایل خراب یا رمزدار را بررسی کنید.",
                    e,
                )
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, options)
            require(
                options.outWidth > 0 &&
                    options.outHeight > 0 &&
                    options.outMimeType in setOf("image/jpeg", "image/png", "image/webp")
            ) {
                "فقط PDF، JPG، PNG و WebP پشتیبانی می‌شود."
            }
            options.outMimeType to 1
        }
    }

    suspend fun pruneUnreferenced(references: suspend () -> Set<String>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val keep = references()
                root.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
                Unit
            }
        }

    suspend fun clear() =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                root.deleteRecursively()
                Unit
            }
        }

    companion object {
        const val MAX_FILE_BYTES = 512L * 1024 * 1024

        suspend fun copyLimited(input: InputStream, output: OutputStream, limit: Long): Long {
            val buffer = ByteArray(64 * 1024)
            var count = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                count += n
                require(count <= limit) { "فایل از اندازهٔ مجاز بزرگ‌تر است." }
                output.write(buffer, 0, n)
            }
            return count
        }

        fun digest(file: File): String = file.inputStream().use { digest(it) }

        fun digest(input: InputStream): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(bytes)
                if (n < 0) break
                md.update(bytes, 0, n)
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
