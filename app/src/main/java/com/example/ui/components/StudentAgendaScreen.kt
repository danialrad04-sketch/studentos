package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.StudentAgendaEngine
import com.example.domain.model.ExamItem
import com.example.domain.util.JalaliCalendarUtil
import kotlinx.coroutines.delay

@Composable
fun StudentAgendaScreen(sessions:List<CourseWithSessions>,tasks:List<TaskEntity>,exams:List<ExamItem>,
    onOpenTask:(TaskEntity)->Unit,onOpenCourse:(String)->Unit,onOpenExams:()->Unit,onAddTask:()->Unit,
    onExport:(StudentAgendaEngine.Week)->Unit,exportBusy:Boolean=false,modifier:Modifier=Modifier) {
    var today by remember { mutableStateOf(JalaliCalendarUtil.today()) }
    LaunchedEffect(Unit){while(true){delay(60000);today=JalaliCalendarUtil.today()}}
    var selectedDate by rememberSaveable { mutableStateOf("today") }
    var offset by rememberSaveable { mutableIntStateOf(0) };var filter by rememberSaveable { mutableStateOf("all") }
    var rowLimit by remember(offset,filter) { mutableIntStateOf(12) }
    val start=StudentAgendaEngine.addDays(StudentAgendaEngine.startOfWeek(today),offset*7)
    val week=remember(start,sessions,tasks,exams,today){StudentAgendaEngine.build(start,sessions,tasks,exams,today)}
    val focusedDate=week.days.find{it.format()==selectedDate} ?: (if(selectedDate=="first")week.days.first() else today.takeIf{it in week.days} ?: week.days.first())
    val shownDays=if(selectedDate=="all")week.days else listOf(focusedDate)
    val visible=week.events.filter{filter=="all" || it.kind.name==filter}
    Column(modifier.fillMaxWidth().testTag("student_agenda_screen"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        AcademicCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("هفته‌ات در یک نگاه",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("کلاس‌ها الگوی هفتگی‌اند؛ تعطیلی و پایان ترم ثبت نشده است.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${start.toPersianDigits()} تا ${week.days.last().toPersianDigits()}",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onAddTask,modifier=Modifier.testTag("agenda_add")){Icon(Icons.Rounded.Add,"افزودن تکلیف")}
                IconButton(onClick={offset--;selectedDate="first"},enabled=offset> -52,modifier=Modifier.testTag("agenda_previous")){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"هفتهٔ قبل")}
                OutlinedButton(onClick={offset=0;selectedDate="today"},Modifier.weight(1f).testTag("agenda_current")){Text("هفتهٔ جاری")}
                IconButton(onClick={offset++;selectedDate="first"},enabled=offset<52,modifier=Modifier.testTag("agenda_next")){Icon(Icons.AutoMirrored.Rounded.ArrowForward,"هفتهٔ بعد")}
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StudentAgendaEngine.Kind.entries.forEach { kind -> AcademicStatusChip("${kind.title}: ${week.events.count{it.kind==kind}}") }
            }
            if(week.conflicts.isNotEmpty()) Text("${week.conflicts.size} رویداد دارای هم‌زمانی ثبت‌شده است؛ زمان‌ها را بررسی کن.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={onExport(week)},enabled=!exportBusy && week.events.isNotEmpty(),modifier=Modifier.fillMaxWidth().testTag("agenda_export")) {
                Icon(Icons.Rounded.FileDownload,null);Spacer(Modifier.width(8.dp));Text(if(exportBusy)"در حال آماده‌سازی…"else"ذخیرهٔ تقویم کامل این هفته")
            }
            Text("فایل را پس از ذخیره در تقویمت وارد کن. فقط همین هفته صادر می‌شود و خودکار به‌روز نمی‌شود؛ ساعت‌ها بر مبنای تهران‌اند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (listOf("all" to "همه")+StudentAgendaEngine.Kind.entries.map{it.name to it.title}).forEach { (id,title)->
                FilterChip(filter==id,{filter=id},label={Text(title)},modifier=Modifier.heightIn(min=48.dp).testTag("agenda_filter_$id"))
            }
        }
        if(offset==0 && week.overdue.isNotEmpty() && (filter=="all" || filter=="TASK")) {
            AcademicCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("موعد گذشته · ${week.overdue.size} تکلیف",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)
                week.overdue.take(8).forEach { task->TextButton(onClick={onOpenTask(task)}){Text("${task.title} · ${task.dueDate}")}}
                if(week.overdue.size>8) Text("همهٔ موارد در بخش تکلیف‌ها قابل بررسی‌اند.",style=MaterialTheme.typography.bodySmall)
            } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selectedDate=="all",{selectedDate="all"},label={Text("تمام هفته")},modifier=Modifier.testTag("agenda_show_all"))
            week.days.forEach { day -> FilterChip(selectedDate!="all" && focusedDate==day,{selectedDate=day.format()},label={Text("${day.getWeekdayName()} ${day.day}")},modifier=Modifier.testTag("agenda_choose_${day.format()}")) }
        }
        shownDays.forEach { day ->
            val items=visible.filter{it.date==day}
            AcademicCard(Modifier.fillMaxWidth().testTag("agenda_day_${day.format()}")) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("${day.getWeekdayName()} · ${day.toPersianDigits()}${if(day==today)" · امروز"else""}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=if(day==today)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    if(items.isEmpty()) Text("رویدادی در این دسته ثبت نشده است.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
                    items.take(rowLimit).forEach { event ->
                        Surface(onClick={when(event.kind){StudentAgendaEngine.Kind.TASK->tasks.find{it.id==event.taskId}?.let(onOpenTask);StudentAgendaEngine.Kind.CLASS->event.courseId?.let(onOpenCourse);StudentAgendaEngine.Kind.EXAM->onOpenExams()}},
                            shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow,modifier=Modifier.fillMaxWidth().testTag("agenda_event_${event.id}")) {
                            Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(when(event.kind){StudentAgendaEngine.Kind.CLASS->Icons.Rounded.School;StudentAgendaEngine.Kind.TASK->Icons.Rounded.CheckCircle;StudentAgendaEngine.Kind.EXAM->Icons.Rounded.Event},null,tint=MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                    Text(event.title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                                    if(event.id in week.conflicts) Text("هم‌زمانی با رویداد دیگر",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelMedium)
                                    Text(event.kind.title+" · "+(event.start?.let{it+(event.end?.let{end->" تا $end"}?:"")} ?: if(event.kind==StudentAgendaEngine.Kind.TASK)"موعد روزانه؛ ساعت مشخص نشده"else"ساعت ثبت نشده"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    if(event.kind!=StudentAgendaEngine.Kind.TASK) Text(event.location.ifBlank{"مکان ثبت نشده"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if(items.size>rowLimit) TextButton(onClick={rowLimit+=12}){Text("نمایش موارد بیشتر")}
                }
            }
        }
        if(week.undatedTasks.isNotEmpty() && (filter=="all" || filter=="TASK")) AcademicCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("تکلیف‌های بدون موعد معتبر",style=MaterialTheme.typography.titleMedium)
                week.undatedTasks.take(8).forEach { task->TextButton(onClick={onOpenTask(task)}){Text(task.title)} }
                Text("برای قرارگرفتن در برنامهٔ هفته، تاریخ این موارد را اصلاح کن.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(week.invalidRecords>0) Text("${week.invalidRecords} مورد دارای تاریخ یا ساعت نامعتبر است؛ اطلاعات نامشخص را در کلاس‌ها یا امتحان‌ها اصلاح کن.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        Button(onClick=onAddTask,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(8.dp));Text("افزودن تکلیف")}
    }
}
