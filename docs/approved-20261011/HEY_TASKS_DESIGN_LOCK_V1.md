# Hey by Ars — Tasks Page Design Lock v1.0

**Status: APPROVED / DESIGN LOCKED**  
**Approval date: 11 October 2026**  
**Scope: Visual composition, information architecture, behavior, and interaction of the Tasks page.**  
**Implementation status: Design specification only. No source-code edit, APK build, repository commit, or deployment is represented by this document.**

## 1. Product intent

Tasks is Hey's lightweight AI activity center: users should immediately understand what Hey is doing, whether help is needed, what finished, and whether there is evidence. It is not a project-management dashboard, task-creation interface, or replacement for ChatGPT. Hey tasks originate from ChatGPT and Hey's existing MCP task system.

## 2. Approved visual composition (based on interactive preview)

- Premium near-black page background (`#0D1014` as visual reference), graphite elevated card surfaces (e.g. `#252C33`, `#212830` as visual references), white/off-white typography, muted secondary copy, soft rounded corners, carefully controlled spacing, and subtle icons.
- No top-left **“Tasks”** heading.
- No top-right global status pill/indicator, irrespective of task state.
- **Remove the entire header row**, including its otherwise-empty vertical space. Page content begins under appropriate safe-area inset.
- The **single most relevant active task** occupies the dominant card at the top when present. The card may carry its own concise status and icon because the user removed only the *global header status*.
- Below it, keep a compact **Aktivitas terakhir** section, with a restrained list of recent actual activities where available.
- Each task's **Lihat detail tugas** affordance reveals contextual steps, outcome, available receipts and supporting evidence when actual data supports them. Closed by default; it must not bloat the main screen.
- No clutter: no summary counters, forced progress percent, wide dashboard chart, generic create-task button, or duplicated global navigation controls.

## 3. System-driven states

Each state reuses the same visual language and changes only information relevant to the real task:

- **Running**: one dominant task card containing actual task name/purpose, real status, meaningful current step only if observable, and optional detail.
- **Awaiting help**: explanation of required human action, with the minimal relevant action to open Browser/complete manual interaction. Avoid implying AI can complete human-sensitive steps automatically.
- **Done**: concise outcome with distinct status for finished execution and independently verified result where applicable. Provide evidence when recorded.
- **Empty**: calm, spacious empty state with no invented entries or decorative fake task history. Most recent real activity can appear if it exists.
- **Errors/interruptions** (implementation requirement, without expanding layout): plain-language cause and recovery guidance only when system evidence exists.

Use actual Hey MCP task state, state versions, event and receipt data as sources of truth. Never imply a job is complete or verified merely because it was queued, dispatched, or a click succeeded; no fake progress percentage or fabricated content.

## 4. Navigation & continuity

Use the separately approved global **Shallow Arc Floating Glass Carousel**: hidden by default; activated by a deliberate lower-edge horizontal gesture; equal-size item cards; center card slightly higher; adjacent cards slightly lower, more translucent, and allowed to clip at screen edges; arc **a little flatter than the prototype**; snaps to selection then fades/slides away after a short delay. No permanent navigation bar.

Tasks adopts the same global design system as approved Home and Browser. Tasks remains a page, not an overlay covering ongoing website interactions.

## 5. Adaptive By System

Visual measurements in previews are references, not screenshot-locked pixels. Implementation shall use Android density-independent dimensions, platform insets and gesture exclusions carefully, responsive/constraint-based layout, font scaling, sufficiently accessible touch targets, reduced-motion settings, and performance-aware blur. The scroll and gestures of page content should not be stolen by the lower-edge navigation activation zone.

## 6. Acceptance checks for implementation

1. No visible or reserved Tasks/header-status top row across all supported states.
2. Active task has visual priority; when empty, calm empty state appears with no fake activities.
3. Task lifecycle statuses and receipts match actual Hey data; completion and verification are not conflated.
4. Detail and evidence only appear when user expands and evidence is available; sensitive contents follow privacy policy.
5. Shallow Arc global gesture works without permanent bottom bar; visual proportions equal-size and flatter arc.
6. Responsive layouts, font scaling, Android insets, keyboard, scrolling, and reduced motion are covered by test plans.
7. Device-side UI, scroll gestures, background recovery, and actual MCP signals require validation on physical device before claiming PASS.

## 7. Explicit boundaries

**Locked:** overall Tasks preview design direction, component hierarchy, graphite visual language, contextual task states, detail/evidence affordance, latest-activity treatment, removal of top page title and global status chip, global approved navigation behavior.

**Not an implementation claim:** this lock does not modify the APK, repo, or MCP behavior. Real content, task receipts, interaction safeguards, performance, and platform constraints must be verified when implementation is authorized.
