// File: app/src/main/java/com/example/kropimagecropper/utils/OpenCVPerspectiveCorrector.kt

package com.example.kropimagecropper.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

class OpenCVPerspectiveCorrector {

    companion object {

        /**
         * Initialize OpenCV (call this in your Application class or main activity)
         */
        fun initializeOpenCV(): Boolean {
            return try {
                // This will load the OpenCV library
                System.loadLibrary("opencv_java4")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        /**
         * Apply perspective correction using manually selected points
         */
        fun correctPerspectiveWithPoints(bitmap: Bitmap, points: PerspectivePoints): Bitmap {
            return try {
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

                // Get transformation matrix using getPerspectiveTransform (equivalent to cv2.getPerspectiveTransform)
                val matrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

                // Apply perspective transformation using warpPerspective (equivalent to cv2.warpPerspective)
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

                resultBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap // Return original bitmap if correction fails
            }
        }

        /**
         * Main function to correct perspective of a document image (automatic detection)
         */
        fun correctPerspective(bitmap: Bitmap): Bitmap {
            return try {
                // Convert bitmap to OpenCV Mat
                val src = Mat()
                Utils.bitmapToMat(bitmap, src)

                // Convert to grayscale for edge detection
                val gray = Mat()
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY)

                // Apply Gaussian blur to reduce noise
                val blurred = Mat()
                Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

                // Edge detection using Canny
                val edges = Mat()
                Imgproc.Canny(blurred, edges, 75.0, 200.0)

                // Find contours
                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

                // Find the largest rectangular contour
                val documentContour = findLargestRectangularContour(contours)

                val result = if (documentContour != null) {
                    // Apply perspective transformation
                    val corrected = applyPerspectiveTransform(src, documentContour)

                    // Convert back to bitmap
                    val resultBitmap = createBitmap(corrected.cols(), corrected.rows())
                    Utils.matToBitmap(corrected, resultBitmap)
                    corrected.release()
                    resultBitmap
                } else {
                    // If no good contour found, try to enhance the original image
                    val enhanced = enhanceDocument(src)

                    val resultBitmap = createBitmap(enhanced.cols(), enhanced.rows())
                    Utils.matToBitmap(enhanced, resultBitmap)
                    enhanced.release()
                    resultBitmap
                }

                // Clean up
                src.release()
                gray.release()
                blurred.release()
                edges.release()
                hierarchy.release()
                documentContour?.release()

                result

            } catch (e: Exception) {
                e.printStackTrace()
                // If OpenCV processing fails, return the original bitmap
                bitmap
            }
        }

        /**
         * Find the largest rectangular contour that likely represents a document
         */
        private fun findLargestRectangularContour(contours: List<MatOfPoint>): MatOfPoint2f? {
            var maxArea = 0.0
            var bestContour: MatOfPoint2f? = null

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)

                // Filter out small areas
                if (area > 1000) {
                    val contour2f = MatOfPoint2f()
                    contour.convertTo(contour2f, CvType.CV_32FC2)

                    // Approximate contour to reduce points
                    val approx = MatOfPoint2f()
                    val epsilon = 0.02 * Imgproc.arcLength(contour2f, true)
                    Imgproc.approxPolyDP(contour2f, approx, epsilon, true)

                    // Check if we have a quadrilateral (4 points)
                    if (approx.total() == 4L && area > maxArea) {
                        // Additional check: ensure it's roughly rectangular
                        if (isRectangularEnough(approx)) {
                            maxArea = area
                            bestContour?.release()
                            bestContour = approx.clone() as MatOfPoint2f?
                        }
                    }

                    contour2f.release()
                    approx.release()
                }
            }

            return bestContour
        }

