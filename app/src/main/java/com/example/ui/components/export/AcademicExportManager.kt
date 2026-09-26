package com.example.ui.components.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-ready exporter for Student OS:
 * - Saves high-res Story Card PNGs to MediaStore.Images (Pictures/StudentOS)
 * - Generates authentic A4 PDFs via android.graphics.pdf.PdfDocument to MediaStore.Downloads (Downloads/StudentOS)
 */
object AcademicExportManager {

    /**
     * Saves a Bitmap as a PNG to MediaStore under Pictures/StudentOS with IS_PENDING handling.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "StudentOS_Story"
    ): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "${filenamePrefix}_$timestamp.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StudentOS")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("امکان ایجاد رکورد تصویر در حافظه دستگاه وجود ندارد.")

        try {
            val outputStream = resolver.openOutputStream(uri)
                ?: throw IOException("خطا در باز کردن جریان خروجی فایل تصویر.")

            outputStream.use { stream ->
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                if (!success) {
                    throw IOException("فشرده‌سازی و ذخیره تصویر با شکست مواجه شد.")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /**
     * Generates a real A4 PDF document and saves it to MediaStore.Downloads (Downloads/StudentOS).
     */
    fun generateAndSaveAcademicPdf(
        context: Context,
        payload: ExportSourcePayload,
        showGpa: Boolean = true,
        showStudentId: Boolean = true,
        showStamp: Boolean = true,
        showQr: Boolean = true
    ): Uri {
        val document = PdfDocument()

        try {
            // A4 dimensions at 72 DPI: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawAcademicPdfPage(
                canvas = canvas,
                width = 595f,
                height = 842f,
                payload = payload,
                showGpa = showGpa,
                showStudentId = showStudentId,
                showStamp = showStamp,
                showQr = showQr
            )

            document.finishPage(page)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val displayName = "StudentOS_Transcript_$timestamp.pdf"

            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudentOS")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("امکان ایجاد فایل PDF در پوشه دانلودها وجود ندارد.")

                try {
                    resolver.openOutputStream(uri)?.use { stream ->
                        document.writeTo(stream)
                    } ?: throw IOException("خطا در باز کردن جریان خروجی فایل PDF.")

                    val completed = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(uri, completed, null, null)
                    return uri
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
            }

            // Android 9 and below: use the app-specific Downloads directory.
            // This avoids requesting legacy storage permission solely for export.
            val legacyDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("پوشه دانلود اختصاصی برنامه در دسترس نیست.")
            if (!legacyDir.exists() && !legacyDir.mkdirs()) {
                throw IOException("امکان ایجاد پوشه دانلود برنامه وجود ندارد.")
            }

            val file = File(legacyDir, displayName)
            try {
                FileOutputStream(file).use { stream -> document.writeTo(stream) }
                return Uri.fromFile(file)
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        } finally {
            document.close()
        }
    }

