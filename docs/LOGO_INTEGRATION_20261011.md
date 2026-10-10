# Hey. logo integration — 2026-10-11

The selected black rounded-square / white Hey. identity is now wired into this Android branch.

## Installed resources

- Manifest launcher: @mipmap/ic_launcher; roundIcon: @mipmap/ic_launcher_round.
- API 26+: 108dp foreground/background adaptive layers. API 33+: same lettering monochrome silhouette.
- Native in-app header: @drawable/ic_hey, custom outlined lettering in the primary rounded-square field.
- Notification small icon: @drawable/ic_hey_notification, glyph-only alpha silhouette. An opaque brand square must not be used as a status-bar small icon.
- Launcher activity: HeyStartingTheme with black native Android 12+ splash and @drawable/ic_splash_logo. MainActivity restores HeyTheme before super.onCreate. No artificial delay or extra splash Activity.
- App minSdk is 31, so no pre-Android-12 bitmap fallback or AndroidX splash dependency is required. Existing app permission, runtime, pairing, browser and audio logic remains as before.

## Master source

SVG masters, exact specification, guideline and asset QA live in assets/branding/hey. Those files are repository design assets, not packaged Android runtime assets. source/build_logo.py uses Python, Pillow and Inkscape to regenerate reference exports and Android XML under that branding directory; review/copy regenerated app resources deliberately. It does not edit Android application code automatically.

The brand field radius is 24% and the primary lettering width is 72%. Adaptive foreground uses 59dp of the 72dp visible viewport and stays within a conservative 66dp-diameter safe circle. The launcher owns the final mask and themed colors.

## Validation

Local asset renders, XML parsing, resource reference wiring, manifest and restored activity theme were checked before commit. APK compilation, existing unit tests and lint are run through the repository build workflow; use the linked run result as build evidence. Device launcher appearance, cold launch and OEM motion remain NOT_RUN until an actual phone test. This logo change does not close browser/background/wake/audio acceptance gates.
