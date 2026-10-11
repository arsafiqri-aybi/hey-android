# HEY BY ARS — CHATGPT WORK MASTER EXECUTION

**Assignment:** Implement the owner-approved Hey by Ars UI/UX across Android, integrate the approved Hey. logo and matching premium color system, protect the real personal AI browser/MCP functionality, validate thoroughly, and deliver a usable candidate APK plus evidence. This instruction **authorizes implementation work on a new feature branch**, not destructive changes or automatic production deployment.

## 0. Inputs and authority

The owner has approved the following design decisions and now requests implementation. Read **all** supplied files BEFORE modifying source. Expect an attached ZIP containing:

- `design-locks/HEY_HOME_DESIGN_LOCK_V1.md`
- `design-locks/HEY_BROWSER_ARC_NAV_DESIGN_LOCK_V1.md`
- `design-locks/HEY_TASKS_DESIGN_LOCK_V1.md`
- `design-locks/HEY_SETTINGS_DESIGN_LOCK_V1.md`
- `brand/hey-primary-4096.png` — **official approved visual logo**; black rounded-square field with a big bold white `Hey.` wordmark and period
- `brand/HEY_BRAND_PALETTE_AND_TOKENS_V1.md` — palette, role mapping, brand constraints
- `visual-reference/hey_shallow_arc_glass_carousel.html` — interactive visual/mechanics reference; the final arc is **SLIGHTLY FLATTER** than this prototype
- `visual-reference/hey_shallow_arc_visible_detail.png` and `visual-reference/hey_shallow_arc_task_visible_detail.png` — auxiliary visual references

Use the four approved screen locks as the **binding product specification**. Older repo UI specs, earlier screenshot designs, wallpaper/pearl experiments and legacy colors **must not override** these newer owner-approved decisions. The most recent global Arc navigation approval supersedes the old permanent bottom nav shown in an earlier Home preview; Home content, visual hierarchy and states remain locked. Latest Settings/Tasks header removals are binding. When the approved docs mention that implementation had not been authorized, that statement was true at creation time; **this new request explicitly authorizes implementation now**.

## 1. Working context and repo discovery

- Main Android repository: `https://github.com/arsafiqri-aybi/hey-android`
- Related gateway/MCP repository: `https://github.com/arsafiqri-aybi/hey-mcp`
- Hey is a standalone Android application providing an app-owned Chromium-backed WebView/browser, service/runtime, HTTPS gateway communication, pairing, task receipts, evidence observation, human takeover, and privacy boundaries. It is **not** a whole-phone controller.
- Inspect current HEAD, all relevant recent feature branches, Android manifest, resources, layout implementation, MainActivity, WebView attachment/lifecycle, StateStore, service, gateway interfaces, existing tests and CI before choosing the integration base. Do not assume an old commit/branch is the newest. Do not clobber independently developed browser-engine work or local secrets.
- Prior reference branch that may be informative (do not blindly use): `feat/hey-home-pearl-4k-browser-20261010`. An independent browser-engine vNext branch may contain gesture/takeover/actionability improvements; inspect and reconcile carefully, do not assume it is shipped or certified.
- Android minimum API is currently 31, target/API build configuration may evolve. Preserve application ID for real release integration, Keystore-backed pairing, WebView session, task journal, app-owned browser profile, and valid runtime status. A debug candidate may have a separate application ID; document update compatibility and signing clearly.
- Prefer small, testable, well-separated UI modules and centralized theme tokens rather than giant hardcoded view constructors. Align with project architecture and existing tests; do not rewrite the browser engine or gateway just to draw a new UI.

## 2. Brand integration and visual design system (mandatory)

### Logo

Treat `hey-primary-4096.png` as authoritative appearance. **Hey.** (including the period) must stay large, bold, clean and white on solid black. No extra globe/sparkles/gradient, no green primary logo, no invented monogram and no font swaps that materially change the mark. Derive/provide proper Android adaptive launcher foreground/background/monochrome resources, round-mask-compatible appearance and Android 12+ splash artwork. The source PNG has a composite rounded square and transparency at corners; do not blindly drop the whole square into foreground and create a double-rounded/clipped icon. Where necessary reconstruct a high-fidelity foreground silhouette, preserving the reference, and distinguish the marketing artwork from the Android masked version. Output appropriate resource files and visual validation of round/rounded-square masks and small sizes. Keep existing signing/application update compatibility.

