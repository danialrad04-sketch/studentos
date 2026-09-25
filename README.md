<div align="center">

# Student OS

### دستیار هوشمند و سیستم‌عامل تحصیلی دانشجو

**Student OS** یک اپلیکیشن اندرویدی برای یکپارچه‌کردن زندگی تحصیلی دانشجو است؛ از برنامه هفتگی و امتحانات تا نمرات، حضور و غیاب، برنامه‌ریزی مطالعه و دستیار هوشمند.

<br/>

[![Release](https://img.shields.io/github/v/release/danialrad04-sketch/studentos?display_name=tag&style=for-the-badge&color=7C3AED)](https://github.com/danialrad04-sketch/studentos/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/danialrad04-sketch/studentos/publish-release.yml?style=for-the-badge&label=build)](https://github.com/danialrad04-sketch/studentos/actions)
[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

</div>

---

## ✦ درباره Student OS

Student OS با یک ایده ساده ساخته شده:

> **همه‌چیز برای دانشگاه، در یک فضای منظم و هوشمند.**

به‌جای پراکنده‌بودن برنامه کلاس‌ها، امتحانات، تکالیف، نمرات و ابزارهای مطالعه در چند اپ، Student OS این جریان را در یک تجربه واحد جمع می‌کند.

این پروژه یک **Android app با Kotlin و Jetpack Compose** است و در کنار قابلیت‌های محلی، از سرویس‌های Firebase و قابلیت‌های هوش مصنوعی برای بخش‌های متصل استفاده می‌کند.

---

## 🚀 قابلیت‌ها

| بخش | امکانات |
| --- | --- |
| 🏠 **داشبورد** | نمای سریع وضعیت تحصیلی و دسترسی به ابزارهای اصلی |
| 🗓️ **برنامه هفتگی** | مدیریت کلاس‌ها، روزها و ساعت‌ها |
| 🎯 **تمرکز و مطالعه** | Pomodoro و ابزارهای Sprint/Task |
| 📝 **امتحانات** | ثبت و پیگیری امتحانات و زمان‌بندی |
| 📊 **نمرات و معدل** | مدیریت نمرات و محاسبات تحصیلی |
| 📡 **Attendance Radar** | رصد وضعیت حضور و غیاب |
| 🤖 **Academic Copilot** | دستیار هوشمند برای کارهای تحصیلی |
| 🔎 **Spotlight / Omnibox** | جست‌وجوی سریع بین داده‌ها و ماژول‌های برنامه |
| 🎓 **Academic Passport** | نمای یکپارچه از مسیر تحصیلی |
| 🏆 **Hall of Fame** | نمایش دستاوردها و سیستم انگیزشی |

---

## 🧠 معماری

بخش‌های اصلی منطق برنامه به شکل ماژولار طراحی شده‌اند تا قابلیت‌های تحصیلی مستقل از UI قابل توسعه و تست باشند.

موتورهای اصلی شامل:

- \`GlobalSearchEngine\`
- \`AcademicGamificationEngine\`
- \`AcademicCopilotEngine\`
- \`CurriculumEngine\`
- \`AcademicRiskEngine\`

رابط کاربری بر پایه **Jetpack Compose + Material 3** ساخته شده و ساختار برنامه از الگوی **MVVM** و ViewModelهای Compose استفاده می‌کند.

---

## 🔐 امنیت

- Secrets محلی در \`.env\` نگهداری می‌شوند و نباید وارد Git شوند.
- Release signing با **Release Keystore** انجام می‌شود.
- Credentials مربوط به CI در **GitHub Actions Secrets** نگهداری می‌شوند.
- ارتباطات سمت سرور از طریق API انجام می‌شود و اپلیکیشن نباید مستقیماً به دیتابیس متصل شود.
- برای App Integrity از Firebase App Check / Play Integrity در پیکربندی پروژه استفاده شده است.

> **هرگز Keystore، password، API key، Firebase credential یا فایل \`.env\` واقعی را commit نکنید.**

---

## 🛠️ تکنولوژی‌ها

### Android
- Kotlin
- Jetpack Compose
- Material 3
- AndroidX
- Navigation Compose
- Room
- Kotlin Coroutines
- WorkManager

### Backend / Cloud
- Firebase Authentication
- Cloud Firestore
- Firebase App Check
- Firebase AI
- Retrofit / OkHttp

### Build & Quality
- Gradle Kotlin DSL
- KSP
- Robolectric
- Compose UI Tests
- Roborazzi

---

## 📦 آخرین Release

نسخه Production فعلی:

**\`v1.0.1-production\`**

فایل‌های انتشار در GitHub Release قرار گرفته‌اند:

- **AAB** — برای انتشار در فروشگاه‌ها
- **APK** — برای نصب و تست مستقیم

### دریافت

👉 **[مشاهده آخرین Release](https://github.com/danialrad04-sketch/studentos/releases/latest)**

---

## 💻 اجرای پروژه

### پیش‌نیازها

- Android Studio
- JDK سازگار با پروژه
- Android SDK با API 36
- Android SDK Platform / Build Tools
- دستگاه Android یا Emulator با API 24+

### Clone

\`\`\`bash
git clone https://github.com/danialrad04-sketch/studentos.git
cd studentos
\`\`\`

### تنظیم Secrets

فایل نمونه:

\`\`\`text
.env.example
\`\`\`

را بررسی کنید و Secrets موردنیاز محیط توسعه را در فایل محلی \`.env\` قرار دهید.

**فایل واقعی \`.env\` را commit نکنید.**

### Build

\`\`\`bash
./gradlew assembleDebug
\`\`\`

برای Release، signing باید با credentialهای امن محیط Release انجام شود.

---

## 🧩 ساختار پروژه

\`\`\`text
studentos/
├── app/                # Android application
├── backend/            # Backend/API layer
├── functions/          # Cloud/server functions
├── firestore.rules     # Firestore security rules
├── gradle/             # Gradle configuration
├── .github/            # CI/CD workflows
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
\`\`\`

---

## 🔄 CI/CD

فرآیند Release پروژه:

\`\`\`text
Commit / Tag
    ↓
GitHub Actions
    ↓
Release Build
    ↓
Release Signing
    ↓
APK + AAB
    ↓
GitHub Release
\`\`\`

Release credentials در Repository Secrets نگهداری می‌شوند و داخل Repository قرار نمی‌گیرند.

---

## 📌 وضعیت پروژه

آخرین نسخه رسمی منتشرشده در `main` برابر `v1.0.2-production` است.

شاخه `feat/academic-premium-design-system` در حال تکمیل verificationهای کیفیت، همگام‌سازی، دسترسی‌پذیری و release است و تا بسته‌شدن gateهای تعریف‌شده، به‌عنوان نسخه جدید production معرفی نمی‌شود.

برای دریافت آخرین نسخه منتشرشده، از بخش **Releases** استفاده کنید.

---

<div align="center">

### Built for students. Designed to make academic life simpler.

**Student OS · Android**

</div>
