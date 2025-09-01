// File: app/src/main/java/com/example/kropimagecropper/ui/screens/ScannerScreen.kt

package com.example.kropimagecropper.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kropimagecropper.R
import com.example.kropimagecropper.data.ScanManager
import com.example.kropimagecropper.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Import Krop library components
import com.attafitamim.krop.core.crop.*
import com.attafitamim.krop.ui.ImageCropperDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State variables
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveLocation by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    // Krop image cropper
    val imageCropper = rememberImageCropper()

    // String resources
    val scanSavedText = stringResource(R.string.scan_saved)
    val saveFailedText = stringResource(R.string.save_failed)
    val processingText = stringResource(R.string.processing)
    val cropErrorString = "Crop operation failed"
    val storagePermissionRequired = stringResource(R.string.storage_permission_required)
    val failedToCompressBitmap = "Failed to compress image"

    // Permissions
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val readPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // Storage permission for legacy Android versions
    val writePermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    )

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        } else {
            saveError = "No image selected"
            scope.launch {
                kotlinx.coroutines.delay(3000)
                saveError = null
            }
        }
    }

    // Camera launcher with improved error handling
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraCaptureUri != null) {
            // Image captured successfully
            Log.d("ScannerScreen", "Camera capture successful, URI: $cameraCaptureUri")
            selectedImageUri = cameraCaptureUri
        } else {
            // Camera capture failed or cancelled
            Log.e("ScannerScreen", "Camera capture failed or cancelled, success: $success")
            selectedImageUri = null
            cameraCaptureUri = null
            isProcessing = false
            if (!success) {
                saveError = "Camera capture failed or was cancelled"
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    saveError = null
                }
            }
        }
    }

    // Convert ImageBitmap to Android Bitmap
    fun imageBitmapToBitmap(imageBitmap: ImageBitmap): Bitmap {
        return imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    }

    // Save image to scans folder using ScanManager
