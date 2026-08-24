package com.renx86.gdlapp.ui

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.ui.theme.*

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Big Title
        Text(
            "SETTINGS",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                color = NeoBorder
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Storage Box (NeoBlue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoBrutalist(backgroundColor = NeoBlue, shadowOffset = 8.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SdStorage, contentDescription = "Storage", tint = NeoBorder)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "STORAGE",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = NeoBorder
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Download Location:",
                    fontWeight = FontWeight.Bold,
                    color = NeoBorder
                )
                Text(
                    downloadDir?.absolutePath ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                val totalFiles = try { downloadDir?.walkTopDown()?.count { it.isFile } ?: 0 } catch(e:Exception){0}
                val totalSize = try { downloadDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L } catch(e:Exception){0}

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Files: $totalFiles", fontWeight = FontWeight.Black)
                    Text("Size: ${totalSize / 1024 / 1024} MB", fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Auth Box (NeoOrange)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoBrutalist(backgroundColor = NeoOrange, shadowOffset = 8.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cookie, contentDescription = "Authentication", tint = NeoBorder)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AUTHENTICATION",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = NeoBorder
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Cookie import and per-site credentials are coming in a future update.",
                    fontWeight = FontWeight.Bold,
                    color = NeoBorder
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Push version info to bottom

        // Version Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GDL-ANDROID V0.1.0",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = NeoBorder
            )
            Text(
                "Powered by gallery-dl + Chaquopy",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}
