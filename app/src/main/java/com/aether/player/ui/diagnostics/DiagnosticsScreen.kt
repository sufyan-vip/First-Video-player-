package com.aether.player.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.BuildConfig
import com.aether.player.domain.TimeFormat
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.library.LibraryViewModel

@Composable
fun DiagnosticsScreen(
    vm: LibraryViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val ui by vm.ui.collectAsState()
    val playerState by app.container.playerManager.state.collectAsState()

    val report = buildString {
        appendLine("Aether diagnostics")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Package: ${context.packageName}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Library: ${ui.count} videos, ${ui.folders.size} folders, ${ui.playlists.size} playlists")
        val cur = playerState.current
        appendLine("Player: ${cur?.title ?: "idle"}")
        if (cur != null) {
            appendLine("Position: ${TimeFormat.formatMs(playerState.positionMs)} / ${TimeFormat.formatMs(playerState.durationMs)}")
            appendLine("Queue: ${playerState.queue.size} items, speed ${playerState.speed}x")
        }
        appendLine("Theme: ${ui.settings.themeMode} / ${ui.settings.accent}")
        appendLine("Buffer: ${ui.settings.bufferProfile}, retries ${ui.settings.maxRetries}")
        appendLine("AI: ${ui.settings.aiProvider} (enabled=${ui.settings.aiEnabled})")
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Diagnostics", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        GlassButton("Copy report", filled = true) {
            val cm = context.getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("Aether diagnostics", report))
        }
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(report, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