### Colors

Implement the **monochrome + cool graphite** design roles exactly as detailed in `HEY_BRAND_PALETTE_AND_TOKENS_V1.md`. Core roles:

- Official logo: `#000000` / `#FFFFFF`
- App background: `#0D1014`
- Surface groups: `#212830`; primary card `#242B33`; Home card `#23292F`; Task card `#252C33`
- Icon wells: `#333D47` / `#354049`; nested details `#161D24`; dividers `#39434C`
- Primary text `#F4F6F7`; secondary text `#A7B5BF`; muted `#8998A3`
- Restrained state-only accents: healthy `#B4DEC5`, active `#C5DEE7`, paused/needs human `#E4CEA5`, error `#E4B8B0`

The UI must read visually as **black-white premium Hey** with understated graphite depth. Do **not** translate the status green into a green app theme; do not introduce bright saturated accents, glass everywhere, busy wallpapers, or gradients. Maintain approved cards, icon family, hierarchy, text labels, radius feel, surface composition and spacing. Use shared semantic tokens and provide a small token/usage documentation file.

### Adaptive By System

Use dp/sp, intrinsic/adaptive layout sizing, window cutout/keyboard/gesture/status/navigation insets, font scale, contrast, TalkBack focus order, ergonomic touch targets, different Android screen sizes, variable densities/orientations and reduced motion. Animations respect animator duration scale. Glass effects use real blur/transparency when performant, and a graceful translucency fallback otherwise. Never pin screenshot positions by fixed coordinates; preview is a **visual fidelity target**, not a single-resolution layout blueprint.

## 3. Global navigation — Shallow Arc Floating Glass Carousel

This is a **cross-page navigation subsystem**, not a permanent bottom bar. Implement on **Home, Browser, Tasks and Settings**. It must be hidden by default and reveal on an intentional HORIZONTAL gesture originating in the safe lower interaction zone. It overlays, not shrinks, page content. Four equal-size glass cards have icon-left/label-right: Home, Browser, Tasks, Settings. ALL CARDS ARE THE SAME SIZE, even the active center card. The center is visually clearer and positioned slightly HIGHER; neighbors are lower and more transparent, with permitted partial clipping at the left/right screen edges. Geometry follows an almost-horizontal, VERY SHALLOW symmetric arc, **noticeably flatter** than the supplied prior HTML rendering. Animate synchronized horizontal translation + subtle vertical arc interpolation and opacity. Snap to the nearest item on gesture release; commit navigation only then; stay visible briefly, then fade/slide down and disappear. A repeated swipe delays dismissal. Respect reduced motion and accessibility; offer an accessible navigation method without leaving a distracting permanent browser toolbar. Never intercept vertical webpage scroll, site carousel gesture, or Android back/system-edge navigation. Provide real multi-device/touch test evidence rather than solely a desktop mouse demo.

## 4. Home — approved design

Reproduce `HEY_HOME_DESIGN_LOCK_V1.md` accurately: deep-dark background, small white `Hey.` top-left, small truthful status pill at top-right; prominent `Browser Hey` card with globe and centered state-sensitive content, plus compact `Aktivitas terbaru` card beneath. Four actual contextual states include Unpaired (`Hubungkan Hey`), Ready (`Hello.`), Working (`Browser aktif`) and Paused (`Hey dijeda`), with appropriate context actions. Status must derive from distinct real pairing, service, browser and task state rather than optimistic fake UI. The old fixed bottom nav is superseded by the global transient Arc. No previous green nature wallpaper theme unless specifically approved later. Maintain breathing room, comfortable typography, subtly raised graphite surfaces and approved icon placements.

## 5. Browser — approved design AND behavior

