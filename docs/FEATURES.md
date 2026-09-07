# Aether feature list

## Implemented

- Native Kotlin + Jetpack Compose glass UI (OLED / dark / light / AMOLED, accent colors, dynamic color)
- Bottom navigation: Home, Videos, Folders, Playlists, Settings
- Home dashboard: continue watching, recently played, recently added, favorites, folders, playlists, quick actions
- MediaStore library scan with thumbnails, duration, size, resolution, bitrate
- Sort (name, added, modified, duration, size, last played) and filters (favorite, unwatched, partial, recent)
- Grid / list / compact views, search with debounce
- ExoPlayer / Media3 playback for local files and HTTP(S), HLS, DASH, RTSP
- Gestures: tap controls, double-tap seek, horizontal seek, left brightness, right volume, pinch zoom, long-press speed, swipe-down mini-player
- Speed 0.25x–2x, frame step, A-B loop, repeat, shuffle, sleep timer, screen lock
- Aspect modes: fit, fill, crop, stretch, original
- Embedded + external subtitles (SRT / VTT / ASS when the engine supports them), size / position
- Audio track picker, audio focus, noisy-audio handling
- Playlists, play next, queue, bookmarks
- In-app mini-player and Android Picture-in-Picture
- Open URL with recent / saved URLs
- Share / open with, hide, favorite, video information sheet
- Resume / continue watching with configurable completion threshold
- MediaSession notification, headset / Bluetooth controls, Android 13+ output switcher
- History + incognito
- Optional AI settings (opt-in endpoint/model; no fake “I watched this video”)
- Performance modes (high quality / balanced / battery saver)
- Unit tests for progress, gestures, playlists, URLs, bookmarks, time formatting

## Supported media (engine-dependent)

Local and network containers ExoPlayer can decode on the device: MP4, MKV, WebM, MOV, TS, HLS (`m3u8`), DASH (`mpd`), plus common audio. If a codec is missing on the SoC, Aether shows **Codec unavailable** instead of crashing.

## Deliberate limitations

- **No Chromecast SDK** — Cast devices are not faked. Use the system media-output switcher (Bluetooth / wired / built-in).
- **No online subtitle store** — loading a local / SAF subtitle file is real; scraping third-party subtitle sites is not.
- **No DRM / paywall bypass**
- **AI never pretends to watch video** — it only runs if the user supplies an endpoint
- **Glass blur** is a translucent material system (Compose cannot backdrop-filter the system wallpaper on all APIs)
- **Equalizer** is best-effort via `android.media.audiofx.Equalizer` and may be unavailable
