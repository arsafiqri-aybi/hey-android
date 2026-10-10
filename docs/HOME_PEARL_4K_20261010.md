# Home Pearl and browser adapter preview

The Home redesign follows the owner's latest corrections: a roomier header, a smaller Hey wordmark, a much larger portrait Hello preview at width:height 4:5, and a clickable Browser Hey card. The header is at least 68 CSS pixels high; the wordmark is 25 pixels on regular phone widths. Compact screens keep the preview ratio while reducing card width. Safe areas come from native Android insets. The activity card and fixed dock remain separate.

Sage glass, a pearl preview, restrained shadows and press feedback retain the planned visual direction. Greeting transitions stop when Home is inactive, the Activity pauses, or reduced motion is requested. Low-memory devices use opaque glass without backdrop blur. Unregistered devices display a neutral status dot; green is reserved for ONLINE.

## Wallpaper provenance and delivery

The bundled photograph comes from https://images.unsplash.com/photo-1501785888041-af3ef285b470, downloaded at its native 5979 × 3986 resolution. A portrait crop produces a genuine 2160 × 3840 master without upscaling. The crop center is (0.58, 0.5). Technical delivery derivatives are 720 × 1280, 1080 × 1920, and 1440 × 2560 WebP. See `app/src/main/assets/wallpaper-manifest.json` for byte sizes and decoded ARGB estimates.

Image use follows the Unsplash license, https://unsplash.com/license, checked on 2026-10-10. The source URL is retained for attribution and traceability. The generated design exploration was not used as a falsely labelled 4K source.

`WallpaperPolicy` selects one asset from display pixel dimensions and Android memory class. Low-RAM devices and memory classes below 192 MB cap at 1080p; classes below 384 MB cap at 1440p. Only the selected asset is opened through the app-owned offline Home WebView. A large image is never decoded and then downscaled on a low-memory device.

## Browser improvements

These are Playwright-inspired Android WebView adapter improvements, not an embedded Node Playwright engine or complete browser parity. Shared semantic helpers scan at most 6000 nodes, include open shadow roots, resolve a limited set of roles and accessible names, and redact sensitive control labels. Ambiguous or incomplete locator scans fail explicitly. Closed roots and cross-origin frame contents are not inspected.

Clicks require an attached, visible, enabled, unobscured element and stable geometry. Fill waits for editability, uses the native value setter and one input/change event pair, and checks the value after the action without returning its contents. Nested scroll chooses the nearest scrollable ancestor and falls back at an edge; verification uses stable scroll-region identities. Checkbox verification is bound to the selected element. Human takeover owns browser touch gestures. Commands have one completion callback and a bounded timeout; renderer loss invalidates operations and removes the dead view. Unknown actions are never automatically replayed.

## Verification and installation

`evaluation/run-browser-fixtures.sh` runs real Chromium fixtures using pinned Playwright 1.58.2. Fixtures cover semantic ambiguity, input state, nested scrolling, shadow roots, sensitive labels, clipping, scan limits, motion, connection states and Home renders at 320×640, 360×740, 393×852, 412×915, 480×960 and 852×393. The existing Android CI runs these before debug unit tests/asset merge, then assembles the APK and runs lint. QA screenshots and the JSON report are bundled only in the debug APK for inspection.

The preview package is `id.ars.hey.preview.pearl`, labelled Hey Pearl Preview. It installs separately from older previews and production apps, avoiding replacement by an unrelated CI debug signing key. Pairing and local app state remain separate. Release identity and main branches are unchanged.

Build, lint, desktop Chromium and pure-Java tests have separate evidence gates. Physical-device behavior, Android WebView rendering, background/wake and audio remain NOT_RUN until tested on an actual device. Source changes do not deploy the MCP Worker.

## Supporting source inventory

All 22 available repositories were inventoried; relevant modules were read rather than claiming a complete audit of every file. Design/performance decisions use `portofolio-web` (3d08dc43b47a), motion ownership/lifecycle use `motion` (f14e2fd22fe6), and browser verification/recovery use `personal-browser-operator` (3fe1bde2cabe), `phone-browser-bridge` (58e0e125652d), and `visual-computer-use` (0206d1925fc7). Architecture and scope references include `scale` (02f2593fd1da), `governor` (b10ab98f6513), and `website-builder` (5e581cb36dc4).
