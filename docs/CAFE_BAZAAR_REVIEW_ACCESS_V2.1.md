# Cafe Bazaar Reviewer Access — Student OS v2.1

## Reviewer path
1. Install the release APK/AAB build.
2. On a fresh install, Student OS starts in local guest mode; Firebase/Google authentication is not required for the core offline experience.
3. Complete or skip the academic setup wizard.
4. Test courses, schedule, tasks, exams, attendance, grades and the course workspace.
5. Open Account to test optional Email/Firebase/Google authentication.
6. Authentication and cloud sync require network access; the core local experience does not.

## Authentication invariant
- Guest-first entry is enabled on fresh install.
- Production package ID and signing identity remain unchanged.
- Existing Firebase/Google SHA-1/SHA-256 fingerprints must not be rotated for this release.

## Review note
The dedicated backend is not required for the core reviewer path. Account/cloud features remain optional until the backend integration is enabled.
