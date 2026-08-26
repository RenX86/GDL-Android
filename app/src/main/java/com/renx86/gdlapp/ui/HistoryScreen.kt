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
import androidx.compose.material.icons.filled.Delete
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
import com.renx86.gdlapp.data.db.DownloadHistoryEntity
import com.renx86.gdlapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    downloads: List<DownloadHistoryEntity>,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoTheme.colors.background)
    ) {
        // Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HISTORY",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = NeoBorder
                )
            )

            if (downloads.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = NeoBorder)
                }
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HISTORY IS EMPTY",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.LightGray
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    NeoHistoryCard(item, onRetry, onRemove)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun NeoHistoryCard(
    item: DownloadHistoryEntity,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val (backgroundColor, statusText) = when (item.status) {
        "QUEUED" -> NeoTheme.colors.surface to "QUEUED"
        "DOWNLOADING" -> NeoYellow to "DOWNLOADING..."
        "COMPLETED" -> NeoGreen to "COMPLETED"
        else -> NeoPink to "FAILED"
    }

    val host = try {
        Uri.parse(item.url).host?.replace("www.", "") ?: "URL"
    } catch (e: Exception) {
        "URL"
    }
    val badgeText = host.take(2).uppercase()

    val formatter = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(item.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoBrutalist(backgroundColor = backgroundColor, shadowOffset = 6.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            
            // Site Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NeoTheme.colors.background)
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
                    color = NeoTextSecondary
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeoTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar / Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == "DOWNLOADING") {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .border(2.dp, NeoBorder),
                            color = NeoBorder,
                            trackColor = NeoTheme.colors.surface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeoBorder
                    )
                }

                if (!item.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.errorMessage,
                        color = NeoBorder,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Delete button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onRemove(item.id) }
                        .neoBrutalist(backgroundColor = NeoPink, borderWidth = 2.dp, shadowOffset = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = NeoBorder
                    )
                }

                // Retry Button (only if not downloading/queued)
                if (item.status == "FAILED" || item.status == "COMPLETED") {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onRetry(item.id) }
                            .neoBrutalist(backgroundColor = NeoYellow, borderWidth = 2.dp, shadowOffset = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Download Again",
                            tint = NeoBorder
                        )
                    }
                }
            }
        }
    }
}
