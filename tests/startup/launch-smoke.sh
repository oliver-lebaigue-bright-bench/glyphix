#!/usr/bin/env bash
# Run only on a disposable CI emulator, never a personal device.
set -euo pipefail
apk=${1:?Pass the debug APK path}
out=build/startup-smoke
mkdir -p "$out"
package=com.glyphix.app
adb wait-for-device
adb install -r "$apk"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
capture() {
  adb logcat -d -v threadtime > "$out/$scenario-logcat.txt"
  adb shell dumpsys activity activities > "$out/$scenario-activity.txt"
  adb exec-out screencap -p > "$out/$scenario.png"
}
launch() {
  scenario=$1
  adb shell am force-stop "$package"
  adb logcat -c
  adb shell am start -W -n "$package/.ui.MainActivity" > "$out/$scenario-start.txt"
  # Assert repeated liveness, rather than merely trusting am start's success status.
  for tick in $(seq 1 15); do
    sleep 1
    if ! adb shell pidof "$package" > /dev/null; then
      capture
      cat "$out/$scenario-logcat.txt"
      echo "App exited during $scenario" >&2
      exit 1
    fi
  done
  capture
  if ! grep -E 'mResumedActivity|topResumedActivity' "$out/$scenario-activity.txt" | grep -q "$package/.ui.MainActivity"; then
    cat "$out/$scenario-activity.txt"
    echo "MainActivity is not resumed during $scenario" >&2
    exit 1
  fi
  echo "PASS: $scenario"
}
# Fresh install without microphone, camera, notification access, or Glyph hardware.
launch fresh-install
# Existing users retain preferences on upgrade. Removed enums must fall back safely.
adb shell am force-stop "$package"
printf '%s' '<?xml version="1.0" encoding="utf-8"?><map><string name="fft_read_method">REMOVED_BETA_VALUE</string><string name="haptic_mode">REMOVED_BETA_VALUE</string><string name="flashlight_mode">REMOVED_BETA_VALUE</string></map>' | adb shell run-as "$package" sh -c "'mkdir -p shared_prefs; cat > shared_prefs/viz_prefs.xml'"
# A failed backend or offline phone must not prevent opening the local app.
adb shell svc wifi disable
adb shell svc data disable
launch upgrade-offline
adb shell svc wifi enable
adb shell svc data enable
launch warm-relaunch
