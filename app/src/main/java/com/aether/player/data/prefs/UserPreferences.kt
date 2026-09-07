package com.aether.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aetherStore by preferencesDataStore("aether_settings")

enum class ThemeMode { AUTO, DARK, LIGHT, AMOLED }
enum class AccentColor { CYAN, BLUE, PURPLE, GREEN, ORANGE, RED, PINK, SYSTEM }
enum class PerformanceMode { HIGH_QUALITY, BALANCED, BATTERY_SAVER }
enum class AspectMode { FIT, FILL, CROP, STRETCH, ORIGINAL, RATIO_16_9, RATIO_4_3 }
enum class SortMode { NAME, DATE_ADDED, DATE_MODIFIED, DURATION, SIZE, LAST_PLAYED }
enum class ViewMode { GRID, LIST, COMPACT }
enum class TimelineStyle { SLIM, BOLD, CHAPTER }
enum class AiProvider { OFF, GEMINI, OPENROUTER }
enum class BufferProfile { SMALL, STANDARD, LARGE }

data class AetherSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val accent: AccentColor = AccentColor.CYAN,
    val dynamicColor: Boolean = false,
    val glassIntensity: Float = 0.72f,
    val blurIntensity: Float = 0.55f,
    val animationScale: Float = 1f,
    val compactMode: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val defaultSpeed: Float = 1f,
    val autoPlayNext: Boolean = true,
    val resumePlayback: Boolean = true,
    val seekIntervalSec: Int = 10,
    val doubleTapIntervalSec: Int = 10,
    val backgroundPlayback: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val gestureSeek: Boolean = true,
    val gestureBrightness: Boolean = true,
    val gestureVolume: Boolean = true,
    val gesturePinch: Boolean = true,
    val gestureLongPressSpeed: Boolean = true,
    val gestureDoubleTap: Boolean = true,
    val gestureSwipeDownMini: Boolean = true,
    val seekSensitivity: Float = 1f,
    val brightnessSensitivity: Float = 1f,
    val volumeSensitivity: Float = 1f,
    val longPressSpeed: Float = 2f,
    val defaultAspect: AspectMode = AspectMode.FIT,
    val controlsTimeoutMs: Int = 4000,
    val timelineStyle: TimelineStyle = TimelineStyle.BOLD,
    val showRemaining: Boolean = true,
    val haptics: Boolean = true,
    val subtitleSize: Float = 1f,
    val subtitlePosition: Float = 0.12f,
    val subtitleDelayMs: Int = 0,
    val defaultAudioLang: String = "system",
    val historyEnabled: Boolean = true,
    val incognito: Boolean = false,
    val sortMode: SortMode = SortMode.DATE_ADDED,
    val viewMode: ViewMode = ViewMode.GRID,
    val gridSize: Int = 2,
    val completionThreshold: Float = 0.92f,
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val autoPip: Boolean = true,
    val rememberOrientation: Boolean = false,
    val orientation: String = "auto",
    val aiEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.OFF,
    val aiEndpoint: String = "",
    val aiModel: String = "",
    val bufferProfile: BufferProfile = BufferProfile.STANDARD,
    val maxRetries: Int = 3,
    val excludedFolders: String = "",
    val hideHiddenFiles: Boolean = true,
)

class UserPreferences(private val context: Context) {
    val settings: Flow<AetherSettings> = context.aetherStore.data.map { it.toSettings() }

    suspend fun update(transform: (AetherSettings) -> AetherSettings) {
        context.aetherStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs.write(next)
        }
    }

    suspend fun putString(key: String, value: String) {
        context.aetherStore.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun getString(key: String): String? =
        context.aetherStore.data.map { it[stringPreferencesKey(key)] }.first()

    fun stringFlow(key: String, default: String = ""): Flow<String> =
        context.aetherStore.data.map { it[stringPreferencesKey(key)] ?: default }
}

