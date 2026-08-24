package com.renx86.gdlapp.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.renx86.gdlapp.data.DownloadPreferences
import com.renx86.gdlapp.ui.theme.*
import java.io.File

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")

@Composable
fun FileBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    val rootDir = remember { File(prefs.getDownloadPath()).also { it.mkdirs() } }
    var currentDir by remember { mutableStateOf(rootDir) }
    var showGallery by remember { mutableStateOf(true) } // Default to Gallery view

    // Folder view: files in current directory
    val files = remember(currentDir) {
        currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: emptyList()
    }

    // Gallery view: ALL images recursively from root
    val allImages = remember(rootDir) {
        try {
            rootDir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                .sortedByDescending { it.lastModified() } // Newest first
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            "FILES",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                color = NeoBorder
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- GALLERY / FOLDERS Toggle ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoTabButton(
                text = "GALLERY",
                isSelected = showGallery,
                onClick = { showGallery = true },
                modifier = Modifier.weight(1f)
            )
            NeoTabButton(
                text = "FOLDERS",
                isSelected = !showGallery,
                onClick = { showGallery = false },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showGallery) {
            // ---- GALLERY VIEW ----
            GalleryView(images = allImages, context = context)
        } else {
            // ---- FOLDER VIEW ----
            FolderView(
                files = files,
                currentDir = currentDir,
                rootDir = rootDir,
                onNavigate = { currentDir = it },
                onBack = { currentDir = currentDir.parentFile ?: rootDir },
                context = context
            )
        }
    }
}

@Composable
private fun NeoTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) NeoYellow else Color.White

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .neoBrutalist(backgroundColor = bgColor, borderWidth = 3.dp, shadowOffset = if (isSelected) 0.dp else 4.dp)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = NeoBorder
        )
    }
}

@Composable
private fun GalleryView(images: List<File>, context: Context) {
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "NO IMAGES YET",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.LightGray
                )
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { file ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { openFile(context, file) }
                    .neoBrutalist(backgroundColor = Color.White, borderWidth = 2.dp, shadowOffset = 3.dp)
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FolderView(
    files: List<File>,
    currentDir: File,
    rootDir: File,
    onNavigate: (File) -> Unit,
    onBack: () -> Unit,
    context: Context
) {
    // Path breadcrumb
    val relativePath = currentDir.path.removePrefix(rootDir.path)
    Text(
        text = "DOWNLOADS${relativePath.uppercase().ifEmpty { "/" }}",
        fontWeight = FontWeight.Bold,
        color = NeoPink,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Back button if not at root
    if (currentDir != rootDir) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clickable { onBack() }
                .neoBrutalist(backgroundColor = NeoBlue, borderWidth = 2.dp, shadowOffset = 4.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "← BACK",
                fontWeight = FontWeight.Black,
                color = NeoBorder
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "FOLDER IS EMPTY",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.LightGray
                )
            )
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(files) { file ->
                val isDir = file.isDirectory
                val bgColor = if (isDir) NeoYellow else Color.White

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clickable {
                            if (isDir) onNavigate(file)
                            else openFile(context, file)
                        }
                        .neoBrutalist(backgroundColor = bgColor, shadowOffset = 4.dp)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon block
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isDir) NeoBackground else NeoGreen)
                                .border(2.dp, NeoBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDir) Icons.Default.Folder else Icons.Default.Image,
                                contentDescription = null,
                                tint = NeoBorder,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = NeoBorder
                            )
                            if (!isDir) {
                                Text(
                                    text = "${file.length() / 1024} KB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // No app to handle this file type
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FileBrowserScreenPreview() {
    MaterialTheme {
        FileBrowserScreen()
    }
}
