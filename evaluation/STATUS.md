# Hey approved Graphite / Arc — 0.4.0-candidate, 2026-10-11

Candidate implementation is isolated to `feat/hey-approved-graphite-arc-20261011` and draft PR #1. The four current owner-approved design locks in `docs/approved-20261011` supersede older visual specifications. Source, APK build, DOM fixtures, lint, native API 31/35 screenshots, adaptive gesture/scroll/session checks and baseline comparison are produced by `.github/workflows/approved-candidate.yml`; inspect the exact run/source commit linked in PR #1 for final results. `docs/APPROVED_IMPLEMENTATION_MATRIX.md` and `docs/HEY_TOKEN_USAGE.md` map the approved requirements to source.

Debug candidate `id.ars.hey.preview` is separate from production `id.ars.hey`, with explicit `hey://pair-preview` links. No production signing key, merge, deployment, owner data reset or credential transfer was performed. Physical pairing/MCP tasks, OEM background/wake, audio consent, real concurrency, TalkBack and production update compatibility remain RETEST_REQUIRED. Synthetic visual states are confined to the separate test APK on an isolated unpaired emulator; they are not live connectivity evidence.

The historical evidence below describes earlier code/branches and must not be read as current candidate results.

# Hey Browser Engine vNext 0.3.0-dev — 2026-10-10 feature candidate

Changes are isolated to `feat/hey-browser-engine-vnext-p0-p1-20261010`. Local Chromium browser fixture PASS; new Java regression tests are source-added but Android/JVM build unavailable here. Real Vivo takeover, nested swipe, screenshot masking/compositor, actionability, lifecycle and media/audio remain **RETEST REQUIRED**, not a release certification. No APK installed, no device/pairing touched, no deployment. See `docs/BROWSER_ENGINE_VNEXT_P0_P1.md`.

# Service recovery — 0.2.1

Android hotfix built and tested; see repair-0.2.1.json and docs/SERVICE_RECOVERY_0.2.1.md. Same signing certificate as the supplied 0.2.0 candidate; no uninstall/reset performed. Gateway remains 0.2.0. Physical first heartbeat, pause/resume and startup tests remain RETEST_REQUIRED after installation. Existing history below is preserved.

# Repair status — 0.2.0

Source repairs implemented. Gateway regression tests: 28 PASS; Android verifier tests: 7 PASS; debug and unsigned release APK builds: PASS; lint: 0 errors, 4 warnings. See repair-0.2.0.json. Physical Vivo visual/media/audio/lifecycle retests remain required. Firebase/FCM configuration remains unavailable. Original 0.1.0 signing key was not recovered, so an in-place update is blocked; installed data was not removed. Gateway public and authenticated plugin checks verified version 0.2.0; existing device pairing/state remains. See hey-mcp/evaluation/deployment-0.2.0.json. Prior evaluation below is historical evidence.

# Android release truth — 0.1.0

Initial source, debug APK compilation, lint and signature inspection: PASS. Lint has 0 errors and 5 warnings: synchronous durable preference removal, required browser JavaScript, bitmap takeover accessibility and two text-resource warnings. See lint-results.txt, apk-build.json, signature-verification.txt and apk-manifest.xml. The supplied APK is a debug-signed test build, not an owner-signed production release.

Local API 31 emulator startup/UI check: BLOCKED_HOST_RUNTIME (emulator process exited). No startup, visual UI or virtual-display runtime pass is claimed. No physical Vivo test has run. Keystore lifecycle, background rendering, input, profile persistence, audio capture, complete audiovisual observation, wake and real ChatGPT connection retain independent gates. Firebase provisioning is pending.

The app uses no ADB, Termux, external Chrome controller, overlay permission or physical phone-screen capture for normal browser use. ADB was used only in the attempted local development smoke test. Android-approved audio-session consent remains explicit; install permission is not perpetual recording authorization. Backup/device-transfer rules exclude the app data domains.

