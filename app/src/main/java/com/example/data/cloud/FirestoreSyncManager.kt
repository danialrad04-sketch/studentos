package com.example.data.cloud

import android.util.Log
import com.example.data.auth.awaitResult
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.SubscriptionTier
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Model representing remote cloud profile snapshot for synchronization and conflict resolution.
 */
data class CloudProfileData(
    val name: String = "",
    val studentId: String = "",
    val major: String = "",
    val university: String = "",
    val entryYear: Int = 0,
    val currentSemester: Int = 0,
    val passedUnits: Int = 0,
    val activeUnits: Int = 0,
    val declaredGpa: Double? = null,
    val term: String? = null,
    val faculty: String = "",
    val updatedAt: Long = 0L
)

/**
 * Production-Grade Cloud Firestore Data Synchronization and Server-Gated Subscription Manager.
 * 
 * Enforces:
 * 1. Strict per-user data isolation under `/users/{uid}/`
 * 2. Server-side subscription validation (Client is read-only for subscription status)
 * 3. Bidirectional cloud synchronization for courses, grades, tasks, and attendance
 */
object FirestoreSyncManager {

    private const val TAG = "FirestoreSyncManager"
    private const val USERS_COLLECTION = "users"
    private const val PROMO_COLLECTION = "promoCodes"

    private val firestore: FirebaseFirestore
        get() = try {
            Firebase.firestore
        } catch (e: Throwable) {
            FirebaseFirestore.getInstance()
        }

    /**
     * Real-time Flow observing the server-validated subscription state from Firestore.
     */
    fun observeUserSubscription(userId: String): Flow<SubscriptionTier> = callbackFlow {
        if (userId.isBlank() || userId.startsWith("guest_")) {
            trySend(SubscriptionTier.FREE)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = firestore.collection(USERS_COLLECTION).document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore subscription listener error: ${error.message}")
                trySend(SubscriptionTier.FREE)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val tierStr = snapshot.getString("subscriptionTier") ?: "FREE"
                val expiresAt = snapshot.getTimestamp("subscriptionExpiresAt")?.toDate()?.time
                val expired = expiresAt != null && expiresAt <= System.currentTimeMillis()
                val tier = if (expired) {
                    SubscriptionTier.FREE
                } else {
                    try {
                        SubscriptionTier.valueOf(tierStr.uppercase())
                    } catch (_: Throwable) {
                        SubscriptionTier.FREE
                    }
                }
                trySend(tier)
            } else {
                trySend(SubscriptionTier.FREE)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Server-side Promo Code validation and redemption.
     */
    suspend fun redeemPromoCode(userId: String, rawCode: String): Result<SubscriptionTier> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.failure(IllegalStateException("برای فعال‌سازی کد هدیه، ابتدا وارد حساب کاربری شوید."))
        }

        val code = rawCode.trim().uppercase()
        if (code.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("کد هدیه نمی‌تواند خالی باشد."))
        }