//    fun saveImageToScans() {
//        scope.launch {
//            try {
//                isProcessing = true
//                croppedImage?.let { imageBitmap ->
//                    withContext(Dispatchers.IO) {
//                        // Convert to Android bitmap
//                        val bitmap = imageBitmapToBitmap(imageBitmap)
//
//                        // Create a temporary file to pass to ScanManager
//                        val tempDir = context.cacheDir
//                        val tempFile = File.createTempFile("temp_scan_", ".jpg", tempDir)
//
//                        // Save bitmap to temporary file
//                        FileOutputStream(tempFile).use { outputStream ->
//                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
//                                throw Exception("Failed to compress image")
//                            }
//                        }
//
//                        // Use ScanManager to save the image
//                        val success = ScanManager.saveCroppedImage(
//                            context,
//                            Uri.fromFile(tempFile)
//                        )
//
//                        // Clean up temp file
//                        tempFile.delete()
//
//                        withContext(Dispatchers.Main) {
//                            isProcessing = false
//                            if (success) {
//                                showSaveSuccess = true
//                                scope.launch {
//                                    kotlinx.coroutines.delay(3000)
//                                    showSaveSuccess = false
//                                    onDone() // Navigate back to scans list after successful save
//                                }
//                            } else {
//                                saveError = saveFailedText
//                                scope.launch {
//                                    kotlinx.coroutines.delay(3000)
//                                    saveError = null
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    isProcessing = false
//                    saveError = "${saveFailedText}: ${e.message ?: ""}"
//                    scope.launch {
//                        kotlinx.coroutines.delay(3000)
//                        saveError = null
//                    }
//                }
//            }
//        }
//    }


    // Save image to scans folder using ScanManager
    fun saveImageToScans() {
        scope.launch {
            try {
                isProcessing = true
                croppedImage?.let { imageBitmap ->
                    withContext(Dispatchers.IO) {
                        // Convert to Android bitmap
                        val bitmap = imageBitmap.asAndroidBitmap()

                        // Use ScanManager to save the image
                        val success = ScanManager.saveCroppedImage(context, bitmap)

                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            if (success) {
                                showSaveSuccess = true
                                scope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    showSaveSuccess = false
                                    onDone() // Navigate back to scans list after successful save
                                }
                            } else {
                                saveError = saveFailedText
                                scope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    saveError = null
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    saveError = "${saveFailedText}: ${e.message ?: ""}"
                    scope.launch {
                        kotlinx.coroutines.delay(3000)
                        saveError = null
                    }
                }
            }
        }
    }

    // Debug the cropping process
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                isProcessing = true
                Log.d("ScannerScreen", "Starting crop for URI: $uri")

                // Use the Krop library's built-in URI handling
                when (val result = imageCropper.crop(uri, context)) {
                    is CropResult.Success -> {
                        Log.d("ScannerScreen", "Crop successful")
                        croppedImage = result.bitmap
                        selectedImageUri = null
                        cameraCaptureUri = null
                        isProcessing = false
                    }
                    is CropResult.Cancelled -> {
                        Log.d("ScannerScreen", "Crop cancelled")
                        selectedImageUri = null
                        cameraCaptureUri = null
                        isProcessing = false
                    }
                    is CropError -> {
                        Log.e("ScannerScreen", "Crop error occurred")
                        selectedImageUri = null
                        cameraCaptureUri = null
                        isProcessing = false
                        saveError = "Crop operation failed. Please try again."
                        scope.launch {
                            kotlinx.coroutines.delay(5000)
                            saveError = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Exception during crop: ${e.message}")
                selectedImageUri = null
                cameraCaptureUri = null
                isProcessing = false
                saveError = "Crop error: ${e.message}"
                scope.launch {
                    kotlinx.coroutines.delay(5000)
                    saveError = null
                }
            }
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Krop Cropper Dialog
                imageCropper.cropState?.let { cropState ->
                    ImageCropperDialog(
                        state = cropState,
                        style = createKropStyle()
                    )
                }

                // Loading indicator
                if (isProcessing || imageCropper.loadingStatus != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (imageCropper.loadingStatus) {
                                    CropperLoading.PreparingImage -> "Loading image..."
                                    CropperLoading.SavingResult -> "Processing crop..."
                                    else -> "Processing..."
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Success message
                if (showSaveSuccess) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scanSavedText,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            saveLocation?.let { location ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Saved to: ${File(location).name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Error message
                saveError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Main content area
                if (croppedImage != null) {
                    // Show cropped image
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scanned Document",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Image(
                                bitmap = croppedImage!!,
                                contentDescription = "Scanned document",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Check if we have storage permission before saving
                                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            readPermissionState.status.isGranted
                                        } else {
                                            writePermissionState.status.isGranted
                                        }

                                        if (hasPermission) {
                                            saveImageToScans()
                                        } else {
                                            // Request the appropriate permission
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                readPermissionState.launchPermissionRequest()
                                            } else {
                                                writePermissionState.launchPermissionRequest()
                                            }

                                            // Show a message about needing permission
                                            saveError = "Storage permission is required to save scans"
                                            scope.launch {
                                                kotlinx.coroutines.delay(3000)
                                                saveError = null
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save as Scan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        croppedImage = null
                                        selectedImageUri = null
                                        cameraCaptureUri = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isProcessing
                                ) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Another")
                                }

                                OutlinedButton(
                                    onClick = { onDone() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isProcessing
                                ) {
                                    Icon(Icons.Default.Folder, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View My Scans")
                                }
                            }
                        }
                    }
                } else {
                    // Image selection UI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.size(120.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.DocumentScanner,
                                        null,
                                        Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Start Scanning",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Take a photo or select an image to crop and scan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera button
                            Button(
                                onClick = {
                                    when {
                                        cameraPermission.status.isGranted -> {
                                            try {
                                                Log.d("ScannerScreen", "Camera permission granted, creating temp URI")
                                                val tempUri = ScanManager.createTempImageUri(context)
                                                if (tempUri != null) {
                                                    Log.d("ScannerScreen", "Temp URI created: $tempUri")
                                                    cameraCaptureUri = tempUri
                                                    isProcessing = true

                                                    cameraLauncher.launch(tempUri)
                                                } else {
                                                    Log.e("ScannerScreen", "Failed to create temp URI")
                                                    saveError = "Failed to create temporary file"
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(3000)
                                                        saveError = null
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ScannerScreen", "Exception in camera button: ${e.message}")
                                                saveError = "Camera error: ${e.message}"
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    saveError = null
                                                }
                                            }
                                        }
                                        else -> {
                                            Log.d("ScannerScreen", "Requesting camera permission")
                                            cameraPermission.launchPermissionRequest()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.take_photo), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }

                            // Gallery button
                            OutlinedButton(
                                onClick = {
                                    when {
                                        readPermissionState.status.isGranted -> {
                                            try {
                                                galleryLauncher.launch("image/*")
                                            } catch (e: Exception) {
                                                saveError = "Gallery error: ${e.message}"
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    saveError = null
                                                }
                                            }
                                        }
                                        else -> {
                                            readPermissionState.launchPermissionRequest()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isProcessing
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.select_from_gallery), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }

                            if (!readPermissionState.status.isGranted || !cameraPermission.status.isGranted) {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                                    Text(
                                        text = "Storage and camera permissions are required to scan documents",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Krop styling configuration
@Composable
fun createKropStyle() = cropperStyle(
    backgroundColor = Color.Black,
    rectColor = MaterialTheme.colorScheme.primary,
    rectStrokeWidth = 4.dp,
    overlay = Color.Black.copy(alpha = 0.7f),
    touchRad = 28.dp,
    autoZoom = true,
    aspects = listOf(
        AspectRatio(1, 1),
        AspectRatio(4, 3),
        AspectRatio(3, 4),
        AspectRatio(16, 9),
        AspectRatio(9, 16),
        AspectRatio(3, 2),
    ),
    shapes = listOf(
        RectCropShape,
        CircleCropShape,
        RoundRectCropShape(15),
        RoundRectCropShape(25),
        TriangleCropShape
    ),
    guidelines = CropperStyleGuidelines(
        count = 2,
        color = Color.White.copy(alpha = 0.6f),
        width = 1.5.dp
    )
)