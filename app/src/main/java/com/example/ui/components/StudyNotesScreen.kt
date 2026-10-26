package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.NoteEntity
import java.util.UUID

@Composable
fun StudyNotesScreen(notes:List<NoteEntity>,courses:List<CourseEntity>,ownerKey:String,
    onSave:(NoteEntity,Boolean,(Boolean)->Unit)->Unit,onArchive:(NoteEntity,Boolean,(Boolean)->Unit)->Unit,
    modifier:Modifier=Modifier) {
    var search by rememberOwnerNoteText(ownerKey, "")
    var archived by rememberSaveable(ownerKey) { mutableStateOf(false) }
    var courseFilter by rememberOwnerNoteText(ownerKey, "all")
    var editorToken by rememberSaveable { mutableStateOf("") }
    var limit by remember(ownerKey,search,archived,courseFilter) { mutableIntStateOf(20) }
    var archiveTarget by remember(ownerKey) { mutableStateOf<NoteEntity?>(null) }
    var archiveError by remember(ownerKey) { mutableStateOf(false) }
    var savingArchive by remember(ownerKey) { mutableStateOf(false) }
    val prefix=ownerKey+"\n"
    val rows=run {
        val q=com.example.domain.engine.GroundedStudentAssistant.normalize(search).lowercase()
        notes.filter { n -> n.isArchived==archived && when(courseFilter){"all"->true;"general"->n.courseId.isNullOrBlank();else->n.courseId==courseFilter} &&
            (q.isBlank() || com.example.domain.engine.GroundedStudentAssistant.normalize(n.title+" "+n.content+" "+courses.find { it.id==n.courseId }?.name.orEmpty()).lowercase().contains(q)) }.sortedByDescending { it.updatedAt }
    }
    Column(modifier.fillMaxWidth().testTag("study_notes_screen"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        AcademicCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("دفتر یادداشت درسی",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("نکته‌ها، فرمول‌ها و خلاصهٔ درس‌ها؛ ذخیره روی دستگاه و قابل نگهداری در پشتیبان.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick={editorToken=prefix+"new:"+UUID.randomUUID()},Modifier.testTag("note_add")) { Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(8.dp));Text("یادداشت تازه") }
        } }
        OutlinedTextField(search,{search=it.take(200)},label={Text("جست‌وجو در عنوان، متن و درس")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("notes_search"))
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(!archived,{archived=false},label={Text("یادداشت‌ها (${notes.count{!it.isArchived}})")},modifier=Modifier.testTag("notes_active"))
            FilterChip(archived,{archived=true},label={Text("بایگانی (${notes.count{it.isArchived}})")},modifier=Modifier.testTag("notes_archived"))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (listOf("all" to "همهٔ درس‌ها","general" to "عمومی")+courses.map{it.id to (it.name+if(it.isArchived)" · درس بایگانی"else"")}).forEach { (id,name) ->
                FilterChip(courseFilter==id,{courseFilter=id},label={Text(name,maxLines=1)},modifier=Modifier.heightIn(min=48.dp))
            }
        }
        if(rows.isEmpty()) AcademicCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(if(archived)Icons.Rounded.Inventory2 else Icons.Rounded.Description,null,tint=MaterialTheme.colorScheme.primary)
            Text(if(search.isNotBlank() || courseFilter!="all") "یادداشتی با این جست‌وجو پیدا نشد." else if(archived) "بایگانی خالی است." else "اولین نکتهٔ درسی‌ات را ثبت کن.",style=MaterialTheme.typography.titleMedium)
            Text(if(archived) "یادداشت‌های بایگانی‌شده قابل بازیابی‌اند." else "عنوان و متن یادداشت در جست‌وجو قابل پیدا کردن است.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        rows.take(limit).forEach { note ->
            AcademicCard(Modifier.fillMaxWidth().testTag("note_${note.id}"),onClick=if(!archived){{editorToken=prefix+"edit:"+note.id}}else null) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(note.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    AcademicStatusChip(if(note.courseId.isNullOrBlank())"عمومی" else courses.find{it.id==note.courseId}?.name ?: "درس خارج از ترم فعلی")
                    Text(note.content,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=4,overflow=TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                        if(!archived) TextButton(onClick={editorToken=prefix+"edit:"+note.id}){Text("بازکردن و ویرایش")}
                        TextButton(onClick={archiveError=false;archiveTarget=note},Modifier.testTag("note_archive_${note.id}")){Text(if(archived)"بازیابی"else"بایگانی")}
                    }
                }
            }
        }
        if(rows.size>limit) OutlinedButton(onClick={limit+=20},Modifier.fillMaxWidth()){Text("نمایش یادداشت‌های بیشتر")}
    }
    if(editorToken.startsWith(prefix)) {
        val token=editorToken.removePrefix(prefix);val creating=token.startsWith("new:");val id=token.substringAfter(':')
        val existing=notes.find{it.id==id}
        if(creating || existing!=null) key(editorToken) {
            StudyNoteEditor(existing ?: NoteEntity(id=id),creating,courses,{editorToken=""},onSave)
        }
    }
    archiveTarget?.let { note ->
        AlertDialog(onDismissRequest={if(!savingArchive)archiveTarget=null},title={Text(if(note.isArchived)"بازیابی یادداشت"else"بایگانی یادداشت")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(if(note.isArchived)"«${note.title}» به یادداشت‌های فعال برگردد؟"else"«${note.title}» بایگانی شود؟ متن حفظ می‌شود و بعداً قابل بازیابی است.")
                if(archiveError)Text("تغییر ذخیره نشد؛ انصراف بزن و نسخهٔ تازهٔ یادداشت را دوباره بررسی کن.",color=MaterialTheme.colorScheme.error)
            }},
            confirmButton={TextButton(enabled=!savingArchive,onClick={archiveError=false;savingArchive=true;onArchive(note,!note.isArchived){ok->savingArchive=false;if(ok)archiveTarget=null else archiveError=true}}){Text(if(savingArchive)"در حال ذخیره…"else"تأیید")}},
            dismissButton={TextButton(enabled=!savingArchive,onClick={archiveTarget=null}){Text("انصراف")}})
    }
}

