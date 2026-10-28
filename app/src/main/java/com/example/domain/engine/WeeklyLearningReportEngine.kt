package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate

data class WeeklyLearningSummary(val declaredMinutes:Int,val confirmedSessions:Int,val uniqueReviewedCards:Int,val overdueTasks:Int)
object WeeklyLearningReportEngine {
    fun summarize(today:JalaliDate,courseIds:Set<String>,completions:List<StudyCompletionEntity>,cards:List<ReviewCardEntity>,tasks:List<TaskEntity>):WeeklyLearningSummary {
        val start=StudentAgendaEngine.addDays(today,-6)
        fun inWeek(raw:String)=JalaliCalendarUtil.parse(raw)?.let { it>=start && it<=today }==true
        val records=completions.distinctBy{it.id}.filter{!it.isDeleted && it.courseId in courseIds && inWeek(it.date) && it.actualMinutes in 1..240}
        return WeeklyLearningSummary(records.sumOf{it.actualMinutes},records.size,
            cards.distinctBy{it.id}.count{it.courseId in courseIds && inWeek(it.lastReviewedDate)},
            tasks.distinctBy{it.id}.count{!it.isCompleted && JalaliCalendarUtil.parse(it.dueDate)?.let{date->date<today}==true})
    }
}
