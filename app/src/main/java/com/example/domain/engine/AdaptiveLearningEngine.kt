package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate
import kotlin.math.ceil

object LearningValidation {
    fun goal(row: StudyGoalEntity) {
        require(row.courseId.isNotBlank() && JalaliCalendarUtil.parse(row.targetDate) != null && row.totalMinutes in 5..6000 && row.difficulty in 1..3)
    }
    fun availability(row: StudyAvailabilityEntity) {
        require(row.weekday in 0..6 && row.startMinute in 0..1439 && row.endMinute in 1..1440 && row.endMinute > row.startMinute && row.budgetMinutes in 0..480 && row.budgetMinutes <= row.endMinute-row.startMinute)
    }
    fun completion(row: StudyCompletionEntity) {
        require(row.id.isNotBlank() && row.courseId.isNotBlank() && JalaliCalendarUtil.parse(row.date) != null && row.startMinute in 0..1439 && row.plannedMinutes in 1..120 && row.startMinute + row.plannedMinutes <= 1440 && row.actualMinutes in 1..240)
    }
    fun card(row: ReviewCardEntity) {
        require(row.id.isNotBlank() && row.courseId.isNotBlank() && row.question.isNotBlank() && row.question.length<=1000 && row.answer.isNotBlank() && row.answer.length<=5000 && JalaliCalendarUtil.parse(row.dueDate)!=null && row.intervalDays in 0..365 && row.repetitions in 0..100000 && row.lapses in 0..100000 && (row.lastReviewedDate.isBlank() || JalaliCalendarUtil.parse(row.lastReviewedDate)!=null))
    }
}

typealias LearningBlock = com.example.domain.model.LearningBlock
data class AdaptiveLearningPlan(val blocks: List<LearningBlock>, val remainingMinutes: Map<String,Int>,
    val overdueGoals: Set<String>, val examDays: Set<String>, val capacityShortfall: Map<String,Int>, val unverifiedClassTimes: Boolean = false)

