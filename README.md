# Aether — Android all-in-one video player

Native Kotlin video player with a glassmorphic Compose UI, Media3/ExoPlayer playback, a real MediaStore library, playlists, gestures, PiP, and a MediaSession service.

This is not a WebView wrapper and not a control mockup. Playback, scanning, progress, and settings are wired end-to-end.

## Screenshots of the product shape

- **Home** — continue watching, recents, favorites, folders, quick actions
- **Videos / Folders / Playlists** — library with sort, filter, grid/list
- **Player** — gesture-first glass controls, timeline, audio/subtitle sheets
- **Settings** — appearance, playback, gestures, privacy, optional AI

## Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Playback | AndroidX Media3 ExoPlayer + MediaSession |
| Data | Room + DataStore |
| Images | Coil (video frames) |
| Min SDK | 26 |
| Target SDK | 35 |

## Build

```bash
# JDK 17 and Android SDK (platform 35) required
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
```

APKs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk` (debug-signed so it is installable)

A ready-to-run GitHub Actions workflow is checked in as [`docs/github-actions-android.yml`](docs/github-actions-android.yml). Copy it to `.github/workflows/android.yml` on a token that has the `workflows` permission, and Actions will emit:

- `dist/Aether-debug.apk`
- `dist/Aether-release.apk`

Until then, build locally with Android Studio or the SDK command line (JDK 17 + Android platform 35).

## Install

```bash
adb install -r dist/Aether-debug.apk
```

Grant **Videos** permission on first launch, then the library scans automatically.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/FEATURES.md](docs/FEATURES.md).

## Tests

```bash
./gradlew testDebugUnitTest
```

Unit tests cover watch-progress math, gesture math, playlist/queue logic, URL validation, bookmarks, and time formatting.

## Privacy

Aether does not upload local videos. History is on-device and can be disabled. AI is off by default and requires a user-provided endpoint.

## License

Apache-2.0 for original source in this repository. AndroidX / Media3 remain under their upstream licenses.
