# Enterprise-Grade Obfuscation & Shrinking Rules for Student OS
# Designed for Retrofit, Moshi, Room, and Firebase

# Keep Line Numbers for Crashlytics Stacktrace Analysis in Production
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Maintain critical attributes for reflection, generics, and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin Coroutines & Flow Meta Info
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    private volatile int wash;
}

# Moshi Serialization Rules (prevents model serialization & codegen failures)
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class * {
    @com.squareup.moshi.JsonClass *;
}

# Retrofit Network Interfaces Rules
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# Room Database Entities & DAOs (prevents runtime database binding exceptions)
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.relation.** { *; }

# Domain / DTO / Network Models
-keep class com.example.domain.model.** { *; }

# Firebase Authentication, Firestore, Remote Config, App Check
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-keepattributes *Annotation*,Signature
# Optional classes referenced by Tink/Ktor integrations that are not bundled by this app.
# These paths are not required for Student OS runtime, but R8 otherwise treats them as errors.
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**
-dontwarn java.lang.management.**
