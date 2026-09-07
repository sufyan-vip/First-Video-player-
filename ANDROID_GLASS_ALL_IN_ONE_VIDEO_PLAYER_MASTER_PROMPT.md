# MASTER BUILD PROMPT — Android Native All‑in‑One Video Player

## ROLE

You are a senior Android architect, native UI/UX engineer, media-engine developer, performance engineer, and QA engineer.

Build a **production-quality Android video player** that feels like a polished, next-generation Android system app. The experience must be **native-first, glassmorphic, fluid, fast, gesture-driven, and extremely complete**.

Do NOT make a basic VLC clone.
Do NOT make a web-app-looking UI.
Do NOT use fake controls that do not work.
Do NOT add placeholder buttons for unfinished features.

The goal is a player that feels like a premium **next-generation Android media player**, with a modern glass UI inspired by contemporary Android design language without copying any proprietary app exactly.

---

# 1. CORE PRODUCT VISION

Create an **All-in-One Video Player** combining:

- Local video player
- Folder/library manager
- Network/URL player
- Subtitle manager
- Audio-track manager
- Playback customization
- Gesture controls
- Picture-in-Picture
- Floating mini-player
- Playlist/queue
- Favorites/history
- Video information
- File operations
- Cast/route support where available
- Media-session/headset controls
- Optional AI assistant layer
- Powerful settings
- Theme/customization system
- Performance diagnostics

Everything must feel like one coherent native Android product.

---

# 2. NATIVE ANDROID REQUIREMENTS

Use a genuinely native Android architecture.

Prefer:

- Kotlin
- Jetpack Compose for UI where appropriate
- Material 3 / modern Android design primitives
- Android Media3 / ExoPlayer as the playback foundation
- Android MediaSession
- Android Picture-in-Picture
- Android lifecycle-aware components
- Storage Access Framework / MediaStore
- WorkManager where background work is appropriate
- Coroutines / Flow
- Proper ViewModel/state architecture

Do not build the primary experience as HTML/CSS inside a WebView.

Target current Android versions while maintaining sensible compatibility with older supported devices.

The architecture must be modular so playback, library, subtitles, settings, AI, networking, and UI can evolve independently.

---

# 3. GLASS UI / VISUAL SYSTEM

The visual identity is extremely important.

Design the app as a **premium translucent glass Android interface**.

### Visual principles

- Deep dark background
- Dynamic blurred/translucent surfaces
- Frosted glass cards
- Soft borders
- Subtle depth
- Large rounded corners
- Clean typography
- Minimal visual noise
- Smooth micro-animations
- Strong hierarchy
- Touch-friendly controls

Use glass effects carefully. Do NOT make everything transparent to the point that text becomes unreadable.

### Glass components

Create reusable components:

- GlassCard
- GlassButton
- GlassIconButton
- GlassBottomSheet
- GlassDialog
- GlassTopBar
- GlassTabBar
- GlassSlider
- GlassChip
- GlassPlayerControls
- GlassMiniPlayer
- GlassSettingsRow

Use adaptive blur/transparency depending on device performance.

### Color

Default theme:

**OLED black + neutral translucent glass**

Allow dynamic accent color.

Possible accents:

- Blue
- Purple
- Green
- Orange
- Red
- Pink
- Cyan

Also support:

- System dynamic color
- Pure AMOLED
- Dark
- Light
- Auto

Do not hard-code the whole UI to one accent.

---

# 4. HOME SCREEN

Create a premium home dashboard.

Sections:

### Continue Watching
Shows:
- thumbnail
- title
- progress
- remaining time

### Recently Added

### Recently Played

### Favorites

### Downloads / Offline

### Folders

### Playlists

### Network Sources

### Quick Actions

Quick actions:

- Open Video
- Open Folder
- Open URL
- Scan Library
- Downloads
- Playlists
- Settings

Allow horizontal scrolling cards and compact list mode.

---

# 5. BOTTOM NAVIGATION

Use a clean native bottom navigation:

- Home
- Videos
- Folders
- Playlists
- Settings

