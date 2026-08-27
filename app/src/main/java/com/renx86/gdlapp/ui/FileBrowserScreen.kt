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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoFile
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
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
import androidx.documentfile.provider.DocumentFile

data class FileNode(
    val name: String,
    val isDirectory: Boolean,
    val uri: android.net.Uri,
    val lastModified: Long,
    val documentFile: DocumentFile? = null,
    val javaFile: File? = null
) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "avi", "3gp", "flv", "m4v")
private val SUPPORTED_MEDIA_EXTENSIONS = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS

@Composable
fun FileBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { DownloadPreferences(context) }
    val pathString = prefs.getDownloadPath()
    val isSaf = pathString.startsWith("content://")
    
    // Root nodes
    val rootDocumentFile = remember(pathString) {
        if (isSaf) DocumentFile.fromTreeUri(context, android.net.Uri.parse(pathString)) else null
    }
    val rootJavaFile = remember(pathString) {
        if (!isSaf) File(pathString).also { it.mkdirs() } else null
    }

    var currentDocDir by remember { mutableStateOf(rootDocumentFile) }
    var currentJavaDir by remember { mutableStateOf(rootJavaFile) }
    
    // Helper to generate FileNode list from current directory
    val files = remember(currentDocDir, currentJavaDir) {
        if (isSaf && currentDocDir != null) {
            currentDocDir!!.listFiles()
                .map { FileNode(it.name ?: "Unknown", it.isDirectory, it.uri, it.lastModified(), documentFile = it) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        } else if (currentJavaDir != null) {
            currentJavaDir!!.listFiles()?.map { 
                FileNode(it.name, it.isDirectory, android.net.Uri.fromFile(it), it.lastModified(), javaFile = it) 
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        } else emptyList()
    }

    var showGallery by remember { mutableStateOf(true) }

    // Gallery view: ALL images recursively from root
    val allImages = remember(rootDocumentFile, rootJavaFile) {
        val result = mutableListOf<FileNode>()
        if (isSaf && rootDocumentFile != null) {
            fun walkSaf(docFile: DocumentFile) {
                docFile.listFiles().forEach {
                    if (it.isDirectory) walkSaf(it)
                    else {
                        val ext = it.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                        if (ext in SUPPORTED_MEDIA_EXTENSIONS) {
                            result.add(FileNode(it.name ?: "Unknown", false, it.uri, it.lastModified(), documentFile = it))
                        }
                    }
                }
            }
            try { walkSaf(rootDocumentFile!!) } catch (e: Exception) {}
        } else if (rootJavaFile != null) {
            try {
                rootJavaFile!!.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in SUPPORTED_MEDIA_EXTENSIONS }
                    .forEach { result.add(FileNode(it.name, false, android.net.Uri.fromFile(it), it.lastModified(), javaFile = it)) }
            } catch (e: Exception) {}
        }
        result.sortedByDescending { it.lastModified }
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
                isRoot = if (isSaf) currentDocDir?.uri == rootDocumentFile?.uri else currentJavaDir?.absolutePath == rootJavaFile?.absolutePath,
                currentPathName = if (isSaf) currentDocDir?.name ?: "Downloads" else currentJavaDir?.name ?: "Downloads",
                onNavigate = { node ->
                    if (isSaf) currentDocDir = node.documentFile
                    else currentJavaDir = node.javaFile
                },
                onBack = {
                    if (isSaf) currentDocDir = currentDocDir?.parentFile ?: rootDocumentFile
                    else currentJavaDir = currentJavaDir?.parentFile ?: rootJavaFile
                },
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
    val bgColor = if (isSelected) NeoYellow else NeoTheme.colors.surface

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
private fun GalleryView(images: List<FileNode>, context: Context) {
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "NO MEDIA YET",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = NeoTextSecondary
                )
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { file ->
            val isVideo = file.extension in VIDEO_EXTENSIONS
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { openFile(context, file) }
                    .neoBrutalist(backgroundColor = NeoTheme.colors.surface, borderWidth = 2.dp, shadowOffset = 3.dp)
            ) {
                AsyncImage(
                    model = if (isVideo) {
                        ImageRequest.Builder(context)
                            .data(file.uri)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .build()
                    } else file.uri,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isVideo) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderView(
    files: List<FileNode>,
    isRoot: Boolean,
    currentPathName: String,
    onNavigate: (FileNode) -> Unit,
    onBack: () -> Unit,
    context: Context
) {
    // Path breadcrumb
    Text(
        text = "DOWNLOADS / ${if (isRoot) "" else currentPathName.uppercase()}",
        fontWeight = FontWeight.Bold,
        color = NeoPink,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Back button if not at root
    if (!isRoot) {
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
                    color = NeoTextSecondary
                )
            )
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(files) { file ->
                val isDir = file.isDirectory
                val bgColor = if (isDir) NeoYellow else NeoTheme.colors.surface

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
                                imageVector = if (isDir) Icons.Default.Folder 
                                              else if (file.extension in VIDEO_EXTENSIONS) Icons.Default.VideoFile
                                              else Icons.Default.Image,
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
                                val sizeText = if (file.documentFile != null) {
                                    "${file.documentFile.length() / 1024} KB"
                                } else {
                                    "${(file.javaFile?.length() ?: 0) / 1024} KB"
                                }
                                Text(
                                    text = sizeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openFile(context: Context, fileNode: FileNode) {
    try {
        val uri = if (fileNode.documentFile != null) {
            fileNode.uri
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                fileNode.javaFile!!
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val extensionMimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileNode.extension)
            val finalMimeType = extensionMimeType ?: context.contentResolver.getType(uri) ?: "*/*"
            setDataAndType(uri, finalMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
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
