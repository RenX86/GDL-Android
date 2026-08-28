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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.renx86.gdlapp.service.DownloadService
import com.renx86.gdlapp.ui.*
import com.renx86.gdlapp.ui.theme.GDLAndroidTheme
import com.renx86.gdlapp.ui.theme.NeoBackground
import com.renx86.gdlapp.ui.theme.NeoBorder
import com.renx86.gdlapp.ui.theme.NeoYellow
import com.renx86.gdlapp.viewmodel.DownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

import androidx.compose.material.icons.filled.History

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

// Type-safe navigation routes
@Serializable object MainTabsRoute
@Serializable data class WebViewLoginRoute(val url: String)
@Serializable object ArchiveManagerRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val themePrefs = com.renx86.gdlapp.data.ThemePreferences(this)
        val isDark = when (themePrefs.getThemeMode()) {
            com.renx86.gdlapp.data.ThemeMode.LIGHT -> false
            com.renx86.gdlapp.data.ThemeMode.DARK -> true
            com.renx86.gdlapp.data.ThemeMode.SYSTEM -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

        enableEdgeToEdge(
            statusBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

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

    NavHost(
        navController = navController,
        startDestination = MainTabsRoute
    ) {
        composable<MainTabsRoute> {
            MainTabsScreen(sharedUrl, viewModel, navController)
        }
        
        composable<ArchiveManagerRoute> {
            com.renx86.gdlapp.ui.ArchiveManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<WebViewLoginRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<WebViewLoginRoute>()
            WebViewLoginScreen(
                initialUrl = route.url,
                onCookiesSaved = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun MainTabsScreen(sharedUrl: String, viewModel: DownloadViewModel, rootNavController: androidx.navigation.NavHostController) {
    val downloads by viewModel.downloads.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home),
        BottomNavItem("History", Icons.Default.History),
        BottomNavItem("Files", Icons.Default.Folder),
        BottomNavItem("Settings", Icons.Default.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NeoBackground,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(NeoBackground)
                    .border(width = 3.dp, color = NeoBorder)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = pagerState.currentPage == index
                    val bgColor = if (isSelected) NeoYellow else Color.Transparent

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    initialUrl = sharedUrl,
                    onDownload = { url ->
                        viewModel.enqueue(url)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1) // Jump to History
                        }
                    }
                )
                1 -> HistoryScreen(
                    downloads = downloads,
                    onRetry = { viewModel.retry(it) },
                    onRemove = { viewModel.removeItem(it) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
                2 -> FileBrowserScreen()
                3 -> SettingsScreen(
                    onLoginToSite = { url ->
                        rootNavController.navigate(WebViewLoginRoute(url))
                    },
                    onManageArchive = {
                        rootNavController.navigate(ArchiveManagerRoute)
                    }
                )
            }
        }
    }
}