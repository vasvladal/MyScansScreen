// File: app/src/main/java/com/example/kropimagecropper/ui/screens/PerspectiveCorrectionScreen.kt
package com.example.kropimagecropper.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.createBitmap

// Enum to identify which corner is being dragged
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

data class PerspectivePoints(
    val topLeft: PointF = PointF(0.2f, 0.2f),
    val topRight: PointF = PointF(0.8f, 0.2f),
    val bottomRight: PointF = PointF(0.8f, 0.8f),
    val bottomLeft: PointF = PointF(0.2f, 0.8f)
)

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
                                val corrected = applyPerspectiveCorrection(
                                    originalBitmap,
                                    perspectivePoints
                                )
                                onCorrected(corrected)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.apply)
                        )
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
                    .align(Alignment.BottomCenter)
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
                            if (distance < 400f) { // 20px radius squared
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

        // Draw the image
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset.Zero,
            dstSize = canvasSize,
            filterQuality = FilterQuality.Low
        )

        // Draw perspective grid
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                strokeWidth = 3f
                style = android.graphics.Paint.Style.STROKE
            }

            val points = listOf(
                perspectivePoints.topLeft,
                perspectivePoints.topRight,
                perspectivePoints.bottomRight,
                perspectivePoints.bottomLeft
            )

            // Convert normalized points to screen coordinates
            val screenPoints = points.map { point ->
                android.graphics.PointF(
                    point.x * size.width,
                    point.y * size.height
                )
            }

            // Draw quadrilateral
            screenPoints.forEachIndexed { index, point ->
                val nextPoint = screenPoints[(index + 1) % screenPoints.size]
                nativeCanvas.drawLine(
                    point.x, point.y,
                    nextPoint.x, nextPoint.y,
                    paint
                )
            }

            // Draw corner points
            screenPoints.forEachIndexed { index, point ->
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
                nativeCanvas.drawCircle(point.x, point.y, 20f, paint)

                // Reset for next drawing
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.RED
            }
        }
    }
}

suspend fun applyPerspectiveCorrection(bitmap: Bitmap, points: PerspectivePoints): Bitmap {
    return withContext(Dispatchers.IO) {
        try {
            val src = floatArrayOf(
                points.topLeft.x * bitmap.width, points.topLeft.y * bitmap.height,
                points.topRight.x * bitmap.width, points.topRight.y * bitmap.height,
                points.bottomRight.x * bitmap.width, points.bottomRight.y * bitmap.height,
                points.bottomLeft.x * bitmap.width, points.bottomLeft.y * bitmap.height
            )

            val dst = floatArrayOf(
                0f, 0f,
                bitmap.width.toFloat(), 0f,
                bitmap.width.toFloat(), bitmap.height.toFloat(),
                0f, bitmap.height.toFloat()
            )

            println("DEBUG: Source points: ${src.contentToString()}")
            println("DEBUG: Destination points: ${dst.contentToString()}")

            val matrix = Matrix()
            val result = matrix.setPolyToPoly(src, 0, dst, 0, 4)
            println("DEBUG: Matrix setPolyToPoly result: $result")

            createBitmap(bitmap.width, bitmap.height).apply {
                val canvas = android.graphics.Canvas(this)
                canvas.concat(matrix)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        } catch (e: Exception) {
            println("DEBUG: Error in perspective correction: ${e.message}")
            e.printStackTrace()
            bitmap // Return original bitmap if correction fails
        }
    }
}