// File: app/src/main/java/com/example/kropimagecropper/utils/PerspectivePoints.kt

package com.example.kropimagecropper.utils

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset

/**
 * Data class representing the four corner points for perspective correction
 * Points are normalized (0.0 to 1.0) relative to image dimensions
 */
data class PerspectivePoints(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset
) {
    companion object {
        /**
         * Create default points that represent the entire image
         */
        fun default() = PerspectivePoints(
            topLeft = Offset(0f, 0f),
            topRight = Offset(1f, 0f),
            bottomRight = Offset(1f, 1f),
            bottomLeft = Offset(0f, 1f)
        )

        /**
         * Create points from absolute coordinates and image dimensions
         */
        fun fromAbsolute(
            topLeft: Offset,
            topRight: Offset,
            bottomRight: Offset,
            bottomLeft: Offset,
            imageWidth: Int,
            imageHeight: Int
        ) = PerspectivePoints(
            topLeft = Offset(topLeft.x / imageWidth, topLeft.y / imageHeight),
            topRight = Offset(topRight.x / imageWidth, topRight.y / imageHeight),
            bottomRight = Offset(bottomRight.x / imageWidth, bottomRight.y / imageHeight),
            bottomLeft = Offset(bottomLeft.x / imageWidth, bottomLeft.y / imageHeight)
        )
    }

    /**
     * Convert to PointF objects for Android graphics
     */
    fun toPointF(imageWidth: Int, imageHeight: Int): Array<PointF> {
        return arrayOf(
            PointF(topLeft.x * imageWidth, topLeft.y * imageHeight),
            PointF(topRight.x * imageWidth, topRight.y * imageHeight),
            PointF(bottomRight.x * imageWidth, bottomRight.y * imageHeight),
            PointF(bottomLeft.x * imageWidth, bottomLeft.y * imageHeight)
        )
    }

    /**
     * Check if the points form a valid quadrilateral
     */
    fun isValid(): Boolean {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)

        // Check if all points are within bounds
        return points.all { point ->
            point.x >= 0f && point.x <= 1f && point.y >= 0f && point.y <= 1f
        }
    }
}