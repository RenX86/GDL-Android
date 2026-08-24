package com.renx86.gdlapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.renx86.gdlapp.ui.theme.NeoTextField

@Composable
fun HomeScreen(
    initialUrl: String = "",
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(initialUrl) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .padding(23.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
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
