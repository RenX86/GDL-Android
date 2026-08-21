package com.renx86.gdlapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    initialUrl: String = "",
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(initialUrl) }
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "GDL Android",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Paste URL") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    clipboardManager.getText()?.let { url = it.text }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isNotBlank()) {
                    onDownload(url.trim())
                    url = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = url.isNotBlank()
        ) {
            Text("Download")
        }
    }
}
