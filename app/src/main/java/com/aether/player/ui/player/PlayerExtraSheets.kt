package com.aether.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.data.library.StreamMeta
import com.aether.player.domain.TimeFormat
import com.aether.player.playback.PlayerManager
import com.aether.player.playback.PlayerUiState
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassSettingsRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(pm: PlayerManager, onDismiss: () -> Unit) {
    val state by pm.state.collectAsState()
    var value by remember(state.speed) { mutableFloatStateOf(state.speed) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).padding(bottom = 32.dp)) {
            Text("Playback speed", style = MaterialTheme.typography.headlineMedium)
            Text(
                "%.2fx".format(value),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0.25f..4f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { preset ->
                    GlassChip("${preset}x", value == preset) { value = preset }
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassButton("Apply", filled = true, modifier = Modifier.fillMaxWidth()) {
                pm.setSpeed(value)
                onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(pm: PlayerManager, onDismiss: () -> Unit) {
    val state by pm.state.collectAsState()
    var custom by remember { mutableStateOf("") }
    val current = state.sleepRemainingMs
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Sleep timer", style = MaterialTheme.typography.headlineMedium)
            Text(
                when {
                    current == null -> "Off"
                    current == -1L -> "Pauses at the end of this video"
                    else -> "Pauses in ${TimeFormat.formatMs(current)}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            listOf(0, 5, 10, 15, 30, 45, 60, -1).chunked(4).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    row.forEach { m ->
                        val label = if (m == -1) "End" else if (m == 0) "Off" else "${m}m"
                        GlassChip(label, false) {
                            pm.startSleepTimer(m)
                            pm.flash("Sleep: ${TimeFormat.sleepLabel(m)}")
                            onDismiss()
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Custom minutes") },
                    singleLine = true,
                )
                GlassButton("Start") {
                    val minutes = custom.toIntOrNull() ?: 0
                    pm.startSleepTimer(minutes)
                    pm.flash("Sleep: ${TimeFormat.sleepLabel(minutes)}")
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqSheet(pm: PlayerManager, onDismiss: () -> Unit) {
    val info = remember { pm.equalizerInfo() }
    var selected by remember { mutableStateOf<String?>(null) }
    var levels by remember { mutableStateOf(pm.bandLevels() ?: emptyList()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Equalizer", style = MaterialTheme.typography.headlineMedium)
            if (info == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Equalizer unavailable on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                EqBody(pm = pm, bands = info.bands, minLevel = info.minLevel, maxLevel = info.maxLevel)
            }
        }
    }
}

@Composable
private fun EqBody(pm: PlayerManager, bands: Int, minLevel: Short, maxLevel: Short) {
    var selected by remember { mutableStateOf<String?>(null) }
    var levels by remember { mutableStateOf(pm.bandLevels() ?: emptyList()) }
    Column {
        Text(
            "$bands bands · device audio effect",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        PlayerManager.EQ_PRESETS.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                row.forEach { preset ->
                    GlassChip(preset, selected == preset) {
                        selected = preset
                        pm.applyEqualizerPreset(preset)
                        levels = pm.bandLevels() ?: levels
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        levels.forEachIndexed { index, level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Band ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.35f),
                )
                Slider(
                    value = level.toFloat(),
                    onValueChange = { v ->
                        val next = levels.toMutableList()
                        next[index] = v.toInt().toShort()
                        levels = next
                        pm.setBandLevel(index, v.toInt().toShort())
                        selected = "Custom"
                    },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1f dB".format(level / 100f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(0.4f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(pm: PlayerManager, onDismiss: () -> Unit) {
    val state by pm.state.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Up next (${state.queue.size})", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { pm.clearUpNext() }) { Text("Clear up-next") }
            }
            Spacer(Modifier.height(8.dp))
            if (state.queue.isEmpty()) {
                Text("Queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.queue.forEachIndexed { index, video ->
                val playing = index == state.index
                GlassSettingsRow(
                    title = "${if (playing) "▶ " else ""}${index + 1}. ${video.title}",
                    subtitle = TimeFormat.formatMs(video.durationMs) + if (playing) " · playing" else "",
                    onClick = {
                        pm.playQueueIndex(index)
                        onDismiss()
                    },
                    trailing = {
                        TextButton(onClick = { pm.removeFromQueue(index) }) { Text("Remove") }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSheet(videoId: String, onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as AetherApp
    val playlists by app.container.library.playlists().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Add to playlist", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            if (playlists.isEmpty()) {
                Text(
                    "No playlists yet — create one below.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            playlists.forEach { pl ->
                GlassSettingsRow(
                    title = pl.name,
                    onClick = {
                        scope.launch { app.container.library.addToPlaylist(pl.id, videoId) }
                        onDismiss()
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New playlist name") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            GlassButton("Create & add", filled = true, modifier = Modifier.fillMaxWidth()) {
                if (name.isBlank()) return@GlassButton
                scope.launch {
                    val id = app.container.library.createPlaylist(name.trim())
                    app.container.library.addToPlaylist(id, videoId)
                }
                onDismiss()
            }
        }
    }
}

@Composable
fun StatsCard(state: PlayerUiState, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as AetherApp
    val video = state.current
    var meta by remember { mutableStateOf<StreamMeta?>(null) }
    LaunchedEffect(video?.id, video?.uri) {
        meta = video?.let { app.container.mediaOps.probe(it.uri) }
    }
    val card = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .padding(horizontal = 32.dp)
            .clip(card)
            .background(Color(0xE6141418))
            .border(1.dp, Color.White.copy(alpha = 0.14f), card)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Playback stats", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        StatRow("Resolution", TimeFormat.prettyResolution(state.videoWidth, state.videoHeight))
        StatRow("Container", video?.mimeType ?: "Unknown")
        meta?.videoCodec?.let { StatRow("Video codec", it) }
        meta?.audioCodec?.let { StatRow("Audio codec", it) }
        meta?.fps?.let { StatRow("Frame rate", "≈%.1f fps".format(it)) }
        StatRow("Speed", "${state.speed}x")
        StatRow("Buffered", TimeFormat.formatMs(state.bufferedMs))
        StatRow("Position", "${TimeFormat.formatMs(state.positionMs)} / ${TimeFormat.formatMs(state.durationMs)}")
        Spacer(Modifier.height(12.dp))
        GlassButton("Close", filled = true, modifier = Modifier.fillMaxWidth(), onClick = onClose)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