        /**
         * Check if a 4-point contour is rectangular enough to be considered a document
         */
        private fun isRectangularEnough(contour: MatOfPoint2f): Boolean {
            val points = contour.toArray()
            if (points.size != 4) return false

            // Calculate angles between consecutive edges
            val angles = mutableListOf<Double>()
            for (i in 0..3) {
                val p1 = points[i]
                val p2 = points[(i + 1) % 4]
                val p3 = points[(i + 2) % 4]

                val v1 = Point(p1.x - p2.x, p1.y - p2.y)
                val v2 = Point(p3.x - p2.x, p3.y - p2.y)

                val angle = calculateAngle(v1, v2)
                angles.add(angle)
            }

            // Check if angles are close to 90 degrees (allow some tolerance)
            return angles.all { abs(it - 90.0) < 45.0 }
        }

        /**
         * Calculate angle between two vectors in degrees
         */
        private fun calculateAngle(v1: Point, v2: Point): Double {
            val dot = v1.x * v2.x + v1.y * v2.y
            val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y)
            val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y)

            if (mag1 == 0.0 || mag2 == 0.0) return 0.0

            val cos = dot / (mag1 * mag2)
            val clampedCos = cos.coerceIn(-1.0, 1.0)
            return Math.toDegrees(kotlin.math.acos(clampedCos))
        }

        /**
         * Apply perspective transformation to correct document perspective
         */
        private fun applyPerspectiveTransform(src: Mat, contour: MatOfPoint2f): Mat {
            val points = contour.toArray()

            // Order points: top-left, top-right, bottom-right, bottom-left
            val orderedPoints = orderPoints(points)

            // Calculate the width and height of the new image
            val widthA = distance(orderedPoints[2], orderedPoints[3])
            val widthB = distance(orderedPoints[1], orderedPoints[0])
            val maxWidth = maxOf(widthA, widthB).toInt()

            val heightA = distance(orderedPoints[1], orderedPoints[2])
            val heightB = distance(orderedPoints[0], orderedPoints[3])
            val maxHeight = maxOf(heightA, heightB).toInt()

            // Ensure minimum dimensions
            val finalWidth = maxOf(maxWidth, 300)
            val finalHeight = maxOf(maxHeight, 300)

            // Define destination points (rectangle)
            val dst = MatOfPoint2f()
            dst.fromArray(
                Point(0.0, 0.0),
                Point(finalWidth - 1.0, 0.0),
                Point(finalWidth - 1.0, finalHeight - 1.0),
                Point(0.0, finalHeight - 1.0)
            )

            // Get transformation matrix using getPerspectiveTransform
            val srcPoints = MatOfPoint2f()
            srcPoints.fromArray(*orderedPoints)

            val matrix = Imgproc.getPerspectiveTransform(srcPoints, dst)

            // Apply perspective transformation using warpPerspective
            val warped = Mat()
            Imgproc.warpPerspective(
                src,
                warped,
                matrix,
                Size(finalWidth.toDouble(), finalHeight.toDouble())
            )

            // Clean up
            dst.release()
            srcPoints.release()
            matrix.release()

            return warped
        }


        /**
         * Apply rotation to the image after perspective correction
         */
        fun applyRotation(bitmap: Bitmap, angle: Float): Bitmap {
            return try {
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
                e.printStackTrace()
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
         * Enhance document image (contrast, brightness, etc.) when perspective correction isn't possible
         */
        private fun enhanceDocument(src: Mat): Mat {
            val enhanced = Mat()

            try {
                // Convert to LAB color space for better contrast enhancement
                val lab = Mat()
                Imgproc.cvtColor(src, lab, Imgproc.COLOR_RGB2Lab)

                // Split channels
                val channels = mutableListOf<Mat>()
                Core.split(lab, channels)

                // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization) to L channel
                val clahe = Imgproc.createCLAHE()
                clahe.clipLimit = 2.0
                clahe.tilesGridSize = Size(8.0, 8.0)

                val lChannel = channels[0]
                clahe.apply(lChannel, lChannel)

                // Merge channels back
                Core.merge(channels, lab)

                // Convert back to RGB
                Imgproc.cvtColor(lab, enhanced, Imgproc.COLOR_Lab2RGB)

                // Clean up
                lab.release()
                channels.forEach { it.release() }

            } catch (e: Exception) {
                // If enhancement fails, just copy the source
                src.copyTo(enhanced)
            }

            return enhanced
        }
    }
}