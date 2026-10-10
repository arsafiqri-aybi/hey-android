# Pearl preview validation — 2026-10-10

APK source commit: `91b1e32848cb98564f5beb88951f0cce4d11f24c` on `feat/hey-home-pearl-4k-browser-20261010`.
MCP source commit: `329b024712a72e3547033bbd5540f8d1cb96e7aa` on the same branch name in `hey-mcp`.

CI run: https://github.com/arsafiqri-aybi/hey-android/actions/runs/38041388022. The existing main workflow checked out the selected feature source; its log confirms the APK source SHA above. Main source was not compiled as the preview.

| Check | Result and scope |
| --- | --- |
| Android build and unit-test task | PASS; 23 source test methods across browser verification, service lifecycle, wallpaper selection and offline asset routes |
| Android lint | PASS |
| Chromium functional/render fixtures | PASS; 82 checks across six viewports |
| Manual render inspection | PASS at 320×640, 360×740, 393×852, 412×915, 480×960 and 852×393; sharp background, 4:5 preview, legible text, no dock overlap |
| MCP unit tests and syntax checks | PASS; 31 tests and `npm run check` |
| APK ZIP integrity and image assets | PASS; all four wallpaper files match the source bytes |
| APK signature | PASS; APK Signature Scheme v2, CI debug certificate |
| APK alignment | PASS; `zipalign -c -P 16 4` |
| Physical Android WebView/device | NOT_RUN; both non-revoked Hey Phone devices were offline when checked |
| Background, wake and audio on a device | NOT_RUN; no certification from source or desktop tests |
| MCP Worker deployment | NOT_PERFORMED; source changes remain in the feature branch |

APK: `Hey-Pearl-Preview.apk`, 20,474,895 bytes. SHA-256: `3b15e30e955bd2b95bce08a6accbf825feaa46de045dbac3ede95f74bc7989e6`.
Package: `id.ars.hey.preview.pearl`, label Hey Pearl Preview, version `0.3.1-preview`, minimum Android 12 (SDK 31), target SDK 35. The app installs separately; it has its own pairing and local state. QA images/report are included only in this debug build, not the release source set.

Read-back confirmed Android main remains `e69a6ec6ff00f32c0b002ce10d13c6c59c4b0cae` and MCP main remains `a702fcfedc989dba9b5609c0c38624fcc3832b94`. The documentation commit containing this record does not change the compiled APK source.
