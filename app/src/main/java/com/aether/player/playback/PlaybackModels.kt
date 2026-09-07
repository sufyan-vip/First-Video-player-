package com.aether.player.playback

import com.aether.player.data.db.VideoEntity
import com.aether.player.data.prefs.AspectMode
import com.aether.player.domain.RepeatMode

data class TrackChoice(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val selected: Boolean,
)

data class PlayerUiState(
    val current: VideoEntity? = null,
    val queue: List<VideoEntity> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val aspect: AspectMode = AspectMode.FIT,
    val zoom: Float = 1f,
    val locked: Boolean = false,
    val sleepRemainingMs: Long? = null,
    val error: PlaybackError? = null,
    val audioTracks: List<TrackChoice> = emptyList(),
    val textTracks: List<TrackChoice> = emptyList(),
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val abLoop: Pair<Long, Long>? = null,
    val miniPlayer: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val playWhenReady: Boolean = false,
)

data class PlaybackError(
    val title: String,
    val detail: String,
    val code: Int = 0,
)

data class OverlayHint(
    val label: String,
    val progress: Float? = null,
)
