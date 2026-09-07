package com.aether.player.ui.url

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.aether.player.domain.UrlValidator
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun OpenUrlScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AetherApp
    val library = app.container.library
    val player = app.container.playerManager
    val saved by library.savedUrls().collectAsState(initial = emptyList())
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun playUrl(raw: String, label: String) {
        val normalized = if (raw.contains("://")) raw.trim() else "https://${raw.trim()}"
        if (!UrlValidator.isPlayableUrl(normalized)) {
            error = "That doesn’t look like a playable HTTP(S) or stream URL."
            return
        }
        error = null
        scope.launch {
            val video = library.upsertNetworkVideo(normalized, label.ifBlank { normalized })
            library.saveUrl(normalized, video.title)
            player.playSingle(video, startOver = true)
            onPlay()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Open URL", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Paste a direct media link. HLS (.m3u8) and DASH (.mpd) are supported when the stream is publicly reachable.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://…") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Optional title") },
            singleLine = true,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        GlassButton("Play", filled = true, modifier = Modifier.fillMaxWidth()) { playUrl(url, title) }
        Spacer(Modifier.height(24.dp))
        Text("Recent URLs", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(saved, key = { it.id }) { item ->
                GlassCard(modifier = Modifier.padding(vertical = 6.dp), onClick = { playUrl(item.url, item.title) }) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
