package com.aether.player.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.components.GlassSliderRow
import com.aether.player.ui.components.SettingsGroup
import com.aether.player.ui.components.SettingsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

private val CATEGORY_TITLES = mapOf(
    "appearance" to "Appearance",
    "playback" to "Playback",
    "gestures" to "Gestures",
    "player" to "Player",
    "audio" to "Audio",
    "subtitles" to "Subtitles",
    "library" to "Library",
    "network" to "Network",
    "storage" to "Storage",
    "privacy" to "Privacy",
    "performance" to "Performance",
    "ai" to "AI assistant",
    "about" to "About",
)

@Composable
fun SettingsDetailScreen(
    category: String,
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
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
    LaunchedEffect(category) { refreshCache() }
    var apiKey by remember { mutableStateOf(app.container.secure.get(SecureStore.AI_API_KEY)) }
    var aiTest by remember { mutableStateOf<String?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupMessage = runCatching {
                val json = settingsToJson(prefs.settings.first())
                app.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: throw IllegalStateException("Could not open file")
                "Settings exported."
            }.getOrElse { "Export failed: ${it.message}" }
        }
    }
    val importSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupMessage = runCatching {
                val text = app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalStateException("Could not read file")
                applySettingsJson(prefs, JSONObject(text))
                "Settings imported."
            }.getOrElse { "Import failed: ${it.message}" }
        }
    }
    val exportLibraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupMessage = runCatching {
                val json = libraryToJson(app)
                app.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: throw IllegalStateException("Could not open file")
                "Library exported."
            }.getOrElse { "Export failed: ${it.message}" }
        }
    }
    val importLibraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupMessage = runCatching {
                val text = app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalStateException("Could not read file")
                val imported = applyLibraryJson(app, JSONObject(text))
                "Library import done: $imported items."
            }.getOrElse { "Import failed: ${it.message}" }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Outlined.ArrowBack, "Back", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                CATEGORY_TITLES[category] ?: "Settings",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        when (category) {
            "appearance" -> SettingsGroup("Appearance") {
                ChipRow(ThemeMode.entries.map { it.name }, settings.themeMode.name, Icons.Outlined.Palette, "Theme") {
                    update { s -> s.copy(themeMode = ThemeMode.valueOf(it)) }
                }
                ChipRow(AccentColor.entries.map { it.name }, settings.accent.name, Icons.Outlined.Tune, "Accent") {
                    update { s -> s.copy(accent = AccentColor.valueOf(it)) }
                }
                ToggleRow("System dynamic color", "Uses wallpaper colors when Accent is SYSTEM", settings.dynamicColor) {
                    update { s -> s.copy(dynamicColor = it) }
                }
                SliderRow("Glass intensity", settings.glassIntensity, "${(settings.glassIntensity * 100).toInt()}%") {
                    update { s -> s.copy(glassIntensity = it) }
                }
                SliderRow("Blur intensity", settings.blurIntensity, "${(settings.blurIntensity * 100).toInt()}%") {
                    update { s -> s.copy(blurIntensity = it) }
                }
                ToggleRow("Reduce motion", "Disables UI animation", settings.reduceMotion) {
                    update { s -> s.copy(reduceMotion = it) }
                }
                ToggleRow("High contrast", "Stronger text and borders", settings.highContrast, Icons.Outlined.Contrast) {
                    update { s -> s.copy(highContrast = it) }
                }
                ToggleRow("Compact mode", "Denser grids", settings.compactMode, Icons.Outlined.GridView) {
                    update { s -> s.copy(compactMode = it) }
                }
                SliderRow(
                    "Animation scale",
                    settings.animationScale / 2f,
                    "%.2f".format(settings.animationScale),
                    Icons.Outlined.Animation,
                ) {
                    update { s -> s.copy(animationScale = (it * 2f).coerceIn(0.25f, 2f)) }
                }
            }

            "playback" -> SettingsGroup("Playback") {
                ChipRow(
                    listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"),
                    "${settings.defaultSpeed}x".replace(".0x", "x"),
                    Icons.Outlined.Speed,
                    "Default speed",
                ) { label ->
                    update { s -> s.copy(defaultSpeed = label.removeSuffix("x").toFloat()) }
                }
                ToggleRow("Auto-play next", null, settings.autoPlayNext, Icons.Outlined.PlayArrow) {
                    update { s -> s.copy(autoPlayNext = it) }
                }
                ToggleRow("Resume playback", null, settings.resumePlayback, Icons.Outlined.History) {
                    update { s -> s.copy(resumePlayback = it) }
                }
                ToggleRow("Background playback", "Keep audio playing when the app is hidden", settings.backgroundPlayback) {
                    update { s -> s.copy(backgroundPlayback = it) }
                }
                ToggleRow("Keep screen awake", null, settings.keepScreenAwake) {
                    update { s -> s.copy(keepScreenAwake = it) }
                }
                SliderRow("Seek interval", settings.seekIntervalSec / 30f, "${settings.seekIntervalSec}s", Icons.Outlined.Timer) {
                    update { s -> s.copy(seekIntervalSec = (it * 30).toInt().coerceIn(5, 30)) }
                }
                SliderRow(
                    "Double-tap interval",
                    settings.doubleTapIntervalSec / 30f,
                    "${settings.doubleTapIntervalSec}s",
                ) {
                    update { s -> s.copy(doubleTapIntervalSec = (it * 30).toInt().coerceIn(5, 30)) }
                }
            }

            "gestures" -> SettingsGroup("Gestures") {
                ToggleRow("Horizontal seek", null, settings.gestureSeek, Icons.Outlined.TouchApp) {
                    update { s -> s.copy(gestureSeek = it) }
                }
                ToggleRow("Brightness (left)", null, settings.gestureBrightness, Icons.Outlined.BrightnessMedium) {
                    update { s -> s.copy(gestureBrightness = it) }
                }
                ToggleRow("Volume (right)", null, settings.gestureVolume) {
                    update { s -> s.copy(gestureVolume = it) }
                }
                ToggleRow("Pinch zoom", null, settings.gesturePinch) {
                    update { s -> s.copy(gesturePinch = it) }
                }
                ToggleRow("Long-press speed boost", null, settings.gestureLongPressSpeed) {
                    update { s -> s.copy(gestureLongPressSpeed = it) }
                }
                ToggleRow("Double-tap seek", null, settings.gestureDoubleTap) {
                    update { s -> s.copy(gestureDoubleTap = it) }
                }
                ToggleRow("Swipe down to mini-player", null, settings.gestureSwipeDownMini) {
                    update { s -> s.copy(gestureSwipeDownMini = it) }
                }
                SliderRow("Seek sensitivity", settings.seekSensitivity / 3f, "%.1fx".format(settings.seekSensitivity)) {
                    update { s -> s.copy(seekSensitivity = (it * 3f).coerceIn(0.25f, 3f)) }
                }
                SliderRow(
                    "Brightness sensitivity",
                    settings.brightnessSensitivity / 3f,
                    "%.1fx".format(settings.brightnessSensitivity),
                ) {
                    update { s -> s.copy(brightnessSensitivity = (it * 3f).coerceIn(0.25f, 3f)) }
                }
                SliderRow("Volume sensitivity", settings.volumeSensitivity / 3f, "%.1fx".format(settings.volumeSensitivity)) {
                    update { s -> s.copy(volumeSensitivity = (it * 3f).coerceIn(0.25f, 3f)) }
                }
                ChipRow(
                    listOf("1.25x", "1.5x", "2x", "3x", "4x"),
                    "${settings.longPressSpeed}x".replace(".0x", "x"),
                    Icons.Outlined.Speed,
                    "Long-press speed",
                ) { label ->
                    update { s -> s.copy(longPressSpeed = label.removeSuffix("x").toFloat()) }
                }
            }

            "player" -> SettingsGroup("Player") {
                ChipRow(AspectMode.entries.map { it.name }, settings.defaultAspect.name, Icons.Outlined.Tune, "Aspect") {
                    update { s -> s.copy(defaultAspect = AspectMode.valueOf(it)) }
                }
                ChipRow(TimelineStyle.entries.map { it.name }, settings.timelineStyle.name, null, "Timeline") {
                    update { s -> s.copy(timelineStyle = TimelineStyle.valueOf(it)) }
                }
                SliderRow(
                    "Controls timeout",
                    (settings.controlsTimeoutMs / 1000) / 10f,
                    "${settings.controlsTimeoutMs / 1000}s",
                    Icons.Outlined.Timer,
                ) {
                    update { s -> s.copy(controlsTimeoutMs = (it * 10).toInt().coerceIn(1, 10) * 1000) }
                }
                ToggleRow("Show remaining time", null, settings.showRemaining) {
                    update { s -> s.copy(showRemaining = it) }
                }
                ToggleRow("Haptic feedback", null, settings.haptics) {
                    update { s -> s.copy(haptics = it) }
                }
                ToggleRow("Auto Picture-in-Picture", null, settings.autoPip) {
                    update { s -> s.copy(autoPip = it) }
                }
                ChipRow(listOf("auto", "portrait", "landscape", "sensor"), settings.orientation, null, "Orientation") {
                    update { s -> s.copy(orientation = it) }
                }
                ToggleRow("Remember orientation per video", null, settings.rememberOrientation) {
                    update { s -> s.copy(rememberOrientation = it) }
                }
            }

            "audio" -> SettingsGroup("Audio") {
                ChipRow(
                    listOf("system", "en", "es", "fr", "de", "ar", "ur"),
                    settings.defaultAudioLang,
                    Icons.Outlined.Audiotrack,
                    "Preferred language",
                ) {
                    update { s -> s.copy(defaultAudioLang = it) }
                }
                NoteText("Audio delay is not supported by the playback engine, so it is intentionally not offered.")
            }

            "subtitles" -> SettingsGroup("Subtitles") {
                SliderRow("Size", settings.subtitleSize / 2f, "%.1fx".format(settings.subtitleSize), Icons.Outlined.TextFields) {
                    update { s -> s.copy(subtitleSize = (it * 2f).coerceIn(0.25f, 2f)) }
                }
                SliderRow("Position", settings.subtitlePosition, "${(settings.subtitlePosition * 100).toInt()}%") {
                    update { s -> s.copy(subtitlePosition = it) }
                }
                SliderRow(
                    "Delay",
                    (settings.subtitleDelayMs + 5000) / 10000f,
                    "${settings.subtitleDelayMs}ms",
                    Icons.Outlined.ClosedCaption,
                ) {
                    update { s -> s.copy(subtitleDelayMs = (((it * 10000).toInt() - 5000) / 100 * 100).coerceIn(-5000, 5000)) }
                }
                NoteText("Delay applies to external SRT / VTT files when they are loaded.")
            }

            "library" -> SettingsGroup("Library") {
                ToggleRow("Hide hidden files", "Skip files starting with a dot", settings.hideHiddenFiles, Icons.Outlined.Folder) {
                    update { s -> s.copy(hideHiddenFiles = it) }
                }
                SliderRow("Grid size", (settings.gridSize - 2) / 2f, "${settings.gridSize} columns", Icons.Outlined.GridView) {
                    update { s -> s.copy(gridSize = ((it * 2).toInt() + 2).coerceIn(2, 4)) }
                }
                OutlinedTextField(
                    value = settings.excludedFolders,
                    onValueChange = { v -> update { s -> s.copy(excludedFolders = v) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Excluded folders (comma separated)") },
                )
            }

            "network" -> SettingsGroup("Network") {
                ChipRow(
                    BufferProfile.entries.map { it.name },
                    settings.bufferProfile.name,
                    Icons.Outlined.Cloud,
                    "Buffer profile",
                ) {
                    update { s -> s.copy(bufferProfile = BufferProfile.valueOf(it)) }
                }
                SliderRow("Stream retries", settings.maxRetries / 10f, "${settings.maxRetries}") {
                    update { s -> s.copy(maxRetries = (it * 10).toInt().coerceIn(0, 10)) }
                }
                savedUrls.take(20).forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                if (savedUrls.isEmpty()) {
                    NoteText("No saved stream URLs.")
                }
            }

            "storage" -> Column {
                SettingsGroup("Storage") {
                    SettingsRow(
                        title = "Cache",
                        subtitle = cacheSize ?: "…",
                        icon = Icons.Outlined.Storage,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
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
                }
                SettingsGroup("Backup") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        GlassButton("Export settings") { exportSettingsLauncher.launch("aether-settings.json") }
                        GlassButton("Import settings") { importSettingsLauncher.launch(arrayOf("application/json")) }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        GlassButton("Export library") { exportLibraryLauncher.launch("aether-library.json") }
                        GlassButton("Import library") { importLibraryLauncher.launch(arrayOf("application/json")) }
                    }
                    backupMessage?.let { NoteText(it) }
                    NoteText("Settings backup includes every option on these pages. Library backup keeps stream links, saved URLs and playlists.")
                }
            }

            "privacy" -> SettingsGroup("Privacy") {
                ToggleRow("Watch history", null, settings.historyEnabled, Icons.Outlined.History) {
                    update { s -> s.copy(historyEnabled = it) }
                }
                ToggleRow("Incognito playback", "Do not write progress or history", settings.incognito, Icons.Outlined.Shield) {
                    update { s -> s.copy(incognito = it) }
                }
            }

            "performance" -> SettingsGroup("Performance") {
                ChipRow(
                    PerformanceMode.entries.map { it.name },
                    settings.performanceMode.name,
                    Icons.Outlined.Speed,
                    "Mode",
                ) {
                    update { s -> s.copy(performanceMode = PerformanceMode.valueOf(it)) }
                }
            }

            "ai" -> SettingsGroup("AI assistant") {
                ToggleRow("Enable AI layer", "Optional. Player works fully without this.", settings.aiEnabled, Icons.Outlined.SmartToy) {
                    update { s -> s.copy(aiEnabled = it) }
                }
                if (settings.aiEnabled) {
                    ChipRow(AiProvider.entries.map { it.name }, settings.aiProvider.name, null, "Provider") {
                        update { s -> s.copy(aiProvider = AiProvider.valueOf(it)) }
                    }
                    if (settings.aiProvider != AiProvider.OFF) {
                        val model = settings.aiModel.ifBlank { AiClient.defaultModel(settings.aiProvider) }
                        OutlinedTextField(
                            value = settings.aiEndpoint,
                            onValueChange = { v -> update { s -> s.copy(aiEndpoint = v) } },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            label = { Text("API endpoint (blank = default)") },
                            placeholder = { Text(AiClient.defaultEndpoint(settings.aiProvider, model)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = settings.aiModel,
                            onValueChange = { v -> update { s -> s.copy(aiModel = v) } },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            label = { Text("Model (blank = ${AiClient.defaultModel(settings.aiProvider)})") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            label = { Text("API key (stored encrypted on-device)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
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
                            NoteText(it)
                        }
                    }
                    NoteText("AI only receives video metadata, bookmarks, and text you provide. It never uploads your videos.")
                }
            }

            "about" -> SettingsGroup("About") {
                SettingsRow(
                    title = "Aether ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    subtitle = "Native Android · Media3 · Jetpack Compose",
                    icon = Icons.Outlined.Info,
                )
                SettingsRow(
                    title = "Made by Sufyan",
                    subtitle = "Free and open source ❤",
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    GlassButton("Diagnostics") { onOpenDiagnostics() }
                    GlassButton("GitHub") {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://github.com/sufyan-vip/First-Video-player-".toUri()),
                            )
                        }
                    }
                    GlassButton("Report issue") {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://github.com/sufyan-vip/First-Video-player-/issues".toUri()),
                            )
                        }
                    }
                }
            }

            else -> NoteText("Unknown category.")
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    icon: ImageVector? = null,
    onChecked: (Boolean) -> Unit,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            )
        },
        onClick = { onChecked(!checked) },
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueText: String,
    icon: ImageVector? = null,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
            GlassSliderRow(title, value, onChange, valueText)
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, icon: ImageVector?, label: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
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

@Composable
private fun NoteText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

private fun dirSize(dir: java.io.File): Long = runCatching {
    dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}.getOrDefault(0L)

private fun settingsToJson(s: AetherSettings): String = JSONObject()
    .put("themeMode", s.themeMode.name)
    .put("accent", s.accent.name)
    .put("dynamicColor", s.dynamicColor)
    .put("glassIntensity", s.glassIntensity.toDouble())
    .put("blurIntensity", s.blurIntensity.toDouble())
    .put("animationScale", s.animationScale.toDouble())
    .put("compactMode", s.compactMode)
    .put("highContrast", s.highContrast)
    .put("reduceMotion", s.reduceMotion)
    .put("defaultSpeed", s.defaultSpeed.toDouble())
    .put("autoPlayNext", s.autoPlayNext)
    .put("resumePlayback", s.resumePlayback)
    .put("seekIntervalSec", s.seekIntervalSec)
    .put("doubleTapIntervalSec", s.doubleTapIntervalSec)
    .put("backgroundPlayback", s.backgroundPlayback)
    .put("keepScreenAwake", s.keepScreenAwake)
    .put("gestureSeek", s.gestureSeek)
    .put("gestureBrightness", s.gestureBrightness)
    .put("gestureVolume", s.gestureVolume)
    .put("gesturePinch", s.gesturePinch)
    .put("gestureLongPressSpeed", s.gestureLongPressSpeed)
    .put("gestureDoubleTap", s.gestureDoubleTap)
    .put("gestureSwipeDownMini", s.gestureSwipeDownMini)
    .put("seekSensitivity", s.seekSensitivity.toDouble())
    .put("brightnessSensitivity", s.brightnessSensitivity.toDouble())
    .put("volumeSensitivity", s.volumeSensitivity.toDouble())
    .put("longPressSpeed", s.longPressSpeed.toDouble())
    .put("defaultAspect", s.defaultAspect.name)
    .put("controlsTimeoutMs", s.controlsTimeoutMs)
    .put("timelineStyle", s.timelineStyle.name)
    .put("showRemaining", s.showRemaining)
    .put("haptics", s.haptics)
    .put("subtitleSize", s.subtitleSize.toDouble())
    .put("subtitlePosition", s.subtitlePosition.toDouble())
    .put("subtitleDelayMs", s.subtitleDelayMs)
    .put("defaultAudioLang", s.defaultAudioLang)
    .put("historyEnabled", s.historyEnabled)
    .put("incognito", s.incognito)
    .put("sortMode", s.sortMode.name)
    .put("viewMode", s.viewMode.name)
    .put("gridSize", s.gridSize)
    .put("completionThreshold", s.completionThreshold.toDouble())
    .put("performanceMode", s.performanceMode.name)
    .put("autoPip", s.autoPip)
    .put("rememberOrientation", s.rememberOrientation)
    .put("orientation", s.orientation)
    .put("aiEnabled", s.aiEnabled)
    .put("aiProvider", s.aiProvider.name)
    .put("aiEndpoint", s.aiEndpoint)
    .put("aiModel", s.aiModel)
    .put("bufferProfile", s.bufferProfile.name)
    .put("maxRetries", s.maxRetries)
    .put("excludedFolders", s.excludedFolders)
    .put("hideHiddenFiles", s.hideHiddenFiles)
    .toString(2)

private inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T =
    runCatching { java.lang.Enum.valueOf(T::class.java, this) }.getOrDefault(fallback)

private suspend fun applySettingsJson(
    prefs: com.aether.player.data.prefs.UserPreferences,
    obj: JSONObject,
) {
    prefs.update { s ->
        s.copy(
            themeMode = obj.optString("themeMode", s.themeMode.name).toEnumOr(s.themeMode),
            accent = obj.optString("accent", s.accent.name).toEnumOr(s.accent),
            dynamicColor = obj.optBoolean("dynamicColor", s.dynamicColor),
            glassIntensity = obj.optDouble("glassIntensity", s.glassIntensity.toDouble()).toFloat(),
            blurIntensity = obj.optDouble("blurIntensity", s.blurIntensity.toDouble()).toFloat(),
            animationScale = obj.optDouble("animationScale", s.animationScale.toDouble()).toFloat(),
            compactMode = obj.optBoolean("compactMode", s.compactMode),
            highContrast = obj.optBoolean("highContrast", s.highContrast),
            reduceMotion = obj.optBoolean("reduceMotion", s.reduceMotion),
            defaultSpeed = obj.optDouble("defaultSpeed", s.defaultSpeed.toDouble()).toFloat(),
            autoPlayNext = obj.optBoolean("autoPlayNext", s.autoPlayNext),
            resumePlayback = obj.optBoolean("resumePlayback", s.resumePlayback),
            seekIntervalSec = obj.optInt("seekIntervalSec", s.seekIntervalSec),
            doubleTapIntervalSec = obj.optInt("doubleTapIntervalSec", s.doubleTapIntervalSec),
            backgroundPlayback = obj.optBoolean("backgroundPlayback", s.backgroundPlayback),
            keepScreenAwake = obj.optBoolean("keepScreenAwake", s.keepScreenAwake),
            gestureSeek = obj.optBoolean("gestureSeek", s.gestureSeek),
            gestureBrightness = obj.optBoolean("gestureBrightness", s.gestureBrightness),
            gestureVolume = obj.optBoolean("gestureVolume", s.gestureVolume),
            gesturePinch = obj.optBoolean("gesturePinch", s.gesturePinch),
            gestureLongPressSpeed = obj.optBoolean("gestureLongPressSpeed", s.gestureLongPressSpeed),
            gestureDoubleTap = obj.optBoolean("gestureDoubleTap", s.gestureDoubleTap),
            gestureSwipeDownMini = obj.optBoolean("gestureSwipeDownMini", s.gestureSwipeDownMini),
            seekSensitivity = obj.optDouble("seekSensitivity", s.seekSensitivity.toDouble()).toFloat(),
            brightnessSensitivity = obj.optDouble("brightnessSensitivity", s.brightnessSensitivity.toDouble()).toFloat(),
            volumeSensitivity = obj.optDouble("volumeSensitivity", s.volumeSensitivity.toDouble()).toFloat(),
            longPressSpeed = obj.optDouble("longPressSpeed", s.longPressSpeed.toDouble()).toFloat(),
            defaultAspect = obj.optString("defaultAspect", s.defaultAspect.name).toEnumOr(s.defaultAspect),
            controlsTimeoutMs = obj.optInt("controlsTimeoutMs", s.controlsTimeoutMs),
            timelineStyle = obj.optString("timelineStyle", s.timelineStyle.name).toEnumOr(s.timelineStyle),
            showRemaining = obj.optBoolean("showRemaining", s.showRemaining),
            haptics = obj.optBoolean("haptics", s.haptics),
            subtitleSize = obj.optDouble("subtitleSize", s.subtitleSize.toDouble()).toFloat(),
            subtitlePosition = obj.optDouble("subtitlePosition", s.subtitlePosition.toDouble()).toFloat(),
            subtitleDelayMs = obj.optInt("subtitleDelayMs", s.subtitleDelayMs),
            defaultAudioLang = obj.optString("defaultAudioLang", s.defaultAudioLang),
            historyEnabled = obj.optBoolean("historyEnabled", s.historyEnabled),
            incognito = obj.optBoolean("incognito", s.incognito),
            sortMode = obj.optString("sortMode", s.sortMode.name).toEnumOr(s.sortMode),
            viewMode = obj.optString("viewMode", s.viewMode.name).toEnumOr(s.viewMode),
            gridSize = obj.optInt("gridSize", s.gridSize),
            completionThreshold = obj.optDouble("completionThreshold", s.completionThreshold.toDouble()).toFloat(),
            performanceMode = obj.optString("performanceMode", s.performanceMode.name).toEnumOr(s.performanceMode),
            autoPip = obj.optBoolean("autoPip", s.autoPip),
            rememberOrientation = obj.optBoolean("rememberOrientation", s.rememberOrientation),
            orientation = obj.optString("orientation", s.orientation),
            aiEnabled = obj.optBoolean("aiEnabled", s.aiEnabled),
            aiProvider = obj.optString("aiProvider", s.aiProvider.name).toEnumOr(s.aiProvider),
            aiEndpoint = obj.optString("aiEndpoint", s.aiEndpoint),
            aiModel = obj.optString("aiModel", s.aiModel),
            bufferProfile = obj.optString("bufferProfile", s.bufferProfile.name).toEnumOr(s.bufferProfile),
            maxRetries = obj.optInt("maxRetries", s.maxRetries),
            excludedFolders = obj.optString("excludedFolders", s.excludedFolders),
            hideHiddenFiles = obj.optBoolean("hideHiddenFiles", s.hideHiddenFiles),
        )
    }
}

private suspend fun libraryToJson(app: AetherApp): String {
    val lib = app.container.library
    val streams = org.json.JSONArray()
    lib.videos().first().filter { it.uri.startsWith("http") }.forEach { v ->
        streams.put(JSONObject().put("url", v.uri).put("title", v.title))
    }
    val urls = org.json.JSONArray()
    lib.savedUrls().first().forEach { u ->
        urls.put(JSONObject().put("url", u.url).put("title", u.title))
    }
    val lists = org.json.JSONArray()
    lib.playlists().first().forEach { pl ->
        val ids = org.json.JSONArray()
        lib.playlistItems(pl.id).first().forEach { ids.put(it.videoId) }
        lists.put(JSONObject().put("name", pl.name).put("videoIds", ids))
    }
    return JSONObject().put("streams", streams).put("urls", urls).put("playlists", lists).toString(2)
}

private suspend fun applyLibraryJson(app: AetherApp, obj: JSONObject): Int {
    val lib = app.container.library
    var count = 0
    val streams = obj.optJSONArray("streams") ?: org.json.JSONArray()
    for (i in 0 until streams.length()) {
        val o = streams.optJSONObject(i) ?: continue
        val url = o.optString("url")
        if (url.isNotBlank()) {
            runCatching {
                lib.upsertNetworkVideo(url, o.optString("title", url))
                count++
            }
        }
    }
    val urls = obj.optJSONArray("urls") ?: org.json.JSONArray()
    for (i in 0 until urls.length()) {
        val o = urls.optJSONObject(i) ?: continue
        val url = o.optString("url")
        if (url.isNotBlank()) {
            runCatching {
                lib.saveUrl(url, o.optString("title", url))
                count++
            }
        }
    }
    val lists = obj.optJSONArray("playlists") ?: org.json.JSONArray()
    for (i in 0 until lists.length()) {
        val o = lists.optJSONObject(i) ?: continue
        val name = o.optString("name")
        if (name.isBlank()) continue
        runCatching {
            val pid = lib.createPlaylist(name)
            val ids = o.optJSONArray("videoIds") ?: org.json.JSONArray()
            for (j in 0 until ids.length()) {
                val vid = ids.optString(j)
                if (vid.isNotBlank() && lib.getVideo(vid) != null) {
                    lib.addToPlaylist(pid, vid)
                }
            }
            count++
        }
    }
    return count
}
