package com.aether.player.ui.player

import android.net.Uri
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.ai.AiClient
import com.aether.player.data.db.BookmarkEntity
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.library.StreamMeta
import com.aether.player.data.prefs.AiProvider
import com.aether.player.data.prefs.AspectMode
import com.aether.player.domain.AiCommand
import com.aether.player.domain.AiCommandParser
import com.aether.player.domain.BookmarkMoment
import com.aether.player.domain.RepeatMode
import com.aether.player.domain.SubtitleShift
import com.aether.player.domain.TimeFormat
import com.aether.player.playback.ChapterItem
import com.aether.player.playback.EqInfo
import com.aether.player.playback.PlayerManager
import com.aether.player.playback.PlayerUiState
import com.aether.player.playback.TrackChoice
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassSettingsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreSheet(
    state: PlayerUiState,
    eqInfo: EqInfo?,
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
    onChapters: () -> Unit,
    onBookmarks: () -> Unit,
    onAi: () -> Unit,
    onCapture: () -> Unit,
    onEqPreset: (String) -> Unit,
) {
    var eqSelected by remember { mutableStateOf(PlayerManager.EQ_FLAT) }
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
            if (eqInfo != null) {
                Spacer(Modifier.height(16.dp))
                Text("Equalizer (${eqInfo.bands} bands)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerManager.EQ_PRESETS.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { preset ->
                                GlassChip(preset, eqSelected == preset) {
                                    eqSelected = preset
                                    onEqPreset(preset)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassSettingsRow("Chapters", subtitle = "Queue parts / stream periods", onClick = onChapters)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Bookmarks", subtitle = "Jump to saved moments", onClick = onBookmarks)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("AI assistant", subtitle = "Explain, ask, translate, control", onClick = onAi)
            Spacer(Modifier.height(8.dp))
            GlassSettingsRow("Capture frame", subtitle = "Save the current frame as PNG", onClick = onCapture)
            Spacer(Modifier.height(8.dp))
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
fun ChaptersSheet(
    chapters: List<ChapterItem>,
    currentIndex: Int,
    onSeek: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Chapters", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (chapters.isEmpty()) "No chapters detected for this video."
                else "${chapters.size} parts — tap to jump.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            chapters.forEach { chapter ->
                GlassSettingsRow(
                    title = "${chapter.index + 1}. ${chapter.title}",
                    subtitle = TimeFormat.formatMs(chapter.durationMs) +
                        if (chapter.index == currentIndex) " · playing" else "",
                    onClick = { onSeek(chapter.index) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    videoId: String,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AetherApp
    val bookmarks by app.container.library.bookmarks(videoId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var renaming: BookmarkEntity? by remember { mutableStateOf(null) }
    var renameText by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Bookmarks", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (bookmarks.isEmpty()) "No bookmarks yet — save one from More → Bookmark."
                else "Tap to jump to a moment.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            bookmarks.forEach { bookmark ->
                GlassSettingsRow(
                    title = "${TimeFormat.formatMs(bookmark.positionMs)} — ${bookmark.title}",
                    onClick = { onSeek(bookmark.positionMs) },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                renaming = bookmark
                                renameText = bookmark.title
                            }) { Text("Rename") }
                            TextButton(onClick = {
                                scope.launch { app.container.library.deleteBookmark(bookmark.id) }
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename bookmark") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        app.container.library.renameBookmark(target.id, renameText.ifBlank { target.title })
                        renaming = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSheet(
    pm: PlayerManager,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val assistant = app.container.aiAssistant
    val client = app.container.aiClient
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var targetLang by remember { mutableStateOf("English") }
    var status by remember { mutableStateOf("Checking AI status…") }

    LaunchedEffect(Unit) {
        status = runCatching {
            val c = client.config()
            when {
                c.provider == AiProvider.OFF -> "AI is off — enable it in Settings → AI assistant."
                c.apiKey.isBlank() -> "${AiClient.label(c.provider)} selected, but no API key is saved yet."
                else -> "${AiClient.label(c.provider)} · ${c.model} — ready."
            }
        }.getOrElse { "AI unavailable: ${it.message}" }
    }

    suspend fun snapshot(): Triple<VideoEntity, String, List<BookmarkMoment>>? {
        val video = pm.state.value.current ?: return null
        val entities = app.container.library.bookmarks(video.id).first()
        val moments = entities.map { BookmarkMoment(it.id, it.videoId, it.positionMs, it.title) }
        val duration = pm.player.duration.takeIf { it > 0 } ?: video.durationMs
        val ctx = assistant.buildContext(video, moments, pm.currentChapters(), pm.player.currentPosition, duration)
        return Triple(video, ctx, moments)
    }

    fun runOp(label: String, block: suspend () -> String) {
        scope.launch {
            busy = true
            output = "$label…"
            output = runCatching { block() }.getOrElse { "Error: ${it.message}" }
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("AI assistant", style = MaterialTheme.typography.headlineMedium)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Explain") {
                    runOp("Explaining") {
                        val (_, ctx, _) = snapshot() ?: return@runOp "Nothing is playing."
                        assistant.explainVideo(ctx)
                    }
                }
                GlassButton("Summarize") {
                    runOp("Summarizing") {
                        val (_, ctx, _) = snapshot() ?: return@runOp "Nothing is playing."
                        assistant.summarize(ctx)
                    }
                }
                GlassButton("Chapters") {
                    runOp("Proposing chapters") {
                        val (_, ctx, _) = snapshot() ?: return@runOp "Nothing is playing."
                        val res = assistant.suggestChapters(ctx)
                        if (res.trim() == "NO_BOOKMARKS") {
                            "Add bookmarks first — chapter titles are proposed from your bookmarks."
                        } else {
                            res
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask about this video") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            GlassButton("Ask", filled = true, modifier = Modifier.fillMaxWidth()) {
                val q = question
                runOp("Answering") {
                    if (q.isBlank()) return@runOp "Type a question first."
                    val (_, ctx, _) = snapshot() ?: return@runOp "Nothing is playing."
                    assistant.ask(q, ctx)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Command — e.g. jump to 12:30, pause, 1.5x") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            GlassButton("Run command", filled = true, modifier = Modifier.fillMaxWidth()) {
                val input = command
                runOp("Running") {
                    if (input.isBlank()) return@runOp "Type a command first."
                    val snap = snapshot() ?: return@runOp "Nothing is playing."
                    val duration = pm.player.duration.takeIf { it > 0 } ?: snap.first.durationMs
                    val local = AiCommandParser.parseLocal(input, duration, snap.third)
                    val cmd = local ?: assistant.parseCommand(input, snap.second, duration)
                    if (cmd == null) "I couldn't understand that command." else executeAiCommand(pm, cmd)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = targetLang,
                onValueChange = { targetLang = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Translate loaded subtitles to…") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            GlassButton("Translate subtitles", modifier = Modifier.fillMaxWidth()) {
                val lang = targetLang
                runOp("Translating") {
                    val raw = pm.readSubtitleText()
                        ?: return@runOp "Load a subtitle file first (Subtitles → Load subtitle file)."
                    val ext = pm.lastSubtitleExtension()
                    if (ext == "vtt") {
                        val (header, cues) = SubtitleShift.parseVttTextBlocks(raw)
                        if (cues.isEmpty()) return@runOp "No cues found in this subtitle file."
                        val translated = assistant.translateTexts(cues.map { it.lines.joinToString("\n") }, lang)
                        val rebuilt = SubtitleShift.buildVtt(
                            header,
                            cues.mapIndexed { i, cue -> cue.copy(lines = translated[i].lines()) },
                        )
                        saveTranslated(app, pm, rebuilt, "vtt", lang)
                    } else {
                        val cues = SubtitleShift.parseSrt(raw)
                        if (cues.isEmpty()) return@runOp "Could not parse this file (SRT / VTT supported)."
                        val translated = assistant.translateTexts(cues.map { it.lines.joinToString("\n") }, lang)
                        val rebuilt = SubtitleShift.buildSrt(
                            cues.mapIndexed { i, cue -> cue.copy(lines = translated[i].lines()) },
                        )
                        saveTranslated(app, pm, rebuilt, "srt", lang)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (busy) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
            }
            if (output.isNotBlank()) {
                SelectionContainer {
                    Text(output, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private suspend fun saveTranslated(
    app: AetherApp,
    pm: PlayerManager,
    content: String,
    ext: String,
    lang: String,
): String = withContext(Dispatchers.IO) {
    val file = File(app.cacheDir, "aether_translated_${System.currentTimeMillis()}.$ext")
    runCatching { file.writeText(content) }
        .onFailure { return@withContext "Could not save translated subtitles." }
    pm.loadExternalSubtitle(Uri.fromFile(file))
    "Translated subtitles loaded ($lang)."
}

private fun executeAiCommand(pm: PlayerManager, cmd: AiCommand): String = when (cmd) {
    is AiCommand.Seek -> {
        pm.seekTo(cmd.positionMs)
        "Seeking to ${TimeFormat.formatMs(cmd.positionMs)}"
    }
    AiCommand.Play -> {
        if (!pm.player.isPlaying) pm.playPause()
        "Playing"
    }
    AiCommand.Pause -> {
        if (pm.player.isPlaying) pm.playPause()
        "Paused"
    }
    AiCommand.Next -> {
        pm.next()
        "Next"
    }
    AiCommand.Previous -> {
        pm.previous()
        "Previous"
    }
    is AiCommand.Speed -> {
        pm.setSpeed(cmd.value)
        "Speed ${cmd.value}x"
    }
    is AiCommand.Subtitles -> {
        if (!cmd.enabled) {
            pm.selectTextTrack(null)
            "Subtitles off"
        } else {
            val first = pm.state.value.textTracks.firstOrNull()
            if (first != null) {
                pm.selectTextTrack(first)
                "Subtitles on (${first.label})"
            } else {
                "No subtitle tracks found"
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
    val app = LocalContext.current.applicationContext as AetherApp
    var meta by remember { mutableStateOf<StreamMeta?>(null) }
    LaunchedEffect(video.id, video.uri) {
        meta = app.container.mediaOps.probe(video.uri)
    }
    val w = state.videoWidth.takeIf { it > 0 } ?: video.width
    val h = state.videoHeight.takeIf { it > 0 } ?: video.height
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
            InfoRow("Resolution", TimeFormat.prettyResolution(w, h))
            if (w > 0 && h > 0) InfoRow("Aspect ratio", aspectLabel(w, h))
            meta?.videoCodec?.let { InfoRow("Video codec", it) }
            meta?.audioCodec?.let { codec ->
                val extra = listOfNotNull(
                    meta?.channels?.let { "$it ch" },
                    meta?.sampleRateHz?.let { "${it / 1000} kHz" },
                ).joinToString(" · ").ifBlank { null }
                InfoRow("Audio codec", if (extra != null) "$codec ($extra)" else codec)
            }
            meta?.fps?.let { InfoRow("Frame rate", "≈${"%.1f".format(it)} fps") }
            InfoRow("Container", video.mimeType ?: "Unknown")
            InfoRow("Bitrate", if (video.bitrate > 0) "${video.bitrate / 1000} kbps" else "Unknown")
            InfoRow("Audio tracks", state.audioTracks.joinToString { it.label }.ifBlank { "Embedded / unknown" })
            InfoRow("Subtitles", state.textTracks.joinToString { it.label }.ifBlank { "None detected" })
            InfoRow("Played", "${video.playCount} times")
        }
    }
}

private fun aspectLabel(w: Int, h: Int): String {
    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val d = gcd(w, h).coerceAtLeast(1)
    return "${w / d}:${h / d}"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { }) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
