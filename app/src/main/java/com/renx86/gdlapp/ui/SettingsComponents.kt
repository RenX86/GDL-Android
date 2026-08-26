package com.renx86.gdlapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.data.CookieExporter
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.ThemeMode
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.data.ThemeStyle
import com.renx86.gdlapp.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Purple color for the About card (not in the standard NeoColors palette)
val NeoPurple = Color(0xFFD8B4FE)

@Composable
fun SettingsHeroCard(versionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 8.dp)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(NeoYellow, shape = androidx.compose.foundation.shape.CircleShape)
                    .border(3.dp, NeoBorder, shape = androidx.compose.foundation.shape.CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            setImageResource(com.renx86.gdlapp.R.mipmap.ic_launcher)
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "GDL ANDROID",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = NeoBorder
                )
                Text(
                    "v$versionName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeoTextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsAppearanceSection(
    currentStyle: ThemeStyle,
    currentMode: ThemeMode,
    themePrefs: ThemePreferences,
    context: Context
) {
    var styleState by remember(currentStyle) { mutableStateOf(currentStyle) }
    var modeState by remember(currentMode) { mutableStateOf(currentMode) }

    NeoCollapsibleCard(
        title = "APPEARANCE",
        icon = { Icon(Icons.Default.Palette, contentDescription = "Appearance", tint = NeoBorder) },
        backgroundColor = NeoPink,
        initiallyExpanded = false
    ) {
        Text("THEME STYLE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Spacer(modifier = Modifier.height(8.dp))
        NeoSegmentedToggle(
            options = ThemeStyle.values().toList(),
            selectedOption = styleState,
            onOptionSelected = { style ->
                themePrefs.setThemeStyle(style)
                styleState = style
                (context as? Activity)?.recreate()
            },
            label = { it.name }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("MODE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Spacer(modifier = Modifier.height(8.dp))
        NeoSegmentedToggle(
            options = ThemeMode.values().toList(),
            selectedOption = modeState,
            onOptionSelected = { mode ->
                themePrefs.setThemeMode(mode)
                modeState = mode
                (context as? Activity)?.recreate()
            },
            label = { it.name }
        )
    }
}

@Composable
fun SettingsStorageSection(
    downloadPath: String,
    totalFiles: Int,
    totalSizeMB: Long,
    directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>
) {
    NeoCollapsibleCard(
        title = "STORAGE",
        icon = { Icon(Icons.Default.SdStorage, contentDescription = "Storage", tint = NeoBorder) },
        backgroundColor = NeoBlue,
        initiallyExpanded = false
    ) {
        Text("DOWNLOAD LOCATION", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            downloadPath,
            style = MaterialTheme.typography.bodySmall,
            color = NeoTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("FILES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeoTextSecondary)
                Text("$totalFiles", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SIZE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeoTextSecondary)
                Text("$totalSizeMB MB", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val storageProgress = (totalSizeMB.toFloat() / 1024f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .border(3.dp, NeoBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeoTheme.colors.surface)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = storageProgress.coerceAtLeast(0.02f))
                    .background(NeoYellow)
            )
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

@Composable
fun SettingsAuthSection(
    cookieDomain: String?,
    cookiesEnabled: Boolean,
    quickSites: List<Pair<String, String>>,
    onLoginToSite: (String) -> Unit,
    cookiePrefs: CookiePreferences,
    context: Context,
    onCookiesCleared: () -> Unit,
    onCookiesImported: (String) -> Unit
) {
    var showAdvancedAuth by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    NeoCollapsibleCard(
        title = "AUTH",
        icon = { Icon(Icons.Default.Cookie, contentDescription = "Authentication", tint = NeoBorder) },
        backgroundColor = NeoOrange,
        initiallyExpanded = false,
        titleTrailing = {
            if (cookieDomain != null && cookiesEnabled) {
                Box(
                    modifier = Modifier
                        .background(NeoGreen)
                        .border(2.dp, NeoBorder)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = NeoBorder,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            cookieDomain ?: "",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = NeoBorder,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) {
        if (cookieDomain == null || !cookiesEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoTheme.colors.surface)
                    .border(2.dp, NeoBorder)
                    .padding(12.dp)
            ) {
                Text(
                    "No cookies saved. Log in to a site below.",
                    fontWeight = FontWeight.Bold,
                    color = NeoTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("QUICK LOGIN", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Spacer(modifier = Modifier.height(8.dp))

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvancedAuth = !showAdvancedAuth }
                .neoBrutalist(
                    backgroundColor = NeoTheme.colors.surface,
                    borderWidth = 2.dp,
                    shadowOffset = 3.dp
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ADVANCED", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Text(if (showAdvancedAuth) "▲" else "▼", fontWeight = FontWeight.Black, color = NeoBorder)
            }
        }

        AnimatedVisibility(visible = showAdvancedAuth, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text("CUSTOM SITE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Spacer(modifier = Modifier.height(8.dp))
                NeoTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    placeholder = "https://example.com/login",
                    modifier = Modifier.fillMaxWidth()
                )
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
                            onCookiesCleared()
                            android.widget.Toast.makeText(context, "Cookies cleared", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        color = NeoPink,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showPasteDialog) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 4.dp)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("PASTE NETSCAPE COOKIES", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Paste a cookies.txt in Netscape format.", fontSize = 11.sp, color = NeoTextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            NeoTextField(
                                value = pasteText,
                                onValueChange = { pasteText = it },
                                placeholder = "# Netscape HTTP Cookie File...",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                                            onCookiesImported("manual-import")
                                            showPasteDialog = false
                                            pasteText = ""
                                            android.widget.Toast.makeText(context, "Cookies imported ✓", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Invalid format.", android.widget.Toast.LENGTH_SHORT).show()
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
        }
    }
}

@Composable
fun SettingsAboutSection(
    versionName: String,
    updateStatus: String?,
    isCheckingUpdate: Boolean,
    scope: CoroutineScope,
    context: Context,
    onUpdateStatusChanged: (String?) -> Unit,
    onCheckingChanged: (Boolean) -> Unit
) {
    NeoCollapsibleCard(
        title = "ABOUT",
        icon = { Icon(Icons.Default.Info, contentDescription = "About", tint = NeoBorder) },
        backgroundColor = NeoPurple,
        initiallyExpanded = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VERSION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
            Text("v$versionName", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GITHUB", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
            Text(
                "RenX86/GDL-Android",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = NeoBlue,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RenX86/GDL-Android"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LICENSE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
            Text("GNU GPLv3", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (updateStatus != null) {
            Text(
                text = updateStatus,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (updateStatus.contains("available")) NeoGreen else NeoBorder,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        NeoButton(
            text = if (isCheckingUpdate) "Checking..." else if (updateStatus?.contains("available") == true) "Download Update" else "Check for Updates",
            onClick = {
                if (isCheckingUpdate) return@NeoButton
                
                if (updateStatus?.contains("available") == true) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RenX86/GDL-Android/releases/latest"))
                    context.startActivity(intent)
                    return@NeoButton
                }
                
                onCheckingChanged(true)
                onUpdateStatusChanged("Checking...")
                
                scope.launch(Dispatchers.IO) {
                    try {
                        val url = URL("https://api.github.com/repos/RenX86/GDL-Android/releases/latest")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                        
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().readText()
                            val json = org.json.JSONObject(response)
                            val latestVersion = json.getString("tag_name").removePrefix("v")
                            val currentVersion = versionName.removePrefix("v")
                            
                            withContext(Dispatchers.Main) {
                                if (latestVersion == currentVersion) {
                                    onUpdateStatusChanged("You are on the latest version!")
                                } else {
                                    onUpdateStatusChanged("Update available: v$latestVersion!")
                                }
                                onCheckingChanged(false)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onUpdateStatusChanged("Failed to check (Error ${connection.responseCode})")
                                onCheckingChanged(false)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onUpdateStatusChanged("Error checking for updates.")
                            onCheckingChanged(false)
                        }
                    }
                }
            },
            color = if (updateStatus?.contains("available") == true) NeoGreen else NeoYellow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
