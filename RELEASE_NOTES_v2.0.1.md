# Student OS v2.0.1

## Bazaar review access
- Fresh installations can enter Student OS without authentication.
- Guest mode is enabled by default only when no saved guest preference exists and first launch has not been completed.
- Existing users keep their stored guest/authentication choice.
- Email/Firebase and Google Sign-In remain available from the account flow.

## Safety
- Production Firebase project configuration is unchanged.
- Production signing identity is unchanged.
- Room schema and backend contracts are unchanged.

## Build
- versionName: 2.0.1
- versionCode: 6
- targetSdk: 36

This release is intended to make review and first-use possible even when the reviewer cannot reach regional Firebase/Google authentication services.
