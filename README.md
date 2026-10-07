# Hey by Ars — Android

An independent Android app with a Chromium-backed WebView profile and an app-owned virtual display. MainActivity, foreground service and transport read the same StateStore. The application contains Keystore-backed credentials, outbound HTTPS polling, task receipts, screenshots, DOM/media observation, scoped audio playback capture, tab control, live surface preview and manual takeover.

Build with JDK 17, Gradle 8.11.1, Android SDK 35 and AGP 8.9.2: `gradle :app:assembleDebug`. Firebase Messaging is pinned to 25.1.3. Minimum Android is 12 / API 31; target is 35. `app/src/main/assets/observe.js` is a browser observation script and has no native JavaScript bridge.

Open the one-time pairing link provided by the Hey plugin on the phone. After pairing, press Siapkan browser. Audio activation lives in Settings and invokes Android session consent. The current connection service polls while foreground-enabled. Firebase configuration is required for dormant push wake; force-stop and battery restrictions remain platform limits.

WebView is a candidate runtime, not certified universal Chrome parity. Google embedded OAuth restrictions, DRM/audio capture and target-site behavior require explicit acceptance testing. The runtime does not capture the physical phone display. Local emulator or compiler evidence cannot replace Vivo background/audio tests.

Companion contracts, sources and gates: https://github.com/arsafiqri-aybi/hey-mcp. No older app, repository, Worker or credentials are reused.
