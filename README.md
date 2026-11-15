# Vivo Screen Sync

## 1.0.1 icon update

The Store and Android launcher icons now share the Studio lavender palette and show
CoreBAR, two VivoCube displays and synchronization arrows. The PNG and Android
vector are generated from the same geometry by `make_icon.py` (requires Pillow).
Run `python make_icon.py` to reproduce both assets.

This release changes only artwork and the version metadata. App behavior,
permissions, settings, package identity and signing identity are unchanged.
Build, signature inspection and timing tests were checked. This artwork update
has not been installed on physical Core hardware.


Keep VivoCube screens in sync with the CoreBAR screen. This is a standalone
**Core app (`corebar-app`)**, not a widget or a preset. It runs on Core without
Studio remaining open after setup.

## Use

Install from UltraBar Studio 0.28.0 or newer with Core and the intended VivoCubes
connected. Studio discovers online VivoCubes, verifies the APK and configures
private app storage. Open Vivo Screen Sync on Core and choose **Follow Core**.
**Always on** restores the configured screens and stops synchronization after
successful readback. Wake delay is adjustable from 0–3000 ms.

Setup requires root-enabled WiFi ADB, CB01/ARM32, LineOS 1.0.8 or 1.0.9, and the
existing approved `ultrabarx-profile-inspection` bridge credential used by Studio.
There is no automatic creation of that credential. Studio reports a setup error
if it is unavailable. Newly connected/replaced cubes require repeating Store
installation to refresh the configured set. Only online VivoCubes are included.

First install leaves synchronization off until you choose Follow Core. Updates
keep the same Android package and signing identity, preserving enable state and
wake delay. The old private 0.4-test build has the same identity; it can be updated
without uninstalling it. Never uninstall first merely to work around an update
error. Before deliberately uninstalling, select Always on and confirm the Vivo
screens have turned on.

## Permissions and data

- INTERNET: authenticated TLS connection to CoreBridge on `127.0.0.1:7431` only.
- FOREGROUND_SERVICE: visible background synchronization notification.
- RECEIVE_BOOT_COMPLETED: resume the user's enabled setting after Core restarts.
- WAKE_LOCK: bounded command processing while Core sleeps.

The bridge credential, certificate pin and selected module IDs are stored in
app-private storage on Core. They are not embedded in the APK or uploaded.
Studio reuses the owner's previously approved bridge credential during setup;
this grants the app access to CoreBridge, so install only the reviewed release.
No analytics, cloud service or PC-control commands. The service reads/writes only
the selected Vivo display property. It does not modify profiles or firmware.

## Build and source review

Requires JDK 17+, Python 3 and Google Android SDK platform 35/build-tools 35.0.0.
Set `ANDROID_BUILD_TOOLS` to the build-tools directory and
`ANDROID_PLATFORM_JAR` to platform 35's android.jar. Set
`SCREEN_SYNC_KEYSTORE` to your private keystore and `SCREEN_SYNC_KEY_PASSWORD`
in the environment, then run `python build.py`. Output: `app.apk`.
The official signing key stays private. Signing with a different key cannot
update an existing official installation. This source is available for inspection
under LICENSE, not a reusable app template.

Run the pure Java timing tests:

```text
javac -d build/timing-tests src/dk/ultrabarx/screensync/WakeTiming.java tests/dk/ultrabarx/screensync/WakeTimingTest.java
java -cp build/timing-tests dk.ultrabarx.screensync.WakeTimingTest
```

Release assets are `app.apk` and `source.zip`; the latter is `git archive --format=zip HEAD`
with every tracked source file at archive root, without private keys or build output.
The Store checks that source.zip matches the pinned Git tree and that the APK
matches the release, manifest, permissions and signing identity. These checks do
not prove that a binary was compiled from its published source; owner review and
hardware testing remain required.

## Status and limitations

The earlier private prototype was tested with two VivoCubes, screen sleep/wake
and reboot. The packaged 1.0.0 release adds generic Studio provisioning and removes
embedded device-specific setup. Compilation, signature inspection and host timing
tests are checked; this new installation path still requires owner testing on Core
before Store approval. Screens may wake at slightly different times. Unknown/AOD
states do not change Vivo state. An unavailable module can delay other modules;
the foreground notification reports retries. Long-running stability is not yet
established. Target Android API 28, minimum 26; no claim of support for newer Core
firmware beyond the declared versions.

## Studio settings API 1 (1.1.0)

Requires Studio 0.45.0+ for the shared settings interface. The root
`studio-settings.json` equals the descriptor embedded at assets/studio-settings.json
and the Store manifest settings property. Studio reads actual `enabled` and
`wakeDelayMs` values through the protected StudioSettingsReceiver, and waits for
durable read/apply confirmation. Existing sync preference names and unrelated
settings are preserved. Root-enabled ADB and the existing approved Studio bridge
credential provisioning are still required; the settings descriptor does not
grant access credentials. Legacy in-app controls and previous Studio setup remain
available.

Integration requirements and reusable helper example:
https://github.com/qlerup/ultrabar-store/blob/main/APP-SETTINGS.md .
The shared transport was tested on physical CB01/LineOS 1.0.9 with a separate
example app: six field types, read/apply confirmation and persistence after
force-stop. This release APK passes static metadata/receiver/schema/signature
inspection; upgrading the user's installed Screen Sync was not performed during
submission preparation. Test the 1.0.1 -> 1.1.0 upgrade on hardware before merging.
