package com.aether.player.ui.url

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassCard
import com.aether.player.ui.library.LibraryViewModel

private val SAMPLE_LINKS = listOf(
    "Big Buck Bunny (MP4)" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    "Tears of Steel (HLS)" to "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
    "Big Buck Bunny (DASH)" to "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd",
)

@Composable
fun OpenUrlScreen(
    vm: LibraryViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val saved by app.container.library.savedUrls().collectAsState(initial = emptyList())
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    val notice by vm.notice.collectAsState()
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeNotice()
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
        Text(
            "YouTube / TikTok / Instagram watch pages are web pages, not video files — those can't play directly.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
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
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("Play", filled = true, modifier = Modifier.weight(1f)) {
                vm.playUrl(url, title) { onPlay() }
            }
            Spacer(Modifier.padding(4.dp))
            GlassButton("Download") { vm.enqueueDownload(url, title) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Try a sample stream", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SAMPLE_LINKS.forEach { (label, link) ->
                GlassButton(label.substringBefore(" (")) {
                    vm.playUrl(link, label) { onPlay() }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Recent URLs", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(saved, key = { it.id }) { item ->
                GlassCard(modifier = Modifier.padding(vertical = 6.dp), onClick = {
                    vm.playUrl(item.url, item.title) { onPlay() }
                }) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        item.url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = { vm.deleteSavedUrl(item.id) },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
