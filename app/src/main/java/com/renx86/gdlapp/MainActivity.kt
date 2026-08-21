package com.renx86.gdlapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.renx86.gdlapp.python.GalleryDlBridge
import com.renx86.gdlapp.ui.theme.GDLAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bridge: GalleryDlBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GDLAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TestScreen(bridge, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TestScreen(bridge: GalleryDlBridge, modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf("https://danbooru.donmai.us/posts/1234") }
    var result by remember { mutableStateOf("Tap a button to test") }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL to download") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                scope.launch {
                    result = "Fetching metadata..."
                    result = bridge.getInfo(url).toString(2)
                }
            }) { Text("Get Info") }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                scope.launch {
                    result = "Downloading..."
                    result = bridge.download(url).toString(2)
                }
            }) { Text("Download") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = result, style = MaterialTheme.typography.bodySmall)
    }
}