package com.aether.player.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.Coil
import com.aether.player.AetherApp
import com.aether.player.BuildConfig
import com.aether.player.ai.AiClient
import com.aether.player.data.prefs.AccentColor
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.AiProvider
import com.aether.player.data.prefs.AspectMode
import com.aether.player.data.prefs.BufferProfile
import com.aether.player.data.prefs.PerformanceMode
import com.aether.player.data.prefs.SecureStore
import com.aether.player.data.prefs.ThemeMode
import com.aether.player.data.prefs.TimelineStyle
import com.aether.player.domain.TimeFormat
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassSliderRow
import com.aether.player.ui.components.GlassToggleRow
import com.aether.player.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val prefs = app.container.preferences
    val settings by prefs.settings.collectAsState(initial = AetherSettings())
    val savedUrls by app.container.library.savedUrls().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    fun update(block: (AetherSettings) -> AetherSettings) {
        scope.launch { prefs.update(block) }
    }
    var cacheSize by remember { mutableStateOf<String?>(null) }
    fun refreshCache() {
        scope.launch(Dispatchers.IO) {
            cacheSize = TimeFormat.prettyBytes(dirSize(app.cacheDir))
        }
    }
    LaunchedEffect(Unit) { refreshCache() }
    var apiKey by remember { mutableStateOf(app.container.secure.get(SecureStore.AI_API_KEY)) }
    var aiTest by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 96.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Glass, playback, privacy — all local.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionHeader("Appearance")
        ChipRow(ThemeMode.entries.map { it.name }, settings.themeMode.name) {
            update { s -> s.copy(themeMode = ThemeMode.valueOf(it)) }
        }
        ChipRow(AccentColor.entries.map { it.name }, settings.accent.name) {
            update { s -> s.copy(accent = AccentColor.valueOf(it)) }
        }
        GlassToggleRow("System dynamic color", "Uses wallpaper colors when Accent is SYSTEM", settings.dynamicColor) {
            update { s -> s.copy(dynamicColor = it) }
        }
        GlassSliderRow("Glass intensity", settings.glassIntensity, { update { s -> s.copy(glassIntensity = it) } }, "${(settings.glassIntensity * 100).toInt()}%")
        GlassSliderRow("Blur intensity", settings.blurIntensity, { update { s -> s.copy(blurIntensity = it) } }, "${(settings.blurIntensity * 100).toInt()}%")
        GlassSliderRow("Animation scale", settings.animationScale / 2f, {
            update { s -> s.copy(animationScale = (it * 2f).coerceIn(0.25f, 2f)) }
        }, "%.2f".format(settings.animationScale))
        GlassToggleRow("Reduce motion", "Disables UI animation", settings.reduceMotion) {
            update { s -> s.copy(reduceMotion = it) }
        }
        GlassToggleRow("High contrast", "Stronger text and borders", settings.highContrast) {
            update { s -> s.copy(highContrast = it) }
        }
        GlassToggleRow("Compact mode", "Denser grids", settings.compactMode) { update { s -> s.copy(compactMode = it) } }

        SectionHeader("Playback")
        ChipRow(listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), "${settings.defaultSpeed}x".replace(".0x", "x")) { label ->
            val v = label.removeSuffix("x").toFloat()
            update { s -> s.copy(defaultSpeed = v) }
        }
        GlassToggleRow("Auto-play next", null, settings.autoPlayNext) { update { s -> s.copy(autoPlayNext = it) } }
        GlassToggleRow("Resume playback", null, settings.resumePlayback) { update { s -> s.copy(resumePlayback = it) } }
        GlassToggleRow("Background playback", "Keep audio playing when the app is hidden", settings.backgroundPlayback) { update { s -> s.copy(backgroundPlayback = it) } }
        GlassToggleRow("Keep screen awake", null, settings.keepScreenAwake) { update { s -> s.copy(keepScreenAwake = it) } }
        GlassSliderRow("Seek interval", settings.seekIntervalSec / 30f, {
            update { s -> s.copy(seekIntervalSec = (it * 30).toInt().coerceIn(5, 30)) }
        }, "${settings.seekIntervalSec}s")
        GlassSliderRow("Double-tap interval", settings.doubleTapIntervalSec / 30f, {
            update { s -> s.copy(doubleTapIntervalSec = (it * 30).toInt().coerceIn(5, 30)) }
        }, "${settings.doubleTapIntervalSec}s")

        SectionHeader("Gestures")
        GlassToggleRow("Horizontal seek", null, settings.gestureSeek) { update { s -> s.copy(gestureSeek = it) } }
        GlassToggleRow("Brightness (left)", null, settings.gestureBrightness) { update { s -> s.copy(gestureBrightness = it) } }
        GlassToggleRow("Volume (right)", null, settings.gestureVolume) { update { s -> s.copy(gestureVolume = it) } }
        GlassToggleRow("Pinch zoom", null, settings.gesturePinch) { update { s -> s.copy(gesturePinch = it) } }
        GlassToggleRow("Long-press speed boost", null, settings.gestureLongPressSpeed) { update { s -> s.copy(gestureLongPressSpeed = it) } }
        GlassToggleRow("Double-tap seek", null, settings.gestureDoubleTap) { update { s -> s.copy(gestureDoubleTap = it) } }
        GlassToggleRow("Swipe down to mini-player", null, settings.gestureSwipeDownMini) { update { s -> s.copy(gestureSwipeDownMini = it) } }
        GlassSliderRow("Seek sensitivity", settings.seekSensitivity / 3f, { update { s -> s.copy(seekSensitivity = (it * 3f).coerceIn(0.25f, 3f)) } }, "${"%.1f".format(settings.seekSensitivity)}x")
        GlassSliderRow("Brightness sensitivity", settings.brightnessSensitivity / 3f, { update { s -> s.copy(brightnessSensitivity = (it * 3f).coerceIn(0.25f, 3f)) } }, "${"%.1f".format(settings.brightnessSensitivity)}x")
        GlassSliderRow("Volume sensitivity", settings.volumeSensitivity / 3f, { update { s -> s.copy(volumeSensitivity = (it * 3f).coerceIn(0.25f, 3f)) } }, "${"%.1f".format(settings.volumeSensitivity)}x")
        ChipRow(listOf("1.25x", "1.5x", "2x", "3x", "4x"), "${settings.longPressSpeed}x".replace(".0x", "x")) { label ->
            val v = label.removeSuffix("x").toFloat()
            update { s -> s.copy(longPressSpeed = v) }
        }

        SectionHeader("Player")
        ChipRow(AspectMode.entries.map { it.name }, settings.defaultAspect.name) {
            update { s -> s.copy(defaultAspect = AspectMode.valueOf(it)) }
        }
        ChipRow(TimelineStyle.entries.map { it.name }, settings.timelineStyle.name) {
            update { s -> s.copy(timelineStyle = TimelineStyle.valueOf(it)) }
        }
        GlassSliderRow("Controls timeout", (settings.controlsTimeoutMs / 1000) / 10f, {
            update { s -> s.copy(controlsTimeoutMs = (it * 10).toInt().coerceIn(1, 10) * 1000) }
        }, "${settings.controlsTimeoutMs / 1000}s")
        GlassToggleRow("Show remaining time", null, settings.showRemaining) { update { s -> s.copy(showRemaining = it) } }
        GlassToggleRow("Haptic feedback", null, settings.haptics) { update { s -> s.copy(haptics = it) } }
        GlassToggleRow("Auto Picture-in-Picture", null, settings.autoPip) { update { s -> s.copy(autoPip = it) } }
        ChipRow(listOf("auto", "portrait", "landscape", "sensor"), settings.orientation) {
            update { s -> s.copy(orientation = it) }
        }
        GlassToggleRow("Remember orientation per video", null, settings.rememberOrientation) {
            update { s -> s.copy(rememberOrientation = it) }
        }

        SectionHeader("Audio")
        ChipRow(listOf("system", "en", "es", "fr", "de", "ar", "ur"), settings.defaultAudioLang) {
            update { s -> s.copy(defaultAudioLang = it) }
        }
        Text(
            "Preferred audio language. Audio delay is not supported by the playback engine, so it is intentionally not offered.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader("Subtitles")
        GlassSliderRow("Size", settings.subtitleSize / 2f, { update { s -> s.copy(subtitleSize = (it * 2f).coerceIn(0.25f, 2f)) } }, "${"%.1f".format(settings.subtitleSize)}x")
        GlassSliderRow("Position", settings.subtitlePosition, { update { s -> s.copy(subtitlePosition = it) } }, "${(settings.subtitlePosition * 100).toInt()}%")
        GlassSliderRow(
            "Delay",
            (settings.subtitleDelayMs + 5000) / 10000f,
            {
                update { s -> s.copy(subtitleDelayMs = (((it * 10000).toInt() - 5000) / 100 * 100).coerceIn(-5000, 5000)) }
            },
            "${settings.subtitleDelayMs}ms",
        )
        Text(
            "Delay applies to external SRT / VTT files when they are loaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader("Library")
        GlassToggleRow("Hide hidden files", "Skip files starting with a dot", settings.hideHiddenFiles) { update { s -> s.copy(hideHiddenFiles = it) } }
        GlassSliderRow("Grid size", (settings.gridSize - 2) / 2f, {
            update { s -> s.copy(gridSize = ((it * 2).toInt() + 2).coerceIn(2, 4)) }
        }, "${settings.gridSize} columns")
        OutlinedTextField(
            value = settings.excludedFolders,
            onValueChange = { v -> update { s -> s.copy(excludedFolders = v) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            label = { Text("Excluded folders (comma separated)") },
        )

        SectionHeader("Network")
        ChipRow(BufferProfile.entries.map { it.name }, settings.bufferProfile.name) {
            update { s -> s.copy(bufferProfile = BufferProfile.valueOf(it)) }
        }
        GlassSliderRow("Stream retries", settings.maxRetries / 10f, {
            update { s -> s.copy(maxRetries = (it * 10).toInt().coerceIn(0, 10)) }
        }, "${settings.maxRetries}")
        Text(
            if (savedUrls.isEmpty()) "No saved stream URLs."
            else "${savedUrls.size} saved stream URL(s) — tap to remove:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        savedUrls.take(20).forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.title.ifBlank { item.url },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    scope.launch { app.container.library.deleteSavedUrl(item.id) }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            }
        }

        SectionHeader("Storage")
        Text(
            "Cache: ${cacheSize ?: "…"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            GlassButton("Clear thumbnails") {
                scope.launch(Dispatchers.IO) {
                    runCatching { Coil.imageLoader(app).diskCache?.clear() }
                    refreshCache()
                }
            }
            GlassButton("Clear cache") {
                scope.launch(Dispatchers.IO) {
                    runCatching { app.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                    refreshCache()
                }
            }
        }

        SectionHeader("Privacy")
        GlassToggleRow("Watch history", null, settings.historyEnabled) { update { s -> s.copy(historyEnabled = it) } }
        GlassToggleRow("Incognito playback", "Do not write progress or history", settings.incognito) { update { s -> s.copy(incognito = it) } }

        SectionHeader("Performance")
        ChipRow(PerformanceMode.entries.map { it.name }, settings.performanceMode.name) {
            update { s -> s.copy(performanceMode = PerformanceMode.valueOf(it)) }
        }

        SectionHeader("AI assistant")
        GlassToggleRow("Enable AI layer", "Optional. Player works fully without this.", settings.aiEnabled) {
            update { s -> s.copy(aiEnabled = it) }
        }
        if (settings.aiEnabled) {
            ChipRow(AiProvider.entries.map { it.name }, settings.aiProvider.name) {
                update { s -> s.copy(aiProvider = AiProvider.valueOf(it)) }
            }
            if (settings.aiProvider != AiProvider.OFF) {
                val model = settings.aiModel.ifBlank { AiClient.defaultModel(settings.aiProvider) }
                OutlinedTextField(
                    value = settings.aiEndpoint,
                    onValueChange = { v -> update { s -> s.copy(aiEndpoint = v) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    label = { Text("API endpoint (blank = default)") },
                    placeholder = { Text(AiClient.defaultEndpoint(settings.aiProvider, model)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = settings.aiModel,
                    onValueChange = { v -> update { s -> s.copy(aiModel = v) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    label = { Text("Model (blank = ${AiClient.defaultModel(settings.aiProvider)})") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    label = { Text("API key (stored encrypted on-device)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Save key", filled = true) {
                        app.container.secure.put(SecureStore.AI_API_KEY, apiKey.trim())
                        aiTest = "Key saved."
                    }
                    GlassButton("Clear key") {
                        app.container.secure.clear(SecureStore.AI_API_KEY)
                        apiKey = ""
                        aiTest = "Key cleared."
                    }
                    GlassButton("Test") {
                        scope.launch {
                            aiTest = "Testing…"
                            aiTest = runCatching {
                                "Success: " + app.container.aiClient.generate("Reply with exactly: OK")
                            }.getOrElse { "Failed: ${it.message}" }
                        }
                    }
                }
                aiTest?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Text(
                "AI only receives video metadata, bookmarks, and text you provide. It never uploads your videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionHeader("About")
        Text("Aether ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Text("Native Android · Media3 · Jetpack Compose")
        Text("Open-source licenses are bundled with AndroidX / Media3 artifacts.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            GlassButton("Diagnostics") { onOpenDiagnostics() }
            GlassButton("Report issue") {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, "https://github.com/sufyan-vip/First-Video-player-/issues".toUri()),
                    )
                }
            }
        }
    }
}

private fun dirSize(dir: java.io.File): Long = runCatching {
    dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}.getOrDefault(0L)

@Composable
private fun ChipRow(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        options.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                row.forEach { option ->
                    GlassChip(option.lowercase().replace('_', ' '), option == selected) { onPick(option) }
                }
            }
        }
    }
}
