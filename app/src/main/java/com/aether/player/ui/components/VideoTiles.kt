package com.aether.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.VideoEntity
import com.aether.player.domain.TimeFormat
import com.aether.player.domain.WatchProgressLogic
import com.aether.player.ui.theme.LocalGlass

@Composable
fun VideoThumb(
    video: VideoEntity,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
) {
    val context = LocalContext.current
    val glass = LocalGlass.current
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(glass.surfaceStrong),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(video.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(
                    if (video.durationMs > 8_000L) minOf(video.lastPositionMs.coerceAtLeast(1_500L), video.durationMs / 2)
                    else 1_000L,
                )
                .crossfade(true)
                .build(),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )
        Text(
            text = TimeFormat.formatMs(video.durationMs),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (video.isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp),
            )
        }
        if (showProgress && video.lastPositionMs > 0 && !video.completed) {
            ProgressHairline(
                WatchProgressLogic.progressFraction(video.lastPositionMs, video.durationMs),
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        VideoThumb(video, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
        Spacer(Modifier.height(8.dp))
        Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        Text(
            listOfNotNull(
                video.folderName,
                TimeFormat.prettyResolution(video.width, video.height).takeIf { video.width > 0 },
            ).joinToString(" · "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VideoListRow(
    video: VideoEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumb(video, Modifier.width(132.dp).height(78.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(
                "${TimeFormat.formatMs(video.durationMs)} · ${TimeFormat.prettyBytes(video.sizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ContinueCard(video: VideoEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.width(260.dp), onClick = onClick) {
        VideoThumb(video, Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.height(10.dp))
        Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        val remaining = TimeFormat.formatRemaining(video.lastPositionMs, video.durationMs)
        Text(
            "Continue from ${TimeFormat.formatMs(video.lastPositionMs)} · $remaining left",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FolderCard(folder: FolderEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glass = LocalGlass.current
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(glass.surface)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(14.dp),
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        Text(
            "${folder.videoCount} videos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
