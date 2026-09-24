package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.util.CrashLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Production-Grade Academic Copilot client powered by Firebase AI & Google AI Studio fallback.
 * 
 * Supports:
 * 1. Dynamic model selection via Firebase Remote Config (key: "gemini_model_name", fallback: "gemini-3.5-flash-lite")
 * 2. Primary: Firebase AI Logic SDK with App Check
 * 3. Secondary/Direct: REST API using API key from BuildConfig or environment
 */
object GeminiApiClient {

    private const val TAG = "GeminiApiClient"
    const val REMOTE_CONFIG_MODEL_KEY = "gemini_model_name"
    const val DEFAULT_MODEL_NAME = "gemini-3.5-flash"

    private val CANDIDATE_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite-preview",
        "gemini-3.1-pro-preview"
    )

    @Volatile
    private var activeModelOverride: String? = null

    @Volatile
    private var cachedGenerativeModel: Pair<String, GenerativeModel>? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Initializes Firebase Remote Config to fetch and activate the latest supported Gemini model name.
     */
    fun initRemoteConfig() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(mapOf(REMOTE_CONFIG_MODEL_KEY to DEFAULT_MODEL_NAME))
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fetchedModel = remoteConfig.getString(REMOTE_CONFIG_MODEL_KEY).trim()
                        if (fetchedModel.isNotBlank()) {
                            activeModelOverride = fetchedModel
                            Log.d(TAG, "Firebase Remote Config activated: active model is '$fetchedModel'")
                        }
                    } else {
                        Log.w(TAG, "Firebase Remote Config fetch incomplete: ${task.exception?.message}")
                    }
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize Firebase Remote Config: ${e.message}")
        }
    }

    /**
     * Returns the active model name dynamically from Remote Config or fallback default.
     */
    fun getActiveModelName(): String {
        activeModelOverride?.takeIf { it.isNotBlank() }?.let { return it }

        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configured = remoteConfig.getString(REMOTE_CONFIG_MODEL_KEY).trim()
            if (configured.isNotBlank()) {
                activeModelOverride = configured
                configured
            } else {
                DEFAULT_MODEL_NAME
            }
        } catch (_: Throwable) {
            DEFAULT_MODEL_NAME
        }
    }

    /**
     * Safely retrieves or instantiates the Firebase AI GenerativeModel for the current active model.
     */
    private fun getGenerativeModel(): GenerativeModel? {
        val currentModel = getActiveModelName()
        cachedGenerativeModel?.let { (name, model) ->
            if (name == currentModel) return model
        }

        return try {
            val model = Firebase.ai.generativeModel(modelName = currentModel)
            cachedGenerativeModel = Pair(currentModel, model)
            model
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase AI Logic initialization failed for model '$currentModel': ${e.message}")
            null
        }
    }

    /**
     * Resolves the effective API key from custom input, BuildConfig, or environment.
     */
    fun resolveApiKey(customApiKey: String? = null): String? {
        customApiKey?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val fromBuildConfig = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim()?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }
        } catch (_: Throwable) {
            null
        }
        if (!fromBuildConfig.isNullOrBlank()) return fromBuildConfig

        val fromEnv = try {
            System.getenv("GEMINI_API_KEY")?.trim()?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }
        } catch (_: Throwable) {
            null
        }
        return fromEnv
    }

    /**
     * Main academic advice query using the currently supported Gemini model.
     * Supports multi-turn conversational history and rich academic context.
     */
    suspend fun generateAcademicAdvice(
        prompt: String,
        studentContext: String,
        history: List<Pair<String, String>> = emptyList(),
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val currentModel = getActiveModelName()
        val systemInstruction = buildString {
            append("شما دستیار هوشمند، حرفه‌ای و مشاور ارشد تحصیلی دانشگاهی (Student OS AI Copilot) هستید.\n")
            append("پاسخ‌های شما باید کاملاً به زبان فارسی رسمی، صمیمی، ساختاریافته با مارک‌داون تمیز و بر اساس آخرین آیین‌نامه‌های مصوب وزارت علوم، تحقیقات و فناوری ایران باشد:\n")
            append("- سقف غیبت مجاز ۳/۱۶ (حداکثر ۳ جلسه در دروس نظری و ۲ جلسه در دروس آزمایشگاهی/کارگاهی طبق ماده ۳۵).\n")
            append("- حداقل نمره قبولی دروس کارشناسی ۱۰ و معدل مشروطی زیر ۱۲ (سقف انتخاب واحد ترم بعد ۱۴ واحد).\n")
            append("- سقف انتخاب واحد عادی ۲۰ واحد و برای معدل الف (بالای ۱۷) حداکثر ۲۴ واحد.\n")
            append("- کف مجاز انتخاب واحد در ترم عادی ۱۲ واحد (به جز ترم آخر).\n")
            append("- حذف اضطراری تک‌درس تا ۵ هفته مانده به پایان ترم (به شرط نریختن کف ۱۲ واحد).\n")
            append("- سنوات مجاز کارشناسی پیوسته ۱۰ نیم‌سال تحصیلی (۵ سال).\n")
            append("- معرفی به استاد: حداکثر ۲ درس نظری تا سقف ۴ واحد برای ترم آخر و فراغت از تحصیل.\n\n")
            append("سوابق تحصیلی و بافت کلاسی زنده دانشجو:\n")
            append(studentContext)
        }

        val effectiveApiKey = resolveApiKey(customApiKey)
        var lastError: Throwable? = null

        // 1. Direct Google AI REST call with multi-turn and automatic model cascading
        if (!effectiveApiKey.isNullOrBlank()) {
            val modelsToTry = listOf(currentModel) + CANDIDATE_MODELS.filter { it != currentModel }
            for (modelCandidate in modelsToTry) {
                val restResult = callGeminiRestApi(
                    prompt = prompt,
                    apiKey = effectiveApiKey,
                    modelName = modelCandidate,
                    systemInstructionText = systemInstruction,
                    history = history
                )
                if (restResult.isSuccess) {
                    activeModelOverride = modelCandidate
                    return@withContext restResult
                } else {
                    val err = restResult.exceptionOrNull()
                    lastError = err
                    Log.e(TAG, "REST call with model $modelCandidate failed: ${err?.message}", err)
                    err?.let { CrashLogger.recordException(it) }
                }
            }
        } else {
            val noKeyMsg = "No API key configured for Gemini REST calls (BuildConfig.GEMINI_API_KEY is empty and no custom key provided)"
            Log.w(TAG, noKeyMsg)
            lastError = IllegalStateException(noKeyMsg)
        }

        // 2. Try Firebase AI SDK with dynamic GenerativeModel
        val model = getGenerativeModel()
        if (model != null) {
            try {
                val fullPrompt = "$systemInstruction\n\nپرسش دانشجو:\n$prompt"
                val response = model.generateContent(fullPrompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(text.trim())
                }
            } catch (e: Throwable) {
                lastError = e
                Log.e(TAG, "Firebase AI generation failed with model $currentModel: ${e.message}", e)
                CrashLogger.recordException(e)
            }
        } else {
            Log.w(TAG, "Firebase AI GenerativeModel is null (Firebase AI / App Check might not be initialized or active)")
        }

        // 3. Fallback error with full recorded exception
        val finalException = lastError ?: IllegalStateException("عدم امکان ارتباط با سرور هوش مصنوعی یا عدم وجود کلید معتبر.")
        Log.e(TAG, "All AI advice attempts failed. Final error: ${finalException.message}", finalException)
        CrashLogger.recordException(finalException)
        Result.failure(finalException)
    }

    /**
     * Extracts courses and sessions from schedule photos using the current multimodal Gemini model.
     */
    suspend fun extractScheduleFromImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        customApiKey: String? = null
    ): Result<List<com.example.data.parser.ParsedCourseDraft>> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("فایل تصویر انتخابی خالی است."))
        }

        val currentModel = getActiveModelName()
        val prompt = """
            شما یک سیستم متخصص استخراج برنامه هفتگی کلاسی و برگه انتخاب واحد دانشگاهی به زبان فارسی هستید.
            لطفاً با دقت تصویر پیوست شده را تحلیل کرده و تمام دروس موجود در جدول برنامه هفتگی را استخراج کنید.
            خروجی باید صرفاً و فقط یک آرایه JSON معتبر (بدون هیچ توضیح متنی، مارک‌داون یا کدبلاک) با ساختار زیر باشد:
            [
              {
                "name": "نام درس",
                "units": 3,
                "dayOfWeek": 0,
                "dayName": "شنبه",
                "startTime": "08:00",
                "endTime": "10:00",
                "location": "کلاس ۱۰۷",
                "instructor": "دکتر سیامک علیپور"
              }
            ]
            قوانین الزامی:
            1. فیلد dayOfWeek باید یک عدد صحیح باشد: 0 برای شنبه، 1 برای یکشنبه، 2 برای دوشنبه، 3 برای سه‌شنبه، 4 برای چهارشنبه، 5 برای پنج‌شنبه، 6 برای جمعه.
            2. فرمت ساعت startTime و endTime باید حتماً HH:mm به ارقام انگلیسی باشد (مانند 08:00 و 10:00).
            3. در صورتی که محل کلاس یا نام استاد ذکر نشده بود، به ترتیب "دانشکده" و "استاد دانشکده" قرار دهید.
            4. تعداد واحد به صورت عدد صحیح (پیش‌فرض 3).
            فقط آرایه JSON را برگردانید.
        """.trimIndent()

        val effectiveApiKey = resolveApiKey(customApiKey)

        // 1. Direct REST Vision call
        if (!effectiveApiKey.isNullOrBlank()) {
            val restResult = callGeminiVisionRestApi(prompt, imageBytes, mimeType, effectiveApiKey, currentModel)
            if (restResult.isSuccess) {
                val parsed = parseScheduleJson(restResult.getOrThrow())
                if (parsed.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            }
        }

        // 2. Try Firebase AI SDK with dynamic GenerativeModel
        val model = getGenerativeModel()
        if (model != null) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    val contentObj = com.google.firebase.ai.type.content {
                        image(bitmap)
                        text(prompt)
                    }
                    val response = model.generateContent(contentObj)
                    val text = response.text
                    if (!text.isNullOrBlank()) {
                        val parsed = parseScheduleJson(text)
                        if (parsed.isNotEmpty()) {
                            return@withContext Result.success(parsed)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase AI vision extraction failed: ${e.message}")
            }
        }

        if (!effectiveApiKey.isNullOrBlank()) {
            val restResult = callGeminiVisionRestApi(prompt, imageBytes, mimeType, effectiveApiKey, currentModel)
            if (restResult.isSuccess) {
                val parsed = parseScheduleJson(restResult.getOrThrow())
                return@withContext Result.success(parsed)
            } else {
                return@withContext Result.failure(restResult.exceptionOrNull() ?: Exception("خطا در پردازش هوشمند تصویر برنامه"))
            }
        }

        Result.failure(
            IllegalStateException("پردازش هوشمند تصویر انجام نشد. لطفاً اتصال اینترنت خود را بررسی نمایید.")
        )
    }

    private fun callGeminiVisionRestApi(
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String,
        apiKey: String,
        modelName: String
    ): Result<String> {
        return try {
            val base64Data = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mime_type", mimeType)
                                    put("data", base64Data)
                                }
                                put("inline_data", inlineData)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful && bodyString.isNotBlank()) {
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return Result.success(text.trim())
                        }
                    }
                }
            }
            Result.failure(Exception("خطا در پاسخ سرویس AI ($modelName): ${response.code} $bodyString"))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Vision REST API exception with model $modelName: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseScheduleJson(rawText: String): List<com.example.data.parser.ParsedCourseDraft> {
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val startIdx = clean.indexOf('[')
        val endIdx = clean.lastIndexOf(']')
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return emptyList()
        }
        val jsonArray = JSONArray(clean.substring(startIdx, endIdx + 1))
        val list = mutableListOf<com.example.data.parser.ParsedCourseDraft>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isBlank()) continue
            val units = obj.optInt("units", 3)
            val dayOfWeek = obj.optInt("dayOfWeek", 0).coerceIn(0, 6)
            val dayName = obj.optString("dayName", dayOfWeekToName(dayOfWeek))
            val startTime = obj.optString("startTime", "08:00")
            val endTime = obj.optString("endTime", "10:00")
            val location = obj.optString("location", "دانشکده")
            val instructor = obj.optString("instructor", "استاد دانشکده")

            list.add(
                com.example.data.parser.ParsedCourseDraft(
                    name = name,
                    units = units,
                    dayOfWeek = dayOfWeek,
                    dayName = dayName,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    instructor = instructor,
                    confidence = 0.96f
                )
            )
        }
        return list
    }

    private fun dayOfWeekToName(day: Int): String = when (day) {
        0 -> "شنبه"
        1 -> "یکشنبه"
        2 -> "دوشنبه"
        3 -> "سه‌شنبه"
        4 -> "چهارشنبه"
        5 -> "پنج‌شنبه"
        6 -> "جمعه"
        else -> "شنبه"
    }

    private fun callGeminiRestApi(
        prompt: String,
        apiKey: String,
        modelName: String,
        systemInstructionText: String? = null,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val jsonBody = JSONObject().apply {
                if (!systemInstructionText.isNullOrBlank()) {
                    val sysObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstructionText) })
                        }
                        put("parts", parts)
                    }
                    put("system_instruction", sysObj)
                }

                val contents = JSONArray().apply {
                    // Previous turns
                    for ((role, text) in history) {
                        val turnObj = JSONObject().apply {
                            put("role", if (role == "user") "user" else "model")
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", text) })
                            }
                            put("parts", parts)
                        }
                        put(turnObj)
                    }

                    // Current user prompt
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.95)
                    put("maxOutputTokens", 2048)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful && bodyString.isNotBlank()) {
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return Result.success(text.trim())
                        }
                    }
                }
            }
            Result.failure(Exception("خطا در پاسخ سرور AI ($modelName): ${response.code} $bodyString"))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini REST API exception with model $modelName: ${e.message}", e)
            Result.failure(e)
        }
    }
}
