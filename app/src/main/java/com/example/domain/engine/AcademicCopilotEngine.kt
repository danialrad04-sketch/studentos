package com.example.domain.engine

import android.util.Log
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.seed.CurriculumSeedData
import com.example.domain.model.ActionImpactType
import com.example.domain.model.AcademicPriorityKind
import com.example.domain.model.CopilotActionProposal
import com.example.domain.model.CopilotMessage
import com.example.domain.model.CopilotPayload
import com.example.domain.model.CopilotSender
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.model.ExamItem
import com.example.util.CrashLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AcademicCopilotEngine {

    fun generateWelcomeMessage(
        profile: StudentProfileEntity,
        courses: List<CourseEntity>,
        attendanceList: List<AttendanceEntity>,
        grades: List<GradeEntity>,
        tasks: List<TaskEntity>
    ): CopilotMessage {
        return CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT,
            "سلام ${profile.name.ifBlank { "دانشجو" }}! می‌توانم از برنامهٔ ثبت‌شده، تکلیف‌ها، نمره‌ها و مطالعه‌ات برای اولویت‌بندی و پیشنهاد قدم بعدی استفاده کنم.\n\nتحلیل اطلاعات شخصی روی دستگاه کار می‌کند. برای توضیح درس و گفت‌وگوی آزاد، پاسخ مدل به اتصال سرویس نیاز دارد. دادهٔ ثبت‌نشده، سرفصل جزوه و زمان آزاد تو را حدس نمی‌زنم. از کجا شروع کنیم؟",
            currentTimeString(), confidenceBadge = "راهنمای دستیار", suggestedQuickReplies = listOf("برنامه امروز من چیست؟", "تکالیف من را بگو", "نمره‌های ثبت‌شده من را بررسی کن", "برای هفت روز برنامه مطالعه بساز"))
    }

    fun processUserQuery(
        query: String,
        profile: StudentProfileEntity,
        courses: List<CourseEntity>,
        attendanceList: List<AttendanceEntity>,
        grades: List<GradeEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        coursesWithSessions: List<CourseWithSessions> = emptyList(),
        curriculumCourses: List<CurriculumCourseEntity> = emptyList()
    ): CopilotMessage {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val gpa = computeGpa(grades, profile.declaredGpa)
        val totalActiveUnits = courses.sumOf { it.units }
        val majorCurriculum = curriculumCourses.ifEmpty { CurriculumSeedData.getCoursesForMajor(profile.major) }

        val todayWeekdayIdx = JalaliCalendarUtil.getTodayWeekdayIndex()
        val todayWeekdayName = JalaliCalendarUtil.getWeekdayName(todayWeekdayIdx)
        val tomorrowWeekdayIdx = JalaliCalendarUtil.getTomorrowWeekdayIndex()
        val tomorrowWeekdayName = JalaliCalendarUtil.getWeekdayName(tomorrowWeekdayIdx)
        val todayJalali = JalaliCalendarUtil.today().format("/")
        val tomorrowJalali = JalaliCalendarUtil.tomorrow().format("/")

        return when {
            // 1. Greetings & Pleasantries (سلام / درود)
            cleanQuery == "سلام" || cleanQuery == "درود" || cleanQuery.contains("خسته نباشید") || cleanQuery == "سلام علیکم" || cleanQuery == "hi" || cleanQuery == "hello" -> {
                val text = buildString {
                    append("سلام و درود ${profile.name} عزیز! 🎓\n\n")
                    append("من دستیار و مشاور تحصیلی شما در دانشگاه هستم. هم‌اکنون برای **ترم ${profile.currentSemester} ${profile.major}** با **$totalActiveUnits واحد درسی فعال** آماده ارائه خدمات زیر هستم:\n\n")
                    append("1. **برنامه امروز و این هفته:** زمان‌بندی کلاس‌ها، استاد و کلاس مربوطه\n")
                    append("2. **تقویم امتحانات و برنامه مطالعه:** بررسی تاریخ امتحانات و تنظیم جلسات پومودورو\n")
                    append("3. **رادار غیبت‌های ۳/۱۶:** محافظت در برابر حذف آموزشی ماده ۳۵\n")
                    append("4. **چارت و پیش‌نیازها:** بررسی دروس مجاز ترم بعد و دروس عقب‌افتاده\n")
                    append("5. **شبیه‌سازی نمرات و معدل الف:** پیش‌بینی معدل کل و شرایط شاگرد اولی\n\n")
                    append("چه موضوعی را مایلید بررسی کنیم؟")
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "فردا چه امتحانی دارم؟",
                        "این هفته چه کلاس‌هایی دارم؟",
                        "نمرات من را تحلیل کن."
                    )
                )
            }

            // 2. Identity & Capabilities (تو چی هستی؟ / کی هستی؟)
            cleanQuery.contains("چی هستی") || cleanQuery.contains("کی هستی") || cleanQuery.contains("معرفی خودت") || cleanQuery.contains("درباره خودت") || cleanQuery.contains("چه کارهایی") -> {
                val text = buildString {
                    append("من **Student OS Copilot** هستم؛ دستیار هوشمند و سیستم‌عامل جامع تحصیلی ویژه دانشجویان ایرانی 🎓\n\n")
                    append("📌 **توانمندی‌ها و وظایف اصلی من:**\n")
                    append("• **مدیریت هوشمند زمان‌بندی:** تحلیل تقویم کلاسی هفتگی و اطلاع‌رسانی کلاس‌های هر روز با ساعت و موقعیت مکانی.\n")
                    append("• **رادار دقیق غیبت‌ها (قانون ۳/۱۶):** پایش مداوم تعداد غیبت‌ها طبق ماده ۳۵ آیین‌نامه وزارت علوم و اعلام هشدار پیش از حذف درس.\n")
                    append("• **برنامه‌ریزی مطالعه امتحانات:** تدوین برنامه‌های مرور چندمرحله‌ای برای امتحانات پایان‌ترم و میان‌ترم همراه با تایمر پومودورو.\n")
                    append("• **تحلیل کارنامه و شبیه‌ساز معدل الف:** بررسی اثر نمرات هر درس بر معدل کل و محاسبه نمرات هدف برای کسب معدل بالای ۱۷.\n")
                    append("• **ناوبر چارت و پیش‌نیازها:** انطباق واحدهای گذرانده با سرفصل وزارت علوم و شناسایی دروس عقب‌افتاده یا مجاز برای انتخاب واحد.\n\n")
                    append("شما می‌توانید هر زمان سوالی درباره برنامه درسی، تکالیف، امتحانات یا قوانین آموزشی داشتید از من بپرسید!")
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "این هفته چه کلاس‌هایی دارم؟",
                        "برای من برنامه مطالعه بساز.",
                        "چه درس‌هایی عقب افتاده‌اند؟"
                    )
                )
            }

            // 3. Today's Schedule (برنامه امروز من چیست؟)
            cleanQuery.contains("امروز") && (cleanQuery.contains("برنامه") || cleanQuery.contains("کلاس") || cleanQuery.contains("درس") || cleanQuery.contains("دارم")) -> {
                val todaySessions = mutableListOf<Pair<CourseEntity, com.example.data.local.entity.CourseSessionEntity>>()

                if (coursesWithSessions.isNotEmpty()) {
                    coursesWithSessions.forEach { cws ->
                        cws.sessions.filter { it.day == todayWeekdayIdx }.forEach { sess ->
                            todaySessions.add(Pair(cws.course, sess))
                        }
                    }
                }

                val text = buildString {
                    append("📅 **برنامه کلاسی امروز ($todayWeekdayName - $todayJalali):**\n\n")
                    if (todaySessions.isNotEmpty()) {
                        todaySessions.sortBy { it.second.start }
                        todaySessions.forEach { (course, session) ->
                            val timeStr = "\u200E${session.start} - ${session.end}\u200E"
                            val loc = session.location.ifBlank { "دانشکده" }
                            val prof = course.professor.takeIf { it.isNotBlank() && it != "استاد دانشکده" }?.let { " | استاد: $it" } ?: ""
                            append("• **${course.name}** (${course.units} واحد)\n")
                            append("  ⏰ زمان: $timeStr | 📍 مکان: $loc$prof\n\n")
                        }
                        append("💡 برای ثبت حضور یا غیبت در کلاس‌های امروز می‌توانید به بخش رادار غیبت مراجعه کنید.")
                    } else if (courses.isNotEmpty()) {
                        append("🎉 امروز ($todayWeekdayName) هیچ کلاسی در برنامه هفتگی شما ثبت نشده است!\n\n")
                        append("می‌توانید از این فرصت برای پیشبرد تکالیف و مرور مباحث درسی با تایمر پومودورو استفاده کنید.")
                    } else {
                        append("هنوز هیچ درسی در برنامه هفتگی شما ثبت نشده است. می‌توانید از بخش برنامه هفتگی دروس خود را اضافه فرمایید.")
                    }
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده تقویم و برنامه هفتگی",
                    description = "مشاهده تقویم کامل هفتگی با زمان‌بندی روزهای مختلف",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("SCHEDULE"),
                    buttonLabel = "ورود به برنامه هفتگی"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "این هفته چه کلاس‌هایی دارم؟",
                        "فردا چه امتحانی دارم؟",
                        "تکالیف باقی‌مانده"
                    )
                )
            }

            // 4. Tomorrow's Exam (فردا چه امتحانی دارم؟)
            cleanQuery.contains("فردا") && (cleanQuery.contains("امتحان") || cleanQuery.contains("آزمون")) -> {
                val tomorrowExams = exams.filter { it.solarDate.trim() == tomorrowJalali }
                val text = buildString {
                    append("📝 **وضعیت امتحانات فردا ($tomorrowWeekdayName - $tomorrowJalali):**\n\n")
                    if (tomorrowExams.isNotEmpty()) {
                        append("🚨 **شما فردا امتحان دارید:**\n\n")
                        tomorrowExams.forEach { exam ->
                            val timeStr = "\u200E${exam.time}\u200E"
                            append("• **${exam.courseName}** (${exam.units} واحد)\n")
                            append("  ⏰ ساعت شروع: $timeStr | 📍 مکان: ${exam.location}\n\n")
                        }
                        append("🎯 **توصیه اضطراری مطالعه:** پیشنهاد می‌کنم فوراً یک برنامه مرور فشرده با پومودورو شروع کنید و امشب خواب کافی داشته باشید.")
                    } else {
                        append("✅ فردا ($tomorrowWeekdayName) هیچ امتحانی در تقویم تحصیلی شما ثبت نشده است.\n\n")
                        if (exams.isNotEmpty()) {
                            val sortedExams = exams.sortedBy { it.solarDate }
                            val nextExam = sortedExams.firstOrNull()
                            if (nextExam != null) {
                                append("📌 **نزدیک‌ترین امتحان پیش‌رو:**\n")
                                append("• **${nextExam.courseName}**: تاریخ ${nextExam.solarDate} ساعت \u200E${nextExam.time}\u200E (${nextExam.location})\n")
                            }
                        } else {
                            append("تاریخ هیچ امتحانی در برنامه ثبت نشده است.")
                        }
                    }
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "برنامه مطالعه پومودورو برای امتحان",
                    description = "تنظیم بازه‌های ۲۵ دقیقه‌ای تمرکز برای آمادگی امتحانات",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("POMODORO"),
                    buttonLabel = "ورود به تایمر مطالعه"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "برای امتحان فردا برنامه مطالعه بده.",
                        "برنامه امروز من چیست؟",
                        "این هفته چه کلاس‌هایی دارم؟"
                    )
                )
            }

            // 5. Exam Study Plan (برای امتحان فردا برنامه مطالعه بده.)
            (cleanQuery.contains("برنامه مطالعه") || cleanQuery.contains("برنامه ریزی مطالعه") || cleanQuery.contains("برنامه مرور")) && (cleanQuery.contains("امتحان") || cleanQuery.contains("فردا")) -> {
                val targetExam = exams.find { it.solarDate.trim() == tomorrowJalali } ?: exams.firstOrNull()
                val targetCourseName = targetExam?.courseName ?: courses.firstOrNull()?.name ?: "دروس اصلی"

                val text = buildString {
                    append("🎯 **برنامه استراتژیک مطالعه فشرده برای امتحان «$targetCourseName»:**\n\n")
                    append("این برنامه بر اساس متد علمی تقسیم‌بندی زمانی (Feynman + Pomodoro) طراحی شده است:\n\n")
                    append("🔹 **فاز اول (۲ پومودورو - ۵۰ دقیقه): مرور مفاهیم کلیدی و اسلایدها**\n")
                    append("• تیترها، فرمول‌های اصلی و نکات پرتکرار جزوه استاد را با هایلایت مرور کنید.\n")
                    append("• ۵ دقیقه استراحت کوتاه و نوشیدن آب.\n\n")
                    append("🔹 **فاز دوم (۳ پومودورو - ۷۵ دقیقه): حل نمونه سوالات و تمارین تحویلی**\n")
                    append("• نمونه سوالات امتحانات سال‌های گذشته و مسائل فصل‌های پرتکرار را بدون نگاه به پاسخ حل کنید.\n")
                    append("• ۱۵ دقیقه استراحت بلند (دور شدن کامل از گوشی و مانیتور).\n\n")
                    append("🔹 **فاز سوم (۲ پومودورو - ۵۰ دقیقه): خلاصه‌نویسی و برگه فرمول**\n")
                    append("• خلاصه یک‌صفحه‌ای از تمام روابط، تعاریف مهم و فرمول‌ها بنویسید (جعبه‌ابزار امتحان).\n\n")
                    append("🔹 **فاز چهارم (آرامش و تثبیت حافظه):**\n")
                    append("• حداقل ۷ ساعت خواب باکیفیت شبانه برای تثبیت سیناپس‌های حافظه بلندمدت الزامی است.\n")
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "شروع اولین پومودورو ۲۵ دقیقه‌ای",
                    description = "شروع مطالعه متمرکز بدون حواس‌پرتی",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("POMODORO"),
                    buttonLabel = "شروع تایمر پومودورو"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "فردا چه امتحانی دارم؟",
                        "نمرات من را تحلیل کن.",
                        "برنامه امروز من چیست؟"
                    )
                )
            }

            // 6. Weekly Schedule (این هفته چه کلاس‌هایی دارم؟)
            cleanQuery.contains("این هفته") || cleanQuery.contains("کلاس های این هفته") || cleanQuery.contains("کلاس‌های این هفته") || (cleanQuery.contains("هفته") && cleanQuery.contains("برنامه")) -> {
                val text = buildString {
                    append("🗓️ **برنامه جامع کلاسی این هفته (${profile.major}):**\n\n")

                    if (coursesWithSessions.isNotEmpty()) {
                        val days = listOf(
                            0 to "شنبه",
                            1 to "یکشنبه",
                            2 to "دوشنبه",
                            3 to "سه‌شنبه",
                            4 to "چهارشنبه",
                            5 to "پنج‌شنبه",
                            6 to "جمعه"
                        )
                        var totalSessionsCount = 0
                        days.forEach { (dayIdx, dayName) ->
                            val sessionsForDay = mutableListOf<Pair<CourseEntity, com.example.data.local.entity.CourseSessionEntity>>()
                            coursesWithSessions.forEach { cws ->
                                cws.sessions.filter { it.day == dayIdx }.forEach { sess ->
                                    sessionsForDay.add(Pair(cws.course, sess))
                                }
                            }
                            if (sessionsForDay.isNotEmpty()) {
                                totalSessionsCount += sessionsForDay.size
                                append("📌 **$dayName:**\n")
                                sessionsForDay.sortBy { it.second.start }
                                sessionsForDay.forEach { (c, s) ->
                                    val timeRange = "\u200E${s.start} - ${s.end}\u200E"
                                    val loc = s.location.ifBlank { "دانشکده" }
                                    append("• **${c.name}** | ساعت $timeRange | $loc\n")
                                }
                                append("\n")
                            }
                        }
                        if (totalSessionsCount == 0) {
                            append("در هیچ یک از روزهای هفته کلاسی ثبت نشده است.")
                        }
                    } else if (courses.isNotEmpty()) {
                        courses.forEach { c ->
                            append("• **${c.name}** (${c.units} واحد) | استاد: ${c.professor}\n")
                        }
                    } else {
                        append("هنوز هیچ درسی در برنامه هفتگی شما اضافه نشده است.")
                    }
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده جدول برنامه کلاسی",
                    description = "بررسی تداخل‌سنجی و ساعات کلاسی در تقویم هفتگی",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("SCHEDULE"),
                    buttonLabel = "ورود به جدول هفتگی"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "فردا چه امتحانی دارم؟",
                        "تحلیل سقف غیبت‌های ۳/۱۶"
                    )
                )
            }

            // 7. Grade Analysis (نمرات من را تحلیل کن.)
            cleanQuery.contains("نمرات") && (cleanQuery.contains("تحلیل") || cleanQuery.contains("بررسی") || cleanQuery.contains("وضعیت")) || (cleanQuery.contains("تحلیل") && cleanQuery.contains("کارنامه")) -> {
                val currentGpa = gpa
                val passedCount = grades.count { (it.midtermGrade + it.finalGrade) >= 10.0 }
                val failingCount = grades.count { (it.midtermGrade + it.finalGrade) < 10.0 && it.finalGrade > 0 }

                val text = buildString {
                    append("📊 **تحلیل آماری و آکادمیک کارنامه و نمرات:**\n\n")
                    append("• **معدل کل کنونی:** ${String.format(Locale.US, "%.2f", currentGpa)}\n")
                    append("• **تعداد واحدهای فعال این ترم:** $totalActiveUnits واحد\n")
                    append("• **واحدهای پاس‌شده قبلی:** ${profile.passedUnits} واحد\n\n")

                    if (grades.isNotEmpty()) {
                        append("📋 **وضعیت دروس ثبت‌شده:**\n")
                        grades.forEach { g ->
                            val avg = g.midtermGrade + g.finalGrade
                            val statusTag = if (avg >= 17.0) "🌟 عالی (الف)" else if (avg >= 12.0) "✅ مناسب" else if (avg >= 10.0) "⚠️ در لبه قبولی" else "❌ نیازمند تلاش"
                            append("• **${g.courseName}** (${g.units} واحد): میانگین ${String.format(Locale.US, "%.1f", avg)} — $statusTag\n")
                        }
                        append("\n")
                    }

                    if (currentGpa >= 17.0) {
                        append("🏆 **وضعیت الف:** شما در زمره دانشجویان ممتاز قرار دارید و در صورت حفظ این معدل، سقف انتخاب واحد ترم بعد برای شما **۲۴ واحد** خواهد بود.\n")
                    } else if (currentGpa < 12.0) {
                        append("⚠️ **هشدار مشروطی:** با معدل زیر ۱۲ سقف انتخاب واحد ترم آینده به ۱۴ واحد محدود می‌شود. تمرکز بر دروس ۳ واحدی توصیه می‌شود.\n")
                    } else {
                        val neededFor17 = if (totalActiveUnits > 0) {
                            val targetUnits = profile.passedUnits + totalActiveUnits
                            val targetScore = 17.0 * targetUnits
                            val currentScore = currentGpa * profile.passedUnits
                            ((targetScore - currentScore) / totalActiveUnits).coerceIn(0.0, 20.0)
                        } else 17.0
                        append("📈 برای رسیدن به **معدل الف (۱۷.۰۰)** در پایان این ترم، به میانگین نمره **${String.format(Locale.US, "%.2f", neededFor17)}** در دروس این ترم نیاز دارید.\n")
                    }
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "ورود به شبیه‌ساز زنده کارنامه",
                    description = "تغییر نمرات برای مشاهده اثر لحظه‌ای بر معدل",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("GRADES"),
                    buttonLabel = "شبیه‌ساز کارنامه"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "چه درس‌هایی عقب افتاده‌اند؟",
                        "پیش‌نیازها و دروس ترم بعد",
                        "برای من برنامه مطالعه بساز."
                    )
                )
            }

            // 8. Backlog / Missed Courses (چه درس‌هایی عقب افتاده‌اند؟)
            cleanQuery.contains("عقب افتاده") || cleanQuery.contains("عقب‌افتاده") || cleanQuery.contains("پاس نشده") || cleanQuery.contains("دروس مانده") -> {
                val currentSem = profile.currentSemester
                val activeNames = courses.map { it.name.trim() }.toSet()

                // Courses from earlier semesters not in active courses
                val earlierCurriculum = majorCurriculum.filter { it.recommendedSemester < currentSem }
                val missedCourses = earlierCurriculum.filter { cur ->
                    !activeNames.any { it.contains(cur.name) || cur.name.contains(it) }
                }

                val text = buildString {
                    append("🔍 **تحلیل دروس عقب‌افتاده از چارت مصوب ${profile.major}:**\n\n")
                    if (missedCourses.isNotEmpty()) {
                        append("دروس سرفصل ترم‌های ۱ تا ${currentSem - 1} که در برنامه ترم جاری شما نیستند:\n\n")
                        missedCourses.forEach { mc ->
                            val preInfo = if (mc.prerequisites.isNotBlank()) " (پیش‌نیاز: ${mc.prerequisites})" else ""
                            append("• **${mc.name}** (${mc.units} واحد - ترم مصوب ${mc.recommendedSemester})$preInfo\n")
                        }
                        append("\n💡 **توصیه آموزشی:** این دروس ممکن است پیش‌نیاز دروس تخصصی ترم‌های بالاتر باشند. اولویت انتخاب واحد در ترم‌های آینده باید با پاس کردن این دروس باشد.")
                    } else {
                        append("🎉 **وضعیت ایده‌آل:** شما هیچ درس عقب‌افتاده‌ای از ترم‌های قبلی چارت ${profile.major} ندارید و کاملاً همگام با چارت استاندارد پیش می‌روید!")
                    }
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده درخت پیش‌نیازها و چارت",
                    description = "نمایش ماتریس ترم‌های ۱ تا ۸ و گراف وابستگی",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("CURRICULUM"),
                    buttonLabel = "ورود به چارت تحصیلی"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "پیش‌نیازهای دروس ترم بعد",
                        "نمرات من را تحلیل کن.",
                        "برای من برنامه مطالعه بساز."
                    )
                )
            }

            // 9. Study Planner Generator (برای من برنامه مطالعه بساز.)
            cleanQuery.contains("برنامه مطالعه بساز") || cleanQuery.contains("برنامه ریزی کن") || cleanQuery.contains("برنامه مطالعه بده") || cleanQuery.contains("برنامه درسی برای من") -> {
                val pendingTasks = tasks.filter { !it.isCompleted }
                val activeCourseNames = courses.map { it.name }

                val text = buildString {
                    append("📅 **برنامه هفتگی مطالعه شخصی‌سازی شده (سیستم پومودورو):**\n\n")
                    append("این برنامه با توجه به واحدهای فعال ترم ${profile.currentSemester} و تکالیف باز شما چیده شده است:\n\n")

                    if (activeCourseNames.isNotEmpty()) {
                        activeCourseNames.take(4).forEachIndexed { index, courseName ->
                            val dayLabel = when (index) {
                                0 -> "شنبه و دوشنبه"
                                1 -> "یکشنبه و سه‌شنبه"
                                2 -> "چهارشنبه"
                                else -> "پنج‌شنبه"
                            }
                            append("🔹 **$dayLabel (مطالعه $courseName):**\n")
                            append("• ۲ پومودورو (۵۰ دقیقه) مرور جزوه و حل تمارین کلاسی\n")
                            append("• ۱۰ دقیقه استراحت چشمی و ذهنی\n\n")
                        }
                    }

                    if (pendingTasks.isNotEmpty()) {
                        append("📋 **تکالیف در اولویت اجرا:**\n")
                        pendingTasks.take(3).forEach { t ->
                            append("• انجام تکلیف «${t.title}» (درس: ${t.courseName})\n")
                        }
                        append("\n")
                    }

                    append("🎯 با فعال‌سازی تایمر پومودورو در برنامه می‌توانید هر جلسه مطالعه را با تمرکز کامل ثبت کنید.")
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "شروع پومودورو مطالعه",
                    description = "ورود به تایمر تمرکز عمیق ۲۵ دقیقه‌ای",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("POMODORO"),
                    buttonLabel = "ورود به تایمر تمرکز"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "فردا چه امتحانی دارم؟",
                        "نمرات من را تحلیل کن."
                    )
                )
            }

            // Emergency Course Drop (حذف اضطراری / تک‌درس)
            cleanQuery.contains("حذف اضطراری") || cleanQuery.contains("حذف تکدرس") || cleanQuery.contains("حذف تک درس") || cleanQuery.contains("حذف درس") -> {
                val text = buildString {
                    append("🛑 **آیین‌نامه حذف اضطراری (تک‌درس) طبق مقررات وزارت علوم:**\n\n")
                    append("• **مهلت زمانی:** تا ۵ هفته قبل از شروع امتحانات پایان‌ترم (با تأیید استاد درس و آموزش دانشکده).\n")
                    append("• **شرط کف واحد:** مجموع واحدهای باقی‌مانده پس از حذف نباید از **۱۲ واحد** کمتر شود.\n")
                    append("• **وضعیت شما در این ترم:** هم‌اکنون دارای **$totalActiveUnits واحد فعال** هستید.\n\n")
                    if (totalActiveUnits - 3 < 12) {
                        append("⚠️ **اخطار کف واحد:** با حذف یک درس ۳ واحدی، تعداد واحدهای شما به ${totalActiveUnits - 3} واحد می‌رسد که کمتر از کف قانونی (۱۲ واحد) است و به صورت سیستمی در گلستان تأیید نخواهد شد (مگر با مجوز کمیسیون موارد خاص).\n")
                    } else {
                        append("✅ **مجوز سیستمی:** با داشتن $totalActiveUnits واحد، می‌توانید ۱ درس (تا سقف ${totalActiveUnits - 12} واحد) را بدون نگرانی از کف واحد حذف اضطراری نمایید.\n")
                    }
                    append("• **نکته کارنامه:** درس حذف اضطراری در معدل کل و نیم‌سال محاسبه نمی‌شود و غیبت‌های آن حذف می‌گردد.")
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    suggestedQuickReplies = listOf("کف و سقف واحد در ترم", "تحلیل سقف غیبت‌ها", "شبیه‌ساز کارنامه")
                )
            }

            // Academic Probation & Dismissal Rules (مشروطی و اخراج آموزشی)
            cleanQuery.contains("مشروط") || cleanQuery.contains("اخراج") || cleanQuery.contains("معدل زیر ۱۲") || cleanQuery.contains("معدل زیر 12") || cleanQuery.contains("کمیسیون") -> {
                val text = buildString {
                    append("⚖️ **قوانین مشروطی و اخطار آموزشی (آیین‌نامه یکپارچه وزارت علوم):**\n\n")
                    append("• **حدنصاب مشروطی:** چنانچه معدل نیم‌سال دانشجو کمتر از **۱۲.۰۰** باشد، آن ترم مشروط محاسبه می‌شود.\n")
                    append("• **سقف واحد ترم بعد:** دانشجوی مشروط در ترم بعد مجاز به اخذ حداکثر **۱۴ واحد** است.\n")
                    append("• **حداکثر دفعات مشروطی:**\n")
                    append("  - مقطع کارشناسی پیوسته: حداکثر **۳ نیم‌سال متوالی** یا **۴ نیم‌سال متناوب** مجاز به مشروطی است و پس از آن پرونده به کمیسیون موارد خاص ارجاع می‌شود.\n")
                    append("• **حداقل نمره قبولی هر درس:** نمره **۱۰.۰۰** برای دروس کارشناسی.\n\n")
                    append("💡 **راهکار جبران:** با تنظیم نمرات در شبیه‌ساز کارنامه می‌توانید راهبرد رسیدن به معدل بالای ۱۲ را ترسیم کنید.")
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "ورود به شبیه‌ساز کارنامه و معدل",
                    description = "تغییر نمرات پیش‌بینی شده برای محاسبه آنی معدل ترم و کل",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("GRADES"),
                    buttonLabel = "شبیه‌ساز زنده نمرات"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf("شبیه‌ساز کارنامه", "سقف و کف انتخاب واحد", "تکالیف باقی‌مانده")
                )
            }

                        // Deterministic daily priority / command-center query
            cleanQuery.contains("مهم‌ترین") ||
                cleanQuery.contains("اولویت") ||
                cleanQuery.contains("الان چی") ||
                cleanQuery.contains("الان چه کاری") ||
                cleanQuery.contains("چی کار کنم") -> {
                val priority = AcademicPriorityEngine.topOrNull(
                    courses = courses,
                    coursesWithSessions = coursesWithSessions,
                    attendance = attendanceList,
                    tasks = tasks,
                    exams = exams,
                    todayWeekdayIndex = todayWeekdayIdx,
                    todayDate = todayJalali,
                    minuteOfDay = java.util.Calendar.getInstance().let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }
                )

                val text = if (priority == null) {
                    "✅ در حال حاضر اولویت بحرانی یا فوری از روی داده‌های ثبت‌شده پیدا نشد. می‌توانی برنامه امروز یا کارهای باز را بررسی کنی."
                } else {
                    buildString {
                        append("🎯 **اولویت اصلی الان:**\n\n")
                        append("**" + priority.title + "**\n")
                        append(priority.reason)
                        if (!priority.courseName.isNullOrBlank()) {
                            append("\n\n📚 درس مرتبط: " + priority.courseName)
                        }
                        if (!priority.dueDate.isNullOrBlank()) {
                            append("\n📅 موعد: " + priority.dueDate)
                        }
                    }
                }

                val proposal = when (priority?.kind) {
                    AcademicPriorityKind.CRITICAL_ATTENDANCE ->
                        CopilotActionProposal(
                            id = UUID.randomUUID().toString(),
                            title = "باز کردن رادار غیبت",
                            description = "بررسی وضعیت حضور و غیاب درس در معرض خطر",
                            impactType = ActionImpactType.SAFE_QUERY,
                            payload = CopilotPayload.NavigateToTab("ATTENDANCE"),
                            buttonLabel = "رادار غیبت"
                        )
                    AcademicPriorityKind.OVERDUE_TASK,
                    AcademicPriorityKind.TASK_TODAY,
                    AcademicPriorityKind.EXAM_LINKED_TASK,
                    AcademicPriorityKind.OPEN_TASK ->
                        CopilotActionProposal(
                            id = UUID.randomUUID().toString(),
                            title = "باز کردن کارهای درسی",
                            description = "مشاهده و تعیین تکلیف فعالیت‌های باز",
                            impactType = ActionImpactType.SAFE_QUERY,
                            payload = CopilotPayload.NavigateToTab("TASKS"),
                            buttonLabel = "مشاهده کارها"
                        )
                    AcademicPriorityKind.EXAM_TODAY ->
                        CopilotActionProposal(
                            id = UUID.randomUUID().toString(),
                            title = "مشاهده امتحان امروز",
                            description = "بررسی جزئیات امتحان و ورود به برنامه امتحانات",
                            impactType = ActionImpactType.SAFE_QUERY,
                            payload = CopilotPayload.NavigateToTab("EXAMS"),
                            buttonLabel = "امتحانات"
                        )
                    AcademicPriorityKind.NEXT_CLASS ->
                        CopilotActionProposal(
                            id = UUID.randomUUID().toString(),
                            title = "باز کردن برنامه کلاسی",
                            description = "مشاهده جلسه بعدی و برنامه روز",
                            impactType = ActionImpactType.SAFE_QUERY,
                            payload = CopilotPayload.NavigateToTab("SCHEDULE"),
                            buttonLabel = "برنامه کلاسی"
                        )
                    else -> null
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "تکالیف عقب‌افتاده من چیست؟",
                        "فردا چه امتحانی دارم؟"
                    )
                )
            }

