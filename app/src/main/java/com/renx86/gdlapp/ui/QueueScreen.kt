package com.renx86.gdlapp.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.model.DownloadItem
import com.renx86.gdlapp.model.DownloadStatus
import com.renx86.gdlapp.ui.theme.*

@Composable
fun QueueScreen(
    downloads: List<DownloadItem>,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NeoBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "QUEUE IS EMPTY",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.LightGray
                )
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .padding(16.dp)
    ) {
        items(downloads, key = { it.id }) { item ->
            NeoDownloadCard(item, onRetry, onRemove)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NeoDownloadCard(
    item: DownloadItem,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val (backgroundColor, statusText) = when (item.status) {
        DownloadStatus.QUEUED -> Color.White to "QUEUED"
        DownloadStatus.DOWNLOADING -> NeoYellow to "DOWNLOADING..."
        DownloadStatus.DONE -> NeoGreen to "DONE"
        DownloadStatus.FAILED -> NeoPink to "FAILED"
    }

    // Extract domain for the badge (e.g., wallhaven.cc -> WH)
    val host = try {
        Uri.parse(item.url).host?.replace("www.", "") ?: "URL"
    } catch (e: Exception) {
        "URL"
    }
    val badgeText = host.take(2).uppercase()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoBrutalist(backgroundColor = backgroundColor, shadowOffset = 6.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            
            // Site Badge (e.g. [ WH ])
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NeoBackground)
                    .border(3.dp, NeoBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = NeoBorder
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NeoBorder
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar / Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .border(2.dp, NeoBorder),
                            color = NeoBorder,
                            trackColor = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                if (item.error.isNotBlank()) {
                    Text(
                        text = item.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                if (item.status == DownloadStatus.FAILED) {
                    Box(
                        modifier = Modifier
                            .clickable { onRetry(item.id) }
                            .neoBrutalist(backgroundColor = NeoBlue, borderWidth = 2.dp, shadowOffset = 3.dp)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = NeoBorder)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .clickable { onRemove(item.id) }
                        .neoBrutalist(backgroundColor = Color.White, borderWidth = 2.dp, shadowOffset = 3.dp)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = NeoBorder)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QueueScreenPreview() {
    val items = listOf(
        DownloadItem(id = "1", url = "https://wallhaven.cc/w/j3m8y5", status = DownloadStatus.DOWNLOADING),
        DownloadItem(id = "2", url = "https://reddit.com/r/pics/123", status = DownloadStatus.QUEUED),
        DownloadItem(id = "3", url = "https://pixiv.net/artworks/987", status = DownloadStatus.DONE),
        DownloadItem(id = "4", url = "https://twitter.com/status/456", status = DownloadStatus.FAILED, error = "Connection timeout")
    )
    MaterialTheme {
        QueueScreen(downloads = items, onRetry = {}, onRemove = {})
    }
}
