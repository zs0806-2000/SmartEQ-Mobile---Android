# SmartEQ Pro — Before You Ship This

This project was written in a sandboxed environment with **no Android
SDK, no emulator, and no device** — so nothing here has been compiled or
run. It was written carefully and cross-checked against documentation
where I could, but you must complete this checklist on a real machine
before treating it as production-ready, per the project's own
"error-free, fully debugged and compiled" requirement.

## 1. First build
- [ ] Open in a current Android Studio (Ladybug/Meerkat or newer).
- [ ] Let it run **Sync Project with Gradle Files** — this regenerates
      `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` (the ones in this zip
      are placeholders, see `gradlew`).
- [ ] Run the **AGP Upgrade Assistant** (Tools menu) and accept current
      stable versions — `gradle/libs.versions.toml` has my best-known
      versions as of my reliable knowledge cutoff, but Android 17 shipped
      after that, so newer point releases almost certainly exist.
- [ ] Resolve any dependency-resolution errors before doing anything else.

## 2. HIGHEST PRIORITY file to manually verify
`app/src/main/java/com/smarteq/pro/audio/engine/SmartEqDspEngine.kt` —
this wraps `android.media.audiofx.DynamicsProcessing`. Open that class's
Android SDK source in Android Studio (Ctrl/Cmd-click into it) and diff
every method call in `SmartEqDspEngine` against it. I'm reasonably
confident in this API's shape (documented, stable since API 28) but I
could not compile-check it here, and this is the file the entire
system-wide EQ feature depends on.

## 3. Real-device audio testing
- [ ] Install on a physical device (system-wide audio effects generally
      don't behave meaningfully in the emulator).
- [ ] Play something in a normal music app (e.g. YouTube Music, a local
      player) and confirm the Equalizer screen's status card correctly
      shows "Now equalizing: <app>" — and that moving a slider audibly
      changes the sound.
- [ ] Test with Spotify specifically once you've set up Client ID (see
      SPOTIFY_SETUP.md) — confirm session detection picks it up. If it
      doesn't on your device, try Legacy mode and confirm the in-app
      status/help copy is telling the truth about what's happening.
- [ ] Test on at least one Samsung and one non-Samsung device if you can
      — Samsung's audio HAL has historically been one of the stricter
      OEM implementations for third-party system effects.

## 4. Google Play submission
- [ ] The `specialUse` foreground service requires a
      `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification (already written
      in `AndroidManifest.xml`) that a **human Play Console reviewer**
      evaluates at submission time — this is outside anything I can
      verify or guarantee from here.
- [ ] Create your own release upload keystore (Build > Generate Signed
      App Bundle) — never reuse a debug keystore for a release build.
- [ ] Fill in a Play Console Data Safety form reflecting exactly what's
      in this app: no data collection by default; Gemini calls only send
      an artist/track/device string when the user supplies their own key
      and enables the toggle; Spotify calls only happen after explicit
      user login.

## 5. Optional but recommended before wide release
- [ ] Add a privacy policy URL (Play Console requires one even for an app
      that collects nothing, once any INTERNET permission + optional
      account-login feature exists).
- [ ] Decide on the final app icon/logo (current one is the placeholder
      Material fader glyph per your instruction — flagged as
      work-in-progress, not final).
