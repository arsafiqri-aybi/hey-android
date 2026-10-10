# Hey by Ars — Home Design Lock v1.0

**Status:** APPROVED — design reference, NOT IMPLEMENTED
**Approved in:** Hey by Ars project conversation, 2026-10-11
**Source of truth:** the assistant's last interactive dark Home simulation approved explicitly by the owner. Rendered composition, visual hierarchy, text and conditional scenarios should be reproduced as closely as possible, while remaining responsive and accessible.

## Product purpose
Hey is a personal AI browser companion. ChatGPT issues instructions; the Android Hey app provides a separate personal browser and a surface for human monitoring and takeover. The user's phone remains usable separately, subject to Android runtime restrictions.

## Locked Home structure and hierarchy
1. Dark, calm, minimal app canvas, reference mock base `#0D1014`.
2. Header: left **Hey.** brand text in white; right small, rounded state pill with a colored indicator and current state. State comes from actual verified app state, not assumptions.
3. Main elevated card: `Browser Hey` identity at upper left with globe icon; generously spaced primary area in the center; central message responds to system state. Mock reference card `#23292F`. Preserve the visual primacy of this card.
4. Within the main card, a context-specific footer/action only when relevant. It must be tied to actual working capability, not an inert button.
5. Below main card: smaller activity card, icon and title **Aktivitas terbaru**, concise truthful summary; no huge list of logs on Home.
6. Bottom navigation: four destinations **Home, Browser, Tasks, Settings** with icons and short labels; selected state visually distinct, but not excessively decorative.
7. Surface shape: softly rounded cards with clean iconography and soft hierarchy. Retain all details/feel from the interactive approved mock; no new ornamental features by default.

## Locked Home textual state samples
| State | Header status | Main title | Description | Context action | Activity summary |
|---|---|---|---|---|---|
| Unpaired | Belum terhubung | Hubungkan Hey | Sambungkan perangkat untuk mulai menggunakan Hey. | Mulai pairing | Belum ada aktivitas |
| Ready/idle | Browser siap | Hello. | Browser Hey siap menerima pekerjaan. | No action | Tidak ada tugas aktif |
| Working | AI bekerja | Browser aktif | AI sedang menjelajahi halaman melalui Browser Hey. | Ambil alih browser | Tugas sedang berlangsung |
| Paused | Dijeda | Hey dijeda | Aktivitas AI telah dihentikan sementara. | Lanjutkan Hey | Menunggu dilanjutkan |

Texts above are approved example copy, not permission to misreport readiness or completion. Derived status must reflect distinct pairing/service/browser/task/control states in the real implementation; show recovery steps where necessary. Exact label adjustments require a reason and user review if they change the approved experience materially.

## Locked design principles
- Essential-first: keep Home focused on readiness, browser, and relevant activity. Detailed control and history belong within Browser/Tasks/Settings.
- **Adaptive By System:** no screenshot-bound px positioning. Implement responsive constraints, semantic spacing and typography tokens, Android window insets, font scaling, orientation/available width, ergonomics and touch target minimums.
- Visual style is the approved dark mock; glass experiments made previously do NOT override this newer explicit Home approval. Material/detail changes require review.
- Dynamic content responds to real system state; avoid claiming success when commands are merely queued.
- Motion should be subtle and meaningful; honor reduced-motion and accessibility settings. Avoid layout jumps on status updates.
- Browser web content must own its own scrolling; app shell scroll must not intercept it.

## Scope and implementation status
- This file records a **design decision**, not code implementation, a changed repo, or an APK build.
- Browser page design is **NOT LOCKED** yet; continue ideation without overwriting Home decision.
- When implementing, verify against the approved Home mock on compact/regular/large/landscape viewports and Android physical-device tests. Do not claim pixel-identical output across devices; preserve visual identity and adaptive behavior.

