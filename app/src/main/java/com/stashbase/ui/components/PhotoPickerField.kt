package com.stashbase.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID

@Composable
fun PhotoPickerField(
    photoPath: String?,
    onPhotoChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var cameraRequested by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraFile?.let { onPhotoChanged(it.absolutePath) }
        } else {
            pendingCameraFile?.delete()
        }
        pendingCameraFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPhotoChanged(copyUriToStorage(context, it)) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && cameraRequested) {
            cameraRequested = false
            val file = createPhotoFile(context)
            pendingCameraFile = file
            cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val file = createPhotoFile(context)
            pendingCameraFile = file
            cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        } else {
            cameraRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        if (photoPath != null) {
            Box {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = { showSourceDialog = true },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Icon(Icons.Default.Edit, "Changer la photo")
                    }
                    SmallFloatingActionButton(
                        onClick = { onPhotoChanged(null) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Supprimer la photo",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSourceDialog = true }
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter une photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Source de la photo") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Appareil photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            launchCamera()
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Galerie") },
                        leadingContent = { Icon(Icons.Default.Photo, null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Annuler") }
            },
        )
    }
}

private fun createPhotoFile(context: android.content.Context): File {
    val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
    return File(photosDir, "${UUID.randomUUID()}.jpg")
}

private fun copyUriToStorage(context: android.content.Context, uri: Uri): String {
    val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
    val dest = File(photosDir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    return dest.absolutePath
}
