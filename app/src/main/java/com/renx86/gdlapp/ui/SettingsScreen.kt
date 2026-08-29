package com.renx86.gdlapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.ui.theme.NeoBackground
import com.renx86.gdlapp.ui.theme.NeoBorder
import java.io.File

@Composable
fun SettingsScreen(
    onLoginToSite: (String) -> Unit = {},
    onManageArchive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    val cookiePrefs = remember { CookiePreferences(context) }
    val themePrefs = remember { ThemePreferences(context) }
    val compPrefs = remember { com.renx86.gdlapp.data.CompressionPreferences(context) }
    
    var currentMode by remember { mutableStateOf(themePrefs.getThemeMode()) }
    var currentStyle by remember { mutableStateOf(themePrefs.getThemeStyle()) }
    var downloadPath by remember { mutableStateOf(prefs.getDownloadPath()) }
    var isHiddenFolderEnabled by remember { mutableStateOf(prefs.isHiddenFolderEnabled()) }
    
    var autoConvertWebp by remember { mutableStateOf(compPrefs.isAutoConvertEnabled()) }
    var webpQuality by remember { mutableStateOf(compPrefs.getWebpQuality().toFloat()) }
    var keepOriginalFiles by remember { mutableStateOf(compPrefs.isKeepOriginalEnabled()) }
    
    var isDeduplicationEnabled by remember { mutableStateOf(prefs.isDeduplicationEnabled()) }
    
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateApkUrl by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

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

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            if (uri != null) {
                val success = com.renx86.gdlapp.util.BackupManager.exportBackup(context, uri)
                android.widget.Toast.makeText(context, if (success) "Backup Saved!" else "Backup Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val success = com.renx86.gdlapp.util.BackupManager.importBackup(context, uri)
                if (success) {
                    android.widget.Toast.makeText(context, "Settings Restored!", android.widget.Toast.LENGTH_SHORT).show()
                    (context as? android.app.Activity)?.recreate()
                } else {
                    android.widget.Toast.makeText(context, "Restore Failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Cookie state
    var loggedSites by remember { mutableStateOf(cookiePrefs.getLoggedSites()) }

    var totalFiles by remember { mutableIntStateOf(0) }
    var totalSize by remember { mutableLongStateOf(0L) }
    var isCalculatingStats by remember { mutableStateOf(false) }

    // Calculate stats async
    LaunchedEffect(downloadPath, isCalculatingStats) {
        if (!isCalculatingStats) return@LaunchedEffect
        
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (downloadPath.startsWith("content://")) {
                    val uri = android.net.Uri.parse(downloadPath)
                    val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                    if (docFile != null && docFile.exists()) {
                        var fCount = 0
                        var sSum = 0L
                        fun walk(dir: androidx.documentfile.provider.DocumentFile) {
                            for (child in dir.listFiles()) {
                                if (child.isDirectory) walk(child)
                                else {
                                    fCount++
                                    sSum += child.length()
                                }
                            }
                        }
                        walk(docFile)
                        totalFiles = fCount
                        totalSize = sSum
                    }
                } else {
                    val dir = java.io.File(downloadPath)
                    if (dir.exists()) {
                        totalFiles = dir.walkTopDown().count { it.isFile }
                        totalSize = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    }
                }
            } catch (e: Exception) {
                totalFiles = 0
                totalSize = 0L
            }
        }
        isCalculatingStats = false
    }

    val totalSizeMB = totalSize / 1024 / 1024

    // Make SAF string readable
    val displayPath = if (downloadPath.startsWith("content://")) {
        try {
            val uri = android.net.Uri.parse(downloadPath)
            android.net.Uri.decode(uri.path)?.removePrefix("/tree/")?.replace(":", " > ") ?: downloadPath
        } catch (e: Exception) {
            downloadPath
        }
    } else {
        downloadPath
    }

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
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ---- TITLE ----
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SETTINGS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = NeoBorder
                    )
                )
            }
        }

        item {
            SettingsHeroCard(
                versionName = versionName,
                updateStatus = updateStatus,
                updateApkUrl = updateApkUrl,
                isCheckingUpdate = isCheckingUpdate,
                scope = scope,
                context = context,
                onUpdateStatusChanged = { updateStatus = it },
                onApkUrlFetched = { updateApkUrl = it },
                onCheckingChanged = { isCheckingUpdate = it }
            )
        }

        item {
            SettingsAppearanceSection(
                currentStyle = currentStyle,
                currentMode = currentMode,
                themePrefs = themePrefs,
                context = context
            )
        }

        item {
            SettingsStorageSection(
                downloadPath = displayPath,
                totalFiles = totalFiles,
                totalSizeMB = totalSizeMB,
                isCalculating = isCalculatingStats,
                onCalculateRequest = { isCalculatingStats = true },
                directoryPickerLauncher = directoryPickerLauncher,
                isHiddenEnabled = isHiddenFolderEnabled,
                onHiddenToggled = { enabled ->
                    prefs.setHiddenFolderEnabled(enabled)
                    isHiddenFolderEnabled = enabled
                    
                    // Create the folder immediately so it exists for the picker
                    val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val folderName = if (enabled) ".GDL" else "GDL"
                    val folder = java.io.File(publicDownloads, folderName)
                    folder.mkdirs()
                    
                    // Launch the picker to get SAF permission for the new folder
                    val uriString = "content://com.android.externalstorage.documents/document/primary%3ADownload%2F$folderName"
                    directoryPickerLauncher.launch(android.net.Uri.parse(uriString))
                }
            )
        }

        item {
            SettingsDeduplicationSection(
                isDeduplicationEnabled = isDeduplicationEnabled,
                onDeduplicationToggled = {
                    isDeduplicationEnabled = it
                    prefs.setDeduplicationEnabled(it)
                },
                onManageArchive = onManageArchive
            )
        }

        item {
            SettingsAuthSection(
                loggedSites = loggedSites,
                quickSites = quickSites,
                onLoginToSite = onLoginToSite,
                cookiePrefs = cookiePrefs,
                context = context,
                onCookiesUpdated = {
                    loggedSites = cookiePrefs.getLoggedSites()
                }
            )
        }

        item {
            SettingsCompressionSection(
                autoConvert = autoConvertWebp,
                onAutoConvertChange = { 
                    autoConvertWebp = it
                    compPrefs.setAutoConvertEnabled(it)
                },
                quality = webpQuality,
                onQualityChange = { 
                    webpQuality = it
                    compPrefs.setWebpQuality(it.toInt())
                },
                keepOriginal = keepOriginalFiles,
                onKeepOriginalChange = {
                    keepOriginalFiles = it
                    compPrefs.setKeepOriginalEnabled(it)
                },
                downloadPath = downloadPath,
                context = context,
                scope = scope
            )
        }

        item {
            SettingsBackupSection(
                onBackupClick = {
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    backupLauncher.launch("GDL_Backup_$dateStr.gdl.bkp")
                },
                onRestoreClick = {
                    restoreLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        item {
            SettingsAboutSection(
                versionName = versionName,
                context = context
            )
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
