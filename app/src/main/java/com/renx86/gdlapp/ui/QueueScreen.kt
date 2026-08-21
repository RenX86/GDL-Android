package com.renx86.gdlapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.renx86.gdlapp.model.DownloadItem
import com.renx86.gdlapp.model.DownloadStatus

@Composable
fun QueueScreen(
    downloads: List<DownloadItem>,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloads.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads yet", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(downloads, key = { it.id }) { item ->
            DownloadCard(item, onRetry, onRemove)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val (containerColor, statusText) = when (item.status) {
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.surfaceVariant to "Queued"
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer to "Downloading..."
        DownloadStatus.DONE -> MaterialTheme.colorScheme.secondaryContainer to "Done ✓"
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to "Failed ✗"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.url,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(statusText, style = MaterialTheme.typography.labelSmall)
                if (item.error.isNotBlank()) {
                    Text(
                        item.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (item.status == DownloadStatus.FAILED) {
                IconButton(onClick = { onRetry(item.id) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }

            IconButton(onClick = { onRemove(item.id) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
    }
}
