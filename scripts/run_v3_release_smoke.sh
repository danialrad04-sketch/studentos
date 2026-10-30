#!/usr/bin/env bash
set -euo pipefail
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher
python3 scripts/smoke_bazaar_release.py --previous-apk baseline-candidates/previous.apk --candidate-apk bazaar-candidates/StudentOS-3.0.0.apk --output build/signed-release-smoke
python3 scripts/smoke_v3_release.py --output build/signed-release-smoke/v3
python3 scripts/benchmark_v3.py --baseline-apk baseline-candidates/performance.apk --candidate-apk bazaar-candidates/StudentOS-3.0.0.apk --output build/signed-release-smoke/performance