The navigation should support gesture navigation and safe-area insets.

Use animated selection indicators.

---

# 6. VIDEO LIBRARY

Build a serious media library.

Features:

- Automatic video discovery
- MediaStore integration
- Folder scanning
- Manual folder selection
- Rescan
- Thumbnail generation
- Duration
- File size
- Resolution
- FPS where available
- Codec information
- Last played
- Playback progress
- Favorite
- Hidden files/folders
- Sort
- Filter
- Search
- Grid view
- List view
- Compact view

Sorting:

- Name
- Date added
- Date modified
- Duration
- Size
- Last played

Filtering:

- Resolution
- Duration
- Folder
- Favorite
- Recently played
- Unwatched
- Partially watched

---

# 7. VIDEO PLAYER — MAIN EXPERIENCE

The player screen must be the strongest part of the app.

### Controls

Top:

- Back
- Video title
- Cast/route
- Audio
- Subtitle
- More

Bottom:

- Previous
- Rewind
- Play/Pause
- Forward
- Next
- Timeline
- Current time
- Duration
- Fullscreen
- Playback speed

Controls automatically hide.

Tap screen:

- Show/hide controls

Double tap:

- Left = rewind
- Right = forward

Swipe horizontally:

- Seek

Swipe vertically:

- Left side = brightness
- Right side = volume

Pinch:

- Zoom
- Fit
- Crop

Long press:

- Temporary 2x playback

All gestures must have configurable sensitivity.

---

# 8. ADVANCED PLAYER GESTURES

Implement:

- Double-tap seek
- Horizontal seek
- Vertical volume
- Vertical brightness
- Pinch zoom
- Two-finger tap play/pause
- Long-press speed boost
- Swipe down = minimize
- Swipe up = show player controls
- Edge gestures where safe
- Tap-and-hold timeline preview

Show beautiful translucent overlays:

Example:

+10 sec
-10 sec
Brightness 70%
Volume 45%
Speed 1.5x
Zoom 125%

Use subtle haptics.

Every gesture must be configurable in Settings.

---

# 9. TIMELINE / SEEK EXPERIENCE

Make seeking significantly better than typical players.

Include:

- Smooth scrubbing
- Seek preview thumbnails when possible
- Chapter markers
- Buffered indicator
- Watch progress
- Remaining time
- Current time
- Optional live indicator
- Tap timeline to jump
- Long press for precise seeking

Allow timeline style customization.

---

# 10. PLAYBACK FEATURES

Implement where supported by the selected media engine/device:

- Play
- Pause
- Previous
- Next
- Rewind
- Forward
- 0.25x
- 0.5x
- 0.75x
- 1x
- 1.25x
- 1.5x
- 1.75x
- 2x
- Custom playback speed
- Frame stepping
- Repeat
- Repeat one
- Shuffle
- A-B repeat
- Sleep timer
- Resume playback
- Auto-play next
- Keep screen awake
- Lock screen
- Screen rotation control

---

# 11. VIDEO DISPLAY MODES

Provide:

- Fit
- Fill
- Crop
- Stretch
- Original
- 16:9
- 4:3
- Custom zoom
- Free zoom

Support immersive fullscreen.

Handle:

- Cutouts
- Notches
- Gesture navigation
- Landscape
- Portrait
- Foldable/tablet layouts where applicable

---

# 12. SUBTITLE SYSTEM

Build a complete subtitle manager.

Support formats where the playback engine supports them:

- SRT
- VTT
- ASS/SSA
- embedded subtitle tracks

Features:

- Enable/disable
- Select track
- External subtitle file
- Subtitle delay
- Font size
- Font family where supported
- Position
- Background
- Outline
- Opacities
- Color
- Sync adjustment
- Subtitle encoding
- Remember settings

Add:

**Load Subtitle**
**Search/Choose Subtitle File**
**Subtitle Settings**

Do not pretend online subtitle downloading works unless actually implemented and legally usable.

---

# 13. AUDIO SYSTEM

Features:

