#!/usr/bin/env bash
set -euo pipefail
./gradlew -PnovaExportTests :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
install_fixture() {
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell pm grant com.example.chatgptnova.debug android.permission.CAMERA
  adb shell pm grant com.example.chatgptnova.debug android.permission.RECORD_AUDIO
  adb shell settings put system screen_off_timeout 1800000
  adb shell input keyevent 224
  adb shell wm dismiss-keyguard
}
install_fixture
export_test_status=0
./gradlew -PnovaExportTests :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.chatgptnova.ConversationExportTest --no-daemon || export_test_status=$?
cp -R app/build/reports/androidTests export-test-reports
if [ "$export_test_status" -eq 0 ]; then
  install_fixture
  ./gradlew -PnovaExportTests :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.chatgptnova.WebShareTest,com.example.chatgptnova.NovaWebViewTest,com.example.chatgptnova.ClipboardUiTest,com.example.chatgptnova.ClipboardProbeTest --no-daemon || export_test_status=$?
fi
# UTP uninstalls the app; retain user-visible saved files and synthetic diagnostics.
adb pull /sdcard/Download android-saved-files || true
adb logcat -d -v threadtime > android-export-logcat.txt || true
exit "$export_test_status"