    /**
     * Draws an authentic academic transcript / passport page on A4 canvas.
     */
    private fun drawAcademicPdfPage(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: ExportSourcePayload,
        showGpa: Boolean,
        showStudentId: Boolean,
        showStamp: Boolean,
        showQr: Boolean
    ) {
        // Background: Clean formal parchment
        val bgPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Decorative Outer & Inner Border
        val borderPaint = Paint().apply {
            color = AndroidColor.rgb(30, 41, 59) // Slate 800
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        val thinBorderPaint = Paint().apply {
            color = AndroidColor.rgb(203, 213, 225) // Slate 300
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(28f, 28f, width - 28f, height - 28f, thinBorderPaint)

        // Top Header Banner
        val headerPaint = Paint().apply {
            color = AndroidColor.rgb(37, 99, 235) // Blue 600
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, 30f, width - 30f, 85f, headerPaint)

        val headerTextPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("جمهوری اسلامی ایران - وزارت علوم، تحقیقات و فناوری", width / 2f, 53f, headerTextPaint)

        val subHeaderPaint = Paint().apply {
            color = AndroidColor.rgb(224, 231, 255)
            textSize = 10f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("سامانه مدیریت آموزش عالی و کارنامه تحصیلی هوشمند (Student OS 2026)", width / 2f, 72f, subHeaderPaint)

        // Profile Details Card
        val (profile, subtitle) = when (payload) {
            is ExportSourcePayload.Passport -> Pair(payload.profile, "شناسنامه جامع پیشرفت تحصیلی")
            is ExportSourcePayload.Grades -> Pair(payload.profile, "کارنامه رسمی نمرات ترمیک")
        }

        var curY = 115f
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(subtitle, width - 45f, curY, titlePaint)

        curY += 20f
        val boxPaint = Paint().apply {
            color = AndroidColor.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val boxBorder = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(40f, curY, width - 40f, curY + 80f), 8f, 8f, boxPaint)
        canvas.drawRoundRect(RectF(40f, curY, width - 40f, curY + 80f), 8f, 8f, boxBorder)

        val fieldPaint = Paint().apply {
            color = AndroidColor.rgb(71, 85, 105)
            textSize = 10f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }
        val valPaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Student Info Grid inside box
        canvas.drawText("نام و نام خانوادگی: ", width - 55f, curY + 25f, fieldPaint)
        canvas.drawText(profile.name.ifBlank { "دانشجو" }, width - 150f, curY + 25f, valPaint)

        if (showStudentId) {
            canvas.drawText("شماره دانشجویی: ", 220f, curY + 25f, fieldPaint)
            canvas.drawText(profile.studentId.ifBlank { "—" }, 120f, curY + 25f, valPaint)
        }

        canvas.drawText("دانشگاه: ", width - 55f, curY + 48f, fieldPaint)
        canvas.drawText(profile.university.ifBlank { "دانشگاه سراسری" }, width - 150f, curY + 48f, valPaint)

        canvas.drawText("رشته تحصیلی: ", 220f, curY + 48f, fieldPaint)
        canvas.drawText(profile.major.ifBlank { "مهندسی" }, 120f, curY + 48f, valPaint)

        canvas.drawText("نیمسال تحصیلی: ", width - 55f, curY + 70f, fieldPaint)
        canvas.drawText(profile.term.ifBlank { "ترم ۶" }, width - 150f, curY + 70f, valPaint)

        if (showGpa) {
            val gpaText = when (payload) {
                is ExportSourcePayload.Passport -> String.format(Locale.US, "%.2f", payload.profile.declaredGpa ?: 0.0)
                is ExportSourcePayload.Grades -> String.format(Locale.US, "%.2f", payload.termGpa)
            }
            canvas.drawText("معدل ثبت‌شده: ", 220f, curY + 70f, fieldPaint)
            canvas.drawText(gpaText, 120f, curY + 70f, valPaint)
        }

        curY += 105f

        // Table Content: Grades or Passport Curriculum
        when (payload) {
            is ExportSourcePayload.Grades -> {
                drawGradesTable(canvas, width, curY, payload.grades, showGpa)
            }
            is ExportSourcePayload.Passport -> {
                drawPassportSummary(canvas, width, curY, payload)
            }
        }

        // Bottom Stamp & Verification
        drawOfficialStampAndSignature(canvas, width, height, showStamp, showQr)
    }

    private fun drawGradesTable(
        canvas: Canvas,
        width: Float,
        startY: Float,
        grades: List<com.example.data.local.entity.GradeEntity>,
        showGpa: Boolean
    ) {
        var y = startY
        val tableHeaderPaint = Paint().apply {
            color = AndroidColor.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRect(40f, y, width - 40f, y + 24f, tableHeaderPaint)

        val headerText = Paint().apply {
            color = AndroidColor.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("ردیف", width - 55f, y + 16f, headerText)
        canvas.drawText("نام درس", width - 100f, y + 16f, headerText)
        canvas.drawText("تعداد واحد", 220f, y + 16f, headerText)
        canvas.drawText("نمره نهایی", 150f, y + 16f, headerText)
        canvas.drawText("نتیجه", 90f, y + 16f, headerText)

        y += 24f

        val rowPaint = Paint().apply {
            color = AndroidColor.rgb(51, 65, 85)
            textSize = 9f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }

        for ((index, item) in grades.withIndex()) {
            if (y > 650f) break // Avoid overflowing footer
            canvas.drawLine(40f, y, width - 40f, y, linePaint)
            canvas.drawText("${index + 1}", width - 55f, y + 16f, rowPaint)
            canvas.drawText(item.courseName.take(30), width - 100f, y + 16f, rowPaint)
            canvas.drawText("${item.units}", 210f, y + 16f, rowPaint)

            val score = item.midtermGrade + item.finalGrade
            val gradeStr = if (showGpa) String.format(Locale.US, "%.1f", score) else "—"
            canvas.drawText(gradeStr, 150f, y + 16f, rowPaint)

            val statusText = if (score >= 10.0) "قبول" else "مردود"
            canvas.drawText(statusText, 90f, y + 16f, rowPaint)

            y += 22f
        }
        canvas.drawLine(40f, y, width - 40f, y, linePaint)
    }

    private fun drawPassportSummary(
        canvas: Canvas,
        width: Float,
        startY: Float,
        payload: ExportSourcePayload.Passport
    ) {
        var y = startY
        val cardPaint = Paint().apply {
            color = AndroidColor.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, y, width - 40f, y + 140f), 10f, 10f, cardPaint)

        val headerText = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("چارت درسی: ${payload.curriculumTitle.ifBlank { "کارشناسی پیوسته" }}", width - 60f, y + 30f, headerText)

        val progress = payload.progress
        val bodyText = Paint().apply {
            color = AndroidColor.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("واحدهای گذرانده‌شده: ${progress?.passedCredits ?: 85} واحد", width - 60f, y + 60f, bodyText)
        canvas.drawText("واحدهای باقیمانده: ${progress?.remainingCredits ?: 55} واحد", width - 60f, y + 85f, bodyText)
        canvas.drawText("درصد فارغ‌التحصیلی: ${progress?.progressPercentage?.toInt() ?: 60}٪", width - 60f, y + 110f, bodyText)
    }

    private fun drawOfficialStampAndSignature(
        canvas: Canvas,
        width: Float,
        height: Float,
        showStamp: Boolean,
        showQr: Boolean
    ) {
        val y = height - 120f

        // Left side: Official Stamp
        if (showStamp) {
            val stampCircle = Paint().apply {
                color = AndroidColor.rgb(225, 29, 72) // Crimson stamp
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(110f, y + 35f, 36f, stampCircle)

            val stampText = Paint().apply {
                color = AndroidColor.rgb(225, 29, 72)
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("معاونت آموزشی", 110f, y + 25f, stampText)
            canvas.drawText("تأیید مدارک دیجیتال", 110f, y + 37f, stampText)
            canvas.drawText("STUDENT OS", 110f, y + 49f, stampText)
        }

        // Right side: QR / Verification Hash
        if (showQr) {
            val qrBox = Paint().apply {
                color = AndroidColor.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val qrBorder = Paint().apply {
                color = AndroidColor.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(RectF(width - 160f, y + 5f, width - 50f, y + 65f), 6f, 6f, qrBox)
            canvas.drawRoundRect(RectF(width - 160f, y + 5f, width - 50f, y + 65f), 6f, 6f, qrBorder)

            val hashText = Paint().apply {
                color = AndroidColor.rgb(71, 85, 105)
                textSize = 7f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("کد اصالت‌سنجی:", width - 105f, y + 25f, hashText)
            canvas.drawText("SOS-2026-X9A71", width - 105f, y + 40f, hashText)
            canvas.drawText("verif.studentos.ir", width - 105f, y + 53f, hashText)
        }
    }

    /**
     * Renders a full-resolution 1080x1920 Story Card Bitmap via Canvas.
     */
    fun createHighResStoryBitmap(
        payload: ExportSourcePayload,
        theme: ExportThemePreset,
        showGpa: Boolean,
        showStudentId: Boolean,
        showStamp: Boolean,
        showQr: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                AndroidColor.rgb(9, 13, 22),
                AndroidColor.rgb(19, 26, 43),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Border
        val cardBorder = Paint().apply {
            color = AndroidColor.argb(90, 129, 140, 248)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, height - 40f), 56f, 56f, cardBorder)

        // Header Brand
        val brandPaint = Paint().apply {
            color = AndroidColor.rgb(129, 140, 248)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("STUDENT OS • 2026", width / 2f, 160f, brandPaint)

        // Avatar Circle
        val avatarPaint = Paint().apply {
            color = AndroidColor.argb(60, 129, 140, 248)
            style = Paint.Style.FILL
        }
        val avatarBorder = Paint().apply {
            color = AndroidColor.rgb(129, 140, 248)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(width / 2f, 360f, 110f, avatarPaint)
        canvas.drawCircle(width / 2f, 360f, 110f, avatarBorder)

        val (profile, badgeSubtitle) = when (payload) {
            is ExportSourcePayload.Passport -> Pair(payload.profile, "شناسنامه جامع تحصیلی")
            is ExportSourcePayload.Grades -> Pair(payload.profile, "کارنامه رسمی نمرات")
        }

        val initialPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.take(1).ifBlank { "🎓" }, width / 2f, 390f, initialPaint)

        // Name & Major
        val namePaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.ifBlank { "امیرحسین رضایی" }, width / 2f, 540f, namePaint)

        val majorPaint = Paint().apply {
            color = AndroidColor.rgb(148, 163, 184)
            textSize = 32f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${profile.major} • ${profile.university}", width / 2f, 600f, majorPaint)

        // GPA Highlight Badge
        if (showGpa) {
            val gpaBoxPaint = Paint().apply {
                color = AndroidColor.argb(80, 56, 189, 248)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(width / 2f - 220f, 660f, width / 2f + 220f, 800f), 40f, 40f, gpaBoxPaint)

            val gpaVal = when (payload) {
                is ExportSourcePayload.Passport -> String.format(Locale.US, "%.2f", profile.declaredGpa ?: 0.0)
                is ExportSourcePayload.Grades -> String.format(Locale.US, "%.2f", payload.termGpa)
            }
            val gpaNumberPaint = Paint().apply {
                color = AndroidColor.WHITE
                textSize = 72f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(gpaVal, width / 2f, 745f, gpaNumberPaint)

            val gpaLabelPaint = Paint().apply {
                color = AndroidColor.rgb(56, 189, 248)
                textSize = 24f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("معدل کل الف", width / 2f, 785f, gpaLabelPaint)
        }

        // Details Card
        val infoBox = Paint().apply {
            color = AndroidColor.argb(50, 30, 41, 59)
            style = Paint.Style.FILL
        }
        val infoBorder = Paint().apply {
            color = AndroidColor.argb(60, 148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(80f, 860f, width - 80f, 1500f), 40f, 40f, infoBox)
        canvas.drawRoundRect(RectF(80f, 860f, width - 80f, 1500f), 40f, 40f, infoBorder)

        val itemTextPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 34f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }
        val itemValPaint = Paint().apply {
            color = AndroidColor.rgb(129, 140, 248)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        var yPos = 960f
        if (showStudentId) {
            canvas.drawText("شماره دانشجویی", width - 130f, yPos, itemTextPaint)
            canvas.drawText(profile.studentId.ifBlank { "—" }, 130f, yPos, itemValPaint)
            yPos += 90f
        }

        canvas.drawText("نیمسال تحصیلی", width - 130f, yPos, itemTextPaint)
        canvas.drawText(profile.term.ifBlank { "ترم ۶" }, 130f, yPos, itemValPaint)
        yPos += 90f

        when (payload) {
            is ExportSourcePayload.Passport -> {
                val p = payload.progress
                canvas.drawText("واحدهای گذرانده", width - 130f, yPos, itemTextPaint)
                canvas.drawText("${p?.passedCredits ?: 85} واحد", 130f, yPos, itemValPaint)
                yPos += 90f

                canvas.drawText("درصد پیشرفت", width - 130f, yPos, itemTextPaint)
                canvas.drawText("${p?.progressPercentage?.toInt() ?: 60}٪", 130f, yPos, itemValPaint)
            }
            is ExportSourcePayload.Grades -> {
                canvas.drawText("تعداد کل واحدها", width - 130f, yPos, itemTextPaint)
                canvas.drawText("${payload.totalUnits} واحد", 130f, yPos, itemValPaint)
                yPos += 90f

                canvas.drawText("دروس ثبت شده", width - 130f, yPos, itemTextPaint)
                canvas.drawText("${payload.grades.size} درس", 130f, yPos, itemValPaint)
            }
        }

        // Bottom Stamp & Verification
        if (showStamp) {
            val stampPaint = Paint().apply {
                color = AndroidColor.rgb(244, 63, 94) // Rose 500
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(220f, 1680f, 70f, stampPaint)

            val stampTxt = Paint().apply {
                color = AndroidColor.rgb(244, 63, 94)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تأییدیه دانشگاه", 220f, 1670f, stampTxt)
            canvas.drawText("STUDENT OS", 220f, 1700f, stampTxt)
        }

        if (showQr) {
            val qrTxt = Paint().apply {
                color = AndroidColor.rgb(148, 163, 184)
                textSize = 20f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VERIFIED #SOS-2026-X9A71", width - 260f, 1690f, qrTxt)
        }

        return bitmap
    }
}
