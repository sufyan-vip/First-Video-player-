package com.aether.player.ui.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.data.db.VideoEntity
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.VideoListRow
import com.aether.player.ui.library.LibraryViewModel
import kotlinx.coroutines.flow.combine

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    vm: LibraryViewModel,
    onBack: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AetherApp
    val library = app.container.library
    val ui by vm.ui.collectAsState()
    val playlist = ui.playlists.find { it.id == playlistId }
    val videosFlow = remember(playlistId) {
        combine(library.playlistItems(playlistId), library.videos()) { items, videos ->
            val map = videos.associateBy { it.id }
            items.mapNotNull { map[it.videoId] }
        }
    }
    val videos by videosFlow.collectAsState(initial = emptyList())
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("← Playlists") }
        Text(playlist?.name ?: "Playlist", style = MaterialTheme.typography.headlineMedium)
        Text("${videos.size} videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassButton("Play all", filled = true, modifier = Modifier.padding(vertical = 12.dp)) {
            videos.firstOrNull()?.let {
                vm.play(it, videos)
                onOpenVideo()
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(videos, key = { it.id }) { video: VideoEntity ->
                VideoListRow(
                    video,
                    onClick = {
                        vm.play(video, videos)
                        onOpenVideo()
                    },
                    onLongClick = { },
                )
            }
        }
    }
}
