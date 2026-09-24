package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.components.datepicker.JalaliCalendarUtil
import com.example.ui.components.datepicker.JalaliDate
import com.example.ui.components.export.AcademicExportManager
import com.example.ui.components.export.ExportSourcePayload
import com.example.ui.components.export.ExportThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [AcademicExportAndJalaliTest.ShadowPdfDocument::class])
class AcademicExportAndJalaliTest {

    @Implements(PdfDocument::class)
    class ShadowPdfDocument {
        private var isClosed = false

        @Implementation
        fun __constructor__() {
            isClosed = false
        }

        @Implementation
        fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
            val bitmap = Bitmap.createBitmap(pageInfo.pageWidth, pageInfo.pageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val constructor = PdfDocument.Page::class.java.getDeclaredConstructor(
                Canvas::class.java,
                PdfDocument.PageInfo::class.java
            )
            constructor.isAccessible = true
            return constructor.newInstance(canvas, pageInfo)
        }

        @Implementation
        fun finishPage(page: PdfDocument.Page) {
            // Completed page
        }

        @Implementation
        fun writeTo(out: OutputStream) {
            out.write("%PDF-1.4\n%StudentOS Academic Report\n%%EOF".toByteArray())
        }

        @Implementation
        fun close() {
            isClosed = true
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ─────────────────────────────────────────────────────────────
    // 1. JALALI CALENDAR TESTS
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testGregorianToJalali_knownDates() {
        // Test known conversion: 2026-09-23 is 1405-07-01 (1 Mehr 1405)
        val jDate = JalaliCalendarUtil.gregorianToJalali(2026, 9, 23)
        assertEquals(1405, jDate.year)
        assertEquals(7, jDate.month)
        assertEquals(1, jDate.day)
        assertEquals("مهر", jDate.getMonthName())

        // Test Nowruz 1405: 2026-03-21 is 1405-01-01
        val nowruz = JalaliCalendarUtil.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, nowruz.year)
        assertEquals(1, nowruz.month)
        assertEquals(1, nowruz.day)
        assertEquals("فروردین", nowruz.getMonthName())
    }

    @Test
    fun testJalaliToGregorian_roundTrip() {
        val original = JalaliDate(1405, 7, 1)
        val (gy, gm, gd) = JalaliCalendarUtil.jalaliToGregorian(original.year, original.month, original.day)
        assertEquals(2026, gy)
        assertEquals(9, gm)
        assertEquals(23, gd)

        val roundTrip = JalaliCalendarUtil.gregorianToJalali(gy, gm, gd)
        assertEquals(original.year, roundTrip.year)
        assertEquals(original.month, roundTrip.month)
        assertEquals(original.day, roundTrip.day)
    }

    @Test
    fun testParsePersianDigitsString() {
        val parsed = JalaliCalendarUtil.parse("۱۴۰۵/۰۷/۰۱")
        assertNotNull(parsed)
        assertEquals(1405, parsed?.year)
        assertEquals(7, parsed?.month)
        assertEquals(1, parsed?.day)

        val parsedDashed = JalaliCalendarUtil.parse("1405-03-20")
        assertNotNull(parsedDashed)
        assertEquals(1405, parsedDashed?.year)
        assertEquals(3, parsedDashed?.month)
        assertEquals(20, parsedDashed?.day)
    }

    @Test
    fun testDaysInMonth() {
        // First 6 months have 31 days
        assertEquals(31, JalaliCalendarUtil.getDaysInMonth(1405, 1))
        assertEquals(31, JalaliCalendarUtil.getDaysInMonth(1405, 6))

        // Next 5 months have 30 days
        assertEquals(30, JalaliCalendarUtil.getDaysInMonth(1405, 7))
        assertEquals(30, JalaliCalendarUtil.getDaysInMonth(1405, 11))

        // Month 12: 29 days in normal year, 30 in leap year
        assertEquals(29, JalaliCalendarUtil.getDaysInMonth(1405, 12))
        // 1403 was a leap year in Jalali
        assertEquals(30, JalaliCalendarUtil.getDaysInMonth(1403, 12))
    }

    // ─────────────────────────────────────────────────────────────
    // 2. ACADEMIC EXPORT MANAGER TESTS
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testCreateHighResStoryBitmap_returnsValidBitmap() {
        val profile = StudentProfileEntity(
            name = "امیرحسین رضایی",
            studentId = "40112345",
            university = "دانشگاه صنعتی شریف",
            major = "مهندسی کامپیوتر",
            term = "ترم ۶",
            declaredGpa = 18.75
        )
        val payload = ExportSourcePayload.Passport(
            profile = profile,
            curriculumTitle = "کارشناسی مهندسی نرم‌افزار",
            progress = null
        )

        val bitmap = AcademicExportManager.createHighResStoryBitmap(
            payload = payload,
            theme = ExportThemePreset.OBSIDIAN_NEON,
            showGpa = true,
            showStudentId = true,
            showStamp = true,
            showQr = true
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
        assertTrue(bitmap.byteCount > 0)
    }

    @Test
    fun testSaveBitmapToGallery_createsValidImageInMediaStore() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val uri = AcademicExportManager.saveBitmapToGallery(context, bitmap, "TestStory")

        assertNotNull(uri)
        assertTrue(uri.toString().startsWith("content://media/external/images/media"))

        // Verify bytes written to ContentResolver
        val inputStream = context.contentResolver.openInputStream(uri)
        assertNotNull(inputStream)
        val bytes = inputStream!!.readBytes()
        inputStream.close()
        assertTrue(bytes.isNotEmpty())
        // PNG magic number: 0x89 'P' 'N' 'G'
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('N'.code.toByte(), bytes[2])
        assertEquals('G'.code.toByte(), bytes[3])
    }

    @Test
    fun testGenerateAndSaveAcademicPdf_createsValidPdfInMediaStore() {
        val profile = StudentProfileEntity(
            name = "سارا علوی",
            studentId = "40298765",
            university = "دانشگاه تهران",
            major = "هوش مصنوعی",
            term = "ترم ۴",
            declaredGpa = 19.20
        )
        val grades = listOf(
            GradeEntity(id = 1, courseName = "یادگیری ماشین", units = 3, midtermGrade = 7.5, finalGrade = 12.0),
            GradeEntity(id = 2, courseName = "پردازش تصویر", units = 3, midtermGrade = 6.0, finalGrade = 11.5)
        )
        val payload = ExportSourcePayload.Grades(
            profile = profile,
            grades = grades,
            termGpa = 18.5,
            totalUnits = 6
        )

        val uri = AcademicExportManager.generateAndSaveAcademicPdf(
            context = context,
            payload = payload,
            showGpa = true,
            showStudentId = true,
            showStamp = true,
            showQr = true
        )

        assertNotNull(uri)
        assertTrue(uri.toString().contains("media"))

        val inputStream = context.contentResolver.openInputStream(uri)
        assertNotNull(inputStream)
        val bytes = inputStream!!.readBytes()
        inputStream.close()
        assertTrue(bytes.isNotEmpty())
        // PDF header magic number: "%PDF"
        val header = String(bytes.sliceArray(0..3))
        assertEquals("%PDF", header)
    }
}
