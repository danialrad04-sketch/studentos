package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.data.local.util.DateTimeNormalizer
import com.example.domain.model.*
import com.example.domain.util.*
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Local academic reasoning. All generated study durations are explicitly proposals. */
object GroundedStudentAssistant {
    fun normalize(text: String): String = DateTimeNormalizer.normalizeDigits(text).replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').trim().replace(Regex("\\s+"), " ")
    fun completionProposal(query: String, tasks: List<TaskEntity>): CopilotActionProposal? {
        val q = normalize(query)
        if (listOf("نکن", "نشده", "نمی", "کنم", "آیا", "انجام بدهم").any { q.contains(it) }) return null
        if (!Regex("انجام (دادم|شد)|تمام کردم|تموم کردم|کامل کردم|تکمیل کردم|تکمیل کن(?:[\\s.!؟]|$)").containsMatchIn(q)) return null
        val task = tasks.filter { !it.isCompleted && normalize(it.title).isNotBlank() && q.contains(normalize(it.title)) }.singleOrNull() ?: return null
        return CopilotActionProposal(UUID.randomUUID().toString(), "تکمیل «${task.title}»", "فقط پس از تأیید تو ذخیره می‌شود.",
            ActionImpactType.REQUIRES_CONFIRMATION, CopilotPayload.CompleteTask(task.id.toString(), task.title), "تأیید انجام تکلیف")
    }
    private fun nav(tab: String, title: String) = CopilotActionProposal(UUID.randomUUID().toString(), title, "بازکردن بخش مربوط به این تحلیل",
        ActionImpactType.SAFE_QUERY, CopilotPayload.NavigateToTab(tab), title)
    private fun message(text: String, action: CopilotActionProposal? = null) = CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT,
        text, java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date()), proposedAction = action,
        confidenceBadge = "تحلیل روی دستگاه · داده‌های ثبت‌شده", responseSource = CopilotResponseSource.LOCAL_ANALYSIS, suggestedQuickReplies = listOf("اول کدام تکلیف را انجام بدهم؟", "برای هفت روز برنامه مطالعه بساز", "نمره‌های ثبت‌شده من را بررسی کن"))
    private fun day(date: JalaliDate): Long {
        val (y,m,d) = JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day)
        return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { clear();set(y,m-1,d) }.timeInMillis / 86400000
    }
    private fun add(date: JalaliDate, amount: Int): JalaliDate {
        val (y,m,d)=JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day)
        val cal=Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { clear();set(y,m-1,d);add(Calendar.DAY_OF_MONTH,amount) }
        return JalaliCalendarUtil.gregorianToJalali(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH))
    }
    fun respond(query: String, kind: String, profile: StudentProfileEntity, courses: List<CourseEntity>, sessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>, grades: List<GradeEntity>, tasks: List<TaskEntity>, exams: List<ExamItem>, history: List<StudyDay> = emptyList(),
        dailyGoal: Int = 120, focusMinutes: Int = 25, now: Long = System.currentTimeMillis()): CopilotMessage? {
        val q=normalize(query);val cal=Calendar.getInstance().apply { timeInMillis=now }
        val today=JalaliCalendarUtil.gregorianToJalali(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH))
        val active=courses.filterNot { it.isArchived };val open=tasks.filterNot { it.isCompleted }
        val conceptual=listOf("چیست", "چیه", "توضیح", "تعریف", "یاد بده").any { q.contains(it) }
        val personal=listOf(" من", "من ", "دارم", "امروز", "فردا", "بعدی", "نزدیک", "ثبت شده", "معدلم", "نمره ام", "اولویت", "کدام", "کی ").any { q.contains(it) }
        val dataQuestion=(!conceptual && (personal || q.length<18)) || listOf("معدل من", "معدلم", "نمره من", "ثبت شده", "برنامه امروز", "برنامه فردا").any { q.contains(it) }
        val minutes=Regex("(?:فقط|روزانه|روزی|هر روز)\\s*(\\d{1,3})\\s*(دقیقه|ساعت)").find(q)?.let {
            val amount=it.groupValues[1].toInt(); (amount * if(it.groupValues[2]=="ساعت")60 else 1).takeIf { value -> value in 5..480 }
        } ?: dailyGoal
        completionProposal(q,tasks)?.let { return message("درخواست تکمیل «${(it.payload as CopilotPayload.CompleteTask).taskTitle}» آمادهٔ تأیید است. هنوز تغییری ذخیره نشده است.",it) }
        if (kind=="coach" || (q.contains("برنامه مطالعه") && (!conceptual || personal)) || q.contains("اولویت") || q.contains("اول کدام")) return message(plan(today,cal.get(Calendar.DAY_OF_WEEK)%7,active,sessions,open,exams,grades,minutes,focusMinutes),nav("POMODORO","بازکردن تایمر تمرکز"))
        if (kind=="review" || (dataQuestion && (q.contains("نمره") || q.contains("معدل") || q.contains("پیشرفت")))) {
            val recorded=AcademicAssistantContext.recordedGrades(grades)
            val stats=StudyStatisticsEngine.calculate(history,java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date(now)))
            return message(buildString {
                append("**بررسی اطلاعات ثبت‌شده**\n")
                append(if(recorded.isEmpty()) "نمره‌ای ثبت نشده؛ معدل نمره‌های ثبت‌شده نامشخص است.\n" else "معدل ${recorded.size} نمرهٔ ثبت‌شده: ${String.format(Locale.US,"%.2f",recorded.sumOf { (it.midtermGrade+it.finalGrade)*it.units }/recorded.sumOf { it.units })}\n")
                profile.declaredGpa?.let { append("معدل کل واردشده توسط تو: $it؛ این مقدار جدا از معدل نمره‌های ثبت‌شده است.\n") }
                append("مطالعهٔ ثبت‌شده در هفت روز اخیر: ${stats.lastSevenDaysMinutes} دقیقه؛ هفت روز قبل: ${stats.previousSevenDaysMinutes} دقیقه.\n")
                append("${open.size} تکلیف باز داری. زمان ثبت‌نشدهٔ مطالعه را صفرِ عملکرد واقعی فرض نمی‌کنم.\n\n**سه قدم بعدی**\n")
                append("۱. ${if(recorded.isEmpty()) "نمره‌های واقعی را ثبت کن تا تحلیل دقیق‌تر شود." else recorded.minBy { it.midtermGrade+it.finalGrade }.let { "مبانی ${it.courseName} را با چند سؤال تشخیصی مرور کن." }}\n")
                append("۲. ${open.minByOrNull { JalaliCalendarUtil.parse(it.dueDate) ?: JalaliDate(9999,1,1) }?.let { "تکلیف «${it.title}» با موعد ${it.dueDate} را بررسی کن." } ?: "برای درس دشوارتر یک هدف کوچک و قابل اندازه‌گیری بنویس."}\n")
                append("۳. یک جلسهٔ ${focusMinutes.coerceIn(5,120)} دقیقه‌ای مطالعه انجام بده و نتیجه را ثبت کن؛ مدت پیشنهادی است.")
            },nav("GRADES","بازکردن کارنامه"))
        }
        if (kind=="exam" || (dataQuestion && (q.contains("امتحان") || q.contains("آزمون")))) {
            val upcoming=exams.mapNotNull { e -> JalaliCalendarUtil.parse(e.solarDate)?.takeIf { it>=today }?.let { it to e } }.sortedBy { it.first }
            val next=upcoming.firstOrNull()?.second ?: return message("امتحان آینده‌ای با تاریخ معتبر ثبت نشده است. تاریخ و سرفصل را مشخص کن تا برنامهٔ مرور بر اساس اطلاعات واقعی ساخته شود.",nav("EXAMS","بازکردن امتحان‌ها"))
            return message("**مرور ${next.courseName}**\nتاریخ ثبت‌شده: ${next.solarDate}؛ ساعت: ${next.time.ifBlank { "ثبت نشده" }}.\n\n۱. سرفصل‌ها و نمونه‌سؤال واقعی درس را مشخص کن؛ محتوای جزوه‌ات در اختیار من نیست.\n۲. در یک جلسهٔ ${focusMinutes.coerceIn(5,120)} دقیقه‌ای، بدون نگاه به جزوه مطالب اصلی را بازگو کن.\n۳. یک تمرین حل کن، خطاها را جدا بنویس و در جلسهٔ بعد فقط نقاط ضعیف را مرور کن.\n\nاین پیشنهاد روش مطالعه است؛ در تقویم ذخیره نشده و سرفصل یا سؤال واقعی آزمون را حدس نمی‌زنم.",nav("EXAMS","بازکردن امتحان‌ها"))
        }
        if (dataQuestion && (q.contains("تکلیف") || q.contains("تکالیف") || q.contains("پروژه") || q.contains("تسک"))) return message(buildString {
            append("**تکلیف‌های ثبت‌شده**\n")
            if(open.isEmpty()) append("تکلیف بازی ثبت نشده است؛ این به معنی اطلاع از تکالیف خارج از برنامه نیست.")
            open.sortedBy { JalaliCalendarUtil.parse(it.dueDate) ?: JalaliDate(9999,1,1) }.take(12).forEach { t ->
                val due=JalaliCalendarUtil.parse(t.dueDate);append("• ${t.title} — ${due?.format() ?: "موعد نامشخص"}${if(due!=null && due<today) " · عقب‌افتاده" else ""}\n")
            }
            if(Regex("تکمیل کن|انجام دادم").containsMatchIn(q)) append("برای تغییر وضعیت، نام دقیق یک تکلیف را مشخص کن؛ بدون انتخاب روشن چیزی تکمیل نمی‌شود.")
        },nav("TASKS","بازکردن تکلیف‌ها"))
        if(dataQuestion && (q.contains("غیبت") || q.contains("حضور"))) return message(if(attendance.isEmpty()) "اطلاعات حضور و غیاب ثبت نشده است؛ سقف مجاز دانشگاهت را حدس نمی‌زنم." else attendance.joinToString("\n",prefix="**بر اساس شمارش و سقف تنظیم‌شدهٔ خودت**\n") {
            "• ${it.courseName}: ${it.absentCount} غیبت ثبت‌شده، سقف تنظیم‌شده ${it.maxAllowed}؛ ${(it.maxAllowed-it.absentCount).coerceAtLeast(0)} جلسه تا این سقف."
        }+"\nمقررات رسمی درس را با آموزش بررسی کن.",nav("ATTENDANCE","بازکردن حضور و غیاب"))
        if((dataQuestion && q.contains("کلاس")) || q.contains("برنامه امروز") || q.contains("برنامه فردا")) {
            val targetDay=(cal.get(Calendar.DAY_OF_WEEK)%7+if(q.contains("فردا"))1 else 0)%7
            val rows=sessions.filterNot { it.isArchived }.flatMap { c -> c.sessions.filter { it.day==targetDay && validSession(it) }.map { c.course to it } }.sortedBy { DateTimeNormalizer.timeToMinutes(it.second.start) }
            return message(if(rows.isEmpty()) "برای ${if(q.contains("فردا")) "فردا" else "امروز"} جلسه‌ای با ساعت معتبر ثبت نشده است؛ از این داده نمی‌توان آزادبودن زمانت را نتیجه گرفت." else rows.joinToString("\n",prefix="**جلسه‌های ثبت‌شده**\n") { (c,s)->"• ${c.name} · ${s.start} تا ${s.end} · ${s.location.ifBlank { "مکان ثبت نشده" }}" },nav("SCHEDULE","بازکردن برنامهٔ کلاس‌ها"))
        }
        return null
    }
    private fun validSession(s: CourseSessionEntity): Boolean {
        val start=AcademicInputValidator.time(s.start);val end=AcademicInputValidator.time(s.end)
        return s.day in 0..6 && start!=null && end!=null && start<end
    }
    private data class Work(val label:String,val deadline:Int,val reason:String,var blocks:Int,val allowCatchup:Boolean=false)
    private fun plan(today:JalaliDate,weekday:Int,courses:List<CourseEntity>,sessions:List<CourseWithSessions>,tasks:List<TaskEntity>,exams:List<ExamItem>,grades:List<GradeEntity>,goal:Int,focus:Int):String {
        if(courses.isEmpty() && tasks.isEmpty() && exams.isEmpty()) return "برای برنامهٔ شخصی، ابتدا درس، تکلیف یا تاریخ امتحانت را ثبت کن. زمان آزاد و سرفصل را حدس نمی‌زنم."
        val block=minOf(focus.coerceIn(5,120),goal.coerceIn(5,480));val items=mutableListOf<Work>()
        tasks.forEach { t -> val d=JalaliCalendarUtil.parse(t.dueDate)?.let { (day(it)-day(today)).toInt() } ?: 30
            items+=Work("تکلیف «${t.title}»",d.coerceAtLeast(0),if(d<0) "موعد ثبت‌شده گذشته است" else "موعد ${t.dueDate}",2,true) }
        exams.forEach { e -> val d=JalaliCalendarUtil.parse(e.solarDate)?.let { (day(it)-day(today)).toInt() } ?: return@forEach
            if(d>=0) items+=Work("مرور ${e.courseName}",(d-1).coerceAtLeast(0),if(d==0) "فقط اگر آزمون هنوز شروع نشده؛ تاریخ ${e.solarDate}" else "آزمون ${e.solarDate}",3) }
        courses.forEach { c -> items+=Work("مرور دوره‌ای ${c.name}",30,"پیشنهاد تمرین؛ سرفصل مشخص نشده",7) }
        return buildString {
            append("**پیشنهاد هفت‌روزه بر اساس اطلاعات ثبت‌شده**\nساعت آزاد تو مشخص نیست؛ زمان‌ها فقط مدت پیشنهادی‌اند و هیچ رویدادی ذخیره نشده است. بودجهٔ این پیشنهاد از درخواست یا تنظیمات: ${goal.coerceIn(5,480)} دقیقه.\n")
            for(index in 0..6) {
                val classMinutes=sessions.filterNot { it.isArchived }.flatMap { it.sessions }.filter { it.day==(weekday+index)%7 && validSession(it) }.sumOf { (DateTimeNormalizer.timeToMinutes(it.end)-DateTimeNormalizer.timeToMinutes(it.start)).coerceIn(0,720) }
                var budget=(goal.coerceIn(5,480)-((classMinutes-240).coerceAtLeast(0)/4)).coerceAtLeast(minOf(block,goal))
                append("\n**${add(today,index).format()}**${if(classMinutes>0) " · $classMinutes دقیقه کلاس ثبت‌شده" else ""}\n")
                val selected=items.filter { it.blocks>0 && (it.deadline>=index || it.allowCatchup) }.sortedWith(compareBy<Work>({ it.deadline },{it.label})).take(8)
                var count=0
                for(item in selected) { if(budget<block) break;append("• ${item.label}: $block دقیقه — ${item.reason}${if(index>item.deadline) " · جبران پس از موعد؛ برای تغییر موعد باید با مدرس هماهنگ کنی" else ""}\n");item.blocks--;budget-=block+5;count++ }
                if(count==0) append("کار دارای موعد نزدیک دیگری ثبت نشده؛ این روز را برای جبران یا تعیین هدف تازه باز بگذار.\n")
            }
            append("\nمیان جلسه‌ها حدود ۵ دقیقه استراحت پیشنهاد می‌شود. اگر زمان آزاد یا حجم تکلیف فرق دارد، مدت‌ها را تغییر بده؛ این برنامه تخمینِ بار کاری است.")
        }
    }
}
