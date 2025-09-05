// File: app/src/main/java/com/example/kropimagecropper/ui/screens/PerspectiveCorrectionScreen.kt
package com.example.kropimagecropper.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.kropimagecropper.R
import com.example.kropimagecropper.utils.OpenCVPerspectiveCorrector
import com.example.kropimagecropper.utils.PerspectivePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size as OpenCVSize
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

// Enum to identify which corner is being dragged
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerspectiveCorrectionScreen(
    originalBitmap: Bitmap,
    onCorrected: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var perspectivePoints by remember { mutableStateOf(PerspectivePoints()) }
    var selectedCorner by remember { mutableStateOf<Corner?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }

    // Convert bitmap to ImageBitmap for Compose
    val imageBitmap = remember(originalBitmap) {
        originalBitmap.asImageBitmap()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.perspective_correction)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            perspectivePoints = PerspectivePoints()
                            selectedCorner = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                try {
                                    val corrected = applyPerspectiveCorrection(
                                        originalBitmap,
                                        perspectivePoints,
                                        rotationAngle
                                    )
                                    onCorrected(corrected)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // If correction fails, return original
                                    onCorrected(originalBitmap)
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.apply)
                            )
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
        ) {
            // Add rotation controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Rotation Adjustment",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { rotationAngle = (rotationAngle - 90f) % 360f }
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left")
                        }

                        Slider(
                            value = rotationAngle,
                            onValueChange = { rotationAngle = it },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { rotationAngle = (rotationAngle + 90f) % 360f }
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right")
                        }

                        Text(
                            text = "${rotationAngle.toInt()}°",
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
            PerspectiveCorrectionCanvas(
                imageBitmap = imageBitmap,
                perspectivePoints = perspectivePoints,
                selectedCorner = selectedCorner,
                onCornerSelected = { corner -> selectedCorner = corner },
                onCornerMoved = { corner, offset ->
                    perspectivePoints = when (corner) {
                        Corner.TOP_LEFT -> perspectivePoints.copy(
                            topLeft = PointF(
                                max(0f, min(1f, perspectivePoints.topLeft.x + offset.x)),
                                max(0f, min(1f, perspectivePoints.topLeft.y + offset.y))
                            )
                        )
                        Corner.TOP_RIGHT -> perspectivePoints.copy(
                            topRight = PointF(
                                max(0f, min(1f, perspectivePoints.topRight.x + offset.x)),
                                max(0f, min(1f, perspectivePoints.topRight.y + offset.y))
                            )
                        )
                        Corner.BOTTOM_RIGHT -> perspectivePoints.copy(
                            bottomRight = PointF(
                                max(0f, min(1f, perspectivePoints.bottomRight.x + offset.x)),
                                max(0f, min(1f, perspectivePoints.bottomRight.y + offset.y))
                            )
                        )
                        Corner.BOTTOM_LEFT -> perspectivePoints.copy(
                            bottomLeft = PointF(
                                max(0f, min(1f, perspectivePoints.bottomLeft.x + offset.x)),
                                max(0f, min(1f, perspectivePoints.bottomLeft.y + offset.y))
                            )
                        )
                    }
                },
                onDragEnd = { selectedCorner = null }
            )

            // Instructions
            Text(
                text = stringResource(R.string.perspective_instructions),
                modifier = Modifier
                    .align(Alignment.BottomCenter as Alignment.Horizontal)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PerspectiveCorrectionCanvas(
    imageBitmap: ImageBitmap,
    perspectivePoints: PerspectivePoints,
    selectedCorner: Corner?,
    onCornerSelected: (Corner) -> Unit,
    onCornerMoved: (Corner, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find which corner was touched
                        val corners = listOf(
                            Corner.TOP_LEFT to perspectivePoints.topLeft,
                            Corner.TOP_RIGHT to perspectivePoints.topRight,
                            Corner.BOTTOM_RIGHT to perspectivePoints.bottomRight,
                            Corner.BOTTOM_LEFT to perspectivePoints.bottomLeft
                        )

                        corners.forEach { (corner, point) ->
                            val cornerX = point.x * size.width
                            val cornerY = point.y * size.height
                            val dx = offset.x - cornerX
                            val dy = offset.y - cornerY
                            val distance = dx * dx + dy * dy
                            if (distance < 1600f) { // 40px radius squared
                                onCornerSelected(corner)
                                return@detectDragGestures
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        selectedCorner?.let { corner ->
                            // Convert drag amount to normalized coordinates
                            val normalizedAmount = Offset(
                                dragAmount.x / size.width,
                                dragAmount.y / size.height
                            )
                            onCornerMoved(corner, normalizedAmount)
                        }
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
    ) {
        canvasSize = IntSize(size.width.toInt(), size.height.toInt())

        // Calculate image bounds to fit within canvas while maintaining aspect ratio
        val imageAspectRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()
        val canvasAspectRatio = size.width / size.height

        val (imageWidth, imageHeight, offsetX, offsetY) = if (imageAspectRatio > canvasAspectRatio) {
            // Image is wider than canvas
            val width = size.width
            val height = size.width / imageAspectRatio
            val yOffset = (size.height - height) / 2f
            listOf(width, height, 0f, yOffset)
        } else {
            // Image is taller than canvas
            val height = size.height
            val width = size.height * imageAspectRatio
            val xOffset = (size.width - width) / 2f
            listOf(width, height, xOffset, 0f)
        }

        // Draw the image fitted to canvas
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(imageWidth.toInt(), imageHeight.toInt()),
            filterQuality = FilterQuality.Low
        )

        // Draw perspective grid overlay
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                strokeWidth = 4f
                style = android.graphics.Paint.Style.STROKE
            }

            val points = listOf(
                perspectivePoints.topLeft,
                perspectivePoints.topRight,
                perspectivePoints.bottomRight,
                perspectivePoints.bottomLeft
            )

            // Convert normalized points to image coordinates
            val imagePoints = points.map { point ->
                android.graphics.PointF(
                    offsetX + point.x * imageWidth,
                    offsetY + point.y * imageHeight
                )
            }

            // Draw quadrilateral
            imagePoints.forEachIndexed { index, point ->
                val nextPoint = imagePoints[(index + 1) % imagePoints.size]
                nativeCanvas.drawLine(
                    point.x, point.y,
                    nextPoint.x, nextPoint.y,
                    paint
                )
            }

            // Draw corner points
            imagePoints.forEachIndexed { index, point ->
                val corner = when (index) {
                    0 -> Corner.TOP_LEFT
                    1 -> Corner.TOP_RIGHT
                    2 -> Corner.BOTTOM_RIGHT
                    else -> Corner.BOTTOM_LEFT
                }

                val isSelected = corner == selectedCorner
                paint.color = if (isSelected) {
                    android.graphics.Color.YELLOW
                } else {
                    android.graphics.Color.RED
                }
                paint.style = android.graphics.Paint.Style.FILL
                nativeCanvas.drawCircle(point.x, point.y, 25f, paint)

                // Draw inner white circle for contrast
                paint.color = android.graphics.Color.WHITE
                nativeCanvas.drawCircle(point.x, point.y, 15f, paint)

                // Reset for next drawing
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.RED
            }
        }
    }
}

suspend fun applyPerspectiveCorrection(
    bitmap: Bitmap,
    points: PerspectivePoints,
    rotationAngle: Float
): Bitmap {
    return withContext(Dispatchers.IO) {
        try {
            correctPerspectiveWithPointsAndRotation(bitmap, points, rotationAngle)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}

fun correctPerspectiveWithPointsAndRotation(
    bitmap: Bitmap,
    points: PerspectivePoints,
    rotationAngle: Float = 0f
): Bitmap {
    return try {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert normalized points to image coordinates
        val srcPoints = MatOfPoint2f(
            Point((points.topLeft.x * bitmap.width).toDouble(),
                (points.topLeft.y * bitmap.height).toDouble()
            ),
            Point((points.topRight.x * bitmap.width).toDouble(),
                (points.topRight.y * bitmap.height).toDouble()
            ),
            Point((points.bottomRight.x * bitmap.width).toDouble(),
                (points.bottomRight.y * bitmap.height).toDouble()
            ),
            Point((points.bottomLeft.x * bitmap.width).toDouble(),
                (points.bottomLeft.y * bitmap.height).toDouble()
            )
        )

        // Create rotation matrix
        val center = Point(bitmap.width / 2.0, bitmap.height / 2.0)
        val rotationMatrix = Imgproc.getRotationMatrix2D(center, rotationAngle.toDouble(), 1.0)

        // Apply rotation to destination points
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(bitmap.width.toDouble(), 0.0),
            Point(bitmap.width.toDouble(), bitmap.height.toDouble()),
            Point(0.0, bitmap.height.toDouble())
        )

        // Rotate destination points
        val rotatedDstPoints = MatOfPoint2f()
        Core.transform(dstPoints, rotatedDstPoints, rotationMatrix)

        // Get perspective transformation matrix
        val perspectiveMatrix = Imgproc.getPerspectiveTransform(srcPoints, rotatedDstPoints)

        // Apply perspective transformation
        val warped = Mat()
        Imgproc.warpPerspective(
            src,
            warped,
            perspectiveMatrix,
            OpenCVSize(bitmap.width.toDouble(), bitmap.height.toDouble())
        )

        // Convert back to bitmap
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, resultBitmap)

        // Clean up
        src.release()
        srcPoints.release()
        dstPoints.release()
        rotatedDstPoints.release()
        rotationMatrix.release()
        perspectiveMatrix.release()
        warped.release()

        resultBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}