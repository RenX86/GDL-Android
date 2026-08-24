package com.renx86.gdlapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.renx86.gdlapp.service.DownloadService
import com.renx86.gdlapp.ui.*
import com.renx86.gdlapp.ui.theme.GDLAndroidTheme
import com.renx86.gdlapp.ui.theme.NeoBackground
import com.renx86.gdlapp.ui.theme.NeoBorder
import com.renx86.gdlapp.ui.theme.NeoYellow
import com.renx86.gdlapp.viewmodel.DownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

// Type-safe navigation routes
@Serializable object HomeRoute
@Serializable object QueueRoute
@Serializable object FilesRoute
@Serializable object SettingsRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+ (API 33+)
        // Without this, the foreground service notification is silently hidden
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Request full storage access on Android 11+ so gallery-dl can write
        // to the public Downloads folder (or any user-chosen folder).
        // This opens a system settings page where the user toggles it on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // Extract shared URL if launched via share intent
        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            else -> ""
        }

        setContent {
            GDLAndroidTheme {
                MainApp(sharedUrl = sharedUrl)
            }
        }
    }
}

@Composable
fun MainApp(sharedUrl: String = "") {
    val navController = rememberNavController()
    val viewModel: DownloadViewModel = hiltViewModel()
    val downloads by viewModel.downloads.collectAsState()

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, HomeRoute),
        BottomNavItem("Queue", Icons.Default.Download, QueueRoute),
        BottomNavItem("Files", Icons.Default.Folder, FilesRoute),
        BottomNavItem("Settings", Icons.Default.Settings, SettingsRoute),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Custom Neobrutalist bottom bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoBackground)
                    .border(width = 3.dp, color = NeoBorder)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentEntry?.destination

                navItems.forEach { item ->
                    val isSelected = currentDestination?.hasRoute(item.route::class) == true
                    val bgColor = if (isSelected) NeoYellow else Color.Transparent

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                            .then(
                                if (isSelected) Modifier
                                    .background(bgColor)
                                    .border(2.dp, NeoBorder)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                else Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = NeoBorder,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.label.uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = NeoBorder
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    initialUrl = sharedUrl,
                    onDownload = { url ->
                        viewModel.enqueue(url)
                        // Navigate to Queue using the same pattern as bottom nav
                        navController.navigate(QueueRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<QueueRoute> {
                QueueScreen(
                    downloads = downloads,
                    onRetry = { viewModel.retry(it) },
                    onRemove = { viewModel.removeItem(it) }
                )
            }

            composable<FilesRoute> {
                FileBrowserScreen()
            }

            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}