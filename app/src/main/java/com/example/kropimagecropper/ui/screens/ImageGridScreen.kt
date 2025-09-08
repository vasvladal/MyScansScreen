package com.example.kropimagecropper.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.kropimagecropper.R
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

//data class ImageItem(
//    val id: String,
//    val file: File,
//    val originalIndex: Int
//)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageGridScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onOrderChanged: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Convert paths to ImageItem objects and filter existing files
    val initialImages = remember(imagePaths) {
        imagePaths.mapIndexed { index, path ->
            ImageItem(
                id = path,
                file = File(path),
                originalIndex = index
            )
        }.filter { it.file.exists() }
    }

    var images by remember { mutableStateOf(initialImages) }
    var draggedItem by remember { mutableStateOf<ImageItem?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteDialog by remember { mutableStateOf<ImageItem?>(null) }

    val gridState = rememberLazyGridState()
    val itemSize = 140.dp
    val itemSizePx = with(density) { itemSize.toPx() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.manage_scans)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (images.isNotEmpty()) {
                        // Reset order button
                        IconButton(
                            onClick = {
                                images = initialImages.sortedBy { it.originalIndex }
                                onOrderChanged(images.map { it.id })
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.order_reset_to_original),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reset_order)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_images_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scan_some_documents_to_get_started),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Instructions card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reorder_your_scans),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.long_press_and_drag_to_reorder_tap_to_view_full_size_long_press_delete_button_to_remove),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Images grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = itemSize),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = images,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isDragged = draggedItem?.id == item.id

                        ImageGridItem(
                            item = item,
                            index = index,
                            isDragged = isDragged,
                            dragOffset = if (isDragged) dragOffset else Offset.Zero,
                            modifier = Modifier
                                .size(itemSize)
                                .zIndex(if (isDragged) 1f else 0f) // Removed animateItemPlacement
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { _ ->
                                            draggedItem = item
                                            dragOffset = Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { _, dragAmount ->
                                            dragOffset += dragAmount

                                            // Simple reordering based on drag distance
                                            val currentIndex = images.indexOf(item)
                                            val dragDistanceInItems =
                                                (dragOffset.x / itemSizePx).roundToInt()
                                            val targetIndex = (currentIndex + dragDistanceInItems)
                                                .coerceIn(0, images.size - 1)

                                            // Reorder if position changed
                                            if (targetIndex != currentIndex && abs(
                                                    dragDistanceInItems
                                                ) >= 1
                                            ) {
                                                val newList = images.toMutableList()
                                                newList.removeAt(currentIndex)
                                                newList.add(targetIndex, item)
                                                images = newList

                                                // Reset drag offset
                                                dragOffset = Offset.Zero
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItem = null
                                            dragOffset = Offset.Zero

                                            // Notify about order change
                                            val newOrder = images.map { it.id }
                                            onOrderChanged(newOrder)
                                        }
                                    )
                                },
                            onClick = { onImageClick(item.id) },
                            onDelete = { showDeleteDialog = item }
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { itemToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(stringResource(R.string.delete_scan)) },
                text = {
                    Text(
                        stringResource(
                            R.string.are_you_sure_you_want_to_delete_this_action_cannot_be_undone,
                            itemToDelete.file.name
                        ))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (itemToDelete.file.delete()) {
                                images = images.filter { it.id != itemToDelete.id }
                                onOrderChanged(images.map { it.id })
//                                Toast.makeText(
//                                    context,
//                                    stringResource(R.string.scan_deleted),
//                                    Toast.LENGTH_SHORT
//                                ).show()
                                Toast.makeText(context, context.getString(R.string.scan_deleted), Toast.LENGTH_SHORT).show()
                            } else {
//                                Toast.makeText(
//                                    context,
//                                    "Failed to delete scan",
//                                    Toast.LENGTH_SHORT
//                                ).show()

                                Toast.makeText(context, context.getString(R.string.failed_to_delete_items), Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ImageGridItem(
    item: ImageItem,
    index: Int,
    isDragged: Boolean,
    dragOffset: Offset,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                alpha = if (isDragged) 0.85f else 1f
                scaleX = if (isDragged) 1.05f else 1f
                scaleY = if (isDragged) 1.05f else 1f
            }
            .then(
                if (isDragged) {
                    Modifier.shadow(16.dp, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragged) 16.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image
            val painter = rememberAsyncImagePainter(
                model = item.file,
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image)
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Position indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${index + 1}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Delete button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                onClick = onDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}