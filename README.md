# Vivo Screen Sync

Keep VivoCube screens in sync with the CoreBAR screen. This is a standalone
**Core app (`corebar-app`)**, not a widget or a preset. It runs on Core without
Studio remaining open after setup.

## Use

Install **Vivo Screen Sync 1.1.0** from App & Plugin Store with Studio 0.45.0
or newer. Keep Core and the intended VivoCubes connected.

1. Open **Design & actions** and drag Vivo Screen Sync into **Background apps**.
2. Select it and choose **Follow Core** or **Always on**. Set the wake delay
   from 0 to 3000 ms.
3. Choose **Deploy to Core**. Studio configures the online VivoCubes and applies
   the settings. The service runs on Core after setup.

**Always on** restores the configured screens and stops synchronization after
successful readback. You can also change these settings on Core.

Setup requires root-enabled WiFi ADB, CB01/ARM32, LineOS 1.0.8 or 1.0.9, and the
existing approved `ultrabarx-profile-inspection` bridge credential used by Studio.
There is no automatic creation of that credential. Studio reports a setup error
if it is unavailable. Newly connected/replaced cubes require repeating Store
installation to refresh the configured set. Only online VivoCubes are included.

Choose **Follow Core** to activate synchronization. Updates
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

The physical test with two VivoCubes covered sleep, wake and reboot. Studio
setup on CB01 was also verified with saved Follow Core settings and Android
reported the foreground service running. The 1.1.0 APK passed static checks.
A physical upgrade of an existing 1.0.1 installation remains unverified.

Screens may wake at slightly different times. Unknown or AOD states leave the
Vivo displays as they are. An unavailable module can delay other modules, and
the foreground notification reports retries. Target Android API 28, minimum 26.

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
submission preparation.

## Artwork

The Store and Android launcher icons now share the Studio lavender palette and show
CoreBAR, two VivoCube displays and synchronization arrows. The PNG and Android
vector are generated from the same geometry by `make_icon.py` (requires Pillow).
Run `python make_icon.py` to reproduce both assets.
