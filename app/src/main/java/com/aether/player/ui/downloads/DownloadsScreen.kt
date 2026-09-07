package com.aether.player.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.player.ui.components.GlassCard
import com.aether.player.ui.library.LibraryViewModel
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    vm: LibraryViewModel,
    onBack: () -> Unit,
) {
    val items by vm.downloads.collectAsState()

    LaunchedEffect(items) {
        val active = items.filter { it.status == "downloading" || it.status == "queued" }
        if (active.isNotEmpty()) {
            delay(3000)
            active.forEach { vm.refreshDownload(it.id) }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Only download URLs you have the right to save.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text(
                "No downloads yet. Add one from Open URL → Download.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                GlassCard {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    val label = when (item.status) {
                        "downloading" -> "Downloading… ${item.progress}%"
                        "done" -> "Completed"
                        "error" -> "Failed"
                        else -> "Queued"
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (item.status) {
                            "done" -> MaterialTheme.colorScheme.primary
                            "error" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (item.status == "downloading" || item.status == "queued") {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { item.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { vm.refreshDownload(item.id) }) { Text("Refresh") }
                        TextButton(onClick = { vm.deleteDownload(item.id) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
