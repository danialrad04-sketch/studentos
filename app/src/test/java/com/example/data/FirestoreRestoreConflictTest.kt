package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.CloudProfileData
import com.example.data.cloud.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.StudentProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestoreRestoreConflictTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLocalProfileNewerThanCloud_preservesLocalProfileWithoutOverwrite() = runBlocking {
        val dao = db.studentDao()
        val now = System.currentTimeMillis()
        val localProfile = StudentProfileEntity(
            id = 1,
            name = "علی محلی",
            studentId = "40111111",
            university = "دانشگاه تهران",
            major = "مهندسی صنایع",
            updatedAt = now
        )
        dao.insertProfile(localProfile)

        // Older cloud profile (1 minute older than local)
        val olderCloudTimestamp = now - 60_000L
        val mockCloudProfile = CloudProfileData(
            name = "علی ابری قدیمی",
            studentId = "99123456",
            university = "دانشگاه صنعتی شریف",
            major = "مهندسی کامپیوتر",
            updatedAt = olderCloudTimestamp
        )

        // Attempt restore from cloud
        val result = FirestoreSyncManager.restoreAllDataFromCloud(
            userId = "test_user_conflict",
            dao = dao,
            cloudProfileOverride = mockCloudProfile
        )

        val restoredLocal = dao.getProfileSync()
        assertNotNull(restoredLocal)
        // Local profile was newer, so it MUST NOT be overwritten
        assertEquals("علی محلی", restoredLocal?.name)
        assertEquals("40111111", restoredLocal?.studentId)
        assertEquals("دانشگاه تهران", restoredLocal?.university)
        assertEquals("مهندسی صنایع", restoredLocal?.major)
        assertEquals(now, restoredLocal?.updatedAt)
    }

    @Test
    fun testCloudProfileNewerThanLocal_overwritesLocalProfile() = runBlocking {
        val dao = db.studentDao()
        val now = System.currentTimeMillis()
        val localProfile = StudentProfileEntity(
            id = 1,
            name = "علی محلی قدیمی",
            studentId = "40111111",
            university = "دانشگاه تهران",
            major = "مهندسی صنایع",
            updatedAt = now - 60_000L
        )
        dao.insertProfile(localProfile)

        // Newer cloud profile (1 minute newer than local)
        val newerCloudTimestamp = now + 60_000L
        val mockCloudProfile = CloudProfileData(
            name = "علی ابری جدید",
            studentId = "99123456",
            university = "دانشگاه صنعتی شریف",
            major = "مهندسی کامپیوتر",
            entryYear = 1400,
            currentSemester = 4,
            passedUnits = 60,
            activeUnits = 18,
            declaredGpa = 18.5,
            term = "ترم ۴ مهندسی کامپیوتر",
            updatedAt = newerCloudTimestamp
        )

        // Attempt restore from cloud
        val result = FirestoreSyncManager.restoreAllDataFromCloud(
            userId = "test_user_conflict",
            dao = dao,
            cloudProfileOverride = mockCloudProfile
        )

        val restoredLocal = dao.getProfileSync()
        assertNotNull(restoredLocal)
        // Cloud profile was newer, so local MUST be overwritten
        assertEquals("علی ابری جدید", restoredLocal?.name)
        assertEquals("99123456", restoredLocal?.studentId)
        assertEquals("دانشگاه صنعتی شریف", restoredLocal?.university)
        assertEquals("مهندسی کامپیوتر", restoredLocal?.major)
        assertEquals(newerCloudTimestamp, restoredLocal?.updatedAt)
    }
}
