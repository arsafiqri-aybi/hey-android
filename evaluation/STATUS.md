# Service recovery — 0.2.1

Android hotfix built and tested; see repair-0.2.1.json and docs/SERVICE_RECOVERY_0.2.1.md. Same signing certificate as the supplied 0.2.0 candidate; no uninstall/reset performed. Gateway remains 0.2.0. Physical first heartbeat, pause/resume and startup tests remain RETEST_REQUIRED after installation. Existing history below is preserved.

# Repair status — 0.2.0

Source repairs implemented. Gateway regression tests: 28 PASS; Android verifier tests: 7 PASS; debug and unsigned release APK builds: PASS; lint: 0 errors, 4 warnings. See repair-0.2.0.json. Physical Vivo visual/media/audio/lifecycle retests remain required. Firebase/FCM configuration remains unavailable. Original 0.1.0 signing key was not recovered, so an in-place update is blocked; installed data was not removed. Gateway public and authenticated plugin checks verified version 0.2.0; existing device pairing/state remains. See hey-mcp/evaluation/deployment-0.2.0.json. Prior evaluation below is historical evidence.

# Android release truth — 0.1.0

Initial source, debug APK compilation, lint and signature inspection: PASS. Lint has 0 errors and 5 warnings: synchronous durable preference removal, required browser JavaScript, bitmap takeover accessibility and two text-resource warnings. See lint-results.txt, apk-build.json, signature-verification.txt and apk-manifest.xml. The supplied APK is a debug-signed test build, not an owner-signed production release.

Local API 31 emulator startup/UI check: BLOCKED_HOST_RUNTIME (emulator process exited). No startup, visual UI or virtual-display runtime pass is claimed. No physical Vivo test has run. Keystore lifecycle, background rendering, input, profile persistence, audio capture, complete audiovisual observation, wake and real ChatGPT connection retain independent gates. Firebase provisioning is pending.

The app uses no ADB, Termux, external Chrome controller, overlay permission or physical phone-screen capture for normal browser use. ADB was used only in the attempted local development smoke test. Android-approved audio-session consent remains explicit; install permission is not perpetual recording authorization. Backup/device-transfer rules exclude the app data domains.