private fun Preferences.toSettings(): AetherSettings = AetherSettings(
    themeMode = enumValueOr(this[stringPreferencesKey("theme")], ThemeMode.AMOLED),
    accent = enumValueOr(this[stringPreferencesKey("accent")], AccentColor.CYAN),
    dynamicColor = this[booleanPreferencesKey("dynamicColor")] ?: false,
    glassIntensity = this[floatPreferencesKey("glass")] ?: 0.72f,
    blurIntensity = this[floatPreferencesKey("blur")] ?: 0.55f,
    animationScale = this[floatPreferencesKey("anim")] ?: 1f,
    compactMode = this[booleanPreferencesKey("compact")] ?: false,
    highContrast = this[booleanPreferencesKey("hiContrast")] ?: false,
    reduceMotion = this[booleanPreferencesKey("reduceMotion")] ?: false,
    defaultSpeed = this[floatPreferencesKey("speed")] ?: 1f,
    autoPlayNext = this[booleanPreferencesKey("autoNext")] ?: true,
    resumePlayback = this[booleanPreferencesKey("resume")] ?: true,
    seekIntervalSec = this[intPreferencesKey("seekInterval")] ?: 10,
    doubleTapIntervalSec = this[intPreferencesKey("dtap")] ?: 10,
    backgroundPlayback = this[booleanPreferencesKey("bgPlay")] ?: true,
    keepScreenAwake = this[booleanPreferencesKey("awake")] ?: true,
    gestureSeek = this[booleanPreferencesKey("gSeek")] ?: true,
    gestureBrightness = this[booleanPreferencesKey("gBright")] ?: true,
    gestureVolume = this[booleanPreferencesKey("gVol")] ?: true,
    gesturePinch = this[booleanPreferencesKey("gPinch")] ?: true,
    gestureLongPressSpeed = this[booleanPreferencesKey("gBoost")] ?: true,
    gestureDoubleTap = this[booleanPreferencesKey("gDtap")] ?: true,
    gestureSwipeDownMini = this[booleanPreferencesKey("gMini")] ?: true,
    seekSensitivity = this[floatPreferencesKey("seekSens")] ?: 1f,
    brightnessSensitivity = this[floatPreferencesKey("brightSens")] ?: 1f,
    volumeSensitivity = this[floatPreferencesKey("volSens")] ?: 1f,
    longPressSpeed = this[floatPreferencesKey("boostSpeed")] ?: 2f,
    defaultAspect = enumValueOr(this[stringPreferencesKey("aspect")], AspectMode.FIT),
    controlsTimeoutMs = this[intPreferencesKey("controlsMs")] ?: 4000,
    timelineStyle = enumValueOr(this[stringPreferencesKey("timeline")], TimelineStyle.BOLD),
    showRemaining = this[booleanPreferencesKey("remaining")] ?: true,
    haptics = this[booleanPreferencesKey("haptics")] ?: true,
    subtitleSize = this[floatPreferencesKey("subSize")] ?: 1f,
    subtitlePosition = this[floatPreferencesKey("subPos")] ?: 0.12f,
    subtitleDelayMs = this[intPreferencesKey("subDelay")] ?: 0,
    defaultAudioLang = this[stringPreferencesKey("audioLang")] ?: "system",
    historyEnabled = this[booleanPreferencesKey("history")] ?: true,
    incognito = this[booleanPreferencesKey("incognito")] ?: false,
    sortMode = enumValueOr(this[stringPreferencesKey("sort")], SortMode.DATE_ADDED),
    viewMode = enumValueOr(this[stringPreferencesKey("view")], ViewMode.GRID),
    gridSize = this[intPreferencesKey("grid")] ?: 2,
    completionThreshold = this[floatPreferencesKey("complete")] ?: 0.92f,
    performanceMode = enumValueOr(this[stringPreferencesKey("perf")], PerformanceMode.BALANCED),
    autoPip = this[booleanPreferencesKey("autoPip")] ?: true,
    rememberOrientation = this[booleanPreferencesKey("rememberOri")] ?: false,
    orientation = this[stringPreferencesKey("orientation")] ?: "auto",
    aiEnabled = this[booleanPreferencesKey("ai")] ?: false,
    aiProvider = enumValueOr(this[stringPreferencesKey("aiProvider")], AiProvider.OFF),
    aiEndpoint = this[stringPreferencesKey("aiEndpoint")] ?: "",
    aiModel = this[stringPreferencesKey("aiModel")] ?: "",
    bufferProfile = enumValueOr(this[stringPreferencesKey("buffer")], BufferProfile.STANDARD),
    maxRetries = this[intPreferencesKey("retries")] ?: 3,
    excludedFolders = this[stringPreferencesKey("excluded")] ?: "",
    hideHiddenFiles = this[booleanPreferencesKey("hideHidden")] ?: true,
)

