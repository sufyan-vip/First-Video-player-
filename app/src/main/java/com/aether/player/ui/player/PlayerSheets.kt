package com.aether.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.prefs.AspectMode
import com.aether.player.domain.RepeatMode
import com.aether.player.domain.TimeFormat
import com.aether.player.playback.PlayerUiState
import com.aether.player.playback.TrackChoice
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassSettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onSpeed: (Float) -> Unit,
    onAspect: (AspectMode) -> Unit,
    onSleep: (Int) -> Unit,
    onLock: () -> Unit,
    onInfo: () -> Unit,
    onSubtitleFile: () -> Unit,
    onAbA: () -> Unit,
    onAbB: () -> Unit,
    onAbClear: () -> Unit,
    onFrame: () -> Unit,
    onRepeat: (RepeatMode) -> Unit,
    onShuffle: () -> Unit,
    onBookmark: () -> Unit,
    onRotate: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Playback", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                    GlassChip("${speed}x", state.speed == speed) { onSpeed(speed) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Aspect", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AspectMode.entries.take(5).forEach { mode ->
                    GlassChip(mode.name.lowercase(), state.aspect == mode) { onAspect(mode) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Sleep timer", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 15, 30, 45, 60, -1).forEach { m ->
                    GlassChip(if (m == -1) "End" else if (m == 0) "Off" else "${m}m", false) { onSleep(m) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatMode.entries.forEach { mode ->
                    GlassChip(mode.name.lowercase(), state.repeatMode == mode) { onRepeat(mode) }
                }
                GlassChip("Shuffle", state.shuffle) { onShuffle() }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("A") { onAbA() }
                GlassButton("B") { onAbB() }
                GlassButton("Clear A-B") { onAbClear() }
                GlassButton("Frame +") { onFrame() }
            }
            Spacer(Modifier.height(12.dp))
            GlassSettingsRow("Bookmark this moment", onClick = onBookmark)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Lock screen", onClick = onLock)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Rotate", onClick = onRotate)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Load subtitle file", onClick = onSubtitleFile)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Video information", onClick = onInfo)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSheet(
    title: String,
    tracks: List<TrackChoice>,
    onPick: (TrackChoice) -> Unit,
    onOff: () -> Unit,
    onDismiss: () -> Unit,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            GlassSettingsRow("Off / default", onClick = onOff)
            tracks.forEach { track ->
                Spacer(Modifier.height(8.dp))
                GlassSettingsRow(
                    title = track.label,
                    subtitle = track.language,
                    onClick = { onPick(track) },
                )
            }
            if (extra != null) {
                Spacer(Modifier.height(12.dp))
                extra()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoSheet(
    video: VideoEntity,
    state: PlayerUiState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Information", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            InfoRow("Name", video.title)
            InfoRow("Location", video.path ?: video.uri)
            InfoRow("Size", TimeFormat.prettyBytes(video.sizeBytes))
            InfoRow("Duration", TimeFormat.formatMs(state.durationMs.takeIf { it > 0 } ?: video.durationMs))
            InfoRow("Resolution", TimeFormat.prettyResolution(state.videoWidth.takeIf { it > 0 } ?: video.width, state.videoHeight.takeIf { it > 0 } ?: video.height))
            InfoRow("Container", video.mimeType ?: "Unknown")
            InfoRow("Bitrate", if (video.bitrate > 0) "${video.bitrate / 1000} kbps" else "Unknown")
            InfoRow("Audio tracks", state.audioTracks.joinToString { it.label }.ifBlank { "Embedded / unknown" })
            InfoRow("Subtitles", state.textTracks.joinToString { it.label }.ifBlank { "None detected" })
            InfoRow("Played", "${video.playCount} times")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { }) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
