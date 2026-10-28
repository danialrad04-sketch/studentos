package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class AdaptiveLearningEngineTest {
    private val today=JalaliDate(1405,8,5)
    private val goal=StudyGoalEntity("math",StudentAgendaEngine.addDays(today,6).format(),300)
    private val free=(0..6).map{StudyAvailabilityEntity(it,540,720,60)}
    @Test fun noAvailabilityDoesNotInventAStudySchedule() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal),emptyList(),emptyList(),emptyList(),emptyList())
        assertTrue(p.blocks.isEmpty());assertEquals(300,p.remainingMinutes["math"]);assertEquals(300,p.capacityShortfall["math"])
    }
    @Test fun classIntervalsAreNeverUsedForStudy() {
        val classes=listOf(CourseWithSessions(CourseEntity(id="c",name="کلاس"),listOf(CourseSessionEntity(id="s",courseId="c",day=StudentAgendaEngine.weekday(today),start="09:00",end="10:00"))))
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),classes,emptyList())
        assertTrue(p.blocks.filter{it.date==today.format()}.all{it.startMinute>=600})
    }
    @Test fun allDayBudgetsAndBreaksAreRespected() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),emptyList(),emptyList())
        p.blocks.groupBy{it.date}.values.forEach { blocks ->
            assertTrue(blocks.sumOf{it.minutes}<=60)
            blocks.zipWithNext().forEach{(a,b)->assertTrue(b.startMinute>=a.startMinute+a.minutes+5)}
        }
    }
    @Test fun noSessionsAreSuggestedOnAnExamDayWithUnknownEndTime() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),emptyList(),listOf(today.format()))
        assertTrue(p.blocks.none{it.date==today.format()});assertTrue(today.format() in p.examDays)
    }
    @Test fun insufficientCapacityIsExplicitRatherThanOverbooking() {
        val g=goal.copy(targetDate=today.format(),totalMinutes=120)
        val p=AdaptiveLearningEngine.build(today,listOf(g),free.map{it.copy(budgetMinutes=30)},emptyList(),emptyList(),emptyList())
        assertEquals(30,p.blocks.sumOf{it.minutes});assertEquals(90,p.capacityShortfall["math"])
    }
    @Test fun confirmedActualWorkReducesGoalAndReservesItsSlotExactlyOnce() {
        val done=StudyCompletionEntity("done","math",today.format(),540,25,20,10)
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,listOf(done),emptyList(),emptyList())
        assertEquals(280,p.remainingMinutes["math"])
        assertTrue(p.blocks.filter{it.date==today.format()}.all{it.startMinute>=570})
        assertTrue(p.blocks.filter{it.date==today.format()}.sumOf{it.minutes}<=35)
    }
    @Test fun missedDayDoesNotCountAsCompletedAndReplansRemainingDays() {
        val next=StudentAgendaEngine.addDays(today,1)
        val p=AdaptiveLearningEngine.build(next,listOf(goal),free,emptyList(),emptyList(),emptyList())
        assertEquals(300,p.remainingMinutes["math"]);assertTrue(p.blocks.none{it.date==today.format()});assertTrue(p.blocks.isNotEmpty())
    }
    @Test fun pausedAndExpiredGoalsDoNotProduceFutureSessions() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal.copy(enabled=false),goal.copy(courseId="old",targetDate=StudentAgendaEngine.addDays(today,-1).format())),free,emptyList(),emptyList(),emptyList())
        assertTrue(p.blocks.isEmpty());assertTrue("old" in p.overdueGoals)
    }
    @Test fun deletedOrFutureRecordsCannotPretendWorkWasCompleted() {
        val done=StudyCompletionEntity("a","math",today.format(),540,25,25,1,true)
        val future=done.copy(id="b",date=StudentAgendaEngine.addDays(today,1).format(),isDeleted=false)
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,listOf(done,future),emptyList(),emptyList())
        assertEquals(300,p.remainingMinutes["math"])
    }
    @Test fun elapsedTimeIsNotScheduledInThePast() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),emptyList(),emptyList(),nowMinute=650)
        assertTrue(p.blocks.filter{it.date==today.format()}.all{it.startMinute>=650})
    }
    @Test fun incompleteClassScheduleIsDisclosed() {
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),listOf(CourseWithSessions(CourseEntity(id="c",name="درس"),emptyList())),emptyList())
        assertTrue(p.unverifiedClassTimes)
    }
    @Test fun pendingTimerBlockIsReservedWithoutBeingCountedAsCompleted() {
        val pending=LearningBlock("pending","math",today.format(),540,25)
        val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),emptyList(),emptyList(),reservedBlocks=listOf(pending))
        assertEquals(300,p.remainingMinutes["math"])
        assertTrue(p.blocks.filter{it.date==today.format()}.all{it.startMinute>=570})
        assertTrue(p.blocks.sumOf{it.minutes}<=275)
    }
    @Test fun persistedDateKeysAndReservationsRemainStableWithPersianDeviceLocale() {
        val original=java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("fa","IR"))
            assertEquals("1405/08/05",today.format())
            val pending=LearningBlock("pending","math",today.toPersianDigits(),540,25)
            val p=AdaptiveLearningEngine.build(today,listOf(goal),free,emptyList(),emptyList(),emptyList(),reservedBlocks=listOf(pending))
            assertTrue(p.blocks.filter{it.date==today.format()}.all{it.startMinute>=570})
            assertEquals(1440,AdaptiveLearningEngine.availabilityEnd("۲۴:۰۰"))
            assertNull(AdaptiveLearningEngine.minute("۲۴:۰۰"))
        } finally {java.util.Locale.setDefault(original)}
    }
    @Test fun reviewsAdvanceFromRealRecallAndAgainRemainsDueToday() {
        val card=ReviewCardEntity("id","math","پرسش","پاسخ",today.format(),updatedAt=10)
        val good=SpacedReviewEngine.rate(card,ReviewRating.GOOD,today,11)
        assertEquals(StudentAgendaEngine.addDays(today,1).format(),good.dueDate)
        val again=SpacedReviewEngine.rate(good,ReviewRating.AGAIN,today,9)
        assertEquals(today.format(),again.dueDate);assertEquals(1,again.lapses);assertTrue(again.updatedAt>good.updatedAt)
        assertEquals(365,SpacedReviewEngine.rate(card.copy(intervalDays=300),ReviewRating.EASY,today,12).intervalDays)
    }
    @Test fun weeklyReportDoesNotCountFutureWorkDeletedWorkOrTasksWithoutACompletionDate() {
        val done=StudyCompletionEntity("a","math",today.format(),540,25,17,10)
        val report=WeeklyLearningReportEngine.summarize(today,setOf("math"),listOf(done,done.copy(id="b",isDeleted=true),done.copy(id="c",date=StudentAgendaEngine.addDays(today,1).format())),
            listOf(ReviewCardEntity("r","math","q","a",today.format(),lastReviewedDate=today.format())),listOf(TaskEntity(id=1,title="تکلیف",dueDate=StudentAgendaEngine.addDays(today,-1).format())))
        assertEquals(17,report.declaredMinutes);assertEquals(1,report.confirmedSessions);assertEquals(1,report.uniqueReviewedCards);assertEquals(1,report.overdueTasks)
    }
}