// Attendance & 3/16 law analysis
            cleanQuery.contains("غیبت") || cleanQuery.contains("حضور") || cleanQuery.contains("رادار") || cleanQuery.contains("3/16") || cleanQuery.contains("۳/۱۶") -> {
                val dangerous = attendanceList.filter { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
                val warning = attendanceList.filter { it.absentCount == it.maxAllowed - 1 && it.maxAllowed > 0 }
                val safe = attendanceList.filter { it.absentCount < it.maxAllowed - 1 }

                val text = buildString {
                    append("🛡️ **تحلیل رادار هوشمند غیبت‌ها (قانون ۳/۱۶ وزارت علوم - ماده ۳۵):**\n\n")
                    if (dangerous.isNotEmpty()) {
                        append("🚨 **دروس در وضعیت بحرانی (رسیده به سقف حذف ماده ۳۵):**\n")
                        dangerous.forEach {
                            append("• **${it.courseName}**: ${it.absentCount} غیبت از سقف مجاز ${it.maxAllowed} جلسه\n")
                        }
                        append("\n⚠️ **توصیه فوری:** در این دروس نباید حتی ۱ جلسه دیگر غیبت داشته باشید تا مشمول حذف آموزشی نشوید.\n\n")
                    }
                    if (warning.isNotEmpty()) {
                        append("⚠️ **دروس در آستانه خطر (تنها ۱ جلسه تا سقف مجاز):**\n")
                        warning.forEach {
                            append("• **${it.courseName}**: ${it.absentCount} غیبت (سقف مجاز: ${it.maxAllowed} جلسه)\n")
                        }
                        append("\n")
                    }
                    if (safe.isNotEmpty()) {
                        append("✅ **دروس در حاشیه امن:**\n")
                        safe.take(4).forEach {
                            val remainingSafe = (it.maxAllowed - it.absentCount).coerceAtLeast(0)
                            append("• **${it.courseName}**: ${it.absentCount} غیبت (حاشیه امن: $remainingSafe جلسه دیگر)\n")
                        }
                    }
                    if (attendanceList.isEmpty()) {
                        append("هنوز کلاسی در رادار حضور و غیاب ثبت نشده است.")
                    }
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    suggestedQuickReplies = listOf("مشاهده رادار کامل غیبت‌ها", "امتحانات پایان‌ترم", "برنامه هفتگی")
                )
            }

            // Next semester & Prerequisite tree analysis
            cleanQuery.contains("پیش‌نیاز") || cleanQuery.contains("ترم بعد") || cleanQuery.contains("انتخاب واحد") || cleanQuery.contains("چارت") || cleanQuery.contains("واحد") -> {
                val nextSemester = (profile.currentSemester + 1).coerceAtMost(8)
                val nextSemCourses = majorCurriculum.filter { it.recommendedSemester == nextSemester }
                val currentSemCourses = majorCurriculum.filter { it.recommendedSemester == profile.currentSemester }

                val text = buildString {
                    append("🗺️ **تحلیل تخصصی چارت ${profile.major} و پیش‌نیازها برای ترم $nextSemester:**\n\n")

                    if (nextSemCourses.isNotEmpty()) {
                        append("دروس مصوب ترم $nextSemester در چارت دانشگاه:\n")
                        nextSemCourses.forEach { c ->
                            val preReqInfo = if (c.prerequisites.isNotBlank()) " | پیش‌نیاز: ${c.prerequisites}" else " | بدون پیش‌نیاز"
                            val coReqInfo = if (c.coRequisites.isNotBlank()) " | هم‌نیاز: ${c.coRequisites}" else ""
                            append("• **${c.name}** (${c.units} واحد - نوع: ${c.courseType})$preReqInfo$coReqInfo\n")
                        }
                    } else {
                        append("دروس پایانی شامل پروژه کارشناسی، کارآموزی صنعتی و دروس اختیاری تخصصی هستند.\n")
                    }

                    append("\n📌 **توصیه استراتژیک آموزشی:**\n")
                    if (currentSemCourses.isNotEmpty()) {
                        val criticalPreReqs = currentSemCourses.take(2).map { it.name }.joinToString(" و ")
                        append("پاس کردن دروس «$criticalPreReqs» در ترم جاری شرط اساسی برای اخذ بدون مانع دروس ترم $nextSemester خواهد بود.\n")
                    }
                    val creditCeiling = if (gpa >= 17.0) 24 else if (gpa < 12.0) 14 else 20
                    append("با توجه به معدل فعلی شما (${String.format(Locale.US, "%.2f", gpa)})، سقف مجاز انتخاب واحد شما در ترم آینده **$creditCeiling واحد** است.")
                }

                val proposal = CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده درخت کامل چارت و پیش‌نیازها",
                    description = "نمایش ماتریس ترم‌های ۱ تا ۸ و گراف وابستگی دروس",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("CURRICULUM"),
                    buttonLabel = "ورود به چارت تحصیلی"
                )

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf("مشاهده چارت کامل", "چند واحد تا فارغ‌التحصیلی مانده؟", "شبیه‌ساز معدل الف")
                )
            }

            // Tasks & Deadlines
            cleanQuery.contains("تکلیف") || cleanQuery.contains("پروژه") || cleanQuery.contains("تسک") || cleanQuery.contains("تمرین") -> {
                val pending = tasks.filter { !it.isCompleted }
                val text = buildString {
                    append("📋 **وضعیت تکالیف و پروژه‌های درسی:**\n\n")
                    if (pending.isNotEmpty()) {
                        append("تکالیف فعال شما:\n")
                        pending.forEach {
                            append("• **${it.title}** (درس: ${it.courseName}) — موعد: ${it.dueDate}\n")
                        }
                    } else {
                        append("🎉 فوق‌العاده است! تمام تکالیف شما تکمیل شده‌اند و هیچ تسک معوقه‌ای ندارید.\n")
                    }
                }

                val proposal = GroundedStudentAssistant.completionProposal(query, tasks) ?: CopilotActionProposal(
                    UUID.randomUUID().toString(), "مشاهدهٔ تکلیف‌ها", "بررسی کارهای ثبت‌شده", ActionImpactType.SAFE_QUERY,
                    CopilotPayload.NavigateToTab("TASKS"), "بازکردن تکلیف‌ها")

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    proposedAction = proposal,
                    suggestedQuickReplies = listOf("افزودن تکلیف جدید", "برنامه هفتگی", "امتحانات")
                )
            }

            // Default intelligent response
            else -> {
                val text = buildString {
                    append("💡 در پاسخ به پرسش شما درباره «$query»:\n\n")
                    append("به عنوان مشاور تحصیلی شما، روی بخش‌های زیر آماده خدمت‌رسانی هستم:\n")
                    append("1. **برنامه امروز و این هفته:** بررسی زمان کلاس‌ها و مکان تشکیل\n")
                    append("2. **تقویم امتحانات و برنامه مطالعه:** بررسی تاریخ آزمون‌ها و جلسات پومودورو\n")
                    append("3. **تحلیل غیبت‌ها و سقف مجاز ۳/۱۶** برای تمام دروس (ماده ۳۵)\n")
                    append("4. **چارت مصوب ${profile.major}**، پیش‌نیازها و دروس عقب‌افتاده\n")
                    append("5. **شبیه‌سازی نمرات و محاسبه معدل الف (بالای ۱۷)**\n\n")
                    append("می‌توانید یکی از گزینه‌های زیر را لمس فرمایید:")
                }

                CopilotMessage(
                    id = UUID.randomUUID().toString(),
                    sender = CopilotSender.COPILOT,
                    text = text,
                    timestamp = currentTimeString(),
                    suggestedQuickReplies = listOf(
                        "برنامه امروز من چیست؟",
                        "فردا چه امتحانی دارم؟",
                        "این هفته چه کلاس‌هایی دارم؟",
                        "نمرات من را تحلیل کن.",
                        "چه درس‌هایی عقب افتاده‌اند؟"
                    )
                )
            }
        }
    }

    suspend fun processUserQueryAsync(
        query: String,
        profile: StudentProfileEntity,
        courses: List<CourseEntity>,
        attendanceList: List<AttendanceEntity>,
        grades: List<GradeEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        coursesWithSessions: List<CourseWithSessions> = emptyList(),
        curriculumCourses: List<CurriculumCourseEntity> = emptyList(),
        conversationHistory: List<CopilotMessage> = emptyList(),
        kind: String = "chat",
        studyHistory: List<com.example.domain.model.StudyDay> = emptyList(),
        dailyGoalMinutes: Int = 120,
        focusMinutes: Int = 25,
        localOnly: Boolean = false,
        adviceProvider: suspend (String, String, List<Pair<String, String>>, String) -> Result<String> = com.example.data.api.AcademicAiApiClient::advice
    ): CopilotMessage {
        val local = GroundedStudentAssistant.respond(query, kind, profile, courses, coursesWithSessions, attendanceList,
            grades, tasks, exams, studyHistory, dailyGoalMinutes, focusMinutes)
        if (localOnly || (kind == "chat" && local != null)) return local ?: CopilotMessage(
            UUID.randomUUID().toString(), CopilotSender.COPILOT,
            "در تحلیل روی دستگاه می‌توانم برنامه، تکلیف‌ها، نمره‌های ثبت‌شده و اولویت مطالعه را بررسی کنم. برای توضیح آزاد این پرسش، حالت پاسخ مدل و اتصال سرویس لازم است.",
            currentTimeString(), confidenceBadge = "تحلیل روی دستگاه · پاسخ مولد دریافت نشده")
        val studentContext = AcademicAssistantContext.build(profile, courses, coursesWithSessions, attendanceList, grades, tasks, exams,
            studyHistory, dailyGoalMinutes, focusMinutes, recentLocalAnalysis = conversationHistory.filter {
                it.responseSource == com.example.domain.model.CopilotResponseSource.LOCAL_ANALYSIS
            }.map { it.text })
        val historyTurns = conversationHistory
            .filter { it.sender == CopilotSender.USER || (it.sender == CopilotSender.COPILOT && it.responseSource == com.example.domain.model.CopilotResponseSource.ONLINE_MODEL) }
            .dropLastWhile { it.sender == CopilotSender.USER && it.text.trim() == query.trim() }.takeLast(8)
            .map { (if (it.sender == CopilotSender.USER) "user" else "model") to it.text }
        val result = adviceProvider(query, studentContext, historyTurns, kind)
        val answer = result.getOrNull()?.trim()
        if (!answer.isNullOrBlank()) return CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT, answer,
            currentTimeString(), confidenceBadge = "پاسخ مدل آنلاین", responseSource = com.example.domain.model.CopilotResponseSource.ONLINE_MODEL, proposedAction = detectActionProposal(query, answer, tasks, courses, profile),
            suggestedQuickReplies = generateDynamicQuickReplies(query, answer))
        val error = com.example.data.api.AcademicAiApiClient.userMessage(result.exceptionOrNull() ?: IllegalStateException("پاسخ مدل خالی بود."))
        return local?.copy(text = "$error\n\n${local.text}", confidenceBadge = "تحلیل روی دستگاه · پاسخ مدل دریافت نشد", responseSource = com.example.domain.model.CopilotResponseSource.ONLINE_UNAVAILABLE)
            ?: CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT,
                "$error\n\nپاسخ مولد دریافت نشد. می‌توانم بدون اینترنت تکلیف‌ها، برنامهٔ کلاس‌ها یا نمره‌های ثبت‌شده را بررسی کنم.",
                currentTimeString(), confidenceBadge = "پاسخ مدل دریافت نشد", responseSource = com.example.domain.model.CopilotResponseSource.ONLINE_UNAVAILABLE, suggestedQuickReplies = listOf("برنامه امروز من چیست؟", "تکالیف من را بگو"))
    }

    private fun detectActionProposal(
        query: String,
        aiText: String,
        tasks: List<TaskEntity>,
        courses: List<CourseEntity>,
        profile: StudentProfileEntity
    ): CopilotActionProposal? {
        val q = query.lowercase(Locale.ROOT)
        val t = aiText.lowercase(Locale.ROOT)

        return when {
            q.contains("کارنامه") || q.contains("معدل") || q.contains("نمره") -> {
                CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "ورود به شبیه‌ساز کارنامه و معدل",
                    description = "تغییر نمرات میان‌ترم و پایان‌ترم برای محاسبه بلادرنگ معدل",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("GRADES"),
                    buttonLabel = "شبیه‌ساز کارنامه"
                )
            }
            q.contains("چارت") || q.contains("پیش‌نیاز") || q.contains("ترم بعد") || q.contains("عقب افتاده") -> {
                CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده درخت پیش‌نیازها و چارت",
                    description = "نمایش ماتریس ترم‌های ۱ تا ۸ و دروس مجاز انتخاب واحد",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("CURRICULUM"),
                    buttonLabel = "چارت تحصیلی"
                )
            }
            q.contains("پومودورو") || q.contains("تمرکز") || q.contains("مطالعه") || q.contains("امتحان") -> {
                CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "شروع جلسه تمرکز عمیق (پومودورو)",
                    description = "تنظیم بازه ۲۵ دقیقه‌ای مطالعه همراه با موسیقی آرامش‌بخش",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("POMODORO"),
                    buttonLabel = "تایمر تمرکز"
                )
            }
            q.contains("غیبت") || q.contains("حضور") -> {
                CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده رادار غیبت‌های کلاسی",
                    description = "بررسی غیبت‌های ثبت‌شده و سقف تنظیم‌شده",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("ATTENDANCE"),
                    buttonLabel = "رادار غیبت‌ها"
                )
            }
            q.contains("برنامه") || q.contains("کلاس") || q.contains("ساعت") -> {
                CopilotActionProposal(
                    id = UUID.randomUUID().toString(),
                    title = "مشاهده تقویم و برنامه هفتگی",
                    description = "بررسی زمان‌بندی روزانه کلاس‌ها و تداخل‌سنجی",
                    impactType = ActionImpactType.SAFE_QUERY,
                    payload = CopilotPayload.NavigateToTab("SCHEDULE"),
                    buttonLabel = "برنامه هفتگی"
                )
            }
            q.contains("تکلیف") || q.contains("پروژه") || q.contains("تسک") -> {
                GroundedStudentAssistant.completionProposal(query, tasks) ?: CopilotActionProposal(
                    UUID.randomUUID().toString(), "مشاهدهٔ تکلیف‌ها", "بررسی کارهای ثبت‌شده", ActionImpactType.SAFE_QUERY,
                    CopilotPayload.NavigateToTab("TASKS"), "بازکردن تکلیف‌ها")
            }
            else -> null
        }
    }

    private fun generateDynamicQuickReplies(query: String, aiText: String): List<String> {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("امروز") || q.contains("کلاس") || q.contains("برنامه") -> listOf(
                "فردا چه امتحانی دارم؟",
                "این هفته چه کلاس‌هایی دارم؟",
                "برای من برنامه مطالعه بساز."
            )
            q.contains("امتحان") || q.contains("استرس") -> listOf(
                "برای امتحان فردا برنامه مطالعه بده.",
                "تایمر پومودورو برای مطالعه",
                "برنامه امروز من چیست؟"
            )
            q.contains("غیبت") || q.contains("حضور") -> listOf(
                "قوانین حذف اضطراری تک‌درس",
                "سقف غیبت در دروس آزمایشگاهی",
                "چطور غیبت موجه بگیرم؟"
            )
            q.contains("معدل") || q.contains("مشروط") || q.contains("نمره") -> listOf(
                "شرایط دانشجوی ممتاز (معدل الف)",
                "سقف انتخاب واحد در ترم بعد",
                "چه درس‌هایی عقب افتاده‌اند؟"
            )
            q.contains("پیش‌نیاز") || q.contains("ترم بعد") || q.contains("چارت") || q.contains("عقب") -> listOf(
                "هم‌نیازی و اخذ همزمان دو درس",
                "چند واحد تا فارغ‌التحصیلی مانده؟",
                "معرفی به استاد چگونه است؟"
            )
            else -> listOf(
                "برنامه امروز من چیست؟",
                "فردا چه امتحانی دارم؟",
                "نمرات من را تحلیل کن.",
                "این هفته چه کلاس‌هایی دارم؟"
            )
        }
    }

    fun computeGpa(grades: List<GradeEntity>, declaredGpa: Double? = null): Double {
        val evaluated = grades.filter { it.hasRecordedScore() }
        return if (evaluated.isNotEmpty() && evaluated.sumOf { it.units } > 0) {
            val totalWeighted = evaluated.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
            val totalU = evaluated.sumOf { it.units }
            val computedGpa = totalWeighted / totalU
            computedGpa.coerceIn(0.0, 20.0)
        } else {
            (declaredGpa?.takeIf { it > 0.0 } ?: 0.0).coerceIn(0.0, 20.0)
        }
    }

    private fun currentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }
}
