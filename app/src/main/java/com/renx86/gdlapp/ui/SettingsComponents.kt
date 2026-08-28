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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
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
fun SettingsHeroCard(
    versionName: String,
    updateStatus: String?,
    updateApkUrl: String?,
    isCheckingUpdate: Boolean,
    scope: CoroutineScope,
    context: Context,
    onUpdateStatusChanged: (String?) -> Unit,
    onApkUrlFetched: (String?) -> Unit,
    onCheckingChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 8.dp)
            .padding(20.dp)
    ) {
        Column {
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

            Spacer(modifier = Modifier.height(16.dp))

            if (updateStatus != null) {
                Text(
                    text = updateStatus,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (updateStatus.contains("available")) NeoGreen else NeoBorder,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            NeoButton(
                text = if (isCheckingUpdate) "Checking..." else if (updateStatus?.contains("available") == true) "Download Update" else "Check for Updates",
                onClick = {
                    if (isCheckingUpdate) return@NeoButton
                    
                    if (updateStatus?.contains("available") == true) {
                        if (updateApkUrl != null) {
                            try {
                                val request = android.app.DownloadManager.Request(android.net.Uri.parse(updateApkUrl))
                                    .setTitle("GDL Android Update")
                                    .setDescription("Downloading update...")
                                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "GDLAndroid-update.apk")
                                    .setMimeType("application/vnd.android.package-archive")

                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                val downloadId = downloadManager.enqueue(request)
                                android.widget.Toast.makeText(context, "Downloading update...", android.widget.Toast.LENGTH_SHORT).show()
                                
                                // Listen for completion and launch install dialog
                                val receiver = object : android.content.BroadcastReceiver() {
                                    override fun onReceive(ctx: Context, intent: android.content.Intent) {
                                        val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                                        if (id == downloadId) {
                                            try {
                                                ctx.unregisterReceiver(this)
                                            } catch (e: Exception) {}
                                            
                                            try {
                                                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                                val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                                installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
                                                installIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                ctx.startActivity(installIntent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
                                } else {
                                    context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                                }

                            } catch (e: Exception) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/RenX86/GDL-Android/releases/latest"))
                                context.startActivity(intent)
                            }
                        } else {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/RenX86/GDL-Android/releases/latest"))
                            context.startActivity(intent)
                        }
                        return@NeoButton
                    }
                    
                    onCheckingChanged(true)
                    onUpdateStatusChanged("Checking...")
                    
                    scope.launch(Dispatchers.IO) {
                        try {
                            val url = java.net.URL("https://api.github.com/repos/RenX86/GDL-Android/releases/latest")
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                            
                            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                val response = connection.inputStream.bufferedReader().readText()
                                val json = org.json.JSONObject(response)
                                val latestVersion = json.getString("tag_name").removePrefix("v")
                                val currentVersion = versionName.removePrefix("v")
                                
                                var apkUrl: String? = null
                                val assets = json.optJSONArray("assets")
                                if (assets != null) {
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.getJSONObject(i)
                                        val name = asset.getString("name")
                                        if (name.endsWith(".apk")) {
                                            apkUrl = asset.getString("browser_download_url")
                                            break
                                        }
                                    }
                                }
                                
                                withContext(Dispatchers.Main) {
                                    if (latestVersion == currentVersion) {
                                        onUpdateStatusChanged("You are on the latest version!")
                                    } else {
                                        onUpdateStatusChanged("Update available: v$latestVersion!")
                                        onApkUrlFetched(apkUrl)
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
    directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    isHiddenEnabled: Boolean,
    onHiddenToggled: (Boolean) -> Unit
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

        Text("FOLDER VISIBILITY", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Spacer(modifier = Modifier.height(8.dp))
        NeoSegmentedToggle(
            options = listOf(false, true),
            selectedOption = isHiddenEnabled,
            onOptionSelected = onHiddenToggled,
            label = { if (it) "HIDDEN (.GDL)" else "NORMAL (GDL)" }
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

        val storageFraction = (totalSizeMB.toFloat() / 1024f).coerceIn(0f, 1f)
        val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = storageFraction,
            animationSpec = androidx.compose.animation.core.tween(1000),
            label = "StorageProgress"
        )
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
                    .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.02f))
                    .background(NeoYellow)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeoButton(
            text = "Change Folder",
            onClick = {
                try {
                    val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val folderName = if (isHiddenEnabled) ".GDL" else "GDL"
                    val gdlFolder = File(publicDownloads, folderName)
                    gdlFolder.mkdirs()
                    val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2F$folderName")
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
fun SettingsDeduplicationSection(
    isDeduplicationEnabled: Boolean,
    onDeduplicationToggled: (Boolean) -> Unit,
    onManageArchive: () -> Unit
) {
    NeoCollapsibleCard(
        title = "DEDUPLICATION",
        icon = { Icon(Icons.Default.List, contentDescription = "Deduplication", tint = NeoBorder) },
        backgroundColor = NeoTheme.colors.orange,
        initiallyExpanded = false
    ) {
        Text("SKIP EXISTS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Text("Uses a fast SQLite database to track what you've downloaded to skip duplicates.", fontSize = 11.sp, color = NeoTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        NeoSegmentedToggle(
            options = listOf(true, false),
            selectedOption = isDeduplicationEnabled,
            onOptionSelected = onDeduplicationToggled,
            label = { if (it) "ON (SKIP)" else "OFF (DOWNLOAD ALL)" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        NeoButton(
            text = "Manage Download History",
            onClick = onManageArchive,
            color = NeoTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsAuthSection(
    loggedSites: Set<String>,
    quickSites: List<Pair<String, String>>,
    onLoginToSite: (String) -> Unit,
    cookiePrefs: CookiePreferences,
    context: Context,
    onCookiesUpdated: () -> Unit
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
            if (loggedSites.isNotEmpty()) {
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
                            "${loggedSites.size} Active",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = NeoBorder
                        )
                    }
                }
            }
        }
    ) {
        if (loggedSites.isEmpty()) {
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
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                loggedSites.forEach { site ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 3.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = NeoGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(site, fontWeight = FontWeight.Black, color = NeoBorder)
                        }
                        
                        Text(
                            "REMOVE",
                            color = NeoPink,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                cookiePrefs.removeLoggedSite(site)
                                val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
                                CookieExporter.exportAll(cookiePrefs.getLoggedSites(), cookieFile)
                                android.webkit.CookieManager.getInstance().setCookie("https://$site", "")
                                android.webkit.CookieManager.getInstance().flush()
                                onCookiesUpdated()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("ADD ACCOUNT", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
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
                        text = "Paste Cookies",
                        onClick = { showPasteDialog = true },
                        color = NeoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    NeoButton(
                        text = "Clear All",
                        onClick = {
                            val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
                            if (cookieFile.exists()) cookieFile.delete()
                            cookiePrefs.clearAll()
                            onCookiesUpdated()
                            android.widget.Toast.makeText(context, "All cookies cleared", android.widget.Toast.LENGTH_SHORT).show()
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
                                            android.widget.Toast.makeText(context, "Cookies imported!", android.widget.Toast.LENGTH_SHORT).show()
                                            showPasteDialog = false
                                            pasteText = ""
                                            onCookiesUpdated()
                                        } else {
                                            android.widget.Toast.makeText(context, "Invalid format", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    color = NeoGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                NeoButton(
                                    text = "Cancel",
                                    onClick = { showPasteDialog = false },
                                    color = NeoTheme.colors.surface,
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
fun SettingsBackupSection(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    NeoCollapsibleCard(
        title = "BACKUP & RESTORE",
        icon = { Icon(Icons.Default.Save, contentDescription = "Backup", tint = NeoBorder) },
        backgroundColor = NeoOrange,
        initiallyExpanded = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeoButton(
                text = "Export Settings",
                onClick = onBackupClick,
                color = NeoBlue,
                modifier = Modifier.weight(1f)
            )
            NeoButton(
                text = "Import Settings",
                onClick = onRestoreClick,
                color = NeoYellow,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SettingsCompressionSection(
    autoConvert: Boolean,
    onAutoConvertChange: (Boolean) -> Unit,
    quality: Float,
    onQualityChange: (Float) -> Unit,
    keepOriginal: Boolean,
    onKeepOriginalChange: (Boolean) -> Unit,
    downloadPath: String,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var isConverting by remember { mutableStateOf(false) }
    var convertProgress by remember { mutableFloatStateOf(0f) }
    var convertText by remember { mutableStateOf("") }

    NeoCollapsibleCard(
        title = "COMPRESSION",
        icon = { Icon(Icons.Default.Image, contentDescription = "Compression", tint = NeoBorder) },
        backgroundColor = NeoPink,
        initiallyExpanded = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AUTO-CONVERT", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Text("Convert new downloads to WebP", fontSize = 11.sp, color = NeoTextSecondary)
            }
            Switch(
                checked = autoConvert,
                onCheckedChange = onAutoConvertChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeoYellow,
                    checkedTrackColor = NeoBorder,
                    uncheckedThumbColor = NeoTheme.colors.surface,
                    uncheckedTrackColor = NeoTextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("KEEP ORIGINALS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Text("Don't delete original JPG/PNG", fontSize = 11.sp, color = NeoTextSecondary)
            }
            Switch(
                checked = keepOriginal,
                onCheckedChange = onKeepOriginalChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeoYellow,
                    checkedTrackColor = NeoBorder,
                    uncheckedThumbColor = NeoTheme.colors.surface,
                    uncheckedTrackColor = NeoTextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("WEBP QUALITY: ${quality.toInt()}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
        Slider(
            value = quality,
            onValueChange = onQualityChange,
            valueRange = 10f..100f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = NeoYellow,
                activeTrackColor = NeoBorder,
                inactiveTrackColor = NeoTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isConverting) {
            Text(convertText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoBorder)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { convertProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .border(2.dp, NeoBorder),
                color = NeoYellow,
                trackColor = NeoTheme.colors.surface,
            )
        } else {
            NeoButton(
                text = "Compress Existing Library",
                onClick = {
                    isConverting = true
                    convertProgress = 0f
                    convertText = "Scanning..."
                    
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val allFiles = mutableListOf<androidx.documentfile.provider.DocumentFile>()
                            val allJavaFiles = mutableListOf<File>()
                            var total = 0
                            
                            if (downloadPath.startsWith("content://")) {
                                val uri = android.net.Uri.parse(downloadPath)
                                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                                if (docFile != null) {
                                    fun walk(dir: androidx.documentfile.provider.DocumentFile) {
                                        for (child in dir.listFiles()) {
                                            if (child.isDirectory) walk(child)
                                            else {
                                                val ext = child.name?.substringAfterLast('.', "")?.lowercase()
                                                if (ext in listOf("jpg", "jpeg", "png")) {
                                                    allFiles.add(child)
                                                }
                                            }
                                        }
                                    }
                                    walk(docFile)
                                    total = allFiles.size
                                }
                            } else {
                                val dir = File(downloadPath)
                                if (dir.exists()) {
                                    allJavaFiles.addAll(dir.walkTopDown().filter { 
                                        it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") 
                                    })
                                    total = allJavaFiles.size
                                }
                            }

                            if (total == 0) {
                                convertText = "No JPG/PNG files found."
                                kotlinx.coroutines.delay(2000)
                                isConverting = false
                                return@launch
                            }

                            var converted = 0
                            val targetQuality = quality.toInt()

                            if (allJavaFiles.isNotEmpty()) {
                                for (file in allJavaFiles) {
                                    convertText = "Converting ${file.name}..."
                                    com.renx86.gdlapp.util.WebPConverter.convertFileToWebp(file, targetQuality, keepOriginal)
                                    converted++
                                    convertProgress = converted.toFloat() / total
                                }
                            } else {
                                // SAF conversion is harder because WebPConverter takes a java.io.File
                                // For now, we'll download to cache, convert, and overwrite
                                for (docFile in allFiles) {
                                    convertText = "Converting ${docFile.name}..."
                                    try {
                                        val tempFile = File(context.cacheDir, docFile.name ?: "temp.jpg")
                                        context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                                            tempFile.outputStream().use { output -> input.copyTo(output) }
                                        }
                                        val webpFile = com.renx86.gdlapp.util.WebPConverter.convertFileToWebp(tempFile, targetQuality, keepOriginal)
                                        if (webpFile.exists() && webpFile.name.endsWith(".webp")) {
                                            val parent = docFile.parentFile
                                            if (!keepOriginal) {
                                                docFile.delete()
                                            }
                                            parent?.createFile("image/webp", webpFile.name)?.let { newDoc ->
                                                context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
                                                    webpFile.inputStream().use { input -> input.copyTo(out) }
                                                }
                                            }
                                        }
                                        if (webpFile.exists()) webpFile.delete()
                                        if (tempFile.exists()) tempFile.delete() // just in case
                                    } catch (e: Exception) {}
                                    converted++
                                    convertProgress = converted.toFloat() / total
                                }
                            }

                            convertText = "Done! Converted $converted files."
                            kotlinx.coroutines.delay(2000)
                        } catch (e: Exception) {
                            convertText = "Error: ${e.message}"
                            kotlinx.coroutines.delay(2000)
                        } finally {
                            isConverting = false
                        }
                    }
                },
                color = NeoYellow,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsAboutSection(
    versionName: String,
    context: Context
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
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/RenX86/GDL-Android"))
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
    }
}
