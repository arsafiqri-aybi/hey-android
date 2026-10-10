#!/usr/bin/env bash
set -euo pipefail
# Run only on an isolated emulator. No credentials, pairing or production gateway.
if ! adb shell getprop ro.kernel.qemu | tr -d '\r' | rg -q '^1$'; then
  echo 'BLOCKED: isolated emulator required'; exit 1
fi
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p evaluation/native-output
run_variant() {
  local variant="$1"
  adb shell am force-stop id.ars.hey.preview
  adb shell am instrument -w -e variant "$variant" id.ars.hey.preview.test/id.ars.hey.NativeAcceptance | tee "evaluation/native-output/$variant-instrumentation.txt"
  adb exec-out run-as id.ars.hey.preview tar -cf - files/acceptance > "evaluation/native-output/$variant.tar"
  tar -xf "evaluation/native-output/$variant.tar" -C evaluation/native-output
  python3 - "$variant" <<'PY'
import json,sys
from pathlib import Path
p=Path('evaluation/native-output/files/acceptance')/(sys.argv[1]+'-results.json')
r=json.loads(p.read_text()); failures={k:v for k,v in r.items() if v['status']=='FAIL'}
assert not failures,failures
print('PASS native variant:',sys.argv[1],len(r),'checks')
PY
}
adb shell wm size 1080x2340
adb shell wm density 440
adb shell settings put system font_scale 1.0
run_variant regular
adb shell wm size 720x1600
adb shell wm density 320
run_variant compact
adb shell wm size 1200x2000
adb shell wm density 320
run_variant large
adb shell settings put system font_scale 1.6
adb shell settings put global animator_duration_scale 0
run_variant large-font-reduced-motion
adb shell settings put system font_scale 1.0
adb shell wm size 2340x1080
adb shell wm density 440
run_variant landscape
