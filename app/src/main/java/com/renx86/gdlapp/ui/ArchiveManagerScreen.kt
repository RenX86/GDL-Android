package com.renx86.gdlapp.ui

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Represents one row from gallery-dl's archive database.
 * gallery-dl stores a single 'entry' TEXT column with a formatted key
 * like "twitter_12345678" (prefix + archive_fmt). We parse the first
 * underscore to split into extractor vs id for display purposes.
 */
data class ArchiveEntry(val entry: String) {
    /** The extractor/category prefix (e.g. "twitter", "reddit") */
    val extractor: String
        get() {
            val idx = entry.indexOf('_')
            return if (idx > 0) entry.substring(0, idx) else "unknown"
        }

    /** The ID portion after the extractor prefix */
    val id: String
        get() {
            val idx = entry.indexOf('_')
            return if (idx > 0 && idx < entry.length - 1) entry.substring(idx + 1) else entry
        }
}

@Composable
fun ArchiveManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ArchiveEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        entries = loadArchiveEntries(context)
        isLoading = false
    }

    val filteredEntries = entries.filter {
        it.entry.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBackground)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = NeoBorder,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onNavigateBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "ARCHIVE MANAGER",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = NeoBorder
            )
        }
        
        // Search & Stats
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("SEARCH IDs OR EXTRACTORS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            NeoTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "e.g. twitter, 123456"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filteredEntries.size} ENTRIES FOUND",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NeoTextSecondary
                )
                if (filteredEntries.isNotEmpty()) {
                    Text(
                        "CLEAR ALL",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color.Red,
                        modifier = Modifier.clickable {
                            if (searchQuery.isEmpty()) {
                                clearAllArchive(context)
                                entries = emptyList()
                            } else {
                                // If searched, only clear those
                                filteredEntries.forEach { deleteArchiveEntry(context, it.entry) }
                                entries = entries.filterNot { it in filteredEntries }
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = NeoBorder, thickness = 2.dp)
        }

        val state = if (isLoading) 0 else if (entries.isEmpty()) 1 else 2
        Crossfade(targetState = state, animationSpec = tween(200), label = "ArchiveState") { currentState ->
            when (currentState) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("LOADING DATABASE...", fontWeight = FontWeight.Black, color = NeoTextSecondary)
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ARCHIVE IS EMPTY", fontWeight = FontWeight.Black, color = NeoTextSecondary)
                    }
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredEntries, key = { it.entry }) { entry ->
                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .neoBrutalist(backgroundColor = NeoTheme.colors.surface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.extractor.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = NeoTextSecondary
                                    )
                                    Text(
                                        text = entry.id,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NeoBorder
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        deleteArchiveEntry(context, entry.entry)
                                        entries = entries.filter { it.entry != entry.entry }
                                    },
                                    modifier = Modifier
                                        .neoBrutalist(backgroundColor = androidx.compose.ui.graphics.Color.Red, shadowOffset = 2.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Load all entries from gallery-dl's archive database.
 * The actual schema is: table "archive", column "entry TEXT PRIMARY KEY".
 * gallery-dl may create the table WITHOUT ROWID, so we query by entry directly.
 */
private suspend fun loadArchiveEntries(context: Context): List<ArchiveEntry> = withContext(Dispatchers.IO) {
    val dbFile = File(context.filesDir, "archive.sqlite3")
    if (!dbFile.exists()) return@withContext emptyList()

    val entries = mutableListOf<ArchiveEntry>()
    try {
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        // Check if table 'archive' exists
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='archive'", null)
        val tableExists = cursor.moveToFirst()
        cursor.close()

        if (tableExists) {
            val rows = db.rawQuery("SELECT entry FROM archive ORDER BY entry", null)
            while (rows.moveToNext()) {
                val entry = rows.getString(0) ?: continue
                entries.add(ArchiveEntry(entry))
            }
            rows.close()
        }
        db.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext entries
}

/**
 * Delete a single entry from the archive by its entry key.
 */
private fun deleteArchiveEntry(context: Context, entry: String) {
    val dbFile = File(context.filesDir, "archive.sqlite3")
    if (!dbFile.exists()) return
    try {
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        db.delete("archive", "entry = ?", arrayOf(entry))
        db.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun clearAllArchive(context: Context) {
    val dbFile = File(context.filesDir, "archive.sqlite3")
    if (dbFile.exists()) {
        dbFile.delete()
    }
}
