package com.example.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class StudyNotePersistenceTest {
    private lateinit var db:AppDatabase
    private lateinit var repo:StudentRepository
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build();repo=StudentRepository(db.studentDao(),db.curriculumDao(),db)}
    @After fun cleanup(){db.close()}
    @Test fun notesEditArchiveRestoreAndBackupPreserveTheActualTextAndIdentity()=runBlocking {
        val first=repo.saveStudyNote(NoteEntity(id="note",title="  انتگرال  ",content="  فرمول\n  مثال فارسی 📝"),true)
        assertEquals("انتگرال",first.title)
        val edited=repo.saveStudyNote(first.copy(content=first.content+"\nتمرین"),false)
        val archived=repo.setNoteArchived(edited,true)
        assertTrue(db.studentDao().getNoteById(first.id)!!.isArchived)
        val json=LocalDataBackupManager.exportCompleteDatabaseJson(db.studentDao())
        db.studentDao().clearAllNotes()
        assertTrue(repo.restoreFullBackupJson(json).isSuccess)
        assertEquals(archived,db.studentDao().getNoteById(first.id))
        val restored=repo.setNoteArchived(archived,false)
        assertFalse(restored.isArchived);assertEquals(edited.content,restored.content);assertEquals(first.id,restored.id)
    }
    @Test fun staleEditsAndArchiveRequestsCannotOverwriteANewerNote()=runBlocking {
        val first=repo.saveStudyNote(NoteEntity(id="n",title="عنوان",content="متن"),true)
        val newer=repo.saveStudyNote(first.copy(content="ویرایش تازه"),false)
        assertTrue(runCatching{repo.saveStudyNote(first.copy(content="قدیمی"),false)}.isFailure)
        assertTrue(runCatching{repo.setNoteArchived(first,true)}.isFailure)
        assertEquals(newer,db.studentDao().getNoteById(first.id))
        val archived=repo.setNoteArchived(newer,true)
        assertTrue(runCatching{repo.saveStudyNote(archived.copy(content="پنهانی"),false)}.isFailure)
    }
    @Test fun legacyBackupsDefaultToActiveAndInvalidEditsPreserveExistingData()=runBlocking {
        assertTrue(repo.restoreFullBackupJson("""{"notes":[{"id":"old","title":"قدیمی","content":"متن","updatedAt":12}]}""").isSuccess)
        val old=db.studentDao().getNoteById("old")!!
        assertFalse(old.isArchived)
        assertTrue(runCatching{repo.saveStudyNote(old.copy(content=" "),false)}.isFailure)
        assertTrue(runCatching{repo.saveStudyNote(old.copy(courseId="missing"),false)}.isFailure)
        assertEquals(old,db.studentDao().getNoteById("old"))
    }
    @Test fun schemaElevenMigratesWithoutLosingNotesAndArchiveSurvivesReopening()=runBlocking {
        val name="notes-upgrade-${java.util.UUID.randomUUID()}.db"
        val schema=org.json.JSONObject(javaClass.classLoader!!.getResourceAsStream("com.example.data.local.AppDatabase/11.json")!!.bufferedReader().use{it.readText()}).getJSONObject("database")
        val helper=androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object:androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(11){
                override fun onCreate(raw:androidx.sqlite.db.SupportSQLiteDatabase){
                    val entities=schema.getJSONArray("entities")
                    for(i in 0 until entities.length()){
                        val e=entities.getJSONObject(i);val table=e.getString("tableName")
                        raw.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table))
                        val indices=e.optJSONArray("indices")?:org.json.JSONArray()
                        for(j in 0 until indices.length())raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
                    }
                    val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())raw.execSQL(setup.getString(i))
                    raw.execSQL("INSERT INTO notes(id, courseId, title, content, updatedAt) VALUES ('legacy', NULL, 'قدیمی', 'متن حفظ‌شده', 123)")
                }
                override fun onUpgrade(raw:androidx.sqlite.db.SupportSQLiteDatabase,oldVersion:Int,newVersion:Int){}
            }).build())
        helper.writableDatabase;helper.close()
        try {
            val upgraded=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13).allowMainThreadQueries().build()
            val note=upgraded.studentDao().getNoteById("legacy")!!
            assertEquals("متن حفظ‌شده",note.content);assertEquals(123L,note.updatedAt);assertFalse(note.isArchived)
            upgraded.studentDao().insertNote(note.copy(isArchived=true));upgraded.close()
            val reopened=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build()
            assertTrue(reopened.studentDao().getNoteById("legacy")!!.isArchived);reopened.close()
        } finally {context.deleteDatabase(name)}
    }
}
