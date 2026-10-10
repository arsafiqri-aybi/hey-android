#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
if ! command -v sdkmanager >/dev/null; then
  echo "Android command-line tools unavailable in $ANDROID_HOME"; exit 1
fi
python3 -c "print('y\n' * 100)" | sdkmanager 'system-images;android-31;google_apis;x86_64' 'emulator' > /tmp/hey-sdk-install.log 2>&1 || { tail -15 /tmp/hey-sdk-install.log; exit 1; }
echo no | avdmanager create avd -n hey_acceptance -k 'system-images;android-31;google_apis;x86_64' --device pixel_5
"${ANDROID_HOME}/emulator/emulator" -avd hey_acceptance -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim > /tmp/hey-ci-emulator.log 2>&1 &
hey_emulator_pid=$!
trap 'kill "$hey_emulator_pid" 2>/dev/null || true' EXIT
adb wait-for-device
hey_boot_deadline=$((SECONDS+600))
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != 1 ]; do
  if [ "$SECONDS" -gt "$hey_boot_deadline" ]; then tail -60 /tmp/hey-ci-emulator.log; exit 1; fi
  sleep 3
done
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent 82
bash evaluation/run_native_acceptance.sh
bash evaluation/capture_baseline.sh
