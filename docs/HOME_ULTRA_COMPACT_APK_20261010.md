# Hey Ultra Compact / Playwright-inspired Browser — APK Candidate

Date: 2026-10-10  
Source branch: `feat/hey-home-ultra-compact-apk-20261010`  
Build source commit: `d33ce389aeff65763a0e1becb60945c943d4646e`  
Android GitHub Actions run: https://github.com/arsafiqri-aybi/hey-android/actions/runs/38035604177

## Approved Home composition

- Keep mountain-and-lake wallpaper foregrounded. **Caveat:** currently bundled as a very small offline WebP optimized for proof-of-build, so detail/sharpness is below the approved full-size mockup; replace with a higher-quality app asset before labeling visual fidelity PASS.
- 48dp Sage Green tinted-glass header showing `Hey` only. Status is an elevated natural-white Pearl Glass pill.
- The Browser Hey title is aligned with its globe icon. The whole Browser card is the link; there is no separate action button, arrow, hero caption, or redundant descriptive block.
- Embedded Pearl Glass welcome panel cycles multilingual greetings including `Hey` with gentle fade, respecting reduced motion.
- Activity is a compact informational tile. Dock is a floating Sage Glass control surface with Pearl Glass active item.
- Home is a trusted, app-owned offline WebView rendering `file:///android_asset/home.html`; there is **no JavaScript interface exposed to website pages**, and external network loads are blocked. Navigation is intercepted by Android and routed into existing native Browser/Tasks/Settings.
- Connection pill reflects `StateStore`, not a pretend ONLINE status. Its tap reveals actual connection/runtime information and can start a paired runtime.

## Playwright foundation

This candidate branches from `feat/hey-browser-engine-vnext-p0-p1-20261010`, retaining `locator.js`, `actionability.js`, enhanced `observe.js`, and BrowserRuntime changes. This is a **Playwright-inspired control layer**, not Microsoft's Playwright/Chromium binary embedded in Android. The companion MCP changes reside on the separate `hey-mcp` feature branch and are **not deployed**.

## Evidence

- Workflow `Build Hey Android APK`: **SUCCESS** (unit test, `:app:assembleDebug`, `:app:lintDebug` and artifact upload all succeeded).
- APK artifact verified with the expected `AndroidManifest.xml`, `classes.dex`, `assets/home.html`, wallpaper, locator, actionability and observe scripts.
- Debug APK application ID: `id.ars.hey.preview`, display label: `Hey Preview`, so it can coexist with an existing `id.ars.hey` installation. Pairing data from existing Hey is intentionally not migrated.
- Downloaded APK SHA-256: `23280c2aacae38fb4fdcc61029282569c1a5266d673707073abe1adbfc188945`.

## Still NOT_RUN / acceptance blockers

- Native Android physical-screen screenshot and touch test on the user's device (density, status/navigation insets, color/blur, tap targets and RTL greetings).
- Full-resolution background fidelity is not met by the bundled low-resolution image.
- Real Browser Hey click → Human takeover → page scrolling → nested scrolling and actual Playwright-style agent locate/action validation.
- Restart/service recovery and Firebase wake tests on physical devices.
- End-to-end MCP test requires separately reviewed deployment of the companion MCP feature branch.

Do not claim native device/production Playwright behavior verified based only on the build, lint, static assets or mocked UI preview.
