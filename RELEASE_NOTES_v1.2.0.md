# یادداشت رسمی انتشار نسخه ۱.۲ - Student OS (نسخه پایدار و نهایی)
## Version 1.2.0 (Build 3)

تاریخ انتشار: ۲۶ سپتامبر ۲۰۲۶  
مخزن رسمی: [danialrad04-sketch/studentos](https://github.com/danialrad04-sketch/studentos)

---

### 🌟 مهم‌ترین تغییرات و بهبودهای نسخه ۱.۲

1. **حل قطعی ورود با گوگل و سرویس‌های فایربیس در حالت Release:**
   - یکپارچه‌سازی با کتابخانه مدرن اندروید `androidx.credentials.CredentialManager` و `com.google.android.libraries.identity.googleid.GetGoogleIdOption`.
   - استخراج ایمن اکتیویتی به وسیله متد اختصاصی `findActivity()` جهت جلوگیری از خطای بافت (Context) در فرآیند نمایش دیالوگ ورود گوگل.
   - اضافه شدن قوانین جامع R8 و Proguard برای دسترسی بدون نقص به کلاس‌های `Firebase Auth`, `Firestore`, `Google Play Services`, و `Credential Manager` در پکیج‌های مینایفای شده.
   - مدیریت هوشمند و ایمن سرویس `Firebase App Check` در بیلد ریلیز جهت عدم ایجاد بن‌بست یا خطای احراز هویت در محیط‌های مختلف شبکه‌ای و کافه بازار.

2. **ارتقای مشخصات بیلد (Build Specifications):**
   - `versionCode: 3`
   - `versionName: "1.2.0"`
   - `targetSdk: 36` (سازگاری کامل با استانداردهای نوین اندروید ۱۶ و کافه بازار)
   - `minSdk: 24` (پوشش بیش از ۹۶٪ دستگاه‌های اندرویدی فعال در ایران)

3. **امضای دیجیتال چندگانه با کلید اختصاصی Release:**
   - امضا شده با کلید اختصاصی `my-upload-key.jks` (الگوریتم RSA 2048 بیتی معتبر تا ۱۰,۰۰۰ روز)
   - پشتیبانی همزمان از شمای امضای **v1 (JAR Signature)**، **v2 (APK Signature Scheme)** و **v3 (APK Signature Scheme v3)**.
   - کاملاً منطبق بر قوانین انتشار در **کافه بازار (Cafe Bazaar)**، **مایکت (Myket)** و **Google Play**.

4. **دیزاین‌سیستم و رابط کاربری ۲۰۲۶:**
   - پیاده‌سازی کامل استانداردهای Glassmorphism 2.0 و Material 3 Expressive
   - پشتیبانی بی‌نقص از فونت وزیرمتن و چیدمان استاندارد راست‌به‌چپ (RTL)
   - داشبورد بنتو گرید (Bento Grid) با ویجت‌های تعاملی، انیمیشن‌های فیزیکی سیال و نوار وضعیت زنده (Dynamic Pill Capsule).

---

### 📥 دانلود و آزمایش مستقیم فایل خروجی

- **فایل Release APK نسخه ۱.۲:** `public/app-release.apk`
- **لینک مستقیم دانلود محیط ابری:** `https://ais-dev-ipbo5hxjbpg3eqqijl2gpe-330473390572.europe-west2.run.app/app-release.apk`