@Composable
private fun StudyNoteEditor(note:NoteEntity,creating:Boolean,courses:List<CourseEntity>,onDismiss:()->Unit,onSave:(NoteEntity,Boolean,(Boolean)->Unit)->Unit) {
    var title by rememberSaveable { mutableStateOf(note.title) };var content by rememberSaveable { mutableStateOf(note.content) }
    var courseId by rememberSaveable { mutableStateOf(note.courseId) };var busy by remember { mutableStateOf(false) };var error by remember { mutableStateOf(false) }
    val version=rememberSaveable { note.updatedAt }
    StudentGlassModalSheet(onDismiss={if(!busy)onDismiss()},title=if(creating)"یادداشت تازه"else"ویرایش یادداشت",subtitle="عنوان و متن را وارد کن؛ تغییرات پس از ذخیره ثبت می‌شوند.") {
        Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title,{title=it},label={Text("عنوان")},isError=title.length>180,supportingText={Text("${title.length} از ۱۸۰ نویسه")},modifier=Modifier.fillMaxWidth().testTag("note_title"))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(courseId==null,{courseId=null},label={Text("عمومی")})
                if(courseId!=null && courses.none{it.id==courseId}) FilterChip(true,{},label={Text("درس قبلی")})
                courses.forEach { c -> FilterChip(courseId==c.id,{courseId=c.id},label={Text(c.name+if(c.isArchived)" · درس بایگانی"else"",maxLines=1)}) }
            }
            OutlinedTextField(content,{content=it},label={Text("متن یادداشت")},isError=content.length>30000,supportingText={Text(if(content.length>30000)"متن را به چند یادداشت تقسیم کن؛ سقف هر یادداشت ۳۰ هزار نویسه است."else"متن روی دستگاه ذخیره می‌شود.")},minLines=6,maxLines=12,modifier=Modifier.fillMaxWidth().testTag("note_content"))
            if(error) Text("ذخیره نشد؛ متن حفظ شده است. اگر یادداشت تغییر کرده، متن را کپی و نسخهٔ تازه را باز کن.",color=MaterialTheme.colorScheme.error)
            Button(enabled=!busy && title.trim().length in 1..180 && content.isNotBlank() && content.length<=30000,onClick={busy=true;error=false;onSave(note.copy(title=title,content=content,courseId=courseId,updatedAt=version),creating){ok->busy=false;if(ok)onDismiss()else error=true}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("note_save")){Text(if(busy)"در حال ذخیره…"else"ذخیرهٔ یادداشت")}
            TextButton(onClick=onDismiss,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("انصراف")}
        }
    }
}

@Composable
private fun rememberOwnerNoteText(owner: String, initial: String): MutableState<String> {
    val saver=remember(owner,initial) { listSaver<String,String>(save={listOf(owner,it)},restore={if(it.firstOrNull()==owner)it.getOrElse(1){initial}else initial}) }
    return rememberSaveable(owner,stateSaver=saver){mutableStateOf(initial)}
}
