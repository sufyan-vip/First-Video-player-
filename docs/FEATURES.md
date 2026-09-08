# Aether feature list

## Implemented

- Native Kotlin + Jetpack Compose glass UI (OLED / dark / light / AMOLED, accent colors, dynamic color, high contrast, reduce-motion, animation scale)
- Bottom navigation: Home, Videos, Folders, Playlists, Settings
- Home dashboard: continue watching, recently played, recently added, favorites, network sources, folders, playlists, quick actions (open video, open folder, open URL, scan, history, downloads)
- MediaStore library scan with thumbnails, duration, size, resolution, bitrate; SAF folder import and single-file open
- Sort (name, added, modified, duration, size, last played), filters (favorite, unwatched, partial, recent) and resolution filter (720p+/1080p+/4K)
- Grid / list / compact views, adaptive tablet columns, search with debounce across videos, folders, and playlists
- ExoPlayer / Media3 playback for local files and HTTP(S), HLS, DASH, RTSP, with buffer profiles and retry policy
- Gestures: tap controls, double-tap seek, horizontal seek, left brightness, right volume, pinch zoom, long-press speed boost (with release), swipe-down mini-player, swipe-up to reveal controls
- Speed 0.25x–2x chips (engine 0.25x–4x), frame step, A-B loop, repeat, shuffle, sleep timer, screen lock, per-video orientation memory
- Aspect modes: fit, fill, crop, stretch, original (+ ratio presets)
- Embedded + external subtitles (SRT / VTT / ASS when the engine supports them), size / position / delay (SRT/VTT)
- Audio track picker, preferred audio language, audio focus, noisy-audio handling, system media-route button, best-effort equalizer presets
- Playlists: create, rename, delete, reorder, remove items; play next, queue, bookmarks with rename / delete / seek
- Chapters from queue parts and multi-period streams, with timeline chapter markers
- Frame capture to Pictures/Aether (Android 10+, local files)
- In-app mini-player and Android Picture-in-Picture (respects the auto-PiP toggle)
- Open URL with recent / saved URLs; downloads with progress, refresh, and completion import
- Share / open with, show in folder, rename, delete, copy, move, hide, favorite, video information sheet with codec / fps / channel probing
- Resume / continue watching with configurable completion threshold
- MediaSession notification, headset / Bluetooth controls, Android 13+ output switcher
- History screen with per-item and full clear + incognito
- Optional AI assistant (Google Gemini or OpenRouter): explain video, summarize markers, propose chapters, grounded Q&A, natural-language commands, subtitle translation — see docs/AI_SETUP.md
- Performance modes (high quality / balanced / battery saver), storage manager (thumbnail + cache clearing), diagnostics report
- Unit tests for progress, gestures, playlists, URLs, bookmarks, time formatting, subtitle shifting, and AI command parsing

## Supported media (engine-dependent)

Local and network containers ExoPlayer can decode on the device: MP4, MKV, WebM, MOV, TS, HLS (`m3u8`), DASH (`mpd`), plus common audio. If a codec is missing on the SoC, Aether shows **Codec unavailable** instead of crashing.

## Deliberate limitations

- **No Chromecast SDK** — the player offers the system media-output / route button instead of fake cast devices.
- **No online subtitle store** — loading a local / SAF subtitle file is real; scraping third-party subtitle sites is not.
- **No DRM / paywall bypass** — downloads are user-provided legal URLs only.
- **Audio delay is not offered** — ExoPlayer exposes no audio-delay API; a fake slider would violate the no-fake-controls rule.
- **Chapters** come from queue parts and multi-period streams (what Media3 exposes); embedded MKV/MP4 chapter atoms are not parsed.
- **Frame capture** uses the system frame decoder: works for local files, may fail for protected/segmented streams.
- **AI never pretends to watch video** — it only reasons over metadata, bookmarks, chapters, and text the user provides.
- **Glass blur** is a translucent material system (Compose cannot backdrop-filter the system wallpaper on all APIs).
- **Equalizer** is best-effort via `android.media.audiofx.Equalizer` and may be unavailable on some devices.
- **Two-finger tap** is not implemented to avoid conflicts with system and pinch gestures.
