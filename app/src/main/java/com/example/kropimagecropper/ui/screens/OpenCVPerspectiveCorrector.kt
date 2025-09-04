// File: app/src/main/java/com/example/kropimagecropper/utils/OpenCVPerspectiveCorrector.kt

package com.example.kropimagecropper.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OpenCVPerspectiveCorrector {

    companion object {

        /**
         * Initialize OpenCV (call this in your Application class or main activity)
         */
        fun initializeOpenCV(): Boolean {
            return try {
                // OpenCV initialization will be handled by OpenCV Manager or static initialization
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        /**
         * Main function to correct perspective of a document image
         */
        fun correctPerspective(bitmap: Bitmap): Bitmap {
            return try {
                // Convert bitmap to OpenCV Mat
                val src = Mat()
                Utils.bitmapToMat(bitmap, src)

                // Convert to grayscale for edge detection
                val gray = Mat()
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

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

                if (documentContour != null) {
                    // Apply perspective transformation
                    val corrected = applyPerspectiveTransform(src, documentContour)

                    // Convert back to bitmap
                    val resultBitmap = Bitmap.createBitmap(
                        corrected.cols(),
                        corrected.rows(),
                        Bitmap.Config.ARGB_8888
                    )
                    Utils.matToBitmap(corrected, resultBitmap)

                    // Clean up
                    src.release()
                    gray.release()
                    blurred.release()
                    edges.release()
                    hierarchy.release()
                    corrected.release()

                    return resultBitmap
                } else {
                    // If no good contour found, try to enhance the original image
                    val enhanced = enhanceDocument(src)

                    val resultBitmap = Bitmap.createBitmap(
                        enhanced.cols(),
                        enhanced.rows(),
                        Bitmap.Config.ARGB_8888
                    )
                    Utils.matToBitmap(enhanced, resultBitmap)

                    // Clean up
                    src.release()
                    gray.release()
                    blurred.release()
                    edges.release()
                    hierarchy.release()
                    enhanced.release()

                    return resultBitmap
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // If OpenCV processing fails, return the original bitmap
                return bitmap
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
                if (area > 10000) {
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
                            bestContour = approx
                        }
                    }

                    contour2f.release()
                    if (approx != bestContour) {
                        approx.release()
                    }
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
            return angles.all { abs(it - 90.0) < 30.0 } // 30 degree tolerance
        }

        /**
         * Calculate angle between two vectors in degrees
         */
        private fun calculateAngle(v1: Point, v2: Point): Double {
            val dot = v1.x * v2.x + v1.y * v2.y
            val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y)
            val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y)

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
            val finalWidth = maxOf(maxWidth, 200)
            val finalHeight = maxOf(maxHeight, 200)

            // Define destination points (rectangle)
            val dst = MatOfPoint2f()
            dst.fromArray(
                Point(0.0, 0.0),                           // Top-left
                Point(finalWidth - 1.0, 0.0),              // Top-right
                Point(finalWidth - 1.0, finalHeight - 1.0), // Bottom-right
                Point(0.0, finalHeight - 1.0)              // Bottom-left
            )

            // Get transformation matrix and apply it
            val srcPoints = MatOfPoint2f()
            srcPoints.fromArray(*orderedPoints)

            val matrix = Imgproc.getPerspectiveTransform(srcPoints, dst)
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

            // Convert to LAB color space for better contrast enhancement
            val lab = Mat()
            Imgproc.cvtColor(src, lab, Imgproc.COLOR_BGR2Lab)

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

            // Convert back to BGR
            Imgproc.cvtColor(lab, enhanced, Imgproc.COLOR_Lab2BGR)

            // Clean up
            lab.release()
            channels.forEach { it.release() }

            return enhanced
        }
    }
}