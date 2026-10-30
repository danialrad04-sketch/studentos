package com.example.data.v3

import android.net.Uri
import com.example.data.backup.StudentOSBackupPayload
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

data class PortableBlob(val name: String, val sha256: String, val size: Long)

data class PortableManifest(val version: Int = 2, val entries: List<PortableBlob> = emptyList())

/**
 * Full archive is validated before existing metadata is touched. Unreferenced blobs are harmless.
 */
class StudentPortableBackup(
    private val files: StudentV3Files,
    private val localFiles: suspend () -> List<com.example.data.local.entity.AttachmentEntity> = {
        emptyList()
    },
) {
    private val manifestAdapter = StudentV3Json.moshi.adapter(PortableManifest::class.java)
    private val payloadAdapter = StudentV3Json.moshi.adapter(StudentOSBackupPayload::class.java)

    suspend fun export(uri: Uri, metadata: suspend () -> String, canWrite: () -> Boolean) =
        withContext(Dispatchers.IO) {
            files.mutex.withLock {
                check(canWrite())
                val stage = File.createTempFile("student-backup-", ".json", files.context.cacheDir)
                try {
                    val json = metadata()
                    stage.writeText(json)
                    val payload = requireNotNull(payloadAdapter.fromJson(json))
                    val blobs =
                        payload.v3
                            ?.files
                            .orEmpty()
                            .filter { !it.isPurged }
                            .distinctBy { it.blobKey }
                            .map { f ->
                                val file = files.blob(f.blobKey)
                                check(
                                    file.isFile &&
                                        file.length() == f.byteCount &&
                                        StudentV3Files.digest(file) == f.blobKey
                                ) {
                                    "فایل «${f.title}» روی دستگاه موجود یا سالم نیست؛ پشتیبان کامل ساخته نشد."
                                }
                                PortableBlob("blobs/${f.blobKey}", f.blobKey, file.length())
                            }
                    require(stage.length() + blobs.sumOf { it.size } <= 2L * 1024 * 1024 * 1024) {
                        "پشتیبان بیش از ۲ گیگابایت است؛ فایل‌های غیرضروری کتابخانه را برای همیشه پاک کن و دوباره تلاش کن."
                    }
                    val manifest =
                        PortableManifest(
                            entries =
                                listOf(
                                    PortableBlob(
                                        "metadata.json",
                                        StudentV3Files.digest(stage),
                                        stage.length(),
                                    )
                                ) + blobs
                        )
                    check(canWrite())
                    files.context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        ZipOutputStream(output).use { zip ->
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            zip.write(manifestAdapter.toJson(manifest).toByteArray())
                            zip.closeEntry()
                            zip.putNextEntry(ZipEntry("metadata.json"))
                            stage.inputStream().use {
                                StudentV3Files.copyLimited(it, zip, 32L * 1024 * 1024)
                            }
                            zip.closeEntry()
                            for (blob in blobs) {
                                check(canWrite())
                                zip.putNextEntry(ZipEntry(blob.name))
                                files.blob(blob.sha256).inputStream().use {
                                    StudentV3Files.copyLimited(
                                        it,
                                        zip,
                                        StudentV3Files.MAX_FILE_BYTES,
                                    )
                                }
                                zip.closeEntry()
                            }
                        }
                    } ?: error("دسترسی به مقصد برقرار نشد.")
                } finally {
                    stage.delete()
                }
            }
        }

    suspend fun import(uri: Uri, canWrite: () -> Boolean, restore: suspend (String) -> Unit) =
        withContext(Dispatchers.IO) {
            files.mutex.withLock {
                check(canWrite())
                val archive =
                    File.createTempFile("student-restore-", ".part", files.context.cacheDir)
                val stage =
                    File(files.context.cacheDir, "restore-${java.util.UUID.randomUUID()}").apply {
                        mkdirs()
                    }
                try {
                    files.context.contentResolver.openInputStream(uri)?.use { input ->
                        archive.outputStream().use {
                            StudentV3Files.copyLimited(input, it, 2L * 1024 * 1024 * 1024)
                        }
                    } ?: error("فایل باز نشد.")
                    ZipFile(archive).use { zip ->
                        val manifestEntry =
                            zip.getEntry("manifest.json") ?: error("فهرست سلامت پشتیبان پیدا نشد.")
                        fun readSmall(name: String, cap: Int): String {
                            val entry = zip.getEntry(name) ?: error("بخشی از پشتیبان وجود ندارد.")
                            return zip.getInputStream(entry).use { input ->
                                val out = java.io.ByteArrayOutputStream()
                                val buf = ByteArray(8192)
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    require(out.size() + n <= cap)
                                    out.write(buf, 0, n)
                                }
                                out.toString("UTF-8")
                            }
                        }
                        val manifest =
                            requireNotNull(
                                manifestAdapter.fromJson(
                                    readSmall(manifestEntry.name, 2 * 1024 * 1024)
                                )
                            )
                        require(manifest.version == 2 && manifest.entries.size in 1..10000)
                        val names = manifest.entries.map { it.name }
                        require(
                            names.distinct().size == names.size &&
                                names.count { it == "metadata.json" } == 1
                        )
                        val actual = zip.entries().asSequence().map { it.name }.toList()
                        require(
                            actual.distinct().size == actual.size &&
                                actual.toSet() == (names + "manifest.json").toSet()
                        ) {
                            "ساختار پشتیبان معتبر نیست."
                        }
                        var total = 0L
                        for (item in manifest.entries) {
                            currentCoroutineContext().ensureActive()
                            require(item.sha256.matches(Regex("[a-f0-9]{64}")))
                            require(
                                item.name == "metadata.json" || item.name == "blobs/${item.sha256}"
                            )
                            require(
                                item.size in
                                    1..if (item.name == "metadata.json") 32L * 1024 * 1024
                                        else StudentV3Files.MAX_FILE_BYTES
                            )
                            total += item.size
                            require(total <= 2L * 1024 * 1024 * 1024)
                            val target =
                                File(
                                    stage,
                                    if (item.name == "metadata.json") "metadata.json"
                                    else item.sha256,
                                )
                            zip.getInputStream(
                                    zip.getEntry(item.name) ?: error("فایل پیوست وجود ندارد.")
                                )
                                .use { input ->
                                    target.outputStream().use {
                                        StudentV3Files.copyLimited(input, it, item.size)
                                    }
                                }
                            require(
                                target.length() == item.size &&
                                    StudentV3Files.digest(target) == item.sha256
                            ) {
                                "بررسی سلامت پشتیبان ناموفق بود؛ اطلاعات فعلی حفظ شد."
                            }
                        }
                        val json = File(stage, "metadata.json").readText()
                        val payload = requireNotNull(payloadAdapter.fromJson(json))
                        val expected =
                            payload.v3
                                ?.files
                                .orEmpty()
                                .filter { !it.isPurged }
                                .map { "blobs/${it.blobKey}" }
                                .toSet()
                        require(names.filter { it != "metadata.json" }.toSet() == expected) {
                            "پیوست‌های پشتیبان کامل نیستند."
                        }
                        payload.v3
                            ?.files
                            .orEmpty()
                            .filter { !it.isPurged }
                            .forEach { row ->
                                StudentV3Validation.file(row)
                                require(File(stage, row.blobKey).length() == row.byteCount)
                                files.validateArchiveFile(File(stage, row.blobKey), row)
                            }
                        check(canWrite()) { "نشست تغییر کرده؛ بازیابی انجام نشد." }
                        val current = localFiles().associateBy { it.id }.toMutableMap()
                        payload.v3?.files.orEmpty().forEach { incoming ->
                            if (incoming.updatedAt > (current[incoming.id]?.updatedAt ?: -1))
                                current[incoming.id] = incoming
                        }
                        val wanted =
                            current.values.filter { !it.isPurged }.map { it.blobKey }.toSet()
                        for (name in expected.filter { it.substringAfter('/') in wanted }) {
                            val key = name.substringAfter('/')
                            val target = files.blob(key)
                            target.parentFile?.mkdirs()
                            files.promote(File(stage, key), target)
                        }
                        check(canWrite())
                        restore(json)
                    }
                } finally {
                    archive.delete()
                    stage.deleteRecursively()
                }
            }
        }
}
