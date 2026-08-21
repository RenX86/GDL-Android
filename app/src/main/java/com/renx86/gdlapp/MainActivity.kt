package com.renx86.gdlapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import com.renx86.gdlapp.service.DownloadService
import com.renx86.gdlapp.ui.*
import com.renx86.gdlapp.ui.theme.GDLAndroidTheme
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
            NavigationBar {
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentEntry?.destination

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hasRoute(item.route::class) == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
                        navController.navigate(QueueRoute)
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