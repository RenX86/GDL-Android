package com.renx86.gdlapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.data.CookieExporter
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.data.ThemeMode
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.data.ThemeStyle
import com.renx86.gdlapp.ui.theme.*
import java.io.File

// Purple color for the About card (not in the standard NeoColors palette)
private val NeoPurple = Color(0xFFD8B4FE)

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
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var showAdvancedAuth by remember { mutableStateOf(false) }

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
    val totalSizeMB = totalSize / 1024 / 1024

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

        // ════════════════════════════════════════
        // 1. STATS HERO CARD
        // ════════════════════════════════════════
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 8.dp)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle with GDL initials
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeoYellow, shape = androidx.compose.foundation.shape.CircleShape)
                            .border(3.dp, NeoBorder, shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "GDL",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = NeoBorder
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
                            "v0.1.5",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeoTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Downloads stat
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(NeoGreen)
                                        .border(1.dp, NeoBorder)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$totalFiles downloads",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = NeoBorder
                                )
                            }
                            // Size stat
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(NeoBlue)
                                        .border(1.dp, NeoBorder)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$totalSizeMB MB saved",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = NeoBorder
                                )
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════
        // 2. APPEARANCE (Collapsible + Segmented Toggles)
        // ════════════════════════════════════════
        item {
            NeoCollapsibleCard(
                title = "APPEARANCE",
                icon = {
                    Icon(Icons.Default.Palette, contentDescription = "Appearance", tint = NeoBorder)
                },
                backgroundColor = NeoPink
            ) {
                // Theme Style toggle
                Text("THEME STYLE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Spacer(modifier = Modifier.height(8.dp))
                NeoSegmentedToggle(
                    options = ThemeStyle.values().toList(),
                    selectedOption = currentStyle,
                    onOptionSelected = { style ->
                        themePrefs.setThemeStyle(style)
                        currentStyle = style
                        (context as? Activity)?.recreate()
                    },
                    label = { it.name }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Mode toggle
                Text("MODE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                Spacer(modifier = Modifier.height(8.dp))
                NeoSegmentedToggle(
                    options = ThemeMode.values().toList(),
                    selectedOption = currentMode,
                    onOptionSelected = { mode ->
                        themePrefs.setThemeMode(mode)
                        currentMode = mode
                        (context as? Activity)?.recreate()
                    },
                    label = { it.name }
                )
            }
        }

        // ════════════════════════════════════════
        // 3. STORAGE (Collapsible + Visual Bar)
        // ════════════════════════════════════════
        item {
            NeoCollapsibleCard(
                title = "STORAGE",
                icon = {
                    Icon(Icons.Default.SdStorage, contentDescription = "Storage", tint = NeoBorder)
                },
                backgroundColor = NeoBlue
            ) {
                // Download path
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

                // Stat labels
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

                // Visual storage bar
                val storageProgress = (totalSizeMB.toFloat() / 1024f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .border(3.dp, NeoBorder)
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NeoTheme.colors.surface)
                    )
                    // Fill bar
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

        // ════════════════════════════════════════
        // 4. AUTHENTICATION (Collapsible + Compact)
        // ════════════════════════════════════════
        item {
            NeoCollapsibleCard(
                title = "AUTH",
                icon = {
                    Icon(Icons.Default.Cookie, contentDescription = "Authentication", tint = NeoBorder)
                },
                backgroundColor = NeoOrange,
                titleTrailing = {
                    // Inline cookie status badge
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

                // Quick login site buttons — compact 2x2 grid
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

                // ---- ADVANCED AUTH (expandable sub-section) ----
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
                        Text(
                            "ADVANCED",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeoBorder
                        )
                        Text(
                            if (showAdvancedAuth) "▲" else "▼",
                            fontWeight = FontWeight.Black,
                            color = NeoBorder
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showAdvancedAuth,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom URL
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

                        // Paste / Clear buttons
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

                        // Paste dialog (inline)
                        if (showPasteDialog) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 4.dp)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        "PASTE NETSCAPE COOKIES",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = NeoBorder
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Paste a cookies.txt in Netscape format (7 tab-separated columns per line).",
                                        fontSize = 11.sp,
                                        color = NeoTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NeoTextField(
                                        value = pasteText,
                                        onValueChange = { pasteText = it },
                                        placeholder = "# Netscape HTTP Cookie File\n.domain.com\tTRUE\t/\tTRUE\t0\tname\tvalue",
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
                }
            }
        }

        // ════════════════════════════════════════
        // 5. ABOUT SECTION
        // ════════════════════════════════════════
        item {
            NeoCollapsibleCard(
                title = "ABOUT",
                icon = {
                    Icon(Icons.Default.Info, contentDescription = "About", tint = NeoBorder)
                },
                backgroundColor = NeoPurple,
                initiallyExpanded = false
            ) {
                // Version
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("VERSION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
                    Text("v0.1.5", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GitHub link
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

                // License
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LICENSE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTextSecondary)
                    Text("gallery-dl GPLv2", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBorder)
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    text = "Check for Updates",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RenX86/GDL-Android/releases"))
                        context.startActivity(intent)
                    },
                    color = NeoYellow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