- Audio track selector
- Embedded audio tracks
- External audio where supported
- Audio delay
- Volume normalization where supported
- Stereo/mono options where supported
- Audio focus handling
- Bluetooth/headset handling
- Media button handling
- Background audio mode where appropriate

Add an optional equalizer only if it can be implemented reliably.

---

# 14. PLAYLIST SYSTEM

Create powerful playlists.

Features:

- Create playlist
- Rename
- Delete
- Add/remove videos
- Reorder
- Shuffle
- Repeat
- Queue
- Play next
- Add to queue
- Save queue as playlist

Player queue should survive rotation and process recreation where practical.

---

# 15. MINI PLAYER

Implement a beautiful in-app mini-player.

Behavior:

- Swipe player downward
- Player becomes floating mini-player
- Library remains usable
- Tap mini-player to restore
- Swipe mini-player away to close

Glass UI.

Also implement Android PiP separately.

---

# 16. ANDROID PICTURE-IN-PICTURE

Implement proper Android PiP:

- Enter PiP when supported
- Playback continues
- Correct aspect ratio
- Media controls
- Restore player correctly
- Handle lifecycle correctly

Do not fake PiP using a normal overlay.

---

# 17. NETWORK / URL PLAYER

Create an **Open URL** feature.

Support network playback where the media engine supports it:

- HTTP/HTTPS
- HLS/M3U8
- Other supported streaming formats

Features:

- Paste URL
- Recent URLs
- Save URL
- Open URL
- Network playback errors
- Retry
- Buffering status

Never claim support for a format the underlying engine cannot actually play.

---

# 18. FILE MANAGER FEATURES

Inside the video library:

- Rename
- Delete
- Move
- Copy
- Share
- Open with
- File information
- Show in folder
- Favorite
- Hide

Use proper Android storage permissions and SAF/MediaStore rules.

Confirm destructive actions.

---

# 19. VIDEO INFORMATION PANEL

Create a beautiful glass information sheet.

Display when available:

- File name
- Path/location
- Size
- Duration
- Resolution
- Aspect ratio
- Frame rate
- Video codec
- Audio codec
- Audio channels
- Audio sample rate
- Bitrate
- Container format
- Subtitle tracks
- Audio tracks

Make this panel visually premium and easy to scan.

---

# 20. CAST / MEDIA ROUTING

If the device/platform supports it:

- Media route button
- Bluetooth output
- External display handling
- Cast integration where legitimately supported

Do not display fake cast devices.

If unavailable, hide or disable the control intelligently.

---

# 21. DOWNLOAD / OFFLINE MODULE

Create an offline section for files already stored locally.

If implementing downloading:

- User-provided legal URLs only
- Download queue
- Pause
- Resume
- Cancel
- Retry
- Progress
- Download history
- Storage location
- Error state

Do not implement bypasses for DRM, authentication, paywalls, or access restrictions.

---

# 22. SEARCH

Global search should find:

- Videos
- Folders
- Playlists
- Favorites
- Recently played

Use instant filtering and debounce.

---

# 23. HISTORY

Track:

- Recently played
- Watch progress
- Last position
- Completed status
- Play count

Allow:

- Clear one item
- Clear history
- Disable history

---

# 24. FAVORITES

Allow:

- Favorite/unfavorite
- Favorite folders
- Favorite playlists

Provide a dedicated Favorites section.

---

# 25. SMART CONTINUE WATCHING

When reopening a partially watched video:

Show:

**Continue from 24:37**

Actions:

- Continue
- Start over

Allow configurable completion threshold.

---

# 26. SLEEP TIMER

Support:

- 10 min
- 15 min
- 30 min
- 45 min
- 60 min
- End of video
- Custom time

Show remaining timer unobtrusively.

---

# 27. SCREEN LOCK

Player lock mode:

- Disable accidental touches
- Keep video playing
- Unlock gesture
- Optional brightness/volume gestures while locked

Use haptic feedback.

---

# 28. NOTIFICATION / MEDIA SESSION

Implement proper Android media integration:

- Media notification
- Play/pause
- Previous
- Next
- Seek where supported
- Title
- Thumbnail/artwork where available
- Lock-screen controls
- Bluetooth headset controls
- Android media session integration

---

# 29. SMART ORIENTATION

Options:

- Auto
- Portrait
- Landscape
- Sensor
- Remember per video

Do not force orientation unexpectedly.

---

# 30. SETTINGS

Create a polished glass Settings system.

Categories:

### Playback
- Default speed
- Auto-play next
- Resume playback
- Seek interval
- Double-tap interval
- Background playback
- Keep screen awake

### Gestures
- Enable/disable each gesture
- Sensitivity
- Seek distance
- Brightness gesture
- Volume gesture
- Pinch behavior
- Long-press speed

### Appearance
- Theme
- AMOLED
- Light/Dark
- Accent color
- Dynamic color
- Glass intensity
- Blur intensity
- Animation scale
- Compact mode

### Player
- Default aspect ratio
- Default zoom
- Controls timeout
- Timeline style
- Show remaining time
- Haptic feedback

### Subtitles
- Size
- Style
- Position
- Delay

### Audio
- Default audio track
- Audio delay
- Output preferences where supported

### Library
- Scan folders
- Excluded folders
- Hidden files
- Thumbnail quality
- Grid size
- Sort order

### Network
- Buffer preferences
- Retry behavior
- Saved URLs

### Storage
- Cache
- Clear thumbnails
- Clear cache
- Storage information

### Privacy
- History
- Incognito playback
- Clear data

### AI
- Enable AI
- Provider
- API endpoint
- API key
- Model
- Privacy controls

### About
- App version
- Open-source licenses
- Diagnostics
- Report issue

---

# 31. AI ASSISTANT — OPTIONAL ADVANCED MODULE

Create an optional AI layer, but keep the player fully functional without AI.

The AI system should support configurable providers through a clean abstraction.

Potential capabilities:

- Explain current video metadata
- Generate chapter labels from available transcript data
- Search transcript
- Summarize user-provided/local transcript
- Answer questions about available transcript text
- Subtitle translation workflow
- Create bookmarks from natural-language commands

Example:

“Jump to the part where they discuss Android.”

The AI must NOT pretend it watched or understood a video when no transcript/audio-analysis data is actually available.

API credentials must be stored securely.

---

# 32. BOOKMARKS

Allow users to bookmark exact moments.

Example:

**01:23:44 — Important Scene**

Features:

- Add bookmark
- Name bookmark
- Edit
- Delete
- Tap bookmark → seek directly
- Bookmark list per video

---

# 33. CHAPTERS

If chapters are available:

- Display chapters
- Chapter list
- Chapter markers
- Next/previous chapter
- Current chapter title

If chapters aren't embedded, do not fabricate them.

---

# 34. SCREENSHOT / FRAME CAPTURE

If technically and legally appropriate:

- Capture current frame
- Save to selected location
- Share captured frame

Respect Android permissions and storage rules.

---

# 35. PERFORMANCE

This is critical.

Optimize for mid-range Android phones.

Requirements:

- Fast cold start
- Lazy library loading
- Lazy thumbnails
- Efficient database
- No unnecessary recomposition
- Avoid memory leaks
- Avoid blocking main thread
- Proper coroutine cancellation
- Efficient video surface handling
- Efficient image caching
- Adaptive glass blur
- Reduced animation mode for weak devices

Create a performance mode:

**High Quality**
**Balanced**
**Battery Saver**

---

# 36. ERROR HANDLING

Never crash because a video is unsupported.

Show useful errors:

- Unsupported format
- Codec unavailable
- Corrupted file
- Permission denied
- Network unavailable
- Stream unavailable
- Subtitle failed
- Storage unavailable

Offer:

- Retry
- Open with another app
- View details
- Copy error information

No generic “Something went wrong” unless unavoidable.

---

# 37. ACCESSIBILITY

Support:

- TalkBack
- Content descriptions
- Large text
- High contrast
- Minimum touch target sizes
- Reduced motion
- Color-independent status indicators

