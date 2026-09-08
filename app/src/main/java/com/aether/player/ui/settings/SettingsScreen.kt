package com.aether.player.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aether.player.ui.components.SettingsGroup
import com.aether.player.ui.components.SettingsRow

private data class SettingEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val ENTRIES = listOf(
    SettingEntry("appearance", "Appearance", "Theme, accent, motion", Icons.Outlined.Palette),
    SettingEntry("playback", "Playback", "Speed, resume, background", Icons.Outlined.PlayArrow),
    SettingEntry("gestures", "Gestures", "Swipe, pinch, double-tap", Icons.Outlined.TouchApp),
    SettingEntry("player", "Player", "Aspect, timeline, PiP", Icons.Outlined.Tune),
    SettingEntry("audio", "Audio", "Preferred language", Icons.Outlined.Audiotrack),
    SettingEntry("subtitles", "Subtitles", "Size, position, delay", Icons.Outlined.ClosedCaption),
    SettingEntry("library", "Library", "Grid, hidden files", Icons.Outlined.Folder),
    SettingEntry("network", "Network", "Buffer, retries, URLs", Icons.Outlined.Cloud),
    SettingEntry("storage", "Storage", "Cache", Icons.Outlined.Storage),
    SettingEntry("privacy", "Privacy", "History, incognito", Icons.Outlined.Shield),
    SettingEntry("performance", "Performance", "Quality mode", Icons.Outlined.Speed),
    SettingEntry("ai", "AI assistant", "Gemini / OpenRouter", Icons.Outlined.SmartToy),
    SettingEntry("about", "About", "Version, diagnostics", Icons.Outlined.Info),
)

@Composable
fun SettingsScreen(onOpenCategory: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Fast, private, all local.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        SettingsGroup("Settings") {
            ENTRIES.forEach { entry ->
                SettingsRow(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    icon = entry.icon,
                    trailing = {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { onOpenCategory(entry.key) },
                )
            }
        }
    }
}
