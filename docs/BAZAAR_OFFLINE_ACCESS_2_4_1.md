# Bazaar review: 2.4.1 / 2011

The public build uses the same offline account-entry policy for all users. There is no reviewer detection, hidden login bypass or simulated authentication.

## Review instructions
1. Install the signed candidate. Launch without a VPN; repeat with no network.
2. The local student setup or existing dashboard should open directly, with no authentication spinner or registration gate.
3. Complete or skip local setup; add a course, task, exam and grade. Restart and confirm preservation.
4. Open the account page: profile, local backup and privacy remain available, while email/Google sign-in and account creation are absent.
5. Guest AI stays offline; paid checkout and online support do not ask the user to enter the disabled login route.
6. Export/import a backup; upgrade from the previous installed production build without clearing its data. Confirm package and original certificate remain unchanged.
7. Repeat on Samsung A35 / Android 15, light/dark themes and enlarged text.

## Architecture and limits
ReleaseAccessPolicy.onlineAccountEntryEnabled is false. Both startup auth waiting and the startup gate are bypassed; account forms have the same guard. This policy is independent of Firebase initialization, connectivity and the persisted guest-mode toggle. It does not clear an existing session or grant a paid entitlement. Existing authenticated-user account management remains intact.

Firebase email/Google, Credential Manager, self-hosted backend, Room schemas and signing material are unchanged. Re-enabling online entry requires a separately reviewed change and production authentication/network validation. Existing connected users may still experience service/network errors; the new-user local path does not depend on those services.

The approved 2.4.1 publisher waits for successful Android CI and Firebase verification on the exact current main SHA. It reuses the signed candidates, verifies identity and original certificate, generates and validates the Bazaar AAB/BIN pair, then publishes v2.4.1 with a Bazaar ZIP. It refuses to replace an existing release. Android CI and publication use version 2.4.1 / 2011. Store acceptance and physical-device validation are still external gates.

## Suggested Persian reviewer note
در نسخهٔ ۲.۴.۱ با کد ۲۰۱۱، ورود و ساخت حساب آنلاین موقتاً برای همهٔ کاربران غیرفعال شده است. برنامه مستقیماً وارد راه‌اندازی محلی یا صفحهٔ اصلی می‌شود و برای استفاده از امکانات محلی نیازی به حساب، اینترنت یا فیلترشکن ندارد. اطلاعات روی دستگاه ذخیره می‌شود و پشتیبان‌گیری و بازیابی فایل در دسترس است. لطفاً بررسی را با فایل جدید انجام دهید؛ نسخهٔ ۲۰۰۸ نسخهٔ پیشین است.

## Validation ledger
Pending: exact-SHA Android CI, signed artifact verification, Samsung A35/no-network install and upgrade, and marketplace review. Do not infer approval from a passing build.
