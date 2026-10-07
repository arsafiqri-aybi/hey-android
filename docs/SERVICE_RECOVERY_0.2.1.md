# Hey 0.2.1 — service recovery hotfix

## Observed failure

On 2026-10-07 at approximately 19:45 Asia/Jakarta, the owner reported Hey Phone OFFLINE, SERVICE_STOPPED, AGENT and UNCONFIGURED wake after installing a newer APK. Authenticated gateway status confirmed offline/stale with the existing device record still paired. The last heartbeat lacked appVersion/runtime diagnostics, so the exact installed APK and physical stop trigger cannot be certified remotely.

## Source-confirmed defects and repairs

- A pause queued stopSelf callbacks and retained pausing=true. RESUME reused initialized runtime without clearing pausing, so polling stayed off; delayed stop callbacks could kill the resumed service. A lifecycle generation invalidates both network completion and timeout callbacks on resume. Only the still-current pause may stop the service.
- Paired ACTIVE startup happened once on Activity resume. Service death while the Activity stayed visible had no retry path. Bounded visible-owner retry permits three starts, spaced ten seconds apart; manual resume resets the limit. Intentional pause and missing local pairing always block automatic starts.
- ACTIVE paired starts return START_STICKY; a null system restart intent is handled without replaying actions or restoring expired audio consent. PAUSED/unpaired/failing startup returns START_NOT_STICKY. Force-stop and manufacturer restrictions are not bypassed or certified.
- Foreground promotion is guarded, uses specialUse on Android 14+, and supplies an explicit non-projection type before audio consent on Android 12/13. Notification permission is optional, no longer requested by automatic startup, and never gates service launch. Failures preserve a safe exception class rather than being overwritten with SERVICE_STOPPED.
- Heartbeat/task delivery precedes optional renewal/push registration. Maintenance uses a separate serialized executor and a bounded cadence; it cannot block the heartbeat executor on a slow FCM/renewal request. Durable pending pause/results retain their reconciliation order.
- Home, Browser and Settings expose Jalankan Hey/Lanjutkan Hey/Periksa koneksi plus local pairing, version, runtime and failure status. A remote paired record is not mistaken for a locally stored token. API health reports appVersion, runtime, runtimeReason and maintenanceReason from the same StateStore.
- Firebase config fetching is independent of successful pairing; a config-fetch failure cannot relabel an already enrolled device as pairing failed.
- Existing encrypted token/profile/journal are preserved. No pairing replacement, phone reset, uninstall or phone command was performed. The gateway code/deployment remains 0.2.0; this is an Android-only hotfix.

## Verification and installation

Consult evaluation/repair-0.2.1.json for actual build, test and certificate results. The candidate is signed with the same certificate as the previously supplied 0.2.0 APK (SHA-256 0dd325106ffd507f33daa4d46afdb05ccf120cf52a81add23f522bf4b22562d2). It cannot authorize an update over the original differently signed 0.1.0 APK.

Install as an update over that 0.2.0 candidate without uninstalling. Open Hey; Home should show version 0.2.1 and Jalankan Hey (or Lanjutkan Hey if intentionally paused). With a valid stored token, pairing again is unnecessary. If it shows Hubungkan dari ChatGPT, local credentials are absent; do not claim server pairing restores them automatically.

Physical acceptance remains RETEST_REQUIRED: first authenticated heartbeat with appVersion 0.2.1, browser ready/visible, rapid pause-resume surviving beyond the old five-second stop deadline, restart while visible, network loss/recovery, owner pause while backgrounded, and startup without notification/audio grant. Source tests do not certify the Vivo behavior. Wake still requires Firebase/FCM.
