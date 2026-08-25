package com.renx86.gdlapp.ui

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.data.CookieExporter
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.ui.theme.*
import java.io.File
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable

fun SettingsScreen(
    onLoginToSite: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    val cookiePrefs = remember { CookiePreferences(context) }
    var downloadPath by remember { mutableStateOf(prefs.getDownloadPath()) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                // Keep permission to write to this folder across app restarts
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                val uriString = uri.toString()
                prefs.setDownloadPath(uriString)
                downloadPath = uriString
            }
        }
    )

    // Cookie state
    var cookieDomain by remember { mutableStateOf(cookiePrefs.getCookieDomain()) }
    var cookiesEnabled by remember { mutableStateOf(cookiePrefs.areCookiesEnabled()) }

    // Calculate stats from the current download directory
    val downloadDir = File(downloadPath)
    val totalFiles = try { downloadDir.walkTopDown().count { it.isFile } } catch (e: Exception) { 0 }
    val totalSize = try { downloadDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } } catch (e: Exception) { 0L }

    // Popular sites for quick-login
    val quickSites = listOf(
        "Twitter" to "https://twitter.com/i/flow/login",
        "Instagram" to "https://www.instagram.com/accounts/login/",
        "Pixiv" to "https://accounts.pixiv.net/login",
        "Reddit" to "https://www.reddit.com/login/"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
    ) {
        item {
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
        }

        // ---- STORAGE BOX ----
        item {
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
                        downloadPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Files: $totalFiles", fontWeight = FontWeight.Black)
                        Text("Size: ${totalSize / 1024 / 1024} MB", fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NeoButton(
                        text = "Change Folder",
                        onClick = {
                            try {
                                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val gdlFolder = File(publicDownloads, "GDL")
                                gdlFolder.mkdirs()
                                val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FGDL")
                                directoryPickerLauncher.launch(initialUri)
                            } catch (e: Exception) {
                                directoryPickerLauncher.launch(null)
                            }
                        },
                        color = NeoYellow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ---- AUTHENTICATION BOX ----
        item {
            Spacer(modifier = Modifier.height(32.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cookie status
                    if (cookieDomain != null && cookiesEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeoGreen)
                                .border(2.dp, NeoBorder)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = NeoBorder, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Cookies active for $cookieDomain",
                                    fontWeight = FontWeight.Black,
                                    color = NeoBorder
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(2.dp, NeoBorder)
                                .padding(12.dp)
                        ) {
                            Text(
                                "No cookies saved. Log in to a site below.",
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick login site buttons
                    Text(
                        "QUICK LOGIN:",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeoBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2x2 grid of site buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSites.take(2).forEach { (name, url) ->
                            NeoButton(
                                text = name,
                                onClick = { onLoginToSite(url) },
                                color = NeoYellow,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSites.drop(2).forEach { (name, url) ->
                            NeoButton(
                                text = name,
                                onClick = { onLoginToSite(url) },
                                color = NeoYellow,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom URL
                    Text(
                        "CUSTOM SITE:",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeoBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            placeholder = "https://example.com/login",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    NeoButton(
                        text = "Open Login",
                        onClick = {
                            val url = customUrl.trim()
                            if (url.isNotBlank()) {
                                val fullUrl = if (url.startsWith("http")) url else "https://$url"
                                onLoginToSite(fullUrl)
                            }
                        },
                        color = NeoBlue,
                        enabled = customUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeoButton(
                            text = "Paste",
                            onClick = { showPasteDialog = true },
                            color = NeoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        NeoButton(
                            text = "Clear",
                            onClick = {
                                val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
                                if (cookieFile.exists()) cookieFile.delete()
                                cookiePrefs.clearAll()
                                cookieDomain = null
                                cookiesEnabled = false
                                android.widget.Toast.makeText(context, "Cookies cleared", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            color = NeoPink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---- PASTE DIALOG ----
        if (showPasteDialog) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoBrutalist(backgroundColor = Color.White, shadowOffset = 6.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "PASTE NETSCAPE COOKIES",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeoBorder
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Paste a cookies.txt file in Netscape format (7 tab-separated columns per line).",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NeoTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = "# Netscape HTTP Cookie File\n.domain.com\tTRUE\t/\tTRUE\t0\tname\tvalue",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeoButton(
                                text = "Import",
                                onClick = {
                                    val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
                                    val success = CookieExporter.exportFromPaste(pasteText, cookieFile)
                                    if (success) {
                                        cookiePrefs.setCookieDomain("manual-import")
                                        cookiePrefs.setCookiesEnabled(true)
                                        cookieDomain = "manual-import"
                                        cookiesEnabled = true
                                        showPasteDialog = false
                                        pasteText = ""
                                        android.widget.Toast.makeText(context, "Cookies imported ✓", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Invalid format. Need 7 tab-separated columns.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                color = NeoGreen,
                                modifier = Modifier.weight(1f)
                            )
                            NeoButton(
                                text = "Cancel",
                                onClick = {
                                    showPasteDialog = false
                                    pasteText = ""
                                },
                                color = NeoPink,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ---- VERSION INFO ----
        item {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GDL-ANDROID V0.1.2",
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
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}
