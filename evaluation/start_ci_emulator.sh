#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
export ANDROID_AVD_HOME="${RUNNER_TEMP:-/tmp}/hey-acceptance-avds"
mkdir -p "$ANDROID_AVD_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
if ! command -v sdkmanager >/dev/null; then
  echo "Android command-line tools unavailable in $ANDROID_HOME"; exit 1
fi
mkdir -p evaluation/native-output
python3 -c "print('y\n' * 100)" | timeout 600 sdkmanager 'system-images;android-31;google_apis;x86_64' 'emulator' > evaluation/native-output/sdk-install.txt 2>&1 || { tail -15 evaluation/native-output/sdk-install.txt; exit 1; }
echo no | avdmanager create avd -n hey_acceptance -k 'system-images;android-31;google_apis;x86_64' --device pixel_5
test -f "$ANDROID_AVD_HOME/hey_acceptance.ini" || { avdmanager list avd; echo 'Expected isolated AVD metadata missing'; exit 1; }
"${ANDROID_HOME}/emulator/emulator" -avd hey_acceptance -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim -cores 2 -memory 2048 > evaluation/native-output/emulator-startup.txt 2>&1 &
hey_emulator_pid=$!
trap 'kill "$hey_emulator_pid" 2>/dev/null || true' EXIT
timeout 120 adb wait-for-device || { tail -60 evaluation/native-output/emulator-startup.txt; exit 1; }
hey_boot_deadline=$((SECONDS+600))
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != 1 ]; do
  if ! kill -0 "$hey_emulator_pid" 2>/dev/null || [ "$SECONDS" -gt "$hey_boot_deadline" ]; then tail -60 evaluation/native-output/emulator-startup.txt; exit 1; fi
  sleep 3
done
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent 82
bash evaluation/run_native_acceptance.sh
bash evaluation/capture_baseline.sh
