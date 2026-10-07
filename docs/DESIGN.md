# Hey visual system

The organizing idea is a quiet personal control surface: a small hello, a clear connection, one visible browser, and an easy way to take control. Off-white surrounds warm white surfaces. Forest green marks the primary action. Typography uses the platform sans-serif family with deliberate weight and size hierarchy. Corners, borders and a shallow elevation separate layers without piling on effects.

Home prioritizes actual connection and browser readiness. Browser shows the same private render surface and takeover state. Tasks separates observed progress from verified completion. Settings exposes audio session consent and Android permission controls. Status text comes from StateStore, never an independent optimistic UI flag.

Transitions use short opacity and 5dp movement when Android animation settings permit. Touch targets are at least 48dp; screen insets are respected. State feedback remains textual. Audio is a browser observation channel, not decorative UI sound. Haptics and stronger motion remain optional refinements subject to device and usability tests.
