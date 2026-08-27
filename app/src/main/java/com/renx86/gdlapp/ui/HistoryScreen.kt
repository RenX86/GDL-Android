package com.renx86.gdlapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.renx86.gdlapp.data.db.DownloadHistoryEntity
import com.renx86.gdlapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilter { ALL, ACTIVE, COMPLETED, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    downloads: List<DownloadHistoryEntity>,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    
    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> downloads
            HistoryFilter.ACTIVE -> downloads.filter { it.status == "DOWNLOADING" || it.status == "QUEUED" }
            HistoryFilter.COMPLETED -> downloads.filter { it.status == "COMPLETED" }
            HistoryFilter.FAILED -> downloads.filter { it.status == "FAILED" }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoTheme.colors.background)
    ) {
        // Title Bar & Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            
            val activeCount = downloads.count { it.status == "DOWNLOADING" || it.status == "QUEUED" }
            val completedCount = downloads.count { it.status == "COMPLETED" }
            val failedCount = downloads.count { it.status == "FAILED" }
            
            Text(
                "${downloads.size} total • $activeCount active • $completedCount done • $failedCount failed",
                style = MaterialTheme.typography.labelMedium,
                color = NeoTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HistoryFilter.entries.toTypedArray()) { filter ->
                val count = when (filter) {
                    HistoryFilter.ALL -> downloads.size
                    HistoryFilter.ACTIVE -> downloads.count { it.status == "DOWNLOADING" || it.status == "QUEUED" }
                    HistoryFilter.COMPLETED -> downloads.count { it.status == "COMPLETED" }
                    HistoryFilter.FAILED -> downloads.count { it.status == "FAILED" }
                }
                
                val isSelected = selectedFilter == filter
                val bgColor = if (isSelected) NeoYellow else NeoTheme.colors.surface
                
                Box(
                    modifier = Modifier
                        .clickable { selectedFilter = filter }
                        .neoBrutalist(backgroundColor = bgColor, borderWidth = 2.dp, shadowOffset = if(isSelected) 0.dp else 4.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${filter.name} • $count",
                        fontWeight = if(isSelected) FontWeight.Black else FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeoBorder
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredDownloads.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = NeoTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("NO DOWNLOADS YET", fontWeight = FontWeight.Black, color = NeoTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Paste a URL on the Home tab to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onRemove(item.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 16.dp)
                                    .neoBrutalist(backgroundColor = NeoPink)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeoBorder, modifier = Modifier.size(32.dp))
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        NeoHistoryCard(item, onRetry, onRemove)
                    }
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
    val context = LocalContext.current
    
    val (statusColor, statusText) = when (item.status) {
        "QUEUED" -> NeoTheme.colors.textSecondary to "QUEUED"
        "DOWNLOADING" -> NeoYellow to "DOWNLOADING..."
        "COMPLETED" -> NeoGreen to "COMPLETED"
        else -> NeoPink to "FAILED"
    }

    val host = try {
        Uri.parse(item.url).host?.replace("www.", "") ?: "URL"
    } catch (e: Exception) {
        "URL"
    }

    val formatter = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(item.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Accent Strip
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(statusColor)
                    .border(1.dp, NeoBorder)
            )
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


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
                            val infiniteTransition = rememberInfiniteTransition(label = "infinite")
                            val animatedAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.6f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                                label = "alpha"
                            )
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .border(2.dp, NeoBorder),
                                color = NeoYellow.copy(alpha = animatedAlpha),
                                trackColor = NeoTheme.colors.surface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = statusColor
                        )
                    }

                    if (!item.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.errorMessage,
                            color = NeoPink,
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
                    // Retry Button
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
                    } else {
                        // Keep spacing stable when no retry button
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}
