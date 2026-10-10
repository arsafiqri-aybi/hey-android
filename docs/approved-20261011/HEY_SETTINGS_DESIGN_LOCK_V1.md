# Hey by Ars — Settings Design Lock v1.0

**Status:** APPROVED — locked by user, 2026-10-11 (Asia/Jakarta)
**Scope:** UI/UX design specification only. No code, APK, repo, deployment, or live device setting is modified by this document.

## 1. Definitive revision

The Settings page **MUST NOT** render the former top header row containing **“Hey.”** on the left and **“Pengaturan”** on the right. Remove the **entire row and the vertical spacing that was reserved for it**. The first visible content of the Settings page is the **Koneksi & Layanan** card. Do not substitute a different title/header.

**Everything else in the Settings preview approved by the user is locked as the visual and interaction reference, including its shown icons and their placement.** Future changes require explicit user approval.

## 2. Global visual language

- Keep the dark, quiet, premium, minimal visual language consistent with approved Home and Tasks previews.
- Base background `#0D1014`; graphite card surface `#242B33`; secondary group surface `#212830`; nested expanded detail surface `#161D24` or `#171E25` as shown.
- Rounded, calm graphite panels; softly contrasted inner icon holders; white primary labels; muted secondary descriptions; subtle dividers. Avoid excess controls, visible bottom navigation, or an extra settings toolbar.
- Keep the icon pictograms and their visual proportions from the preview; do not arbitrarily replace with another icon family or use emojis.
- UI must be **Adaptive By System**, respecting device density, insets, font scaling, touch target accessibility, reduced motion, power and performance constraints. Colors/geometric values document reference appearance, not license to hardcode screenshot coordinates.
- Global Shallow Arc Floating Glass Carousel remains hidden unless activated by lower-edge horizontal swipe; it uses equal-size cards, subtly lowered neighbors, softer-than-original curvature, edge clipping, and transient show/snap/hide. See approved Browser/Arc design lock for detailed behavior.

## 3. Page content and exact order

### A. Koneksi & Layanan (first visual card)

- Background graphite `#242B33`, generously rounded corners and balanced inset padding.
- First row: small icon well with `radio-tower` icon (pale white-blue); right of icon, bold `Koneksi & Layanan`, smaller colored contextual state, and a subtle `circle-dot` status marker at far right.
- Divider beneath heading.
- Three pairs of left labels/right current value:
  1. `ChatGPT Pairing` — `Terpasang` or `Belum terpasang`.
  2. `Layanan Hey` — `Berjalan`, `Dijeda`, or `Terhenti` / state from actual app.
  3. `Browser` — `Siap`, `Tidak aktif`, or `Belum siap` / state from actual browser runtime.
- Bottom contextual action (quiet outline): `Jeda Hey` while active, `Lanjutkan Hey` while paused, `Coba jalankan Hey` when interrupted, `Panduan pairing` when unpaired. Use existing state truth and confirmed action results; no invented success.
- Mock preview represented four illustrative states: Ready, Paused, Offline, and Unpaired. Do not render the demo state selector in the shipped production UI.
- Status values must come from truthful runtime and pairing sources; never conflate a paired account with an active service or ready browser.

### B. Browser & Privasi

- Section title: `Browser & Privasi`, small, clear, left-aligned.
- Single rounded group card `#212830`, containing three interactive rows separated by understated dividers.
- Row 1: globe icon (`globe`) on graphite rounded icon well; `Sesi Browser`; subtitle `Data browsing dan sesi login`; right `chevron-right` (or expanded `chevron-down`). Expansion includes browser profile/session and guarded data deletion guidance.
- Row 2: shield-check icon (`shield-check`); `Privasi & Izin`; subtitle `Akses Android dan pengamatan audio`; same disclosure affordance. Expansion mentions actual Android consent and privacy protection.
- Row 3: hand icon (`hand`); `Kendali AI & Manusia`; subtitle `Prioritas dan keamanan interaksi`; same disclosure affordance. Expansion describes human-priority collaborative control and safe orchestration, framed as planned until certified.
- Expanded content appears inline on a deeper dark surface, without overwhelming the overview.

### C. Aplikasi

- Section title `Aplikasi`.
- Rounded group card `#212830` with two rows.
- Row 1: sliders-horizontal icon (`sliders-horizontal`); `Tampilan & Gerakan`; subtitle `Adaptif mengikuti perangkat`; expandable details about adaptive motion/accessibility/performance.
- Row 2: info icon (`info`); `Tentang Hey`; subtitle `Versi aplikasi dan diagnostik`; expandable about/build/runtime diagnostic information.
- Both maintain icon holders, soft separators, right chevrons and legible typography shown in preview.

### D. Footer

- Quiet centered footer: `Hey by Ars` and smaller `Personal AI Browser Companion`.
- Keep muted colors and generous breathing space. No navbar permanently visible below it.

## 4. Interaction invariants

- Tapping a setting row toggles its inline explanation/detail. The preview showed one detail expanded at a time.
- Pairing guide is contextual; session/token rules are governed by actual product safety and pairing implementation.
- Starting/pausing the service, modifying permissions, deleting browser data, and other live actions must operate on real system state and present truthful result/confirmation.
- Clearing browser cookies/login data requires a clear, informed confirmation.
- Audio observation requires valid Android permission/consent; no fake enabled state.
- No permanent title/header **Hey. | Pengaturan** and no replacement heading.

## 5. Review and implementation boundaries

The user approved the full Settings preview *including specific icons* and requested the top header row's complete removal. This document is the new locked baseline. Maintain consistency with already locked Home, Browser and Tasks designs. **Implementation is not yet authorized.** When implementation is requested, validate on real Android devices for insets, responsive sizing, accessible gesture controls, visual fidelity, and service state accuracy before claiming completion.
