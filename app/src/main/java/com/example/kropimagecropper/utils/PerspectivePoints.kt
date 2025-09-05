// File: app/src/main/java/com/example/kropimagecropper/utils/PerspectivePoints.kt
package com.example.kropimagecropper.utils

import android.graphics.PointF

data class PerspectivePoints(
    val topLeft: PointF = PointF(0.1f, 0.1f),
    val topRight: PointF = PointF(0.9f, 0.1f),
    val bottomRight: PointF = PointF(0.9f, 0.9f),
    val bottomLeft: PointF = PointF(0.1f, 0.9f)
)