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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    val cookiePrefs = remember { CookiePreferences(context) }
    val themePrefs = remember { ThemePreferences(context) }
    
    var currentMode by remember { mutableStateOf(themePrefs.getThemeMode()) }
    var currentStyle by remember { mutableStateOf(themePrefs.getThemeStyle()) }
    var downloadPath by remember { mutableStateOf(prefs.getDownloadPath()) }
    
    var updateStatus by remember { mutableStateOf<String?>(null) }
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

    // Cookie state
    var cookieDomain by remember { mutableStateOf(cookiePrefs.getCookieDomain()) }
    var cookiesEnabled by remember { mutableStateOf(cookiePrefs.areCookiesEnabled()) }

    var totalFiles by remember { mutableIntStateOf(0) }
    var totalSize by remember { mutableLongStateOf(0L) }
    var isCalculatingStats by remember { mutableStateOf(false) }

    // Calculate stats async
    LaunchedEffect(downloadPath) {
        isCalculatingStats = true
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
            SettingsHeroCard(versionName = versionName)
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
                directoryPickerLauncher = directoryPickerLauncher
            )
        }

        item {
            SettingsAuthSection(
                cookieDomain = cookieDomain,
                cookiesEnabled = cookiesEnabled,
                quickSites = quickSites,
                onLoginToSite = onLoginToSite,
                cookiePrefs = cookiePrefs,
                context = context,
                onCookiesCleared = {
                    cookieDomain = null
                    cookiesEnabled = false
                },
                onCookiesImported = { domain ->
                    cookieDomain = domain
                    cookiesEnabled = true
                }
            )
        }

        item {
            SettingsAboutSection(
                versionName = versionName,
                updateStatus = updateStatus,
                isCheckingUpdate = isCheckingUpdate,
                scope = scope,
                context = context,
                onUpdateStatusChanged = { updateStatus = it },
                onCheckingChanged = { isCheckingUpdate = it }
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
