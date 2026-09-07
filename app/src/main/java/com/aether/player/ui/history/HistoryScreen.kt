package com.aether.player.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.player.domain.TimeFormat
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.VideoListRow
import com.aether.player.ui.library.LibraryViewModel

@Composable
fun HistoryScreen(
    vm: LibraryViewModel,
    onBack: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val items by vm.historyItems.collectAsState()
    val videos = items.mapNotNull { it.video }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (items.isEmpty()) "Nothing watched yet" else "${items.size} entries — long-press to remove one",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (items.isNotEmpty()) {
            GlassButton("Clear all history") { vm.clearHistory() }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(items, key = { it.id }) { item ->
                val video = item.video
                if (video == null) {
                    Text(
                        "(removed video) · ${TimeFormat.formatMs(item.positionMs)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    VideoListRow(
                        video,
                        onClick = {
                            vm.play(video, videos.ifEmpty { listOf(video) })
                            onOpenVideo()
                        },
                        onLongClick = { vm.deleteHistory(item.id) },
                    )
                }
            }
        }
    }
}