private fun androidx.datastore.preferences.core.MutablePreferences.write(s: AetherSettings) {
    this[stringPreferencesKey("theme")] = s.themeMode.name
    this[stringPreferencesKey("accent")] = s.accent.name
    this[booleanPreferencesKey("dynamicColor")] = s.dynamicColor
    this[floatPreferencesKey("glass")] = s.glassIntensity
    this[floatPreferencesKey("blur")] = s.blurIntensity
    this[floatPreferencesKey("anim")] = s.animationScale
    this[booleanPreferencesKey("compact")] = s.compactMode
    this[booleanPreferencesKey("hiContrast")] = s.highContrast
    this[booleanPreferencesKey("reduceMotion")] = s.reduceMotion
    this[floatPreferencesKey("speed")] = s.defaultSpeed
    this[booleanPreferencesKey("autoNext")] = s.autoPlayNext
    this[booleanPreferencesKey("resume")] = s.resumePlayback
    this[intPreferencesKey("seekInterval")] = s.seekIntervalSec
    this[intPreferencesKey("dtap")] = s.doubleTapIntervalSec
    this[booleanPreferencesKey("bgPlay")] = s.backgroundPlayback
    this[booleanPreferencesKey("awake")] = s.keepScreenAwake
    this[booleanPreferencesKey("gSeek")] = s.gestureSeek
    this[booleanPreferencesKey("gBright")] = s.gestureBrightness
    this[booleanPreferencesKey("gVol")] = s.gestureVolume
    this[booleanPreferencesKey("gPinch")] = s.gesturePinch
    this[booleanPreferencesKey("gBoost")] = s.gestureLongPressSpeed
    this[booleanPreferencesKey("gDtap")] = s.gestureDoubleTap
    this[booleanPreferencesKey("gMini")] = s.gestureSwipeDownMini
    this[floatPreferencesKey("seekSens")] = s.seekSensitivity
    this[floatPreferencesKey("brightSens")] = s.brightnessSensitivity
    this[floatPreferencesKey("volSens")] = s.volumeSensitivity
    this[floatPreferencesKey("boostSpeed")] = s.longPressSpeed
    this[stringPreferencesKey("aspect")] = s.defaultAspect.name
    this[intPreferencesKey("controlsMs")] = s.controlsTimeoutMs
    this[stringPreferencesKey("timeline")] = s.timelineStyle.name
    this[booleanPreferencesKey("remaining")] = s.showRemaining
    this[booleanPreferencesKey("haptics")] = s.haptics
    this[floatPreferencesKey("subSize")] = s.subtitleSize
    this[floatPreferencesKey("subPos")] = s.subtitlePosition
    this[intPreferencesKey("subDelay")] = s.subtitleDelayMs
    this[stringPreferencesKey("audioLang")] = s.defaultAudioLang
    this[booleanPreferencesKey("history")] = s.historyEnabled
    this[booleanPreferencesKey("incognito")] = s.incognito
    this[stringPreferencesKey("sort")] = s.sortMode.name
    this[stringPreferencesKey("view")] = s.viewMode.name
    this[intPreferencesKey("grid")] = s.gridSize
    this[floatPreferencesKey("complete")] = s.completionThreshold
    this[stringPreferencesKey("perf")] = s.performanceMode.name
    this[booleanPreferencesKey("autoPip")] = s.autoPip
    this[booleanPreferencesKey("rememberOri")] = s.rememberOrientation
    this[stringPreferencesKey("orientation")] = s.orientation
    this[booleanPreferencesKey("ai")] = s.aiEnabled
    this[stringPreferencesKey("aiProvider")] = s.aiProvider.name
    this[stringPreferencesKey("aiEndpoint")] = s.aiEndpoint
    this[stringPreferencesKey("aiModel")] = s.aiModel
    this[stringPreferencesKey("buffer")] = s.bufferProfile.name
    this[intPreferencesKey("retries")] = s.maxRetries
    this[stringPreferencesKey("excluded")] = s.excludedFolders
    this[booleanPreferencesKey("hideHidden")] = s.hideHiddenFiles
}

private inline fun <reified T : Enum<T>> enumValueOr(raw: String?, fallback: T): T {
    if (raw.isNullOrBlank()) return fallback
    return runCatching { java.lang.Enum.valueOf(T::class.java, raw) }.getOrDefault(fallback)
}
