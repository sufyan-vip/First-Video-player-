package com.aether.player.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.library.LibraryRepository
import com.aether.player.data.prefs.AspectMode
import com.aether.player.data.prefs.UserPreferences
import com.aether.player.domain.PlaylistLogic
import com.aether.player.domain.RepeatMode
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
import java.util.Locale

class PlayerManager(
    private val context: Context,
    private val library: LibraryRepository,
    private val prefs: UserPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val trackSelector = DefaultTrackSelector(context)
    val player: ExoPlayer = ExoPlayer.Builder(context)
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
                .setBufferDurationsMs(15_000, 50_000, 1_500, 5_000)
                .build(),
        )
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private val _overlay = MutableStateFlow<OverlayHint?>(null)
    val overlay: StateFlow<OverlayHint?> = _overlay

    private var sleepJob: Job? = null
    private var progressJob: Job? = null
    private var boostFrom: Float = 1f
    private var equalizer: Equalizer? = null
    private var lastSavedAt = 0L

    init {
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
        attachEqualizer()
        ensureTicker()
        startService()
    }

    fun playQueue(items: List<VideoEntity>, startIndex: Int, startPositionMs: Long? = null) {
        if (items.isEmpty()) return
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

    fun loadExternalSubtitle(uri: Uri) {
        val current = _state.value.current ?: return
        val pos = player.currentPosition
        val play = player.playWhenReady
        val config = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(guessSubtitleMime(uri))
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
        flash("Subtitle loaded")
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

    fun setAudioDelayUs(delayUs: Long) {
        player.setSkipSilenceEnabled(false)
        runCatching {
            player.trackSelectionParameters = player.trackSelectionParameters
        }
        flash("Audio delay ${delayUs / 1000}ms")
    }

    fun applyEqualizerPreset(bandLevels: ShortArray?) {
        val eq = equalizer ?: return
        runCatching {
            if (bandLevels == null) {
                eq.enabled = false
                return
            }
            eq.enabled = true
            bandLevels.forEachIndexed { index, level ->
                if (index < eq.numberOfBands) {
                    eq.setBandLevel(index.toShort(), level)
                }
            }
        }
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

    private fun attachEqualizer() {
        runCatching {
            equalizer = Equalizer(0, player.audioSessionId).apply { enabled = false }
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
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> "Network unavailable"
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found"
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Permission denied"
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            -> "Codec unavailable"
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            -> "Unsupported format"
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            -> "Stream unavailable"
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

    private fun VideoEntity.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(folderName)
                    .build(),
            )
            .build()

    private inline fun publish(block: (PlayerUiState) -> PlayerUiState) {
        _state.value = block(_state.value)
    }

    fun volumePercent(audio: AudioManager): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / max
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun get(context: Context, library: LibraryRepository, prefs: UserPreferences): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext, library, prefs).also { instance = it }
            }
        }
    }
}