- Only one PERMANENT top component: URL/search field. No `Browser Hey` top title, no `Siap` chip, no back, forward, refresh, tabs, settings menu or fixed bottom browser tools.
- Actual WebView/site takes the entire remaining available viewport; website owns scroll, tap, swipe, keyboard and pinch. Do not make a parent ScrollView consume website interactions. Address field must support actual navigation and search, proper URL/security behavior and IME.
- Pull down deliberately near the top of content to request a **best-effort cache-bypassing reload**, approaching desktop Ctrl+Shift+R without clearing cookies/logins. WebView is not guaranteed exact Chrome parity (service workers, response caches): document known limits, verify with controlled caching fixtures, restore previous cache policy after operation.
- Swipe from left edge within web content = browser history Back when available, designed around Android gesture-navigation and embedded horizontal controls. No history => predictable safe behavior; do not accidentally leave Hey or hijack a site carousel.
- AI + Human share the **same browser session/context**, with human input priority and serialized, conflict-safe actions. If physical user interaction interrupts AI work, safely hold/cancel conflicting commands, re-observe page, verify version/action state, then coordinate next step. Maintain sensitive input protections, pairing and existing browser authentication. Do not claim fully simultaneous autonomous control if engineering evidence cannot verify it; provide the safest honest degraded behavior and a clearly noted follow-up gate. Only change MCP contracts if strictly necessary and compatible; document both sides and avoid unauthorized live gateway deployment.
- Arc is transient and page-safe, with accessible fallback.

## 6. Tasks — approved design

Reproduce `HEY_TASKS_DESIGN_LOCK_V1.md`. Remove the ENTIRE prior top header row including both `Tasks` left title and global right status chip AND any leftover top spacer. The most relevant active task card starts the visual hierarchy. Include real contextual statuses for running, waiting for human, completed, interrupted and empty. `Lihat detail tugas` expands actual step/receipt/evidence information only if available, with separate notions of action performed vs verified outcome. `Aktivitas terakhir` is concise, no fabricated history, no misleading percentages or dashboards. Keep the approved icons, graphite surfaces, status accents, and Arc gesture. No `Buat Tugas` primary action because Hey tasks originate through ChatGPT.

## 7. Settings — approved design

Reproduce `HEY_SETTINGS_DESIGN_LOCK_V1.md` with **identical icon choices and order**. NO top `Hey.` text and NO `Pengaturan` heading; remove the whole row and vertical gap. First visual component = rounded `Koneksi & Layanan` card with `radio-tower` icon well, contextual status dot and truthful separation of `ChatGPT Pairing`, `Layanan Hey`, and `Browser`. One appropriate real action: `Jeda Hey`, `Lanjutkan Hey`, `Coba jalankan Hey`, or `Panduan pairing`, based on the actual state.

Next `Browser & Privasi` grouped rows with exact icon → label → subtitle structure:
1. `globe` · `Sesi Browser` · `Data browsing dan sesi login`
2. `shield-check` · `Privasi & Izin` · `Akses Android dan pengamatan audio`
3. `hand` · `Kendali AI & Manusia` · `Prioritas dan keamanan interaksi`

Then `Aplikasi` rows:
1. `sliders-horizontal` · `Tampilan & Gerakan` · `Adaptif mengikuti perangkat`
2. `info` · `Tentang Hey` · `Versi aplikasi dan diagnostik`

Grouped graphite cards, inset icon holders, subtle separators/chevrons; tap to expand contextual details inline. Preserve footer `Hey by Ars` / `Personal AI Browser Companion`. Never produce fake permission state or silently clear browser session/cookies. Permissions and audio consent remain Android-mediated. The entire page inherits transient Arc navigation.

## 8. Engineering invariants — preserve functionality

- Keep the existing Hey Phone/MCP interface and actual task receipts. Queued/pending is not completed/verified.
- Preserve encrypted credentials, registered pairing, browser profile and cookie persistence, safe DOM/media observation, task state, background service/pause-resume recovery, lifecycle guards and privacy protections.
- Preserve web browser WebView ownership across UI navigation; do not destroy/recreate active browsing state every time the Home/Tasks/Settings view renders. Avoid global callbacks that mark actions complete prematurely.
- Observe Android OEM/force-stop/background and FCM/wake limitations, and state these candidly. Never claim to bypass Android restrictions.
- Avoid broad unreviewed protocol/API changes, credential migration, unapproved deployment, disabling security or lowering target behavior merely to make visuals easy.

## 9. Implementation and verification plan — actually execute

