package com.renx86.gdlapp.ui

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun FileBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rootDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    var currentDir by remember { mutableStateOf(rootDir) }
    val files = remember(currentDir) {
        currentDir?.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()
    }

    Column(modifier = modifier.padding(8.dp)) {
        // Show current path relative to root
        val relativePath = currentDir?.path?.removePrefix(rootDir?.path ?: "") ?: "/"
        Text(
            "Downloads${relativePath.ifEmpty { "/" }}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        // Back button if not at root
        if (currentDir != rootDir) {
            TextButton(onClick = { currentDir = currentDir?.parentFile }) {
                Text("← Back")
            }
        }

        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files downloaded yet")
            }
        } else {
            LazyColumn {
                items(files) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        leadingContent = {
                            Icon(
                                if (file.isDirectory) Icons.Default.Folder else Icons.Default.Image,
                                contentDescription = null
                            )
                        },
                        supportingContent = {
                            if (!file.isDirectory) {
                                Text("${file.length() / 1024} KB")
                            }
                        },
                        modifier = Modifier.clickable {
                            if (file.isDirectory) {
                                currentDir = file
                            } else {
                                openFile(context, file)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun openFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // No app to handle this file type
    }
}