/** Dated plans are projections from durable goals and confirmed work, never a second task store. */
object AdaptiveLearningEngine {
    fun clock(minute: Int) = "%02d:%02d".format(java.util.Locale.US, minute/60, minute%60)
    fun availabilityEnd(value: String): Int? = if(com.example.data.local.util.DateTimeNormalizer.normalizeDigits(value).trim()=="24:00") 1440 else minute(value)
    fun minute(value: String): Int? = AcademicInputValidator.time(value)?.let { it.substringBefore(':').toInt()*60 + it.substringAfter(':').toInt() }
    fun build(today: JalaliDate, goals: List<StudyGoalEntity>, availability: List<StudyAvailabilityEntity>,
        completed: List<StudyCompletionEntity>, classes: List<CourseWithSessions>, examDates: List<String>, nowMinute: Int = 0, reservedBlocks: List<LearningBlock> = emptyList()): AdaptiveLearningPlan {
        val live = goals.filter { it.enabled && runCatching { LearningValidation.goal(it) }.isSuccess }.distinctBy { it.courseId }
        val work = completed.filter { !it.isDeleted && runCatching { LearningValidation.completion(it) }.isSuccess && JalaliCalendarUtil.parse(it.date)!!<=today }
        val remaining = live.associate { g -> g.courseId to (g.totalMinutes - work.filter { it.courseId==g.courseId }.sumOf { it.actualMinutes }).coerceAtLeast(0) }
        val reserved=reservedBlocks.distinctBy{it.id}.filter { b -> b.courseId in remaining && b.minutes in 5..120 && b.startMinute in 0..1439 && b.startMinute+b.minutes<=1440 && JalaliCalendarUtil.parse(b.date)!=null && work.none{it.id==b.id} }
        val debt = remaining.mapValues { (id, minutes)->(minutes-reserved.filter{it.courseId==id}.sumOf{it.minutes}).coerceAtLeast(0) }.toMutableMap()
        val exams = examDates.mapNotNull(JalaliCalendarUtil::parse).map { it.format() }.toSet()
        val blocks = mutableListOf<LearningBlock>()
        val due = live.associate { it.courseId to JalaliCalendarUtil.parse(it.targetDate)!! }
        for (offset in 0..6) {
            val date=StudentAgendaEngine.addDays(today,offset);val key=date.format();val day=StudentAgendaEngine.weekday(date)
            if (key in exams) continue // Exam end times are unknown: keep the whole exam day free.
            val free=availability.firstOrNull { it.weekday==day && runCatching { LearningValidation.availability(it) }.isSuccess } ?: continue
            val todayWork=work.filter { JalaliCalendarUtil.parse(it.date)==date }
            val reservedToday=reserved.filter{JalaliCalendarUtil.parse(it.date)==date}
            var budget=(free.budgetMinutes-todayWork.sumOf { maxOf(it.actualMinutes,it.plannedMinutes) }-reservedToday.sumOf{it.minutes}).coerceAtLeast(0)
            val busy = classes.filterNot { it.isArchived }.flatMap { it.sessions }.filter { it.day==day }.mapNotNull { s ->
                val a=minute(s.start);val b=minute(s.end);if(a!=null && b!=null && a<b) a until b else null
            } + reservedToday.map{it.startMinute until (it.startMinute+it.minutes+5).coerceAtMost(1440)} + todayWork.map { it.startMinute until (it.startMinute+it.plannedMinutes+5).coerceAtMost(1440) }
            var cursor=maxOf(free.startMinute,if(offset==0) nowMinute.coerceIn(0,1440) else 0)
            val perDay=mutableMapOf<String,Int>()
            while(cursor<free.endMinute && budget>=5) {
                val collision=busy.firstOrNull { cursor in it }
                if(collision!=null){cursor=collision.last+1;continue}
                val end=minOf(free.endMinute,busy.filter { it.first>cursor }.minOfOrNull { it.first } ?: free.endMinute)
                if(end-cursor<5){cursor=end;continue}
                val candidates=live.filter { (debt[it.courseId] ?: 0)>0 && due.getValue(it.courseId)>=date }
                val g=candidates.sortedWith(compareBy<StudyGoalEntity> { due.getValue(it.courseId) }.thenByDescending { it.difficulty }.thenBy { perDay[it.courseId] ?: 0 }.thenBy { it.courseId }).firstOrNull { goal ->
                    val daysLeft=(0..90).count { shift -> val d=StudentAgendaEngine.addDays(date,shift);d<=due.getValue(goal.courseId) && d.format() !in exams && availability.any { it.weekday==StudentAgendaEngine.weekday(d) && it.budgetMinutes>0 } }.coerceAtLeast(1)
                    val share=ceil((debt.getValue(goal.courseId)+(perDay[goal.courseId] ?: 0)).toDouble()/daysLeft).toInt().coerceAtLeast(5)
                    (perDay[goal.courseId] ?: 0)<share
                } ?: break
                val minutes=minOf(25,budget,end-cursor,debt.getValue(g.courseId).coerceAtLeast(5))
                if(minutes<5) break
                val id="${g.courseId}:$key:$cursor"
                blocks+=LearningBlock(id,g.courseId,key,cursor,minutes)
                debt[g.courseId]=(debt.getValue(g.courseId)-minutes).coerceAtLeast(0)
                perDay[g.courseId]=(perDay[g.courseId] ?: 0)+minutes
                budget-=minutes;cursor+=minutes+5 // Explicit five-minute break, never fill every minute.
            }
        }
        val overdue=live.filter { due.getValue(it.courseId)<today && remaining.getValue(it.courseId)>0 }.map { it.courseId }.toSet()
        val shortfall=live.filter { due.getValue(it.courseId)<=StudentAgendaEngine.addDays(today,6) && debt.getValue(it.courseId)>0 }.associate { it.courseId to debt.getValue(it.courseId) }
        val uncertain=classes.filterNot { it.isArchived }.any { c -> c.sessions.isEmpty() || c.sessions.any { minute(it.start)==null || minute(it.end)==null || minute(it.start)!!>=minute(it.end)!! || it.day !in 0..6 } }
        return AdaptiveLearningPlan(blocks,remaining,overdue,exams,shortfall,uncertain)
    }
}

enum class ReviewRating(val title: String) { AGAIN("یادم نبود"), HARD("سخت بود"), GOOD("بلد بودم"), EASY("آسان بود") }
object SpacedReviewEngine {
    fun rate(card: ReviewCardEntity, rating: ReviewRating, today: JalaliDate, now: Long): ReviewCardEntity {
        LearningValidation.card(card)
        val interval=when(rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> maxOf(1,(card.intervalDays*1.2).toInt())
            ReviewRating.GOOD -> if(card.intervalDays==0) 1 else maxOf(3,(card.intervalDays*2.0).toInt())
            ReviewRating.EASY -> if(card.intervalDays==0) 4 else maxOf(4,(card.intervalDays*2.8).toInt())
        }.coerceAtMost(365)
        return card.copy(dueDate=StudentAgendaEngine.addDays(today,interval).format(),intervalDays=interval,
            repetitions=(card.repetitions+1).coerceAtMost(100000),lapses=(card.lapses+if(rating==ReviewRating.AGAIN)1 else 0).coerceAtMost(100000),
            lastReviewedDate=today.format(),updatedAt=maxOf(now,card.updatedAt+1))
    }
}
