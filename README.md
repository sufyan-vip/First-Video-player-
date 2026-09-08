# Aether — Android all-in-one video player

Native Kotlin video player with an Arc-style Compose UI, Media3/ExoPlayer playback,
a real MediaStore library, playlists, gestures, PiP, downloads, an optional AI
assistant, and a MediaSession foreground service.

This is not a WebView wrapper and not a control mockup. Playback, scanning,
progress, downloads, backup, and settings are wired end-to-end.

Made by **Sufyan** · Free and open source ❤
https://github.com/sufyan-vip/First-Video-player-

## Screens

- **Home** — continue watching, recently played/added, favorites, folders,
  network sources, storage stats, quick actions
- **Videos** — sort, filters (favorites, unwatched, partial, recent, hidden),
  quality filter, grid/list, Play all / Shuffle, rescan + progress
- **Folders / Playlists / History / Downloads / Search** — full library,
  playlist reorder, history management, system downloads, global search
- **Player** — Arc-style layout: top bar, right-edge rotate/mute/capture/cast
  column, audio/subtitle/chapter/bookmark/speed/menu row, seekbar with chapter
  ticks, lock pill, and a 21-item grid menu (AI, Up next, Favorite, Share,
  Stats, Volume boost, Speed, Sleep, EQ, Frame step, CC, Playlist…)
- **Settings** — 13 grouped pages: appearance, playback, gestures, player,
  audio, subtitles, library, network, storage + backup, privacy, performance,
  AI assistant, about

## Gestures (player)

- Left edge up/down: brightness · Right edge up/down: volume · Sideways: seek
- Double-tap sides: skip ±N s · Pinch: zoom · Long-press: speed boost
- Swipe down: mini-player · First-run hint card included

## Online streams

Paste a direct link (`.mp4`, `.m3u8`, `.mpd`, …) on the Open URL screen —
browser user-agent, redirect support, and adaptive MIME hints included, plus
built-in sample streams to try. Watch/share pages (YouTube, TikTok, …) are
detected with a friendly hint since they are not directly playable.

## AI assistant (optional, off by default)

Explain videos, summarize markers, propose chapters, ask grounded questions,
run natural-language commands (`jump to 12:30`), and translate loaded SRT/VTT
subtitles — via **Google Gemini** or **OpenRouter**. Answers appear in a
dedicated output box (copy / save-as-note / clear). Keys are stored encrypted
on-device. See [docs/AI_SETUP.md](docs/AI_SETUP.md).

## Backup

Settings → Storage can export/import all settings as JSON, and export/import
the library (stream links, saved URLs, playlists) as JSON.

## Stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Playback | AndroidX Media3 ExoPlayer + MediaSession |
| Data | Room + DataStore + EncryptedSharedPreferences |
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

Every push also builds on GitHub Actions — download the APKs from the run's
**Artifacts** (`aether-apks`).

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant **Videos** permission on first launch, then the library scans automatically.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/FEATURES.md](docs/FEATURES.md).

## Tests

```bash
./gradlew testDebugUnitTest
```

Unit tests cover watch-progress math, gesture math, playlist/queue logic, URL
validation, bookmarks, time formatting, subtitle shifting, and AI command parsing.

## Privacy

Aether does not upload local videos. History is on-device and can be disabled.
AI is off by default and requires a user-provided endpoint and key.

## Credit

Made by **Sufyan** — free and open source. Star the repo, report issues, and
feel free to fork: https://github.com/sufyan-vip/First-Video-player-

## License

Apache-2.0 for original source in this repository. AndroidX / Media3 remain
under their upstream licenses.
