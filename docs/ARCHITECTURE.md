# Aether architecture

Aether is a single-module native Android app.

```
UI (Jetpack Compose, Material 3 glass)
  ├─ Home / Videos / Folders / Playlists / Settings
  ├─ Player (gestures, PiP, mini-player)
  └─ Open URL / Search
         │
         ▼
ViewModels  ──►  AppContainer
                     ├─ LibraryRepository (Room)
                     ├─ MediaStoreScanner
                     ├─ UserPreferences (DataStore)
                     └─ PlayerManager (Media3 ExoPlayer)
                              │
                              ▼
                    AetherPlayerService (MediaSession)
```

## Layers

- **UI** — Compose screens, glass components, navigation. No playback engine details leak into widgets beyond `PlayerUiState`.
- **Playback** — `PlayerManager` owns a single `ExoPlayer`. `AetherPlayerService` wraps it in a `MediaSession` so notifications, Bluetooth, and the system output switcher work.
- **Library** — MediaStore scan is incremental: existing favorites, progress, and play counts survive a rescan.
- **Persistence** — Room (`aether.db`) for videos, folders, playlists, bookmarks, history, saved URLs, downloads. DataStore for settings.
- **Domain** — Pure Kotlin (`WatchProgressLogic`, `GestureMath`, `PlaylistLogic`, `UrlValidator`, `TimeFormat`) covered by JVM unit tests.

## Why no Hilt

A tiny `AppContainer` on `AetherApp` keeps cold start simple and avoids annotation-processor failure modes. Screens obtain it from `application as AetherApp`.

## Playback ownership

The player outlives the player screen. Back / swipe-down minimizes into an in-app glass mini-player. System PiP is a separate Android API (`PictureInPictureParams`), not a fake overlay.
