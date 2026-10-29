#!/usr/bin/env bash
# Preserve the real Gradle result while collecting the device's rendered evidence on either outcome.
set +e
./gradlew connectedDebugAndroidTest --stacktrace
studentos_test_result=$?
mkdir -p app/build/outputs/androidTest-results/device-diagnostics
adb pull /sdcard/Android/data/com.aistudio.studentos.appvzk/files/navigation-diagnostics/. app/build/outputs/androidTest-results/device-diagnostics/
exit "$studentos_test_result"
