# Student OS v1.3.0

## UX Overhaul

Student OS 1.3.0 یک بازطراحی تجربه کاربری روی هسته فعلی است؛ داده‌های تحصیلی، Room persistence، sync، authentication و موتورهای domain تغییر ماهوی نکرده‌اند.

### تغییرات اصلی

- بازطراحی Course Workspace به یک تجربه یکپارچه و خواناتر
- نمایش یکپارچه خلاصه درس، جلسات، حضور و غیاب، کارها، امتحان و نمرات
- افزودن مسیر سریع «شروع تمرکز» و «کار جدید»
- بازطراحی کارت‌های کلاس در برنامه هفتگی با تمرکز بر زمان، استاد، مکان، واحد و تداخل
- تأیید حذف درس در UX جدید حفظ شده است
- بهبود hierarchy و surface نوار Floating Island
- حفظ RTL، reduced-motion و touch targetهای موجود
- بدون تغییر در قراردادهای persistence و backend

## Build

- versionName: 1.3.0
- versionCode: 4
- targetSdk: 36

## Release Verification

قبل از انتشار نهایی باید gateهای CI شامل unit tests، lint، backend tests، instrumentation smoke، signed APK/AAB verification و certificate verification سبز باشند.

## Security

Release keystore و credentialها خارج از repository نگهداری می‌شوند و نباید commit شوند.
