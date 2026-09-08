package com.aether.player.playback

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.library.LibraryRepository
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.AspectMode
import com.aether.player.data.prefs.BufferProfile
import com.aether.player.data.prefs.UserPreferences
import com.aether.player.domain.PlaylistLogic
import com.aether.player.domain.RepeatMode
import com.aether.player.domain.SubtitleShift
import com.aether.player.domain.WatchProgressLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class PlayerManager(
    private val context: Context,
    private val library: LibraryRepository,
    private val prefs: UserPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val trackSelector = DefaultTrackSelector(context)

    var player: ExoPlayer = buildEngine(AetherSettings())
        private set

    private var settingsCache = AetherSettings()
    private var appliedNetKey = netKey(AetherSettings())

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private val _overlay = MutableStateFlow<OverlayHint?>(null)
    val overlay: StateFlow<OverlayHint?> = _overlay

    private var sleepJob: Job? = null
    private var progressJob: Job? = null
    private var boostFrom: Float = 1f
    private var equalizer: Equalizer? = null
    private var lastSavedAt = 0L
    var lastSubtitleUri: Uri? = null
        private set

    init {
        attachListener()
        attachEqualizer()
        ensureTicker()
        applyPerformanceMode(settingsCache.performanceMode)
        scope.launch {
            prefs.settings.collect { next ->
                val prevLang = settingsCache.defaultAudioLang
                val prevPerf = settingsCache.performanceMode
                settingsCache = next
                if (next.defaultAudioLang != prevLang) applyAudioPrefs()
                if (next.performanceMode != prevPerf) applyPerformanceMode(next.performanceMode)
            }
        }
    }

    private fun buildEngine(s: AetherSettings): ExoPlayer {
        val b = bufferDurations(s.bufferProfile)
        // Browser-like UA + redirect support so more hosts' online links play.
        val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(b[0], b[1], b[2], b[3])
                    .build(),
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setLoadErrorHandlingPolicy(
                    AetherLoadPolicy(DefaultLoadErrorHandlingPolicy(), s.maxRetries.coerceIn(0, 10)),
                ),
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }

    private fun bufferDurations(profile: BufferProfile): IntArray = when (profile) {
        BufferProfile.SMALL -> intArrayOf(5_000, 15_000, 1_000, 2_000)
        BufferProfile.STANDARD -> intArrayOf(15_000, 50_000, 1_500, 5_000)
        BufferProfile.LARGE -> intArrayOf(30_000, 120_000, 2_500, 5_000)
    }

    private fun netKey(s: AetherSettings): String = "${s.bufferProfile.name}:${s.maxRetries}"

    /** Rebuilds the engine when buffer/retry settings changed. Called at queue start (no state loss). */
    private fun maybeRebuildEngine() {
        val key = netKey(settingsCache)
        if (key == appliedNetKey) return
        appliedNetKey = key
        persistProgress(increment = false)
        runCatching { player.stop() }
        runCatching { player.release() }
        player = buildEngine(settingsCache)
        attachListener()
        attachEqualizer()
        applyAudioPrefs()
        requestSessionRebuild()
    }

    private fun attachListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish { it.copy(playing = isPlaying, playWhenReady = player.playWhenReady) }
                ensureTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                publish {
                    it.copy(
                        buffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = player.duration.coerceAtLeast(0L),
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    scope.launch { onEnded() }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                publish {
                    it.copy(
                        playing = false,
                        error = mapError(error),
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                publish { it.copy(audioTracks = audioChoices(tracks), textTracks = textChoices(tracks)) }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                publish { it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex
                val queue = _state.value.queue
                val current = queue.getOrNull(idx)
                publish {
                    it.copy(
                        index = idx,
                        current = current,
                        error = null,
                        hasNext = idx < queue.lastIndex,
                        hasPrevious = idx > 0,
                        durationMs = player.duration.coerceAtLeast(0L),
                    )
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                publish { it.copy(speed = playbackParameters.speed) }
            }
        })
    }

    fun playQueue(items: List<VideoEntity>, startIndex: Int, startPositionMs: Long? = null) {
        if (items.isEmpty()) return
        maybeRebuildEngine()
        val idx = startIndex.coerceIn(0, items.lastIndex)
        val mediaItems = items.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, idx, startPositionMs ?: C.TIME_UNSET)
        player.prepare()
        player.playWhenReady = true
        publish {
            it.copy(
                queue = items,
                index = idx,
                current = items[idx],
                miniPlayer = false,
                locked = false,
                error = null,
                hasNext = idx < items.lastIndex,
                hasPrevious = idx > 0,
            )
        }
        startService()
        scope.launch {
            val settings = prefs.settings.first()
            player.setPlaybackSpeed(settings.defaultSpeed)
            applyAudioPrefs()
            if (settings.resumePlayback && startPositionMs == null) {
                val pos = items[idx].lastPositionMs
                if (WatchProgressLogic.shouldOfferResume(pos, items[idx].durationMs, settings.completionThreshold)) {
                    player.seekTo(pos)
                }
            }
        }
    }

    fun playSingle(video: VideoEntity, startOver: Boolean = false) {
        val pos = if (startOver) 0L else video.lastPositionMs
        playQueue(listOf(video), 0, if (startOver) 0L else pos.takeIf { it > 5_000L })
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it > 0 } ?: _state.value.durationMs
        player.seekTo(positionMs.coerceIn(0L, duration.coerceAtLeast(0L)))
        publish { it.copy(positionMs = player.currentPosition) }
    }

    fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun skip(forward: Boolean) {
        scope.launch {
            val settings = prefs.settings.first()
            val amount = settings.seekIntervalSec * 1000L
            seekBy(if (forward) amount else -amount)
            flash(if (forward) "+${settings.seekIntervalSec}s" else "−${settings.seekIntervalSec}s")
        }
    }

    fun next() {
        val s = _state.value
        val next = PlaylistLogic.nextIndex(s.queue.size, s.index, s.shuffle, s.repeatMode, userNext = true)
        if (next != null) {
            player.seekTo(next, 0L)
            player.play()
        }
    }

    fun previous() {
        if (player.currentPosition > 4_000L) {
            seekTo(0L)
            return
        }
        val s = _state.value
        val prev = PlaylistLogic.previousIndex(s.queue.size, s.index, s.repeatMode)
        if (prev != null) {
            player.seekTo(prev, 0L)
            player.play()
        }
    }

    fun setSpeed(speed: Float) {
        val s = speed.coerceIn(0.25f, 4f)
        player.setPlaybackSpeed(s)
        publish { it.copy(speed = s) }
        flash("${s}x")
    }

    fun beginSpeedBoost() {
        boostFrom = player.playbackParameters.speed
        scope.launch {
            val target = prefs.settings.first().longPressSpeed
            setSpeed(target)
        }
    }

    fun endSpeedBoost() {
        setSpeed(boostFrom)
    }

    fun setRepeat(mode: RepeatMode) {
        player.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        publish { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffle
        player.shuffleModeEnabled = next
        publish { it.copy(shuffle = next) }
    }

    fun toggleMute() {
        val muted = player.volume > 0f
        player.volume = if (muted) 0f else 1f
        publish { it.copy(muted = muted) }
        flash(if (muted) "Muted" else "Sound on")
    }

    fun setAspect(mode: AspectMode) = publish { it.copy(aspect = mode) }

    fun setZoom(zoom: Float) {
        val z = zoom.coerceIn(1f, 8f)
        publish { it.copy(zoom = z) }
        flash("Zoom ${(z * 100).toInt()}%")
    }

    fun setLocked(locked: Boolean) = publish { it.copy(locked = locked) }

    fun setMiniPlayer(mini: Boolean) = publish { it.copy(miniPlayer = mini) }

    fun stopAndClear() {
        persistProgress(increment = false)
        player.stop()
        player.clearMediaItems()
        publish { PlayerUiState() }
        runCatching { context.stopService(Intent(context, AetherPlayerService::class.java)) }
    }

    fun frameStep(forward: Boolean) {
        player.pause()
        val fps = 30
        seekBy(if (forward) 1000L / fps else -1000L / fps)
    }

    fun setAbPoint(start: Boolean) {
        val pos = player.currentPosition
        val current = _state.value.abLoop
        val loop = if (start) {
            pos to (current?.second ?: Long.MAX_VALUE)
        } else {
            (current?.first ?: 0L) to pos
        }
        publish { it.copy(abLoop = loop) }
        flash(if (start) "A = ${com.aether.player.domain.TimeFormat.formatMs(pos)}" else "B = ${com.aether.player.domain.TimeFormat.formatMs(pos)}")
    }

    fun clearAbLoop() = publish { it.copy(abLoop = null) }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes == 0) {
            publish { it.copy(sleepRemainingMs = null) }
            return
        }
        if (minutes == -1) {
            publish { it.copy(sleepRemainingMs = -1L) }
            return
        }
        val total = minutes * 60_000L
        sleepJob = scope.launch {
            var left = total
            while (isActive && left > 0) {
                publish { it.copy(sleepRemainingMs = left) }
                delay(1000)
                left -= 1000
            }
            if (isActive) {
                player.pause()
                publish { it.copy(sleepRemainingMs = null) }
                flash("Sleep timer")
            }
        }
    }

    // ---------- chapters (timeline windows: queue items / multi-period streams) ----------

    fun currentChapters(): List<ChapterItem> {
        val tl = player.currentTimeline
        if (tl.isEmpty || tl.windowCount <= 1) return emptyList()
        val queue = _state.value.queue
        val w = Timeline.Window()
        return (0 until tl.windowCount).map { i ->
            tl.getWindow(i, w)
            val title = w.mediaItem.mediaMetadata.title?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: queue.getOrNull(i)?.title
                ?: "Part ${i + 1}"
            val dur = w.durationUs.takeIf { it != C.TIME_UNSET }?.div(1000L)
                ?: queue.getOrNull(i)?.durationMs
                ?: 0L
            ChapterItem(i, title, dur.coerceAtLeast(0L))
        }
    }

    fun chapterFractions(): List<Float> {
        val chapters = currentChapters()
        if (chapters.isEmpty()) return emptyList()
        val total = chapters.sumOf { it.durationMs }.takeIf { it > 0 } ?: return emptyList()
        var acc = 0L
        return chapters.dropLast(1).map { chapter ->
            acc += chapter.durationMs
            (acc / total.toFloat()).coerceIn(0f, 1f)
        }
    }

    fun seekToChapter(index: Int) {
        val tl = player.currentTimeline
        if (index in 0 until tl.windowCount) {
            player.seekTo(index, C.TIME_UNSET)
            player.play()
        }
    }

    // ---------- subtitles ----------

    fun loadExternalSubtitle(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        lastSubtitleUri = uri
        scope.launch {
            val effective = withContext(Dispatchers.IO) { applySubtitleDelay(uri) }
            val current = _state.value.current ?: return@launch
            val pos = player.currentPosition
            val play = player.playWhenReady
            val config = MediaItem.SubtitleConfiguration.Builder(effective)
                .setMimeType(guessSubtitleMime(effective))
                .setLanguage("und")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val item = MediaItem.Builder()
                .setUri(current.uri)
                .setMediaId(current.id)
                .setSubtitleConfigurations(listOf(config))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(current.title).build())
                .build()
            player.setMediaItem(item, pos)
            player.prepare()
            player.playWhenReady = play
            flash(if (effective != uri) "Subtitle loaded (delay applied)" else "Subtitle loaded")
        }
    }

    suspend fun readSubtitleText(): String? = withContext(Dispatchers.IO) {
        val uri = lastSubtitleUri ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.take(2_000_000)
        }.getOrNull()
    }

    fun lastSubtitleExtension(): String? {
        val raw = lastSubtitleUri?.toString()?.lowercase(Locale.US)?.substringBefore('?') ?: return null
        return raw.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }

    private fun applySubtitleDelay(uri: Uri): Uri {
        val delay = settingsCache.subtitleDelayMs
        if (delay == 0) return uri
        val name = uri.toString().lowercase(Locale.US).substringBefore('?')
        val isSrt = name.endsWith(".srt")
        val isVtt = name.endsWith(".vtt")
        if (!isSrt && !isVtt) return uri
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.take(2_000_000)
        }.getOrNull() ?: return uri
        val shifted = if (isSrt) SubtitleShift.shiftSrt(text, delay) else SubtitleShift.shiftVtt(text, delay)
        val file = File(context.cacheDir, "aether_sub_${System.currentTimeMillis()}.${if (isSrt) "srt" else "vtt"}")
        runCatching { file.writeText(shifted) }.onFailure { return uri }
        return Uri.fromFile(file)
    }

    fun selectAudioTrack(choice: TrackChoice?) {
        val params = trackSelector.buildUponParameters()
        if (choice == null) {
            trackSelector.parameters = params.clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()
            return
        }
        val tracks = player.currentTracks
        val groups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val group = groups.getOrNull(choice.groupIndex) ?: return
        trackSelector.parameters = params
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, choice.trackIndex),
            )
            .build()
    }

    fun selectTextTrack(choice: TrackChoice?) {
        val params = trackSelector.buildUponParameters()
        if (choice == null) {
            trackSelector.parameters = params
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
            return
        }
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val group = groups.getOrNull(choice.groupIndex) ?: return
        trackSelector.parameters = params
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, choice.trackIndex),
            )
            .build()
    }

    fun applyAudioPrefs() {
        val lang = settingsCache.defaultAudioLang
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(if (lang == "system" || lang.isBlank()) null else lang)
            .build()
    }

    /** Live-applies Settings → Performance by capping the picked video track. Never interrupts playback. */
    fun applyPerformanceMode(mode: com.aether.player.data.prefs.PerformanceMode) {
        val maxSize = when (mode) {
            com.aether.player.data.prefs.PerformanceMode.HIGH_QUALITY -> Int.MAX_VALUE
            com.aether.player.data.prefs.PerformanceMode.BALANCED -> 1920
            com.aether.player.data.prefs.PerformanceMode.BATTERY_SAVER -> 1280
        }
        runCatching {
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setMaxVideoSize(maxSize, maxSize)
                .build()
        }
    }

    // ---------- equalizer (best-effort, device dependent) ----------

    fun equalizerInfo(): EqInfo? {
        val eq = equalizer ?: return null
        return runCatching {
            EqInfo(eq.numberOfBands.toInt(), eq.bandLevelRange[0], eq.bandLevelRange[1])
        }.getOrNull()
    }

    fun applyEqualizerPreset(name: String) {
        val eq = equalizer ?: run {
            flash("Equalizer unavailable")
            return
        }
        runCatching {
            if (name == EQ_OFF) {
                eq.enabled = false
                flash("Equalizer off")
                return
            }
            val n = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val curve = eqCurves[name] ?: eqCurves[EQ_FLAT]!!
            eq.enabled = true
            for (band in 0 until n) {
                val pos = if (n == 1) 2f else band * 4f / (n - 1)
                val lo = pos.toInt().coerceIn(0, 3)
                val frac = curve[lo] + (curve[lo + 1] - curve[lo]) * (pos - lo)
                val level = if (frac >= 0) frac * range[1] else frac * -range[0]
                eq.setBandLevel(band.toShort(), level.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            flash("EQ: $name")
        }.onFailure {
            flash("Equalizer unavailable")
        }
    }

    // ---------- frame capture ----------

    /**
     * Captures the currently rendered frame. Primary path grabs the bitmap from the
     * live player view (exact on-screen frame, works for files and most streams); falls
     * back to MediaMetadataRetriever. Never throws — failures return Unavailable.
     */
    suspend fun captureFrame(surfaceHost: View?): CaptureResult {
        if (Build.VERSION.SDK_INT < 29) {
            return CaptureResult.Unavailable("Frame capture needs Android 10 or newer")
        }
        val video = state.value.current ?: return CaptureResult.Unavailable("Nothing is playing")
        val shot = runCatching { pixelCopyFrame(surfaceHost) }.getOrNull()
        val bitmap = shot ?: runCatching {
            withContext(Dispatchers.IO) { retrieverFrame(video.uri) }
        }.getOrNull() ?: return CaptureResult.Unavailable("This video does not allow frame capture")
        val scaled = runCatching { downscale(bitmap) }.getOrDefault(bitmap)
        val saved = runCatching { saveImageToGallery(scaled) }.getOrNull()
        runCatching { if (scaled !== bitmap) bitmap.recycle() }
        runCatching { scaled.recycle() }
        return if (saved != null) CaptureResult.Saved(saved)
        else CaptureResult.Unavailable("Storage unavailable")
    }

    private suspend fun pixelCopyFrame(host: View?): Bitmap? {
        if (host == null) return null
        return withContext(Dispatchers.Main) {
            val target = findSurfaceView(host)
            if (target is TextureView) runCatching { target.bitmap }.getOrNull() else null
        }
    }

    private fun findSurfaceView(host: View): View {
        if (host is SurfaceView || host is TextureView) return host
        if (host is android.view.ViewGroup) {
            for (i in 0 until host.childCount) {
                val found = findSurfaceView(host.getChildAt(i))
                if (found is SurfaceView || found is TextureView) return found
            }
        }
        return host
    }

    private fun retrieverFrame(uri: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            runCatching { retriever.setDataSource(context, Uri.parse(uri)) }.getOrThrow()
            val posUs = (if (player.currentPosition > 0) player.currentPosition else state.value.positionMs) * 1000L
            retriever.getFrameAtTime(posUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(0)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun downscale(src: Bitmap): Bitmap {
        val maxW = 1920
        if (src.width <= maxW) return src
        val h = (src.height.toLong() * maxW / src.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        return runCatching { Bitmap.createScaledBitmap(src, maxW, h, true) }.getOrDefault(src)
    }

    private suspend fun saveImageToGallery(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val name = "Aether_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Aether")
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val dest = context.contentResolver.insert(collection, values) ?: return@runCatching null
            context.contentResolver.openOutputStream(dest)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                    runCatching { context.contentResolver.delete(dest, null, null) }
                    return@runCatching null
                }
            } ?: return@runCatching null
            dest
        }.getOrNull()
    }

    fun retry() {
        val err = _state.value.error
        if (err != null) {
            publish { it.copy(error = null) }
            player.prepare()
            player.play()
        }
    }

    fun flash(label: String, progress: Float? = null) {
        _overlay.value = OverlayHint(label, progress)
        scope.launch {
            delay(850)
            if (_overlay.value?.label == label) _overlay.value = null
        }
    }

    fun persistProgress(increment: Boolean) {
        val current = _state.value.current ?: return
        val pos = player.currentPosition
        scope.launch {
            val settings = prefs.settings.first()
            library.recordPlayback(
                video = current.copy(durationMs = player.duration.takeIf { it > 0 } ?: current.durationMs),
                positionMs = pos,
                incrementPlay = increment,
                historyEnabled = settings.historyEnabled,
                incognito = settings.incognito,
                threshold = settings.completionThreshold,
            )
        }
    }

    fun addToQueue(video: VideoEntity, playNext: Boolean) {
        val s = _state.value
        val queue = s.queue.toMutableList()
        if (queue.isEmpty()) {
            playSingle(video)
            return
        }
        val insertAt = if (playNext) (s.index + 1).coerceAtMost(queue.size) else queue.size
        queue.add(insertAt, video)
        val media = video.toMediaItem()
        player.addMediaItem(insertAt, media)
        publish {
            it.copy(
                queue = queue,
                hasNext = it.index < queue.lastIndex,
            )
        }
    }

    fun playQueueIndex(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        player.seekTo(index, 0L)
        player.play()
    }

    fun removeFromQueue(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || s.queue.size <= 1) return
        val queue = s.queue.toMutableList()
        queue.removeAt(index)
        runCatching { player.removeMediaItem(index) }
        val now = player.currentMediaItemIndex.coerceIn(0, queue.lastIndex)
        publish {
            it.copy(
                queue = queue,
                index = now,
                current = queue.getOrNull(now),
                hasNext = now < queue.lastIndex,
                hasPrevious = now > 0,
            )
        }
    }

    fun clearUpNext() {
        val s = _state.value
        val keep = s.queue.take(s.index + 1)
        if (keep.size == s.queue.size) return
        for (i in s.queue.lastIndex downTo s.index + 1) {
            runCatching { player.removeMediaItem(i) }
        }
        publish { it.copy(queue = keep, hasNext = false) }
    }

    private fun ensureTicker() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val duration = player.duration.coerceAtLeast(0L)
                val pos = player.currentPosition
                val buffered = player.bufferedPosition
                val ab = _state.value.abLoop
                if (ab != null && ab.second != Long.MAX_VALUE && pos >= ab.second) {
                    player.seekTo(ab.first)
                }
                publish {
                    it.copy(
                        positionMs = pos,
                        durationMs = duration,
                        bufferedMs = buffered,
                        playing = player.isPlaying,
                    )
                }
                val now = System.currentTimeMillis()
                if (now - lastSavedAt > 8_000L && player.isPlaying) {
                    lastSavedAt = now
                    persistProgress(increment = false)
                }
                delay(250)
            }
        }
    }

    private suspend fun onEnded() {
        persistProgress(increment = false)
        val settings = prefs.settings.first()
        if (_state.value.sleepRemainingMs == -1L) {
            player.pause()
            publish { it.copy(sleepRemainingMs = null) }
            return
        }
        if (settings.autoPlayNext) next()
    }

    private fun startService() {
        val intent = Intent(context, AetherPlayerService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    private fun requestSessionRebuild() {
        val intent = Intent(context, AetherPlayerService::class.java)
            .setAction(AetherPlayerService.ACTION_REBUILD)
        runCatching { context.startService(intent) }
    }

    private fun attachEqualizer() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching {
            equalizer = Equalizer(0, player.audioSessionId).apply { enabled = false }
        }
        if (boostOn) {
            runCatching { booster?.release() }
            booster = runCatching {
                LoudnessEnhancer(player.audioSessionId).apply {
                    enabled = true
                    setTargetGain(900)
                }
            }.getOrNull()
            if (booster == null) boostOn = false
        }
    }

    // ---------- equalizer bands + volume boost ----------

    /** Current band levels in millibels, or null when the device effect is unavailable. */
    fun bandLevels(): List<Short>? {
        val eq = equalizer ?: return null
        return runCatching {
            (0 until eq.numberOfBands.toInt()).map { eq.getBandLevel(it.toShort()) }
        }.getOrNull()
    }

    fun setBandLevel(band: Int, level: Short) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = true
            eq.setBandLevel(band.toShort(), level)
        }
    }

    private var booster: LoudnessEnhancer? = null
    private var boostOn: Boolean = false

    fun isVolumeBoosted(): Boolean = boostOn && booster != null

    fun setVolumeBoost(on: Boolean) {
        runCatching { booster?.release() }
        booster = null
        boostOn = false
        if (!on) {
            flash("Boost off")
            return
        }
        runCatching {
            booster = LoudnessEnhancer(player.audioSessionId).apply {
                enabled = true
                setTargetGain(900)
            }
            boostOn = true
            flash("Volume boost on")
        }.onFailure {
            booster = null
            flash("Boost unavailable on this device")
        }
    }

    private fun audioChoices(tracks: Tracks): List<TrackChoice> {
        val result = mutableListOf<TrackChoice>()
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.forEachIndexed { gIndex, group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                result += TrackChoice(
                    groupIndex = gIndex,
                    trackIndex = i,
                    label = format.label
                        ?: format.language?.uppercase(Locale.US)
                        ?: "Audio ${gIndex + 1}.${i + 1}",
                    language = format.language,
                    selected = group.isTrackSelected(i),
                )
            }
        }
        return result
    }

    private fun textChoices(tracks: Tracks): List<TrackChoice> {
        val result = mutableListOf<TrackChoice>()
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.forEachIndexed { gIndex, group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                result += TrackChoice(
                    groupIndex = gIndex,
                    trackIndex = i,
                    label = format.label ?: format.language ?: "Subtitle ${gIndex + 1}.${i + 1}",
                    language = format.language,
                    selected = group.isTrackSelected(i),
                )
            }
        }
        return result
    }

    private fun mapError(error: PlaybackException): PlaybackError {
        val title = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network unavailable"
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found"
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Permission denied"
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> "Codec unavailable"
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "Unsupported format"
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Stream unavailable"
            else -> "Playback error"
        }
        return PlaybackError(title, error.message ?: error.errorCodeName, error.errorCode)
    }

    private fun guessSubtitleMime(uri: Uri): String {
        val name = uri.toString().lowercase(Locale.US)
        return when {
            name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            name.endsWith(".ssa") || name.endsWith(".ass") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    private fun VideoEntity.toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(folderName)
                    .build(),
            )
        // Explicit MIME helps adaptive streams whose type can't be sniffed from the URL.
        when (com.aether.player.domain.UrlValidator.extensionOf(uri)) {
            "m3u8" -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            "mpd" -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        return builder.build()
    }

    private inline fun publish(block: (PlayerUiState) -> PlayerUiState) {
        _state.value = block(_state.value)
    }

    fun volumePercent(audio: AudioManager): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / max
    }

    private class AetherLoadPolicy(
        delegate: DefaultLoadErrorHandlingPolicy,
        private val maxRetries: Int,
    ) : LoadErrorHandlingPolicy by delegate {
        override fun getMinimumLoadableRetryCount(dataType: Int): Int = maxRetries
    }

    companion object {
        const val EQ_OFF = "Off"
        const val EQ_FLAT = "Flat"
        val EQ_PRESETS = listOf("Off", "Flat", "Bass", "Treble", "Vocal", "Movie")

        /** 5-point curves in -1..1, interpolated across device bands. */
        private val eqCurves = mapOf(
            EQ_FLAT to floatArrayOf(0f, 0f, 0f, 0f, 0f),
            "Bass" to floatArrayOf(0.8f, 0.5f, 0f, -0.3f, -0.4f),
            "Treble" to floatArrayOf(-0.4f, -0.2f, 0.1f, 0.5f, 0.8f),
            "Vocal" to floatArrayOf(-0.3f, 0f, 0.5f, 0.6f, 0.1f),
            "Movie" to floatArrayOf(0.7f, 0.1f, -0.2f, 0.1f, 0.6f),
        )

        @Volatile
        private var instance: PlayerManager? = null

        fun get(context: Context, library: LibraryRepository, prefs: UserPreferences): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext, library, prefs).also { instance = it }
            }
        }
    }
}
