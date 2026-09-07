package com.aether.player.ui.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.BuildConfig
import com.aether.player.data.prefs.AccentColor
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.AspectMode
import com.aether.player.data.prefs.PerformanceMode
import com.aether.player.data.prefs.ThemeMode
import com.aether.player.data.prefs.TimelineStyle
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassSliderRow
import com.aether.player.ui.components.GlassToggleRow
import com.aether.player.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as AetherApp
    val prefs = app.container.preferences
    val settings by prefs.settings.collectAsState(initial = AetherSettings())
    val scope = rememberCoroutineScope()
    fun update(block: (AetherSettings) -> AetherSettings) {
        scope.launch { prefs.update(block) }
    }
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
        GlassToggleRow("Compact mode", null, settings.compactMode) { update { s -> s.copy(compactMode = it) } }

        SectionHeader("Playback")
        ChipRow(listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), "${settings.defaultSpeed}x".replace(".0x", "x")) { label ->
            val v = label.removeSuffix("x").toFloat()
            update { s -> s.copy(defaultSpeed = v) }
        }
        GlassToggleRow("Auto-play next", null, settings.autoPlayNext) { update { s -> s.copy(autoPlayNext = it) } }
        GlassToggleRow("Resume playback", null, settings.resumePlayback) { update { s -> s.copy(resumePlayback = it) } }
        GlassToggleRow("Background playback", null, settings.backgroundPlayback) { update { s -> s.copy(backgroundPlayback = it) } }
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
        GlassSliderRow("Seek sensitivity", settings.seekSensitivity / 3f, { update { s -> s.copy(seekSensitivity = it * 3f) } }, "${"%.1f".format(settings.seekSensitivity)}x")

        SectionHeader("Player")
        ChipRow(AspectMode.entries.map { it.name }, settings.defaultAspect.name) {
            update { s -> s.copy(defaultAspect = AspectMode.valueOf(it)) }
        }
        ChipRow(TimelineStyle.entries.map { it.name }, settings.timelineStyle.name) {
            update { s -> s.copy(timelineStyle = TimelineStyle.valueOf(it)) }
        }
        GlassToggleRow("Show remaining time", null, settings.showRemaining) { update { s -> s.copy(showRemaining = it) } }
        GlassToggleRow("Haptic feedback", null, settings.haptics) { update { s -> s.copy(haptics = it) } }
        GlassToggleRow("Auto Picture-in-Picture", null, settings.autoPip) { update { s -> s.copy(autoPip = it) } }
        ChipRow(listOf("auto", "portrait", "landscape", "sensor"), settings.orientation) {
            update { s -> s.copy(orientation = it) }
        }

        SectionHeader("Subtitles")
        GlassSliderRow("Size", settings.subtitleSize / 2f, { update { s -> s.copy(subtitleSize = it * 2f) } }, "${"%.1f".format(settings.subtitleSize)}x")
        GlassSliderRow("Position", settings.subtitlePosition, { update { s -> s.copy(subtitlePosition = it) } }, "${(settings.subtitlePosition * 100).toInt()}%")

        SectionHeader("Library")
        GlassToggleRow("Hide hidden files", null, settings.hideHiddenFiles) { update { s -> s.copy(hideHiddenFiles = it) } }
        OutlinedTextField(
            value = settings.excludedFolders,
            onValueChange = { v -> update { s -> s.copy(excludedFolders = v) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            label = { Text("Excluded folders (comma separated)") },
        )

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
            OutlinedTextField(
                value = settings.aiEndpoint,
                onValueChange = { v -> update { s -> s.copy(aiEndpoint = v) } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                label = { Text("API endpoint") },
            )
            OutlinedTextField(
                value = settings.aiModel,
                onValueChange = { v -> update { s -> s.copy(aiModel = v) } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                label = { Text("Model") },
            )
            Text(
                "API keys are stored only on-device in encrypted preferences when provided. Aether never uploads local videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader("About")
        Text("Aether ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Text("Native Android · Media3 · Jetpack Compose")
        Text("Open-source licenses are bundled with AndroidX / Media3 artifacts.")
    }
}

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