1. Audit source, current feature branches, test evidence, current logo/icon resources and task/servicing architecture. Create a short implementation matrix mapping every approved design requirement to an actual source component and acceptance test.
2. Create a clean **feature branch** based on a sensible, current tested integration base; keep commits reviewable. Preserve valuable new code from other branches after review, instead of overwriting it.
3. Implement centralized visual tokens and accurate Android logo/adaptive icon/splash integration, then shared shell/Arc, Home, Browser, Tasks and Settings. UI components should be maintainable and use the existing Android architecture appropriately.
4. Implement/repair end-to-end UI behaviors: truthful service/pairing status, contextual actions, safe browser pull refresh and back gesture, page/WebView continuity, human-priority action coordination to the extent verifiable, expandable Settings/Tasks details and safe consent handling.
5. Run all existing repository tests and any necessary new automated tests: Java/JVM tests, lint, build/compile, browser fixture/Chromium tests, UI snapshots, gesture state-machine and accessibility tests. Use actual configured Gradle tasks and toolchains after inspecting the repo rather than blindly assuming command names.
6. Where possible, run an Android emulator, capture real screenshots for all four pages and states at compact/regular/large widths, font-scale enlargement, light/dark system constraints, accessibility/reduced motion, API compatibility and landscape. Compare with approved visual references (especially corrected missing headers and flatter Arc). **Do not certify real hardware behavior based on an emulator or HTML preview.**
7. Test risky journeys: pairing retained across update, service pause/resume, offline/service stopped, browser session/login surviving app navigation, real URL entry, long web scrolling, nested site carousels, pull-to-refresh cache behavior, Android Back/edge gesture, keyboard, Arc while browser scrolling, task evidence, Android audio consent, and no accidental takeover conflicts.
8. Generate a candidate debug/preview APK (and signed release only if owner provides authorized signing flow); report exact artifact, application ID/version, SHA-256, install/update compatibility and whether it was ever installed on physical device. Export test evidence, screenshots and a concise pass/fail/blocked matrix with remaining risks. Include a PR/commit reference and build artifact links that actually exist.
9. Do **not** merge to `main`, publish a production release, deploy gateway/Worker, rotate secrets, wipe data, uninstall the user's app or claim physical-device success without explicit authorization and evidence. Prepare a PR and candidate APK. Only interrupt for required user permissions, secrets, destructive/incompatible actions, or truly unresolved product decisions; otherwise execute the authorized scope autonomously through completion.

## 10. Acceptance / Definition of done

**A. Visual:** Four screens visually consistent with the approved mock, correct monochrome brand identity/launcher icon, graphite tokens and semantic accent usage, correct exact icon choices in Settings, no removed headers reintroduced, global Arc equal-sized flatter curve and transient behavior. Provide before/after screenshots and review checklist.

**B. Functional:** Browser URL WebView and browsing gestures work, no website scroll hijack; service/pairing/browser status real; tasks read actual records; expanders and contextual actions work; browser session persists during navigation; human safety controls hold.

**C. Android:** Build and tests pass where environment supports; API 31+ compatibility verified as far as possible; accessibility and reduced-motion coverage; source and output artifacts reproducible; no credentials leaked or previously working security paths disabled.

**D. Evidence:** For every item say `PASS`, `FAIL`, or `BLOCKED/RETEST_REQUIRED`. Separate source/test proof from emulator proof and from real-device proof. Include known engineering compromises, external-service limitations, and clear next acceptance gate.

## 11. Output format and final handoff

Deliver:
- A short design/architecture audit and source-to-design implementation map.
- A feature-branch PR and tested commit SHA (or a clear explanation of repository access blocker).
- A ready-to-install candidate APK with valid artifact path/link, SHA-256 and compatibility notes (or concrete build blocker).
- Icon/splash assets and a token sheet; screenshots/short recordings of all approved pages and transient Arc in multiple states.
- Test matrix of completed tests and blockers, what actually works, and remaining physical-phone validation steps.
- A concise Indonesian release note addressed to the owner.

**Priority:** Faithfulness to every owner-approved visual detail AND genuine app functionality. Do not stop at a beautiful mockup; implement a maintainable Android experience. Do not misrepresent incomplete code, pending tasks, emulators, or unavailable hardware tests as finished.
