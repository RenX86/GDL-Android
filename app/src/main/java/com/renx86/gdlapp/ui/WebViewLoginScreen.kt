package com.renx86.gdlapp.ui

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.renx86.gdlapp.data.CookieExporter
import com.renx86.gdlapp.data.CookiePreferences
import com.renx86.gdlapp.ui.theme.*
import java.io.File

@Composable
fun WebViewLoginScreen(
    initialUrl: String,
    onCookiesSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cookiePrefs = remember { CookiePreferences(context) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
    ) {
        // ---- TOP BAR ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Lock, contentDescription = "Secure", tint = NeoGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentUrl,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        // ---- WEBVIEW ----
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    // Accept cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            url?.let { currentUrl = it }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            title?.let { pageTitle = it }
                        }
                    }

                    loadUrl(initialUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // ---- BOTTOM ACTION BAR ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoBackground)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            NeoButton(
                text = "Cancel",
                onClick = onCancel,
                color = NeoPink,
                modifier = Modifier.weight(1f)
            )

            // Save Cookies button
            NeoButton(
                text = "Save Cookies",
                onClick = {
                    val webView = webViewRef ?: return@NeoButton
                    val userAgent = webView.settings.userAgentString

                    val cookieFile = File(context.filesDir, CookiePreferences.COOKIE_FILENAME)
                    val success = CookieExporter.exportFromWebView(currentUrl, cookieFile)

                    if (success) {
                        // Save the domain and user-agent for matching during downloads
                        val domain = android.net.Uri.parse(currentUrl).host
                            ?.removePrefix("www.") ?: currentUrl
                        cookiePrefs.setCookieDomain(domain)
                        cookiePrefs.setUserAgent(userAgent)
                        cookiePrefs.setCookiesEnabled(true)

                        Toast.makeText(context, "Cookies saved for $domain ✓", Toast.LENGTH_SHORT).show()
                        onCookiesSaved()
                    } else {
                        Toast.makeText(context, "No cookies found. Try logging in first.", Toast.LENGTH_SHORT).show()
                    }
                },
                color = NeoGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
