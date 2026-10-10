# Hey by Ars — Complete Browser Experience & Shallow Arc Navigation — Design Lock v1.1

Status: **DESIGN APPROVED** — 11 October 2026. Scope: **SELURUH PENGALAMAN HALAMAN BROWSER HEY + GLOBAL SHALLOW ARC NAVIGATION**, bukan hanya komponen navigasi. Ini spesifikasi desain dan perilaku; **bukan** implementasi APK atau commit repository.

**Penegasan dari pengguna:** Semua konsep Browser yang disepakati sebelumnya telah dikunci bersama dengan Shallow Arc. Perubahan Tasks setelahnya tidak mengubah keputusan Browser ini.

## Baseline and approved correction

Approved visual baseline: the **Shallow Arc Floating Glass Carousel** preview from the design discussion. Final correction approved by the user: **arc must be slightly flatter than that preview**. Keep the visual identity and behavior of the preview otherwise.

## Floating bottom navigation — fixed product rules

1. Hidden by default; does not continuously consume browser viewport space.
2. Revealed by a deliberate horizontal swipe originating in a safe lower-edge navigation zone, not by ordinary vertical page scrolling.
3. Four destinations: Home, Browser, Tasks, Settings.
4. Every navigation card has **identical dimensions**, in every selected/non-selected state. Do **not** enlarge the centered item or widen its label container.
5. Every card contains an icon at left and destination label at right when legible. Center card is the selected/previewed destination.
6. Side cards use more translucency/less prominence, remain visibly glass-like, and are allowed to be clipped at viewport edges.
7. Items follow a *very shallow* symmetric arc: center slightly elevated, neighboring cards slightly lower. **Flatter than the previously approved rendered prototype**, close to horizontal; no steep circular wheel, no exaggerated vertical arc.
8. Arc position continuously interpolates while dragging; snap to nearest card on release. Navigate only on committed release so multiple page switches do not occur mid-swipe.
9. Briefly remain visible after selection, then smooth fade/slide downward and become invisible. Interrupted gestures or renewed interaction restart/delay auto-hide. Exact timing and easing subject to empirical UX testing; respect system animation scale and reduced motion.
10. Maintain coherent state when switching pages, and no overlay capturing website swipes outside the bottom interaction zone.
11. Fit safe areas, system gesture insets, landscape/screen dimensions, Android accessibility font scaling and touch target requirements; **Adaptive By System**, no brittle device-specific absolute positioning.
12. Provide a discoverable and accessible alternative for users who cannot perform a swipe; without permanent visible navigation clutter. Android system back must remain usable, and browser back-edge swipe must not conflict with the bottom navigation zone.

### Implementation acceptance checklist

- Equal card size verified in Home, Browser, Tasks, Settings, including animation frames.
- Center item merely higher, not larger; neighbors have progressively lower Y positions and greater translucency.
- Cards on either edge can clip gracefully; labels must not jitter or stretch.
- No navigation UI when idle; starts visible on deliberate horizontal swipe at lower zone; hides elegantly after a brief pause.
- Interactions with horizontal site carousels, vertical scrolling, keyboard, TalkBack, gesture-navigation insets, and rapid consecutive page switches retested on physical Android device.
- Flatter final arc compared with prior HTML preview verified visually by user.

## Browser Hey — complete design lock (not only Arc)

- Browser viewport is predominant; **only URL/search field** is permanently visible at top (no Browser Hey title, no 'ready' label, no back/forward/reload/tab/menu toolbar).
- Pull down at top of web content = cache-bypassing best-effort hard refresh (closer to Ctrl+Shift+R) with session/login preservation; **do not promise exact desktop equivalence** in WebView due to service workers and HTTP caching.
- Swipe from left edge of web content = back in web history when supported; honor Android back and side gesture conflicts.
- Browser page owns normal scroll/swipe/pinch; parent layouts may not steal web gestures.
- Human and AI are intended to share one session context, with human input precedence, explicit observable action coordination, and prevention of concurrent conflicting actions. **Proposed feature, not yet verified existing app capability.**
- No additional fixed lower browser controls; floating navigation overlays only during an intentional navigation gesture.

### Browser layout and visual rules

1. **Only the URL/search field is persistently displayed at the top**. It serves both navigation and search; use the system keyboard and appropriate focus/IME behavior.
2. Below it, **the website is the primary, expansive viewport**: no duplicate app header, no permanent AI status pill, no browser toolbar, no control strip underneath. Preserve the existing Hey premium dark/minimal styling in application chrome; web content retains its own styling.
3. Navigation to Home / Browser / Tasks / Settings comes solely via the global, transient Shallow Arc gesture control (with accessible fallback). Website interaction remains the main gesture owner outside its activation zone.
4. A deliberate pull-down from the top of scrollable web content triggers a **best-effort hard refresh**, using the safest WebView-supported cache bypass strategy. It must not clear login data/cookies. Do not claim equivalence with every aspect of desktop Ctrl+Shift+R. Refresh indicators, if any, are temporary and contextual, not a toolbar.
5. Left-edge horizontal swipe aims to go **back in the browser's own page history**. Coordinate with Android system Back navigation, edge insets, horizontal webpage controls, and situations with no history; avoid a gesture that blocks ordinary site scrolling or Android Back.
6. When a user operates the browser, their input takes precedence. AI may observe permitted current-page context and act only under explicit action-ownership coordination. Do not send competing clicks, typing, scrolls or navigations to the same surface concurrently. If human input interrupts an AI action, defer/cancel safely, then re-observe and re-verify before resuming. No claim that this cooperative mode already works in deployed Hey.
7. Respect sensitive input, user approvals, session privacy, permission limitations, playback/webview restrictions, accessibility, and actual Android background/lifecycle behavior. Never fabricate AI-ready/success states.

### Browser acceptance gates before real APK sign-off

- Only URL/search field remains visible in browser chrome in normal use, with website occupying remaining safe viewport.
- Real URLs and searches work; keyboard focus, address changes, fullscreen media and website scroll behave correctly.
- Refresh on deliberate top pull bypasses caches where supported, does not clear credentials or log users out, and clearly documents service-worker limitations.
- Browser Back behaves correctly with history and Android Back gesture/navigation without accidental system or cross-page switches.
- Human input is prioritized; simultaneous AI actions must not conflict, and any takeover/resume relies on fresh observable state.
- Arc remains hidden until lower horizontal gesture, does not steal website interactions, and is flatter than the approved prototype.
- Run real-device interaction/a11y/gesture QA; visuals alone do not certify runtime behavior.


## Relationship to Home and Tasks

- Home design is separately locked from prior approved mockup. Do not silently redesign it.
- Same floating bottom navigation system is used across all four destinations, with page-specific handling to avoid hijacking browser gestures.
- Tasks screen still in design phase; its concept and details require review/approval before any separate task-page lock.

## Change control

No Android source, APK, MCP, or deployment changed by creating this document. These design decisions are binding for subsequent design work until explicitly changed by the user. Before implementation, map conceptual interactions to actual runtime capabilities and validate against real-device QA.
