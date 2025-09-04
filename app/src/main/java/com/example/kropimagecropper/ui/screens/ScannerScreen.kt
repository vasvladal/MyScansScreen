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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.kropimagecropper.utils.OpenCVPerspectiveCorrector
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Import Krop library components (keeping as fallback option)
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
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var showCropOptions by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Krop image cropper (keeping as fallback)
    val imageCropper = rememberImageCropper()

    // String resources
    val scanSavedText = stringResource(R.string.scan_saved)
    val saveFailedText = stringResource(R.string.save_failed)
    val processingText = stringResource(R.string.processing)
    val cropErrorString = stringResource(R.string.crop_operation_failed)
    val storagePermissionRequired = stringResource(R.string.storage_permission_required)
    val failedToCompressBitmap = stringResource(R.string.failed_to_compress_image)

    // Permissions with Android 14+ support
    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Media permissions based on Android version
    val mediaPermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // Android 14+ (API 34+)
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // Android 13 (API 33)
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        else -> { // Android 12 and below
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val mediaPermissionsState = rememberMultiplePermissionsState(permissions = mediaPermissions)

    // Write permission for Android 10 and below
    val writePermissionState = rememberPermissionState(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    // Gallery picker with better error handling
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            println("DEBUG: Gallery selected URI: $it")
            // Copy gallery URI to temp file for better compatibility
            val tempUri = ScanManager.copyUriToTempFile(context, it)
            if (tempUri != null) {
                println("DEBUG: Created temp copy: $tempUri")
                selectedImageUri = tempUri
            } else {
                println("DEBUG: Failed to copy gallery image")
                selectedImageUri = it // Fallback to original URI
            }
        }
    }

    // Camera launcher with better error handling
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        println("DEBUG: Camera capture result: $success")
        if (success && cameraTempUri != null) {
            println("DEBUG: Camera capture successful, URI: $cameraTempUri")
            // Verify the captured image exists and is valid
            val isValid = ScanManager.isValidImageUri(context, cameraTempUri!!)
            if (isValid) {
                println("DEBUG: Captured image is valid, proceeding with processing")
                selectedImageUri = cameraTempUri
            } else {
                println("DEBUG: Captured image is invalid")
                selectedImageUri = null
                cameraTempUri = null
                saveError = "Camera captured an invalid image. Please try again."
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    saveError = null
                }
            }
        } else {
            println("DEBUG: Camera capture failed or cancelled")
            selectedImageUri = null
            cameraTempUri = null
            if (!success) {
                saveError = "Camera capture failed or was cancelled"
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    saveError = null
                }
            }
        }
    }

    // OpenCV Perspective Correction Function
    fun applyOpenCVPerspectiveCorrection(uri: Uri) {
        scope.launch {
            isProcessing = true
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (originalBitmap != null) {
                        println("DEBUG: Starting OpenCV perspective correction")
                        val correctedBitmap = OpenCVPerspectiveCorrector.correctPerspective(originalBitmap)

                        withContext(Dispatchers.Main) {
                            croppedImage = correctedBitmap.asImageBitmap()
                            selectedImageUri = null
                            showCropOptions = false
                            isProcessing = false
                            println("DEBUG: OpenCV perspective correction completed successfully")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            saveError = "Failed to load image for processing"
                            isProcessing = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveError = "Perspective correction failed: ${e.message}"
                    isProcessing = false
                    println("DEBUG: OpenCV perspective correction failed: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // Fallback to Krop cropping
    fun applyKropCropping(uri: Uri) {
        scope.launch {
            isProcessing = true
            try {
                println("DEBUG: Starting Krop cropping as fallback")
                val result = imageCropper.crop(uri, context)

                when (result) {
                    is CropResult.Success -> {
                        println("DEBUG: Krop crop successful")
                        croppedImage = result.bitmap
                        selectedImageUri = null
                        showCropOptions = false
                        isProcessing = false
                    }
                    is CropResult.Cancelled -> {
                        println("DEBUG: Krop crop cancelled by user")
                        selectedImageUri = null
                        showCropOptions = false
                        isProcessing = false
                    }
                    is CropError -> {
                        println("DEBUG: Krop crop error occurred")
                        selectedImageUri = null
                        showCropOptions = false
                        isProcessing = false
                        saveError = "Crop operation failed. Please try again."
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Exception during Krop cropping: ${e.message}")
                e.printStackTrace()
                selectedImageUri = null
                showCropOptions = false
                isProcessing = false
                saveError = "Crop error: ${e.message ?: "Unknown error"}. Please try again."
            }
        }
    }

    // Convert ImageBitmap to Android Bitmap
    fun imageBitmapToBitmap(imageBitmap: ImageBitmap): Bitmap {
        return imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    }

    // Check if we need write permission
    fun needsWritePermission(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !writePermissionState.status.isGranted
    }

    // Save image to scans folder and gallery
    fun saveImageToScans() {
        scope.launch {
            try {
                croppedImage?.let { imageBitmap ->
                    withContext(Dispatchers.IO) {
                        val bitmap = imageBitmapToBitmap(imageBitmap)

                        // Create scans directory
                        val scansDir = ScanManager.getScansDirectory(context)

                        // Save to app scans folder
                        val fileName = "Scan_${System.currentTimeMillis()}.jpg"
                        val scanFile = File(scansDir, fileName)

                        FileOutputStream(scanFile).use { outputStream ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                                throw Exception("Failed to save scan")
                            }
                        }

                        // Save to gallery
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            saveImageToGalleryQ(bitmap, context, failedToCompressBitmap)
                        } else {
                            if (writePermissionState.status.isGranted) {
                                saveImageToGalleryLegacy(bitmap, context, failedToCompressBitmap)
                            } else {
                                throw Exception(storagePermissionRequired)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            saveLocation = scanFile.absolutePath
                            showSaveSuccess = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showSaveSuccess = false
                                saveLocation = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveError = "Save failed: ${e.message ?: ""}"
                    scope.launch {
                        kotlinx.coroutines.delay(3000)
                        saveError = null
                    }
                }
            }
        }
    }

    // Handle image selection - show crop options
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                println("DEBUG: Image selected, validating: $uri")
                kotlinx.coroutines.delay(1500) // Wait for file to be ready

                var isValid = false
                var attempts = 0
                while (!isValid && attempts < 5) {
                    isValid = ScanManager.isValidImageUri(context, uri)
                    if (!isValid) {
                        println("DEBUG: Validation attempt ${attempts + 1} failed, waiting...")
                        kotlinx.coroutines.delay(1000)
                        attempts++
                    }
                }

                if (!isValid) {
                    selectedImageUri = null
                    saveError = "Image not ready or corrupted. Please try again."
                    scope.launch {
                        kotlinx.coroutines.delay(3000)
                        saveError = null
                    }
                    return@let
                }

                println("DEBUG: Image validated successfully, showing crop options")
                showCropOptions = true
            } catch (e: Exception) {
                println("DEBUG: Exception during image validation: ${e.message}")
                selectedImageUri = null
                saveError = "Image processing error: ${e.message}"
                scope.launch {
                    kotlinx.coroutines.delay(3000)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                // Krop Cropper Dialog (fallback)
                imageCropper.cropState?.let { cropState ->
                    ImageCropperDialog(
                        state = cropState,
                        style = createKropStyle()
                    )
                }

                // Processing indicator
                if (isProcessing) {
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
                                text = "Correcting document perspective...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Krop loading indicator (fallback)
                imageCropper.loadingStatus?.let { status ->
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
                                text = when (status) {
                                    CropperLoading.PreparingImage -> stringResource(R.string.loading_image)
                                    CropperLoading.SavingResult -> stringResource(R.string.processing_crop)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Crop Options Dialog
                if (showCropOptions && selectedImageUri != null && !isProcessing) {
                    AlertDialog(
                        onDismissRequest = {
                            showCropOptions = false
                            selectedImageUri = null
                        },
                        title = { Text("Document Processing") },
                        text = {
                            Column {
                                Text("Choose how to process your document:")
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "🔧 Smart Perspective Correction: Automatically detects and corrects document perspective (Recommended)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "✂️ Manual Crop: Traditional rectangular cropping with manual adjustment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        },
                        confirmButton = {
                            Column {
                                Button(
                                    onClick = {
                                        showCropOptions = false
                                        applyOpenCVPerspectiveCorrection(selectedImageUri!!)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Smart Correction")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        showCropOptions = false
                                        applyKropCropping(selectedImageUri!!)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Crop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Manual Crop")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showCropOptions = false
                                    selectedImageUri = null
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
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
                                    text = stringResource(R.string.saved_to, File(location).name),
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
                    // Show processed image
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.scanned_document),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Image(
                                bitmap = croppedImage!!,
                                contentDescription = stringResource(R.string.scanned_document),
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
                                        if (needsWritePermission()) {
                                            writePermissionState.launchPermissionRequest()
                                        } else {
                                            saveImageToScans()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.save_as_scan), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        croppedImage = null
                                        showCropOptions = false
                                        selectedImageUri = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.scan_another))
                                }

                                // Always visible "View My Scans" button
                                Button(
                                    onClick = { onDone() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Folder, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.view_my_scans), fontWeight = FontWeight.Medium)
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
                                    text = stringResource(R.string.start_scanning),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Capture or select a document image for smart perspective correction",
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
                                                println("DEBUG: Camera permission granted, creating temp URI")
                                                ScanManager.cleanupTempFiles(context)

                                                val tempUri = ScanManager.createTempImageUri(context)
                                                if (tempUri != null) {
                                                    println("DEBUG: Temp URI created successfully: $tempUri")
                                                    cameraTempUri = tempUri
                                                    println("DEBUG: Launching camera with URI: $tempUri")
                                                    cameraLauncher.launch(tempUri)
                                                } else {
                                                    println("DEBUG: Failed to create temp URI")
                                                    saveError = "Failed to create temporary file for camera"
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(3000)
                                                        saveError = null
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                println("DEBUG: Exception in camera button: ${e.message}")
                                                e.printStackTrace()
                                                saveError = "Camera error: ${e.message}"
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    saveError = null
                                                }
                                            }
                                        }
                                        else -> {
                                            println("DEBUG: Requesting camera permission")
                                            cameraPermission.launchPermissionRequest()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
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
                                        mediaPermissionsState.allPermissionsGranted -> {
                                            try {
                                                println("DEBUG: Launching gallery picker")
                                                galleryLauncher.launch("image/*")
                                            } catch (e: Exception) {
                                                println("DEBUG: Gallery launcher exception: ${e.message}")
                                                saveError = "Gallery error: ${e.message}"
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    saveError = null
                                                }
                                            }
                                        }
                                        else -> {
                                            println("DEBUG: Requesting media permissions")
                                            mediaPermissionsState.launchMultiplePermissionRequest()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.select_from_gallery), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }

                            // Always visible "View My Scans" button at the bottom
                            Button(
                                onClick = { onDone() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Folder, null, Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.view_my_scans), fontWeight = FontWeight.Medium)
                            }

                            // Permission status message
                            if (!mediaPermissionsState.allPermissionsGranted || !cameraPermission.status.isGranted) {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = when {
                                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                                                    stringResource(R.string.camera_and_photo_access_permissions_are_required_on_android_14_you_can_grant_partial_photo_access)
                                                }
                                                else -> {
                                                    stringResource(R.string.storage_and_camera_permissions_are_required_to_scan_documents)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )

                                        // Show specific permission status for Android 14+
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val hasPartialAccess = mediaPermissionsState.permissions.any {
                                                it.permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED && it.status.isGranted
                                            }
                                            val hasFullAccess = mediaPermissionsState.permissions.any {
                                                it.permission == Manifest.permission.READ_MEDIA_IMAGES && it.status.isGranted
                                            }

                                            Text(
                                                text = when {
                                                    hasFullAccess -> stringResource(R.string.full_photo_access_granted)
                                                    hasPartialAccess -> stringResource(R.string.partial_photo_access_granted)
                                                    else -> stringResource(R.string.photo_access_needed)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (hasFullAccess || hasPartialAccess) {
                                                    Color(0xFF4CAF50)
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                },
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
    }
}

// Krop styling configuration (keeping as fallback)
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

// Gallery save functions (unchanged)
suspend fun saveImageToGalleryQ(bitmap: Bitmap, context: Context, errorMessage: String): Uri? = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocumentScans")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

    try {
        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw Exception(errorMessage)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}

suspend fun saveImageToGalleryLegacy(bitmap: Bitmap, context: Context, errorMessage: String): Uri? = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

    try {
        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw Exception(errorMessage)
            }
        }
        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}