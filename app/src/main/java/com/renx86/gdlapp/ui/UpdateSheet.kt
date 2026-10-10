package com.renx86.gdlapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.ui.theme.*
import com.renx86.gdlapp.viewmodel.UpdateState
import com.renx86.gdlapp.viewmodel.UpdateViewModel
import com.renx86.gdlapp.util.ApkInstaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // True only while the user is in system settings granting "install unknown apps"
    var awaitingPermission by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val tryInstall: (java.io.File) -> Unit = { apk ->
        if (!ApkInstaller.canInstallUnknownApps(context)) {
            awaitingPermission = true
            ApkInstaller.openInstallPermissionSettings(context)
        } else {
            ApkInstaller.launchInstaller(context, apk)
        }
    }

    // Removed auto-launch to prevent infinite popping up when returning from installer.
    // User must click 'OPEN INSTALLER' manually.

    // Only resume the install if we're coming back from the permission settings screen.
    // Returning after cancelling the installer itself must NOT relaunch it.
    val latestState by rememberUpdatedState(state)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingPermission) {
                awaitingPermission = false
                val ready = latestState as? UpdateState.ReadyToInstall
                if (ready != null && ApkInstaller.canInstallUnknownApps(context)) {
                    ApkInstaller.launchInstaller(context, ready.apk)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeoBackground,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    if (initialState::class == targetState::class) {
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    } else {
                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
                    }
                },
                label = "UpdateStateContent",
                contentAlignment = Alignment.TopCenter
            ) { currentState ->
                when (currentState) {
                    is UpdateState.Idle, is UpdateState.UpToDate, is UpdateState.Checking -> {
                        // These states usually mean the sheet shouldn't be open,
                        // but we handle Checking here in case it was opened manually.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) {
                            if (currentState is UpdateState.Checking) {
                                CircularProgressIndicator(color = NeoBorder, strokeWidth = 4.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Checking for updates...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeoGreen, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("You're up to date!", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
                                Spacer(modifier = Modifier.height(32.dp))
                                NeoButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    
                    is UpdateState.Available -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Update, contentDescription = null, tint = NeoBorder, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("UPDATE AVAILABLE", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
                                    Text("v${currentState.info.version.removePrefix("v")} • ${String.format("%.1f", currentState.info.apkSizeBytes / 1024f / 1024f)} MB", fontWeight = FontWeight.Bold, color = NeoTextSecondary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 4.dp)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(currentState.info.changelog, color = NeoBorder, fontSize = 14.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            NeoButton(
                                text = "DOWNLOAD & INSTALL",
                                onClick = { viewModel.startDownload(currentState.info) },
                                color = NeoGreen,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = onDismiss) {
                                    Text("LATER", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { viewModel.skipVersion(currentState.info.version) }) {
                                    Text("SKIP THIS VERSION", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    is UpdateState.Downloading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DOWNLOADING UPDATE", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeoBorder)
                            Text("v${currentState.info.version.removePrefix("v")}", fontWeight = FontWeight.Bold, color = NeoTextSecondary)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val percent = (currentState.progress * 100).toInt()
                            Text("$percent%", fontWeight = FontWeight.Black, fontSize = 48.sp, color = NeoBorder)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedStripedProgressBar(progress = currentState.progress)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${String.format("%.1f", currentState.read / 1024f / 1024f)} / ${String.format("%.1f", currentState.total / 1024f / 1024f)} MB",
                                fontWeight = FontWeight.Bold, color = NeoTextSecondary, fontSize = 12.sp
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            NeoButton(
                                text = "CANCEL",
                                onClick = { viewModel.cancelDownload() },
                                color = NeoPink,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    is UpdateState.ReadyToInstall -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            var showStamp by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { showStamp = true }
                            
                            val scale by animateFloatAsState(
                                targetValue = if (showStamp) 1f else 3f,
                                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
                                label = "StampScale"
                            )
                            val alpha by animateFloatAsState(
                                targetValue = if (showStamp) 1f else 0f,
                                animationSpec = tween(300),
                                label = "StampAlpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .background(NeoGreen, shape = androidx.compose.foundation.shape.CircleShape)
                                    .border(4.dp, NeoBorder, shape = androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeoBorder, modifier = Modifier.size(48.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("READY TO INSTALL", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            NeoButton(
                                text = "OPEN INSTALLER",
                                onClick = { tryInstall(currentState.apk) },
                                color = NeoYellow,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    is UpdateState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = NeoPink, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ERROR", fontWeight = FontWeight.Black, fontSize = 24.sp, color = NeoBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(currentState.message, textAlign = TextAlign.Center, color = NeoTextSecondary, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            if (currentState.info != null && currentState.info.htmlUrl.isNotEmpty()) {
                                NeoButton(
                                    text = "OPEN RELEASE PAGE",
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentState.info.htmlUrl)))
                                    },
                                    color = NeoYellow,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                NeoButton(
                                    text = "RETRY",
                                    onClick = { viewModel.checkNow() },
                                    color = NeoYellow,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedStripedProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "ProgressBarFill"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "Stripes")
    val stripeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f, // Width of one stripe pair (yellow + transparent)
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StripeOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .neoBrutalist(backgroundColor = NeoTheme.colors.surface, shadowOffset = 4.dp)
    ) {
        // Fill area
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedProgress.coerceIn(0.01f, 1f))
                .clipToBounds()
                .background(NeoYellow)
                .drawWithContent {
                    drawContent()
                    // Draw diagonal stripes
                    clipRect {
                        val stripeWidth = 20f
                        val spacing = 20f
                        val totalWidth = stripeWidth + spacing
                        
                        // Start drawing from way off-screen to the left to cover the whole width even when offset
                        var x = -size.height - totalWidth + stripeOffset
                        
                        val path = Path()
                        while (x < size.width + size.height) {
                            path.moveTo(x, size.height)
                            path.lineTo(x + stripeWidth, size.height)
                            path.lineTo(x + stripeWidth + size.height, 0f)
                            path.lineTo(x + size.height, 0f)
                            path.close()
                            x += totalWidth
                        }
                        
                        drawPath(path, color = Color(0x33000000))
                    }
                }
                .border(2.dp, NeoBorder)
        )
    }
}
