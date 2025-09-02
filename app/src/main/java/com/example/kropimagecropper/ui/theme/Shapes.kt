package com.example.kropimagecropper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ModernShapes = Shapes(
    // Small components (chips, small buttons)
    small = RoundedCornerShape(12.dp),

    // Medium components (cards, dialogs, buttons)
    medium = RoundedCornerShape(16.dp),

    // Large components (bottom sheets, large modals)
    large = RoundedCornerShape(24.dp),

    // Extra large components (full screen modals)
    extraLarge = RoundedCornerShape(28.dp)
)

// Additional shapes for specific use cases
object CustomShapes {
    // Button shapes
    val buttonSmall = RoundedCornerShape(8.dp)
    val buttonMedium = RoundedCornerShape(12.dp)
    val buttonLarge = RoundedCornerShape(16.dp)
    val buttonPill = RoundedCornerShape(50)

    // Card shapes
    val cardSmall = RoundedCornerShape(12.dp)
    val cardMedium = RoundedCornerShape(16.dp)
    val cardLarge = RoundedCornerShape(20.dp)
    val cardXLarge = RoundedCornerShape(24.dp)

    // Input field shapes
    val textFieldRounded = RoundedCornerShape(12.dp)
    val searchBarShape = RoundedCornerShape(28.dp)

    // Modal and dialog shapes
    val modalShape = RoundedCornerShape(28.dp)
    val bottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val alertDialogShape = RoundedCornerShape(24.dp)

    // Image and media shapes
    val imageRounded = RoundedCornerShape(16.dp)
    val avatarShape = RoundedCornerShape(50)
    val thumbnailShape = RoundedCornerShape(12.dp)

    // Navigation shapes
    val bottomNavShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val tabShape = RoundedCornerShape(12.dp)

    // Floating elements
    val fabShape = RoundedCornerShape(16.dp)
    val chipShape = RoundedCornerShape(50)

    // Progress and loading shapes
    val progressShape = RoundedCornerShape(8.dp)
    val skeletonShape = RoundedCornerShape(8.dp)

    // Menu and dropdown shapes
    val menuShape = RoundedCornerShape(16.dp)
    val dropdownShape = RoundedCornerShape(12.dp)
}