# SmartEQ Pro — Android

Native Android (Kotlin + Jetpack Compose, Material 3 Expressive) port of
the SmartEQ Pro web app, rebuilt as a **system-wide, real-time equalizer**
in the spirit of PowerAmp EQ / Wavelet, plus the original app's AI-assisted
Auto-Tune, presets, and (now optional) Spotify integration.

## What's different from the web app, and why
| Web app | This app | Why |
|---|---|---|
| Web Audio API graph on a local `<audio>` element only | `android.media.audiofx.DynamicsProcessing` attached to other apps' audio sessions | You asked for real system-wide processing, not just local-file playback |
| Gemini key + Spotify secret held by a Node backend | User-supplied Gemini key (optional); Spotify via PKCE, no secret at all | No backend was provided, and secrets can't be safely bundled in a mobile app |
| Manual ID3 byte parsing | `MediaMetadataRetriever` | Android already has a robust, format-agnostic tag reader |
| No app-level lock | Biometric / device-credential / PIN App Lock | Requested addition — protects presets and the Spotify session |
| — | Fader-glyph placeholder icon | Final logo explicitly deferred per your instruction |

## Requirements
- Android Studio (current stable)
- compileSdk / targetSdk 37 (Android 17) — installable via SDK Manager
- minSdk 28 (hard floor — `DynamicsProcessing` requires API 28+)

## Before you build
Read `RELEASE_CHECKLIST.md` first — it's short and covers exactly what
still needs verifying on a real machine/device, since this project was
authored without access to the Android SDK or a device.

## Optional integrations, both off until you turn them on
- **Gemini AI tuning** — Settings > Web Connections > paste your own API
  key. Nothing about the app requires this; Auto-Tune has a full local
  fallback library.
- **Spotify** — see `SPOTIFY_SETUP.md`. Requires your own free Spotify
  Developer Client ID; no client secret involved anywhere.

## Project layout
```
app/src/main/java/com/smarteq/pro/
  audio/engine/     — DynamicsProcessing wrapper + session registry (the core EQ engine)
  service/          — the specialUse foreground service
  receiver/         — audio-session broadcast receiver, boot receiver
  data/             — models, Room presets DB, DataStore settings, Gemini/Spotify clients, AutoEQ + artist libraries
  ui/               — Compose screens: equalizer, autotune, presets, settings, help, applock, onboarding
  di/               — Hilt modules
```