        try {
            val callable = com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("validateAndApplyPromoCode")
            val result = callable.call(mapOf("code" to code)).awaitResult()
            val data = result.data as? Map<*, *>
                ?: return@withContext Result.failure(IllegalStateException("پاسخ سرور اشتراک نامعتبر است."))
            val tierName = data["tier"]?.toString()?.uppercase()
                ?: return@withContext Result.failure(IllegalStateException("سطح اشتراک از سرور دریافت نشد."))
            val tier = runCatching { SubscriptionTier.valueOf(tierName) }.getOrElse {
                return@withContext Result.failure(IllegalStateException("سطح اشتراک دریافت‌شده معتبر نیست."))
            }
            Log.i(TAG, "Server-side promo redemption succeeded for user " + userId + " -> " + tier)
            Result.success(tier)
        } catch (e: Exception) {
            Log.e(TAG, "Server-side promo redemption failed: " + e.message, e)
            Result.failure(e)
        }
    }

    /**
     * Uploads local Room database snapshot to Cloud Firestore.
     */
    suspend fun syncAllDataToCloud(
        userId: String,
        profile: StudentProfileEntity?,
        courses: List<CourseEntity>,
        attendance: List<AttendanceEntity>,
        grades: List<GradeEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamEntity> = emptyList(),
        notes: List<NoteEntity> = emptyList(),
        sessions: List<com.example.data.local.entity.CourseSessionEntity> = emptyList()
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.failure(IllegalStateException("همگام‌سازی ابری نیازمند ورود به حساب کاربری است."))
        }

        try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
            val batch = firestore.batch()

            if (profile != null) {
                val profileMap = mapOf(
                    "name" to profile.name,
                    "studentId" to profile.studentId,
                    "major" to profile.major,
                    "university" to profile.university,
                    "entryYear" to profile.entryYear,
                    "currentSemester" to profile.currentSemester,
                    "passedUnits" to profile.passedUnits,
                    "activeUnits" to profile.activeUnits,
                    "declaredGpa" to profile.declaredGpa,
                    "term" to profile.term,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(userDoc.collection("profile").document("current"), profileMap, SetOptions.merge())
            }

            courses.forEach { course ->
                val courseDoc = userDoc.collection("courses").document(course.id)
                val map = mapOf(
                    "id" to course.id,
                    "name" to course.name,
                    "professor" to course.professor,
                    "units" to course.units,
                    "colorHex" to course.colorHex,
                    "semesterId" to course.semesterId,
                    "courseCode" to course.courseCode,
                    "professorId" to (course.professorId ?: ""),
                    "examDate" to course.examDate,
                    "examTime" to course.examTime,
                    "examLocation" to course.examLocation,
                    "notes" to course.notes,
                    "isArchived" to course.isArchived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(courseDoc, map, SetOptions.merge())
            }

            sessions.forEach { sess ->
                val sessDoc = userDoc.collection("course_sessions").document(sess.id)
                val map = mapOf(
                    "id" to sess.id,
                    "courseId" to sess.courseId,
                    "day" to sess.day,
                    "start" to sess.start,
                    "end" to sess.end,
                    "location" to sess.location,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(sessDoc, map, SetOptions.merge())
            }

            attendance.forEach { att ->
                val attDoc = userDoc.collection("attendance").document(att.courseId)
                val map = mapOf(
                    "courseId" to att.courseId,
                    "courseName" to att.courseName,
                    "absentCount" to att.absentCount,
                    "maxAllowed" to att.maxAllowed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(attDoc, map, SetOptions.merge())
            }

            grades.forEach { g ->
                val gDoc = userDoc.collection("grades").document(g.id.toString())
                val map = mapOf(
                    "id" to g.id,
                    "courseName" to g.courseName,
                    "units" to g.units,
                    "midtermGrade" to g.midtermGrade,
                    "finalGrade" to g.finalGrade,
                    "courseId" to g.courseId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(gDoc, map, SetOptions.merge())
            }

            tasks.forEach { t ->
                val tDoc = userDoc.collection("tasks").document(t.id.toString())
                val map = mapOf(
                    "id" to t.id,
                    "title" to t.title,
                    "courseName" to t.courseName,
                    "dueDate" to t.dueDate,
                    "isCompleted" to t.isCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(tDoc, map, SetOptions.merge())
            }

            exams.forEach { e ->
                val eDoc = userDoc.collection("exams").document(e.id)
                val map = mapOf(
                    "id" to e.id,
                    "courseId" to e.courseId,
                    "courseName" to e.courseName,
                    "date" to e.date,
                    "time" to e.time,
                    "location" to e.location,
                    "notes" to e.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(eDoc, map, SetOptions.merge())
            }

            notes.forEach { n ->
                val nDoc = userDoc.collection("notes").document(n.id)
                val map = mapOf(
                    "id" to n.id,
                    "courseId" to (n.courseId ?: ""),
                    "title" to n.title,
                    "content" to n.content,
                    "updatedAt" to n.updatedAt
                )
                batch.set(nDoc, map, SetOptions.merge())
            }

            val now = System.currentTimeMillis()
            batch.update(userDoc, "lastSyncedAt", FieldValue.serverTimestamp())

            batch.commit().awaitResult()
            Log.i(TAG, "Successfully synced ${courses.size} courses, ${sessions.size} sessions, ${exams.size} exams, ${notes.size} notes and related data to Firestore.")
            Result.success(now)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync data to Firestore: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Completely deletes all documents and subcollections for the given user from Firestore.
     * Required for Google Play account deletion compliance.
     */
    suspend fun deleteAllUserDataFromCloud(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId.startsWith("guest_")) return@withContext Result.success(Unit)
        try {
            val db = firestore
            val userDoc = db.collection(USERS_COLLECTION).document(userId)

            val subcollections = listOf("courses", "course_sessions", "attendance", "grades", "tasks", "exams", "notes", "profile")
            for (subcol in subcollections) {
                try {
                    val snapshot = userDoc.collection(subcol).get().awaitResult()
                    if (!snapshot.isEmpty) {
                        val batch = db.batch()
                        snapshot.documents.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting subcollection $subcol: ${e.message}")
                    com.example.util.CrashLogger.recordException(e)
                }
            }

            userDoc.delete().awaitResult()
            Log.i(TAG, "Successfully wiped all cloud Firestore data for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cloud data: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Downloads and restores all collections under `/users/{uid}/` into Room.
     * Implements bidirectional cloud restore with conflict resolution.
     * Prefers the record with the more recent updatedAt/lastSyncedAt timestamp when both local and cloud have data.
     */
    suspend fun restoreAllDataFromCloud(
        userId: String,
        dao: com.example.data.local.dao.StudentDao,
        curriculumDao: com.example.data.local.dao.CurriculumDao? = null,
        firestoreOverride: FirebaseFirestore? = null,
        cloudProfileOverride: CloudProfileData? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.failure(IllegalStateException("بازیابی اطلاعات ابری نیازمند ورود به حساب کاربری است."))
        }

        val targetFirestore = firestoreOverride ?: if (cloudProfileOverride != null) null else firestore
        var restoredCount = 0

        try {
            val userDoc = targetFirestore?.collection(USERS_COLLECTION)?.document(userId)
            val userSnapshot = try {
                userDoc?.get()?.awaitResult()
            } catch (e: Exception) {
                null
            }
            val userLastSyncedAt = userSnapshot?.getTimestamp("lastSyncedAt")?.toDate()?.time ?: 0L

            // 1. Profile
            try {
                val cloudProfile = if (cloudProfileOverride != null) {
                    cloudProfileOverride
                } else if (userDoc != null) {
                    val profileDoc = userDoc.collection("profile").document("current").get().awaitResult()
                    if (profileDoc != null && profileDoc.exists()) {
                        val cloudSemester = profileDoc.getLong("currentSemester")?.toInt() ?: 0
                        val cloudMajor = profileDoc.getString("major") ?: ""
                        CloudProfileData(
                            name = profileDoc.getString("name") ?: "",
                            studentId = profileDoc.getString("studentId") ?: "",
                            major = cloudMajor,
                            university = profileDoc.getString("university") ?: "",
                            entryYear = profileDoc.getLong("entryYear")?.toInt() ?: 0,
                            currentSemester = cloudSemester,
                            passedUnits = profileDoc.getLong("passedUnits")?.toInt() ?: 0,
                            activeUnits = profileDoc.getLong("activeUnits")?.toInt() ?: 0,
                            declaredGpa = profileDoc.getDouble("declaredGpa"),
                            term = profileDoc.getString("term") ?: "",
                            faculty = profileDoc.getString("faculty") ?: "",
                            updatedAt = profileDoc.getTimestamp("updatedAt")?.toDate()?.time
                                ?: profileDoc.getLong("updatedAt")
                                ?: userLastSyncedAt
                        )
                    } else null
                } else null

                if (cloudProfile != null) {
                    val cloudName = cloudProfile.name
                    val cloudStdId = cloudProfile.studentId
                    val cloudMajor = cloudProfile.major
                    val cloudUni = cloudProfile.university
                    val cloudEntryYear = cloudProfile.entryYear
                    val cloudSemester = cloudProfile.currentSemester
                    val cloudPassed = cloudProfile.passedUnits
                    val cloudActive = cloudProfile.activeUnits
                    val cloudGpa = cloudProfile.declaredGpa
                    val cloudTerm = cloudProfile.term ?: ""
                    val cloudFaculty = cloudProfile.faculty
                    val cloudUpdatedAt = cloudProfile.updatedAt

                    val localProfile = dao.getProfileSync()
                    val hasCloudIdentity = cloudName.isNotBlank() || cloudUni.isNotBlank() || cloudMajor.isNotBlank()

                    if (hasCloudIdentity) {
                        val shouldOverwrite = if (localProfile == null) {
                            true
                        } else {
                            val localUpdatedAt = localProfile.updatedAt
                            if (cloudUpdatedAt > localUpdatedAt) {
                                true
                            } else {
                                Log.i(TAG, "Preserving newer local profile (localUpdatedAt=$localUpdatedAt >= cloudUpdatedAt=$cloudUpdatedAt). Skipping cloud restore.")
                                false
                            }
                        }

                        if (shouldOverwrite) {
                            val restoredProfile = StudentProfileEntity(
                                id = 1,
                                name = cloudName.ifBlank { localProfile?.name ?: "دانشجو" },
                                studentId = cloudStdId.ifBlank { localProfile?.studentId ?: "" },
                                university = cloudUni.ifBlank { localProfile?.university ?: "" },
                                major = cloudMajor.ifBlank { localProfile?.major ?: "" },
                                entryYear = if (cloudEntryYear > 0) cloudEntryYear else (localProfile?.entryYear ?: 0),
                                currentSemester = if (cloudSemester > 0) cloudSemester else (localProfile?.currentSemester ?: 0),
                                passedUnits = if (cloudPassed > 0) cloudPassed else (localProfile?.passedUnits ?: 0),
                                declaredPassedCredits = if (cloudPassed > 0) cloudPassed else (localProfile?.declaredPassedCredits ?: 0),
                                activeUnits = if (cloudActive > 0) cloudActive else (localProfile?.activeUnits ?: 0),
                                declaredGpa = cloudGpa ?: localProfile?.declaredGpa,
                                term = cloudTerm,
                                faculty = cloudFaculty.ifBlank { localProfile?.faculty ?: "" },
                                isOnboardingCompleted = localProfile?.isOnboardingCompleted ?: hasCloudIdentity,
                                updatedAt = cloudUpdatedAt
                            )
                            dao.insertProfile(restoredProfile)
                            restoredCount++
                            Log.i(TAG, "Restored profile from cloud (cloudUpdatedAt=$cloudUpdatedAt > localUpdatedAt=${localProfile?.updatedAt ?: 0})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring profile from cloud: ${e.message}")
            }

            // 2. Courses
            if (userDoc != null) {
                try {
                    val coursesSnapshot = userDoc.collection("courses").get().awaitResult()
                if (coursesSnapshot != null && !coursesSnapshot.isEmpty) {
                    val localCoursesMap = dao.getAllCoursesIncludingArchivedSync().associateBy { it.id }
                    coursesSnapshot.documents.forEach { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("name") ?: ""
                        if (name.isNotBlank()) {
                            val cloudCourseUpdatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: userLastSyncedAt
                            val local = localCoursesMap[id]
                            if (local == null || cloudCourseUpdatedAt >= userLastSyncedAt) {
                                val course = CourseEntity(
                                    id = id,
                                    name = name,
                                    colorHex = doc.getString("colorHex")?.takeIf { it.isNotBlank() }
                                        ?: (local?.colorHex ?: ""),
                                    units = doc.getLong("units")?.toInt()
                                        ?: (local?.units ?: 0),
                                    semesterId = doc.getString("semesterId")?.takeIf { it.isNotBlank() }
                                        ?: (local?.semesterId ?: "sem_current"),
                                    courseCode = doc.getString("courseCode") ?: (local?.courseCode ?: ""),
                                    professorId = doc.getString("professorId") ?: local?.professorId,
                                    professor = doc.getString("professor") ?: (local?.professor ?: ""),
                                    examDate = doc.getString("examDate") ?: (local?.examDate ?: ""),
                                    examTime = doc.getString("examTime") ?: (local?.examTime ?: ""),
                                    examLocation = doc.getString("examLocation") ?: (local?.examLocation ?: ""),
                                    notes = doc.getString("notes") ?: (local?.notes ?: ""),
                                    isArchived = doc.getBoolean("isArchived") ?: (local?.isArchived ?: false)
                                )
                                dao.insertCourse(course)
                                restoredCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring courses from cloud: ${e.message}")
            }

            // 2.5 Course Sessions (1-to-N weekly slots)
            if (userDoc != null) {
                try {
                    val sessionsSnapshot = userDoc.collection("course_sessions").get().awaitResult()
                    if (sessionsSnapshot != null && !sessionsSnapshot.isEmpty) {
                        val localSessionsMap = dao.getAllSessionsSync().associateBy { it.id }
                        sessionsSnapshot.documents.forEach { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val courseId = doc.getString("courseId") ?: ""
                            if (courseId.isNotBlank()) {
                                val local = localSessionsMap[id]
                                val session = com.example.data.local.entity.CourseSessionEntity(
                                    id = id,
                                    courseId = courseId,
                                    day = doc.getLong("day")?.toInt() ?: (local?.day ?: -1),
                                    start = doc.getString("start")?.takeIf { it.isNotBlank() } ?: (local?.start ?: ""),
                                    end = doc.getString("end")?.takeIf { it.isNotBlank() } ?: (local?.end ?: ""),
                                    location = doc.getString("location") ?: (local?.location ?: "")
                                )
                                dao.insertCourseSession(session)
                                restoredCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error restoring course sessions from cloud: ${e.message}")
                }
            }

            // 3. Attendance
            try {
                val attSnapshot = userDoc.collection("attendance").get().awaitResult()
                if (attSnapshot != null && !attSnapshot.isEmpty) {
                    attSnapshot.documents.forEach { doc ->
                        val cId = doc.getString("courseId") ?: doc.id
                        if (cId.isNotBlank()) {
                            val localAtt = dao.getAttendanceByCourseId(cId)
                            val absentCount = doc.getLong("absentCount")?.toInt() ?: 0
                            val maxAllowed = doc.getLong("maxAllowed")?.toInt() ?: 3
                            val courseName = doc.getString("courseName") ?: (localAtt?.courseName ?: "")
                            if (localAtt == null || absentCount != localAtt.absentCount) {
                                dao.insertAttendance(
                                    AttendanceEntity(
                                        courseId = cId,
                                        courseName = courseName,
                                        absentCount = absentCount,
                                        maxAllowed = maxAllowed
                                    )
                                )
                                restoredCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring attendance from cloud: ${e.message}")
            }

            // 4. Grades
            try {
                val gradesSnapshot = userDoc.collection("grades").get().awaitResult()
                if (gradesSnapshot != null && !gradesSnapshot.isEmpty) {
                    val localGrades = dao.getAllGradesSync()
                    gradesSnapshot.documents.forEach { doc ->
                        val id = doc.getLong("id")?.toInt() ?: (doc.id.toIntOrNull() ?: 0)
                        val courseName = doc.getString("courseName") ?: ""
                        val courseId = doc.getString("courseId") ?: ""
                        val units = doc.getLong("units")?.toInt() ?: 3
                        val mid = doc.getDouble("midtermGrade") ?: 0.0
                        val fin = doc.getDouble("finalGrade") ?: 0.0

                        val existing = localGrades.find { (it.id > 0 && it.id == id) || (courseId.isNotBlank() && it.courseId == courseId) }
                        if (existing == null) {
                            dao.insertGrade(
                                GradeEntity(
                                    id = if (id > 0) id else 0,
                                    courseName = courseName,
                                    units = units,
                                    midtermGrade = mid,
                                    finalGrade = fin,
                                    courseId = courseId
                                )
                            )
                            restoredCount++
                        } else if (existing.midtermGrade != mid || existing.finalGrade != fin) {
                            dao.insertGrade(
                                existing.copy(
                                    midtermGrade = mid,
                                    finalGrade = fin
                                )
                            )
                            restoredCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring grades from cloud: ${e.message}")
            }

            // 5. Tasks
            try {
                val tasksSnapshot = userDoc.collection("tasks").get().awaitResult()
                if (tasksSnapshot != null && !tasksSnapshot.isEmpty) {
                    val localTasks = dao.getAllTasksSync()
                    tasksSnapshot.documents.forEach { doc ->
                        val id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L)
                        val title = doc.getString("title") ?: ""
                        val courseName = doc.getString("courseName") ?: ""
                        val dueDate = doc.getString("dueDate") ?: ""
                        val isCompleted = doc.getBoolean("isCompleted") ?: false
                        val courseId = doc.getString("courseId") ?: ""
                        val semesterId = doc.getString("semesterId")

                        val existing = localTasks.find { it.id == id || (it.title == title && it.dueDate == dueDate) }
                        if (existing == null) {
                            dao.insertTask(
                                TaskEntity(
                                    id = if (id > 0L) id else 0L,
                                    title = title,
                                    courseName = courseName,
                                    dueDate = dueDate,
                                    isCompleted = isCompleted,
                                    courseId = courseId,
                                    semesterId = semesterId
                                )
                            )
                            restoredCount++
                        } else if (existing.isCompleted != isCompleted) {
                            dao.insertTask(existing.copy(isCompleted = isCompleted))
                            restoredCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring tasks from cloud: ${e.message}")
            }

            // 6. Exams
            try {
                val examsSnapshot = userDoc.collection("exams").get().awaitResult()
                if (examsSnapshot != null && !examsSnapshot.isEmpty) {
                    examsSnapshot.documents.forEach { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val courseId = doc.getString("courseId") ?: ""
                        val courseName = doc.getString("courseName") ?: ""
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val location = doc.getString("location") ?: ""
                        val notes = doc.getString("notes") ?: ""

                        val existing = dao.getExamById(id) ?: dao.getExamByCourseId(courseId)
                        if (existing == null) {
                            dao.insertExam(
                                ExamEntity(
                                    id = id,
                                    courseId = courseId,
                                    courseName = courseName,
                                    date = date,
                                    time = time,
                                    location = location,
                                    notes = notes
                                )
                            )
                            restoredCount++
                        } else {
                            dao.insertExam(
                                existing.copy(
                                    date = date.ifBlank { existing.date },
                                    time = time.ifBlank { existing.time },
                                    location = location.ifBlank { existing.location },
                                    notes = notes.ifBlank { existing.notes }
                                )
                            )
                            restoredCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring exams from cloud: ${e.message}")
            }

            // 7. Notes
            try {
                val notesSnapshot = userDoc.collection("notes").get().awaitResult()
                if (notesSnapshot != null && !notesSnapshot.isEmpty) {
                    notesSnapshot.documents.forEach { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        val content = doc.getString("content") ?: ""
                        val courseId = doc.getString("courseId")?.ifBlank { null }
                        val cloudUpdatedAt = doc.getLong("updatedAt")
                            ?: (doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L)

                        val localNote = dao.getNoteById(id)
                        if (localNote == null || cloudUpdatedAt >= localNote.updatedAt) {
                            dao.insertNote(
                                NoteEntity(
                                    id = id,
                                    courseId = courseId,
                                    title = title,
                                    content = content,
                                    updatedAt = if (cloudUpdatedAt > 0L) cloudUpdatedAt else System.currentTimeMillis()
                                )
                            )
                            restoredCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring notes from cloud: ${e.message}")
            }
            }

            Log.i(TAG, "Completed restoring data from cloud for user $userId. Total items processed/upserted: $restoredCount")
            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore all data from cloud: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }
}
