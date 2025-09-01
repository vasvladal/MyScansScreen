// File: app/src/main/java/com/example/kropimagecropper/ui/screens/ScannerScreen.kt

package com.example.kropimagecropper.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Simple approach - let's go back to basic Jetpack Compose for cropping
// You might need to use a different cropping library or implement a simple solution
import com.example.kropimagecropper.R
import com.example.kropimagecropper.data.ScanManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCropper by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Request camera permission
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            showCropper = true
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && imageUri != null) {
            showCropper = true
        }
    }

    if (showCropper && imageUri != null) {
        SimpleCropperScreen(
            imageUri = imageUri!!,
            onCropComplete = { croppedUri ->
                scope.launch {
                    isProcessing = true
                    try {
                        val saved = ScanManager.saveCroppedImage(context, croppedUri)
                        if (saved) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.scan_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                            onDone()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.save_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isProcessing = false
                        showCropper = false
                        imageUri = null
                    }
                }
            },
            onCancel = {
                showCropper = false
                imageUri = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.document_scanner)) },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Camera button
                Button(
                    onClick = {
                        when (cameraPermission.status) {
                            PermissionStatus.Granted -> {
                                // Create temporary file for camera capture
                                val tempUri = ScanManager.createTempImageUri(context)
                                if (tempUri != null) {
                                    imageUri = tempUri
                                    cameraLauncher.launch(tempUri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.camera_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            else -> {
                                cameraPermission.launchPermissionRequest()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo))
                }

                Spacer(Modifier.height(16.dp))

                // Gallery button
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.select_from_gallery))
                }

                if (isProcessing) {
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.processing),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleCropperScreen(
    imageUri: Uri,
    onCropComplete: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    // For now, this is a placeholder cropper screen
    // You'll need to implement actual cropping functionality
    // or use a different cropping library that's compatible with your setup

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crop Image") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // For now, just pass through the original image
                            // In a real implementation, you'd crop the image here
                            onCropComplete(imageUri)
                        }
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for cropping UI
            // You need to implement actual cropping functionality here
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Image Cropping Placeholder",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Replace this with actual cropping implementation",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onCropComplete(imageUri) }
                ) {
                    Text("Use Original Image")
                }
            }
        }
    }
}