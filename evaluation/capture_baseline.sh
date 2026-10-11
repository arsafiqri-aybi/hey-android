#!/usr/bin/env bash
set -euo pipefail
# Read-only baseline source, installed ONLY in the isolated CI emulator.
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
git fetch origin 7a3d821c358fd1eb5d93e6b6591aed1af0a5cb61
hey_baseline_dir="$(mktemp -d /tmp/hey-baseline-XXXXXX)"
git worktree add --detach "$hey_baseline_dir" 7a3d821c358fd1eb5d93e6b6591aed1af0a5cb61
gradle -p "$hey_baseline_dir" --no-daemon :app:assembleDebug > /tmp/hey-baseline-build.log 2>&1 || { tail -40 /tmp/hey-baseline-build.log; exit 1; }
adb install "$hey_baseline_dir/app/build/outputs/apk/debug/app-debug.apk"
adb shell wm size 1080x2340
adb shell wm density 440
adb shell settings put system font_scale 1.0
adb shell settings put global animator_duration_scale 1.0
adb shell am start -n id.ars.hey/.MainActivity
sleep 3
mkdir -p evaluation/native-output/baseline
for hey_page in Home Browser Tasks Settings; do
  adb shell uiautomator dump /sdcard/hey-baseline.xml >/dev/null
  adb exec-out cat /sdcard/hey-baseline.xml > /tmp/hey-baseline.xml
  read -r hey_x hey_y < <(python3 - "$hey_page" <<'PY'
import re,sys,xml.etree.ElementTree as ET
n=next(n for n in ET.parse('/tmp/hey-baseline.xml').iter('node') if n.attrib.get('text')==sys.argv[1])
x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
print((x1+x2)//2,(y1+y2)//2)
PY
)
  adb shell input tap "$hey_x" "$hey_y"
  sleep 1
  adb exec-out screencap -p > "evaluation/native-output/baseline/$hey_page.png"
done
adb shell am force-stop id.ars.hey
adb shell am start -n id.ars.hey.preview/id.ars.hey.MainActivity
