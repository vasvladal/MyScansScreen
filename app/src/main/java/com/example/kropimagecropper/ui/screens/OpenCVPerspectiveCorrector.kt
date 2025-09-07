package com.example.kropimagecropper.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap
import java.util.ArrayList

class OpenCVPerspectiveCorrector {

    companion object {
        private const val TAG = "OpenCVPerspective"

        /**
         * Initialize OpenCV (call this in your Application class or main activity)
         */
        fun initializeOpenCV(): Boolean {
            return try {
                // This will load the OpenCV library
                System.loadLibrary("opencv_java4")
                Log.d(TAG, "OpenCV initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "OpenCV initialization failed", e)
                false
            }
        }

        /**
         * Apply perspective correction using manually selected points
         */
        fun correctPerspectiveWithPoints(bitmap: Bitmap, points: PerspectivePoints): Bitmap {
            return try {
                Log.d(TAG, "Starting perspective correction with manual points")

                // Convert bitmap to OpenCV Mat
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

                // Destination points (the entire bitmap)
                val dstPoints = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(bitmap.width.toDouble(), 0.0),
                    Point(bitmap.width.toDouble(), bitmap.height.toDouble()),
                    Point(0.0, bitmap.height.toDouble())
                )

                // Get transformation matrix
                val matrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

                // Apply perspective transformation
                val warped = Mat()
                Imgproc.warpPerspective(
                    src,
                    warped,
                    matrix,
                    Size(bitmap.width.toDouble(), bitmap.height.toDouble())
                )

                // Convert back to bitmap
                val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(warped, resultBitmap)

                // Clean up
                src.release()
                srcPoints.release()
                dstPoints.release()
                matrix.release()
                warped.release()

                Log.d(TAG, "Manual perspective correction completed successfully")
                resultBitmap
            } catch (e: Exception) {
                Log.e(TAG, "Manual perspective correction failed", e)
                bitmap // Return original bitmap if correction fails
            }
        }

        /**
         * Main function to correct perspective of a document image (automatic detection)
         */
        fun correctPerspective(bitmap: Bitmap): Bitmap {
            return try {
                Log.d(TAG, "Starting automatic perspective correction")
                Log.d(TAG, "Input bitmap: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

                // Convert bitmap to OpenCV Mat
                val src = Mat()
                Utils.bitmapToMat(bitmap, src)
                Log.d(TAG, "Converted to Mat: ${src.cols()}x${src.rows()}, channels: ${src.channels()}")

                // Convert to grayscale for processing
                val gray = Mat()
                when (src.channels()) {
                    4 -> Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
                    3 -> Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY)
                    1 -> src.copyTo(gray)
                    else -> {
                        Log.w(TAG, "Unexpected number of channels: ${src.channels()}")
                        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
                    }
                }

                // Preprocessing steps for better edge detection
                val processed = preprocessImage(gray)

                // Try multiple edge detection approaches
                val documentContour = findDocumentContour(processed)

                val result = if (documentContour != null) {
                    Log.d(TAG, "Document contour found, applying perspective transformation")
                    val corrected = applyPerspectiveTransform(src, documentContour)

                    // Convert back to bitmap with proper format handling
                    val resultBitmap = convertMatToBitmap(corrected)
                    corrected.release()
                    resultBitmap
                } else {
                    Log.d(TAG, "No document contour found, returning original image")
                    // Return original image instead of enhanced grayscale
                    convertMatToBitmap(src)
                }

                // Clean up
                src.release()
                gray.release()
                processed.release()
                documentContour?.release()

                Log.d(TAG, "Automatic perspective correction completed")
                result

            } catch (e: Exception) {
                Log.e(TAG, "Automatic perspective correction failed", e)
                e.printStackTrace()
                // If OpenCV processing fails, return the original bitmap
                bitmap
            }
        }

        /**
         * Preprocess image for better edge detection
         */
        private fun preprocessImage(gray: Mat): Mat {
            val processed = Mat()

            try {
                // Apply bilateral filter to reduce noise while preserving edges
                val filtered = Mat()
                Imgproc.bilateralFilter(gray, filtered, 9, 75.0, 75.0)

                // Apply adaptive threshold to handle varying lighting conditions
                val thresh = Mat()
                Imgproc.adaptiveThreshold(
                    filtered, thresh, 255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11, 2.0
                )

                // Apply morphological operations to clean up the image
                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
                Imgproc.morphologyEx(thresh, processed, Imgproc.MORPH_CLOSE, kernel)

                // Clean up intermediate results
                filtered.release()
                thresh.release()
                kernel.release()

            } catch (e: Exception) {
                Log.e(TAG, "Preprocessing failed, using original", e)
                gray.copyTo(processed)
            }

            return processed
        }

        /**
         * Find document contour using multiple approaches
         */
        private fun findDocumentContour(processed: Mat): MatOfPoint2f? {
            try {
                // Try Canny edge detection first
                val cannyContour = findContourWithCanny(processed)
                if (cannyContour != null) {
                    Log.d(TAG, "Found contour using Canny edge detection")
                    return cannyContour
                }

                // If Canny fails, try direct contour detection on thresholded image
                val directContour = findContourDirect(processed)
                if (directContour != null) {
                    Log.d(TAG, "Found contour using direct detection")
                    return directContour
                }

                Log.d(TAG, "No suitable contour found")
                return null
            } catch (e: Exception) {
                Log.e(TAG, "Contour detection failed", e)
                return null
            }
        }

        /**
         * Find contour using Canny edge detection
         */
        private fun findContourWithCanny(processed: Mat): MatOfPoint2f? {
            return try {
                // Apply Gaussian blur
                val blurred = Mat()
                Imgproc.GaussianBlur(processed, blurred, Size(5.0, 5.0), 0.0)

                // Apply Canny edge detection with multiple thresholds
                val edges = Mat()
                Imgproc.Canny(blurred, edges, 50.0, 150.0, 3, false)

                // Dilate edges to connect broken lines
                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
                Imgproc.dilate(edges, edges, kernel)

                // Find contours
                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                val result = findLargestRectangularContour(contours)

                // Clean up
                blurred.release()
                edges.release()
                kernel.release()
                hierarchy.release()
                contours.forEach { it.release() }

                result
            } catch (e: Exception) {
                Log.e(TAG, "Canny contour detection failed", e)
                null
            }
        }

        /**
         * Find contour directly from processed image
         */
        private fun findContourDirect(processed: Mat): MatOfPoint2f? {
            return try {
                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                Imgproc.findContours(processed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                val result = findLargestRectangularContour(contours)

                // Clean up
                hierarchy.release()
                contours.forEach { it.release() }

                result
            } catch (e: Exception) {
                Log.e(TAG, "Direct contour detection failed", e)
                null
            }
        }

        /**
         * Find the largest rectangular contour that likely represents a document
         */
        private fun findLargestRectangularContour(contours: List<MatOfPoint>): MatOfPoint2f? {
            var maxArea = 0.0
            var bestContour: MatOfPoint2f? = null
            val imageArea = 1000000.0 // Approximate image area for filtering

            Log.d(TAG, "Processing ${contours.size} contours")

            for ((index, contour) in contours.withIndex()) {
                try {
                    val area = Imgproc.contourArea(contour)

                    // Filter contours by area (should be at least 1% of image area)
                    if (area < max(1000.0, imageArea * 0.01)) {
                        continue
                    }

                    val contour2f = MatOfPoint2f()
                    contour.convertTo(contour2f, CvType.CV_32FC2)

                    // Approximate contour with different epsilon values
                    val epsilons = listOf(0.01, 0.02, 0.03, 0.05)

                    for (epsilon in epsilons) {
                        val approx = MatOfPoint2f()
                        val arcLength = Imgproc.arcLength(contour2f, true)
                        Imgproc.approxPolyDP(contour2f, approx, epsilon * arcLength, true)

                        if (approx.total() == 4L) {
                            if (isValidQuadrilateral(approx) && area > maxArea) {
                                Log.d(TAG, "Found better contour at index $index with area $area and epsilon $epsilon")
                                maxArea = area
                                bestContour?.release()
                                bestContour = approx.clone() as MatOfPoint2f
                                break
                            }
                        }
                        approx.release()
                    }

                    contour2f.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing contour $index", e)
                }
            }

            Log.d(TAG, if (bestContour != null) "Best contour found with area $maxArea" else "No suitable contour found")
            return bestContour
        }

        /**
         * Check if a 4-point contour is a valid quadrilateral
         */
        private fun isValidQuadrilateral(contour: MatOfPoint2f): Boolean {
            val points = contour.toArray()
            if (points.size != 4) return false

            // Check if points form a convex quadrilateral
            if (!isConvexQuadrilateral(points)) return false

            // Check minimum area
            val area = Imgproc.contourArea(contour)
            if (area < 1000) return false

            // Check aspect ratio (shouldn't be too extreme)
            val orderedPoints = orderPoints(points)
            val width1 = distance(orderedPoints[0], orderedPoints[1])
            val width2 = distance(orderedPoints[2], orderedPoints[3])
            val height1 = distance(orderedPoints[1], orderedPoints[2])
            val height2 = distance(orderedPoints[3], orderedPoints[0])

            val avgWidth = (width1 + width2) / 2
            val avgHeight = (height1 + height2) / 2

            val aspectRatio = max(avgWidth / avgHeight, avgHeight / avgWidth)

            // Allow reasonable aspect ratios (up to 4:1)
            return aspectRatio < 4.0
        }

        /**
         * Check if quadrilateral is convex
         */
        private fun isConvexQuadrilateral(points: Array<Point>): Boolean {
            if (points.size != 4) return false

            // Calculate cross products for all adjacent edges
            var signChanges = 0
            var lastSign = 0

            for (i in 0..3) {
                val p1 = points[i]
                val p2 = points[(i + 1) % 4]
                val p3 = points[(i + 2) % 4]

                val crossProduct = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x)
                val sign = if (crossProduct > 0) 1 else if (crossProduct < 0) -1 else 0

                if (sign != 0) {
                    if (lastSign != 0 && sign != lastSign) {
                        signChanges++
                    }
                    lastSign = sign
                }
            }

            // Convex quadrilateral should have at most 1 sign change
            return signChanges <= 1
        }

        /**
         * Apply perspective transformation to correct document perspective
         */
        private fun applyPerspectiveTransform(src: Mat, contour: MatOfPoint2f): Mat {
            val points = contour.toArray()
            Log.d(TAG, "Applying perspective transform with ${points.size} points")

            // Order points: top-left, top-right, bottom-right, bottom-left
            val orderedPoints = orderPoints(points)

            // Calculate the optimal dimensions for the output
            val widthTop = distance(orderedPoints[0], orderedPoints[1])
            val widthBottom = distance(orderedPoints[3], orderedPoints[2])
            val heightLeft = distance(orderedPoints[0], orderedPoints[3])
            val heightRight = distance(orderedPoints[1], orderedPoints[2])

            val maxWidth = max(widthTop, widthBottom).toInt()
            val maxHeight = max(heightLeft, heightRight).toInt()

            // Ensure minimum reasonable dimensions
            val finalWidth = max(maxWidth, 400)
            val finalHeight = max(maxHeight, 300)

            Log.d(TAG, "Output dimensions: ${finalWidth}x${finalHeight}")

            // Define destination points (rectangle)
            val dst = MatOfPoint2f()
            dst.fromArray(
                Point(0.0, 0.0),
                Point(finalWidth.toDouble(), 0.0),
                Point(finalWidth.toDouble(), finalHeight.toDouble()),
                Point(0.0, finalHeight.toDouble())
            )

            // Get transformation matrix
            val srcPoints = MatOfPoint2f()
            srcPoints.fromArray(*orderedPoints)

            val matrix = Imgproc.getPerspectiveTransform(srcPoints, dst)

            // Apply perspective transformation
            val warped = Mat()
            Imgproc.warpPerspective(
                src,
                warped,
                matrix,
                Size(finalWidth.toDouble(), finalHeight.toDouble()),
                Imgproc.INTER_LINEAR,
                Core.BORDER_CONSTANT,
                Scalar(255.0, 255.0, 255.0) // White background
            )

            // Clean up
            dst.release()
            srcPoints.release()
            matrix.release()

            return warped
        }

        /**
         * Convert Mat to Bitmap with proper format handling
         */
        private fun convertMatToBitmap(mat: Mat): Bitmap {
            val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)

            try {
                // Handle different channel configurations
                when (mat.channels()) {
                    1 -> {
                        // Grayscale - convert to RGB first
                        val rgbMat = Mat()
                        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_GRAY2RGBA)
                        Utils.matToBitmap(rgbMat, bitmap)
                        rgbMat.release()
                    }
                    3 -> {
                        // RGB - convert to RGBA
                        val rgbaMat = Mat()
                        Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_RGB2RGBA)
                        Utils.matToBitmap(rgbaMat, bitmap)
                        rgbaMat.release()
                    }
                    4 -> {
                        // RGBA - direct conversion
                        Utils.matToBitmap(mat, bitmap)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected channel count: ${mat.channels()}")
                        Utils.matToBitmap(mat, bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error converting Mat to Bitmap", e)
                // Fallback: try direct conversion
                try {
                    Utils.matToBitmap(mat, bitmap)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback conversion also failed", e2)
                }
            }

            return bitmap
        }

        /**
         * Apply rotation to the image after perspective correction
         */
        fun applyRotation(bitmap: Bitmap, angle: Float): Bitmap {
            return try {
                if (angle == 0f) return bitmap

                val matrix = Matrix()
                matrix.postRotate(angle)

                Bitmap.createBitmap(
                    bitmap,
                    0, 0,
                    bitmap.width, bitmap.height,
                    matrix,
                    true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Rotation failed", e)
                bitmap
            }
        }

        /**
         * Apply perspective correction with optional rotation
         */
        fun correctPerspectiveWithPointsAndRotation(
            bitmap: Bitmap,
            points: PerspectivePoints,
            rotationAngle: Float = 0f
        ): Bitmap {
            val perspectiveCorrected = correctPerspectiveWithPoints(bitmap, points)
            return if (rotationAngle != 0f) {
                applyRotation(perspectiveCorrected, rotationAngle)
            } else {
                perspectiveCorrected
            }
        }

        /**
         * Order points in the sequence: top-left, top-right, bottom-right, bottom-left
         */
        private fun orderPoints(points: Array<Point>): Array<Point> {
            val ordered = Array(4) { Point() }

            // Calculate sums and differences of coordinates
            val sums = points.map { it.x + it.y }
            val diffs = points.map { it.x - it.y }

            // Top-left point has the smallest sum
            val tlIndex = sums.indexOf(sums.minOrNull()!!)
            ordered[0] = points[tlIndex]

            // Bottom-right point has the largest sum
            val brIndex = sums.indexOf(sums.maxOrNull()!!)
            ordered[2] = points[brIndex]

            // Top-right point has the smallest difference (x - y)
            val trIndex = diffs.indexOf(diffs.minOrNull()!!)
            ordered[1] = points[trIndex]

            // Bottom-left point has the largest difference (x - y)
            val blIndex = diffs.indexOf(diffs.maxOrNull()!!)
            ordered[3] = points[blIndex]

            return ordered
        }

        /**
         * Calculate Euclidean distance between two points
         */
        private fun distance(p1: Point, p2: Point): Double {
            return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
        }

        /**
         * Enhance document image when perspective correction isn't possible
         */
        private fun enhanceDocument(src: Mat): Mat {
            val enhanced = Mat()

            try {
                Log.d(TAG, "Applying document enhancement")

                // Preserve color while enhancing
                if (src.channels() >= 3) {
                    // Convert to LAB color space
                    val lab = Mat()
                    Imgproc.cvtColor(src, lab, Imgproc.COLOR_BGR2Lab)

                    // Split channels
                    val channels = ArrayList<Mat>()
                    Core.split(lab, channels)

                    // Apply CLAHE to L channel
                    val clahe = Imgproc.createCLAHE()
                    clahe.clipLimit = 3.0
                    clahe.tilesGridSize = Size(8.0, 8.0)
                    clahe.apply(channels[0], channels[0])

                    // Merge channels back
                    Core.merge(channels, lab)

                    // Convert back to BGR
                    Imgproc.cvtColor(lab, enhanced, Imgproc.COLOR_Lab2BGR)

                    // Cleanup
                    lab.release()
                    channels.forEach { it.release() }
                } else {
                    // For grayscale images
                    val clahe = Imgproc.createCLAHE()
                    clahe.clipLimit = 3.0
                    clahe.tilesGridSize = Size(8.0, 8.0)
                    clahe.apply(src, enhanced)
                }
            } catch (e: Exception) {
                src.copyTo(enhanced)
            }
            return enhanced
        }
    }
}