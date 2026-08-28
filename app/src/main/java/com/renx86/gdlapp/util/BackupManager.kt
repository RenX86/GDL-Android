package com.renx86.gdlapp.util

import android.content.Context
import android.net.Uri
import com.renx86.gdlapp.data.CompressionPreferences
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.data.ThemeMode
import com.renx86.gdlapp.data.ThemePreferences
import com.renx86.gdlapp.data.ThemeStyle
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupManager {

    fun exportBackup(context: Context, uri: Uri): Boolean {
        return try {
            val themePrefs = ThemePreferences(context)
            val cookiePrefs = CookiePreferences(context)
            val downloadPrefs = DownloadPreferences(context)
            val compPrefs = CompressionPreferences(context)

            val root = JSONObject()
            root.put("version", 1)

            val theme = JSONObject()
            theme.put("theme_mode", themePrefs.getThemeMode().name)
            theme.put("theme_style", themePrefs.getThemeStyle().name)
            root.put("theme", theme)

            val cookies = JSONObject()
            val cookieData = JSONObject()
            val loggedSitesArray = JSONArray()
            val cookieManager = android.webkit.CookieManager.getInstance()
            
            cookiePrefs.getLoggedSites().forEach { site ->
                loggedSitesArray.put(site)
                val cookieStr = cookieManager.getCookie("https://$site")
                if (cookieStr != null) {
                    cookieData.put(site, cookieStr)
                }
            }
            cookies.put("logged_sites", loggedSitesArray)
            cookies.put("cookie_data", cookieData)
            root.put("cookies", cookies)

            val downloads = JSONObject()
            downloads.put("download_path", downloadPrefs.getDownloadPath())
            downloads.put("hidden_folder", downloadPrefs.isHiddenFolderEnabled())
            downloads.put("deduplication", downloadPrefs.isDeduplicationEnabled())
            root.put("downloads", downloads)

            val compression = JSONObject()
            compression.put("auto_convert_webp", compPrefs.isAutoConvertEnabled())
            compression.put("webp_quality", compPrefs.getWebpQuality())
            compression.put("keep_original_files", compPrefs.isKeepOriginalEnabled())
            root.put("compression", compression)
            
            val archiveFile = java.io.File(context.filesDir, "archive.sqlite3")
            if (archiveFile.exists()) {
                // Checkpoint WAL to ensure all data is flushed to the main db file
                try {
                    val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        archiveFile.absolutePath, null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                    )
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                    db.close()
                } catch (_: Exception) { /* DB might not use WAL, that's fine */ }
                
                val bytes = archiveFile.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                root.put("archive_sqlite3_base64", base64)
            }

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                OutputStreamWriter(outStream).use { writer ->
                    writer.write(root.toString(2))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackup(context: Context, uri: Uri): Boolean {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inStream ->
                InputStreamReader(inStream).readText()
            } ?: return false

            val root = JSONObject(jsonString)
            
            val themePrefs = ThemePreferences(context)
            val cookiePrefs = CookiePreferences(context)
            val downloadPrefs = DownloadPreferences(context)
            val compPrefs = CompressionPreferences(context)

            // Theme
            val theme = root.optJSONObject("theme")
            if (theme != null) {
                try { themePrefs.setThemeMode(ThemeMode.valueOf(theme.optString("theme_mode", "SYSTEM"))) } catch (e: Exception) {}
                try { themePrefs.setThemeStyle(ThemeStyle.valueOf(theme.optString("theme_style", "CLASSIC"))) } catch (e: Exception) {}
            }

            // Cookies
            val cookies = root.optJSONObject("cookies")
            if (cookies != null) {
                val loggedSitesArray = cookies.optJSONArray("logged_sites")
                val cookieData = cookies.optJSONObject("cookie_data")
                val cookieManager = android.webkit.CookieManager.getInstance()
                
                if (loggedSitesArray != null) {
                    val sites = mutableSetOf<String>()
                    for (i in 0 until loggedSitesArray.length()) {
                        val site = loggedSitesArray.getString(i)
                        sites.add(site)
                        
                        // Restore raw cookies into WebView
                        if (cookieData != null && cookieData.has(site)) {
                            val rawCookie = cookieData.optString(site)
                            if (rawCookie.isNotEmpty()) {
                                rawCookie.split(";").forEach { piece ->
                                    cookieManager.setCookie("https://$site", piece.trim())
                                }
                            }
                        }
                    }
                    cookieManager.flush() // Force write to WebView database
                    
                    cookiePrefs.setLoggedSites(sites)
                    // Trigger a re-export of cookies to generate cookies.txt
                    val cookieFile = java.io.File(context.filesDir, "cookies.txt")
                    com.renx86.gdlapp.data.CookieExporter.exportAll(sites, cookieFile)
                }
            }

            // Downloads
            val downloads = root.optJSONObject("downloads")
            if (downloads != null) {
                downloadPrefs.setDownloadPath(downloads.optString("download_path", downloadPrefs.getDownloadPath()))
                downloadPrefs.setHiddenFolderEnabled(downloads.optBoolean("hidden_folder", downloadPrefs.isHiddenFolderEnabled()))
                downloadPrefs.setDeduplicationEnabled(downloads.optBoolean("deduplication", downloadPrefs.isDeduplicationEnabled()))
            }

            // Compression
            val compression = root.optJSONObject("compression")
            if (compression != null) {
                compPrefs.setAutoConvertEnabled(compression.optBoolean("auto_convert_webp", compPrefs.isAutoConvertEnabled()))
                compPrefs.setWebpQuality(compression.optInt("webp_quality", compPrefs.getWebpQuality()))
                compPrefs.setKeepOriginalEnabled(compression.optBoolean("keep_original_files", compPrefs.isKeepOriginalEnabled()))
            }
            
            // Deduplication Archive
            val archiveB64 = root.optString("archive_sqlite3_base64", null)
            if (!archiveB64.isNullOrEmpty()) {
                try {
                    val bytes = android.util.Base64.decode(archiveB64, android.util.Base64.NO_WRAP)
                    val archiveFile = java.io.File(context.filesDir, "archive.sqlite3")
                    // Delete stale WAL/SHM journal files to prevent corruption
                    java.io.File(context.filesDir, "archive.sqlite3-wal").delete()
                    java.io.File(context.filesDir, "archive.sqlite3-shm").delete()
                    archiveFile.writeBytes(bytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