Glass effects must never destroy readability.

---

# 38. RESPONSIVE DESIGN

Support:

- Phones
- Large phones
- Tablets
- Landscape
- Foldables where practical

Do not simply stretch the phone UI.

Use adaptive layouts.

---

# 39. ANIMATIONS

Use polished but restrained animation:

- Shared transitions
- Bottom-sheet spring animations
- Glass card transitions
- Player control fade
- Mini-player transition
- Tab indicator movement
- Thumbnail loading
- Haptic confirmation

Respect Android animation scale settings.

---

# 40. SECURITY / PRIVACY

Never collect unnecessary user data.

Do not upload local videos automatically.

AI features must be opt-in.

Network requests should be explicit.

Never expose API keys in source code.

Use secure storage for secrets.

---

# 41. DATA ARCHITECTURE

Use a persistent local database for:

- Video metadata/cache
- Watch progress
- History
- Favorites
- Playlists
- Bookmarks
- Saved URLs
- User preferences

Database operations must be asynchronous.

Library scanning must be incremental rather than blocking the UI.

---

# 42. TESTING

Write real tests.

At minimum:

### Unit tests
- Playback state
- Watch progress
- Playlist logic
- History logic
- Settings
- Gesture calculations
- URL validation
- Bookmark logic

### UI tests
- Play/pause
- Seek
- Fullscreen
- Orientation
- Mini-player
- Settings
- Library search
- Playlist actions

### Integration tests
- Media3 playback
- MediaSession
- PiP
- Storage access
- Library scanning

Do not mark tests as passed without actually running them.

---

# 43. QUALITY GATE

Before declaring the project complete:

1. Build the app.
2. Run all tests.
3. Fix compile errors.
4. Fix runtime crashes.
5. Test on a real Android device/emulator.
6. Verify portrait and landscape.
7. Verify dark/light themes.
8. Verify gestures.
9. Verify local playback.
10. Verify unsupported media handling.
11. Verify subtitle handling.
12. Verify PiP.
13. Verify mini-player.
14. Verify library scanning.
15. Verify storage permissions.
16. Verify process recreation.
17. Verify accessibility basics.
18. Verify performance.
19. Remove placeholder controls.
20. Remove dead code.
21. Run final clean build.

Only after all of this report the project as complete.

---

# 44. UX RULES — VERY IMPORTANT

The app must feel:

**Fast**
**Native**
**Premium**
**Simple**
**Powerful**
**Responsive**

Never make the user hunt through five menus for basic playback controls.

Advanced controls belong in elegant glass bottom sheets.

The main player should remain visually clean.

Do not overcrowd the video with buttons.

Use progressive disclosure:
basic controls first, advanced controls on demand.

---

# 45. FINAL UI TARGET

The final result should feel like:

> “This looks like a premium next-generation Android system media player.”

Not:

> “This is an old-school VLC clone with hundreds of buttons.”

Use glass surfaces, native Android behavior, excellent typography, adaptive layouts, smooth gestures, subtle motion, and strong visual hierarchy.

---

# 46. IMPLEMENTATION RULE

Work incrementally.

For every major phase:

1. Inspect existing project.
2. Plan changes.
3. Implement.
4. Build.
5. Test.
6. Fix.
7. Rebuild.
8. Verify.
9. Only then move to the next phase.

Do not rewrite working modules unnecessarily.

Do not create duplicate implementations.

Do not leave TODO placeholders for core functionality.

If a requested feature cannot be reliably implemented on the target Android/API level, implement the best supported native alternative and clearly document the limitation rather than faking it.

---

# 47. DELIVERY

At the end provide:

- Complete source code
- Working Android project
- Build instructions
- Feature list
- Architecture summary
- Supported formats/features
- Known limitations
- Test results
- Release APK if the environment can build it
- Debug APK if useful

Most importantly:

**The app must actually work.**

A beautiful mockup is not acceptable.
A collection of fake buttons is not acceptable.
A WebView pretending to be a native Android app is not acceptable.

Build the real thing.
