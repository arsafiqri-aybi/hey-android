# Hey Browser Engine vNext — P0/P1 candidate

Branch: `feat/hey-browser-engine-vnext-p0-p1-20261010` (Android and MCP).
This is source code in isolated feature branches, **not a deployed or device-certified release**.
No APK update, credential migration, Firebase change, production Worker deployment or browser action occurred.

## Implemented in this candidate

- Existing direct shared WebView ownership retained: AGENT/HUMAN handoff uses the same WebView, and HUMAN touches request that the outer ScrollView never intercept page gestures.
- Screenshot capture tries a composed surface first, detects a flat/blank candidate, and falls back to document canvas. Flat resulting images are flagged `SUSPECT_BLANK` and never called verified frames; password/OTP/card masking remains in place. Canvas cannot establish video compositor coverage.
- Scroll actions target the nearest scrollable container beneath viewport center, falling back to window scroll. Observations now include bounded nested scroll positions so the verifier can observe website scroll without relying only on window.scrollY.
- Generic click verification can recognize visible text/title changes, not merely navigation/focus. It establishes a browser-side effect only, **not** the completion of a purchase or other user goal.
- `hey_locate` is an ephemeral semantic lookup: role+accessible name, text, associated label, placeholder, test ID, and CSS. Multiple matches and missing results are explicit errors.
- Click awaits attachment, visibility, native scroll-into-view, viewport center hit target, unobscured element, enabled state and two stable geometry samples before a single touch is dispatched. Device receipt and old idempotency/journal remain mandatory.
- Fill checks editable text inputs/textarea and retains existing input/change event semantics. Sensitive values are never part of locator results.

## MCP usage (after a future approved deployment)

1. Invoke `hey_locate` with `deviceId`, a stable unique `actionId`, `by: "role"`, `query: "button"`, `name: "Lanjutkan"`.
2. Read the task with `hey_task` (cursor 0). Do **not** treat QUEUED as success.
3. From the result/observation, use the returned `locator.ref` and top-level `stateVersion` in `hey_action(click)`.
4. If the page has changed, locate afresh. Do not reuse stale refs. If control is HUMAN, agent commands are rejected.
5. Read the resulting task and evidence. A verified browser-side click effect is not evidence that an external business operation completed.

## Verification commands

- `python3 evaluation/browser_fixture_test.py` (requires `playwright` Python package and a local Chromium executable, optionally `HEY_CHROMIUM=...`)
- `node --check app/src/main/assets/observe.js`
- `node --check app/src/main/assets/locator.js`
- `node --check app/src/main/assets/actionability.js`
- `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (needs Android SDK 35/AGP toolchain)
- MCP side: `npm test && npm run check` with Node 22+

## Acceptance gates still open

1. Real Vivo physical takeover must prove swipes scroll website, not Hey outer UI, including nested scroll & keyboard.
2. Real WebView screenshot must be nonblank on normal pages and correctly redacted; separately prove compositor video frames.
3. Android Gradle compilation, JVM verifier tests, service recovery pause/resume, background/lock screen, audiovisual compatibility require an Android test environment.
4. End-to-end MCP `hey_locate` → `hey_action` requires installed app and deployed candidate; neither was altered during this implementation.
5. FCM wake and OEM force-stop remain platform/environment gates, not solved by locator changes.

All these are `RETEST REQUIRED`, never silently marked PASS.
