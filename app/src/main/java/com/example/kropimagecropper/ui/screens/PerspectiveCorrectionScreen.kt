package com.example.kropimagecropper.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.kropimagecropper.utils.CustomPoint
import com.example.kropimagecropper.utils.OpenCVPerspectiveCorrector
import com.example.kropimagecropper.utils.PerspectivePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerspectiveCorrectionScreen(
    imageUri: Uri,
    onResult: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // State variables
    var originalBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var correctedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }
    var imageScale by remember { mutableStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }

    // Corner points for manual adjustment (normalized 0-1)
    var topLeft by remember { mutableStateOf(Offset(0.1f, 0.1f)) }
    var topRight by remember { mutableStateOf(Offset(0.9f, 0.1f)) }
    var bottomRight by remember { mutableStateOf(Offset(0.9f, 0.9f)) }
    var bottomLeft by remember { mutableStateOf(Offset(0.1f, 0.9f)) }

    // Load original image
    LaunchedEffect(imageUri) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            originalBitmap = bitmap.asImageBitmap()
                            imageSize = IntSize(bitmap.width, bitmap.height)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Failed to load image"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error loading image: ${e.message}"
                }
            }
        }
    }

    // Function to apply perspective correction
    fun applyCorrection() {
        originalBitmap?.let { bitmap ->
            scope.launch {
                isProcessing = true
                errorMessage = null

                try {
                    withContext(Dispatchers.IO) {
                        // Convert PerspectivePoints to List<CustomPoint>
                        val pointsList = listOf(
                            CustomPoint(topLeft.x, topLeft.y),
                            CustomPoint(topRight.x, topRight.y),
                            CustomPoint(bottomRight.x, bottomRight.y),
                            CustomPoint(bottomLeft.x, bottomLeft.y)
                        )

                        val androidBitmap = bitmap.asAndroidBitmap()
                        val correctedAndroidBitmap = OpenCVPerspectiveCorrector.correctPerspectiveWithPoints(
                            androidBitmap, pointsList
                        )

                        withContext(Dispatchers.Main) {
                            correctedBitmap = correctedAndroidBitmap.asImageBitmap()
                            isProcessing = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Correction failed: ${e.message}"
                        isProcessing = false
                    }
                }
            }
        }
    }

    // Function to reset points to auto-detected or default positions
    fun resetPoints() {
        topLeft = Offset(0.1f, 0.1f)
        topRight = Offset(0.9f, 0.1f)
        bottomRight = Offset(0.9f, 0.9f)
        bottomLeft = Offset(0.1f, 0.9f)
    }

    // Function to auto-detect corners (simplified version)
    fun autoDetectCorners() {
        originalBitmap?.let { bitmap ->
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val androidBitmap = bitmap.asAndroidBitmap()
                        // This would ideally use the automatic detection from OpenCV
                        // For now, we'll set reasonable default positions
                        withContext(Dispatchers.Main) {
                            topLeft = Offset(0.05f, 0.05f)
                            topRight = Offset(0.95f, 0.05f)
                            bottomRight = Offset(0.95f, 0.95f)
                            bottomLeft = Offset(0.05f, 0.95f)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Auto-detection failed: ${e.message}"
                    }
                }
            }
        }
    }

    // Helper function to calculate scale
    fun calculateScale(container: IntSize, image: IntSize): Float {
        val widthScale = container.width / image.width.toFloat()
        val heightScale = container.height / image.height.toFloat()
        return minOf(widthScale, heightScale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perspective Correction") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (correctedBitmap != null) {
                        TextButton(
                            onClick = {
                                correctedBitmap?.let { bitmap ->
                                    onResult(bitmap.asAndroidBitmap())
                                }
                            }
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Processing indicator
            if (isProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing perspective correction...")
                    }
                }
            }

            // Image display area
            originalBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        if (correctedBitmap != null) {
                            // Show corrected image
                            Image(
                                bitmap = correctedBitmap!!,
                                contentDescription = "Corrected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Show original image with corner points overlay
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Original Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { containerSize ->
                                            // Calculate actual displayed image size and scale
                                            val scale = calculateScale(containerSize, imageSize)
                                            val displayedSize = IntSize(
                                                (imageSize.width * scale).toInt(),
                                                (imageSize.height * scale).toInt()
                                            )
                                            val offset = Offset(
                                                (containerSize.width - displayedSize.width) / 2f,
                                                (containerSize.height - displayedSize.height) / 2f
                                            )

                                            imageDisplaySize = displayedSize
                                            imageScale = scale
                                            imageOffset = offset
                                        },
                                    contentScale = ContentScale.Fit
                                )

                                // Overlay canvas for corner points
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { size ->
                                            canvasSize = size
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, _ ->
                                                val rawPosition = change.position
                                                // Convert screen coordinates to image coordinates
                                                val imageX = (rawPosition.x - imageOffset.x) / imageScale
                                                val imageY = (rawPosition.y - imageOffset.y) / imageScale

                                                // Normalize coordinates
                                                val position = Offset(
                                                    (imageX / imageSize.width).coerceIn(0f, 1f),
                                                    (imageY / imageSize.height).coerceIn(0f, 1f)
                                                )

                                                // Find closest corner and update it
                                                val corners = listOf(
                                                    topLeft to { offset: Offset -> topLeft = offset },
                                                    topRight to { offset: Offset -> topRight = offset },
                                                    bottomRight to { offset: Offset -> bottomRight = offset },
                                                    bottomLeft to { offset: Offset -> bottomLeft = offset }
                                                )

                                                val closest = corners.minByOrNull { (corner, _) ->
                                                    val dx = corner.x - position.x
                                                    val dy = corner.y - position.y
                                                    sqrt(dx * dx + dy * dy)
                                                }

                                                closest?.let { (_, setter) ->
                                                    setter(position.coerceIn(Offset.Zero, Offset(1f, 1f)))
                                                }
                                            }
                                        }
                                ) {
                                    drawPerspectiveOverlay(
                                        topLeft = topLeft,
                                        topRight = topRight,
                                        bottomRight = bottomRight,
                                        bottomLeft = bottomLeft,
                                        canvasSize = size,
                                        imageSize = imageSize,
                                        imageScale = imageScale,
                                        imageOffset = imageOffset
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (correctedBitmap == null) {
                    OutlinedButton(
                        onClick = { autoDetectCorners() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto")
                    }

                    OutlinedButton(
                        onClick = { resetPoints() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }

                    Button(
                        onClick = { applyCorrection() },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            correctedBitmap = null
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit Again")
                    }

                    Button(
                        onClick = {
                            correctedBitmap?.let { bitmap ->
                                onResult(bitmap.asAndroidBitmap())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }

            // Instructions
            if (correctedBitmap == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Drag the corner points to adjust the document boundaries, then tap Apply to correct perspective.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Extension function to coerce Offset values
private fun Offset.coerceIn(min: Offset, max: Offset): Offset {
    return Offset(
        x = x.coerceIn(min.x, max.x),
        y = y.coerceIn(min.y, max.y)
    )
}

// Function to draw the perspective overlay
private fun DrawScope.drawPerspectiveOverlay(
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
    canvasSize: Size,
    imageSize: IntSize,
    imageScale: Float,
    imageOffset: Offset
) {
    val strokeWidth = 3.dp.toPx()
    val cornerRadius = 12.dp.toPx()

    // Convert normalized coordinates to canvas coordinates with proper scaling
    fun toCanvasPoint(normalized: Offset): Offset {
        val x = normalized.x * imageSize.width * imageScale + imageOffset.x
        val y = normalized.y * imageSize.height * imageScale + imageOffset.y
        return Offset(x, y)
    }

    val tl = toCanvasPoint(topLeft)
    val tr = toCanvasPoint(topRight)
    val br = toCanvasPoint(bottomRight)
    val bl = toCanvasPoint(bottomLeft)

    // Draw connecting lines
    drawLine(
        color = Color.Red,
        start = tl,
        end = tr,
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = tr,
        end = br,
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = br,
        end = bl,
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = bl,
        end = tl,
        strokeWidth = strokeWidth
    )

    // Draw corner circles
    listOf(tl, tr, br, bl).forEach { corner ->
        drawCircle(
            color = Color.Red,
            radius = cornerRadius,
            center = corner,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = Color.White,
            radius = cornerRadius - strokeWidth,
            center = corner
        )
    }
}