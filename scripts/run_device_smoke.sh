#!/usr/bin/env bash
# Preserve the real Gradle result while collecting the device's rendered evidence on either outcome.
set +e
# The headless Google image's unrelated launcher can ANR and steal the app's focus.
# Explicit Activity launches and all app/system-window assertions remain enabled.
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher
./gradlew connectedDebugAndroidTest --stacktrace
studentos_test_result=$?
mkdir -p app/build/outputs/androidTest-results/device-diagnostics
adb pull /sdcard/Download/studentos-navigation-diagnostics/. app/build/outputs/androidTest-results/device-diagnostics/
exit "$studentos_test_result"
