package com.renx86.gdlapp.ui

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    Column(modifier = modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // Storage info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Download location:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    downloadDir?.absolutePath ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall
                )

                val totalFiles = downloadDir?.walkTopDown()?.count { it.isFile } ?: 0
                val totalSize = downloadDir?.walkTopDown()
                    ?.filter { it.isFile }
                    ?.sumOf { it.length() } ?: 0L

                Spacer(modifier = Modifier.height(8.dp))
                Text("Files: $totalFiles")
                Text("Total size: ${totalSize / 1024 / 1024} MB")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder for future cookie/auth settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Authentication", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cookie import and per-site credentials coming in a future update.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("GDL-Android v0.1.0", style = MaterialTheme.typography.labelSmall)
        Text("Powered by gallery-dl + Chaquopy", style = MaterialTheme.typography.labelSmall)
    }
}
