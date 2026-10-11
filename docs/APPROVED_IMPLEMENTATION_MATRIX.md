# Approved Hey implementation — 2026-10-11

Source authority: all files in the owner's execution pack; archived under `docs/approved-20261011`.
Integration base: main `7a3d821` plus browser-engine candidate `82e3e4b`. The older Pearl branch has the same relevant branding source; its wallpaper/HTML Home is superseded. Gateway main `a702fcf` is read/tested, unchanged and not deployed.

| Requirement | Source component | Acceptance evidence |
|---|---|---|
| Official black/white Hey. silhouette | `drawable-nodpi/hey_wordmark.png`, adaptive foreground, launcher XML, styles | Exact raster extraction, mask preview, compiled APK resources |
| Central monochrome/cool graphite roles | `hey_colors.xml`, `HeyUi` | Token mapping, native renders, contrast calculations |
| Home brand, real status, spacious Browser Hey, compact activity | `HomeScreen`, `UiState` | Native unpaired screenshot; pure status tests; ready/working/paused presentation fixtures with live ticker temporarily suspended; pairing/live-service remains physical retest |
| Browser only URL/search field + actual remaining WebView | `MainActivity.browser`, retained `browserPage`/host | Native runtime fixture, URL policy tests, native scrolling |
| Tasks no header, active card, receipts/details/history | `TaskScreen`, service summaries + encrypted bounded history | Empty/fixture native screens, actual receipt data contract, no fake runtime history |
| Settings no header, exact six icons/order, inline details | `SettingsScreen` | Native screenshot/expanders, runtime status projection |
| Equal-size global shallow Arc; no permanent nav | `HeyShell`, `ArcNavigation`, `ArcGesture` | JVM direction/snap tests; native touch, timeout, geometry captures |
| Adaptive system insets/IME/font/motion | Activity insets, intrinsic layouts, `HeyUi`, shell/Arc | Compact/regular/large/font 1.6/landscape emulator variants |
| Human touch priority + fresh state on resume | `ControlledWebView`, `HeyService.control`, operation epoch invalidation | Native touch fixture; physical concurrency RETEST_REQUIRED |
| Cache-bypassing refresh preserves sessions | runtime cache mode scoped to reload with finish/error/15s restoration | Native cache-mode/cookie test; controlled HTTP/SW network caching RETEST_REQUIRED |
| Browser history/system Back | runtime `humanBack`, Activity back, content edge guard | Source + native history fixture; OEM gesture conflict RETEST_REQUIRED |
| Pairing/security/runtime retained | original Keystore/transport/service guards, no native JS bridge | Existing JVM and gateway tests; physical update/wake/audio RETEST_REQUIRED |

The pack contains Arc detail images and its HTML. It does not contain the full-screen Home/Tasks/Settings interactive previews referenced by the locks. Their documents are implemented as authoritative composition/copy/icon constraints; pixel-level matching to those unavailable full previews cannot be certified.

## Engineering choices

- Native renderer modules separate page composition from Activity lifecycle, browser runtime, semantic state, Arc gesture/geometry and tokens.
- Debug candidate ID `id.ars.hey.preview`, version `0.4.0-candidate`/5, min API 31, target 35. Release ID remains `id.ars.hey`.
- Preview deep-link host `hey://pair-preview` is intentionally isolated; it does not intercept installed Hey's `hey://pair` links. To pair a preview deliberately, change only the host in a freshly issued pairing link. Do not transfer credential storage between apps.
- No release signing key was used. Debug certificate identifies this candidate. Matching the installed release's ID alone would not establish signing/update compatibility.
- Arc uses restrained alpha graphite glass. No real background blur is applied because blurring the shared webpage/PixelCopy surface could corrupt observation evidence; this is the specified performance fallback, not a blur claim.
- Site hit testing declines Arc/refresh/history interception on nested scrollers, inputs, buttons, editable content and media. Unknown asynchronous probe results fail closed. The idle activation band is 28dp tall, with a 24dp buffer above full Android system-gesture insets. The active band is 104dp; system side edges and the reserved bottom area remain excluded. Native API 35 compact touch traces exposed quick-switch cancellation and drove this correction.
- Human input switches ownership to HUMAN and aborts in-flight epochs. Interrupted tasks are UNKNOWN/`HUMAN_CONTROL_ACTIVE`, never fabricated success. Explicit return to AGENT invalidates old references; ChatGPT must re-observe. Autonomous simultaneous control is not claimed.
- Cache policy is `LOAD_NO_CACHE` scoped to one reload, restored on finish/error/abort/timeout. Cookies, WebStorage and service-worker stores are not cleared. Exact desktop hard-refresh and service-worker cache bypass are not certified.
- Task history persists up to 16 encrypted metadata summaries, without web text/credentials/raw media. The UI renders up to three; previous lastTask remains readable. Gateway contracts are unchanged.

- Native URL long-press retains standard text selection/copy/paste. Accessible destination actions and Arc provide Browser navigation; native cards also offer the destination menu.
