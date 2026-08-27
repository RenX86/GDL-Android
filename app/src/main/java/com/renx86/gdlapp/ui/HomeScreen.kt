package com.renx86.gdlapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.ui.theme.NeoBackground
import com.renx86.gdlapp.ui.theme.NeoBorder
import com.renx86.gdlapp.ui.theme.NeoButton
import com.renx86.gdlapp.ui.theme.NeoPink
import com.renx86.gdlapp.ui.theme.NeoTextSecondary
import com.renx86.gdlapp.ui.theme.NeoTextField
import com.renx86.gdlapp.ui.theme.NeoTheme
import com.renx86.gdlapp.ui.theme.NeoYellow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import android.os.Environment
import java.io.File
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.renx86.gdlapp.data.DownloadPreferences

@Composable
fun HomeScreen(
    initialUrl: String = "",
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(initialUrl) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    var needsSetup by remember { mutableStateOf(!prefs.getDownloadPath().startsWith("content://")) }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.setDownloadPath(uri.toString())
                needsSetup = false
            }
        }
    )

    if (needsSetup) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeoBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoTheme.colors.surface)
                    .border(4.dp, NeoBorder)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "WELCOME TO GDL",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = NeoBorder
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "To start downloading, you need to select a folder where downloaded media will be saved.",
                    color = NeoTextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        try {
                            // Pre-create the directory in the public Downloads folder
                            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val gdlFolder = File(publicDownloads, "GDL")
                            gdlFolder.mkdirs() // Allowed on Android 11+ without permission for public folders
                            
                            // Construct the document URI for this specific folder so the picker opens it automatically
                            val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FGDL")
                            directoryPickerLauncher.launch(initialUri)
                        } catch (e: Exception) {
                            // Fallback if anything fails
                            directoryPickerLauncher.launch(null)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                    modifier = Modifier.border(3.dp, NeoBorder)
                ) {
                    Text("SELECT FOLDER", color = NeoBorder, fontWeight = FontWeight.Black)
                }
            }
        }
        return // Block the home screen until folder is picked
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = 0.dp, y = 100.dp) // <--- CHANGE THESE VALUES
                .padding(horizontal = 24.dp), // Keeps it from touching the left/right screen edges
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "GDL",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    color = NeoBorder
                )
            )
        Text(
            "ANDROID",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = NeoPink
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        NeoTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = "Paste Gallery-DL URL...",
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    clipboardManager.getText()?.let { url = it.text }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeoBorder)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        NeoButton(
            text = "Download",
            onClick = {
                if (url.isNotBlank()) {
                    onDownload(url.trim())
                    url = ""
                }
            },
            enabled = url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            initialUrl = "https://example.com/gallery",
            onDownload = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
