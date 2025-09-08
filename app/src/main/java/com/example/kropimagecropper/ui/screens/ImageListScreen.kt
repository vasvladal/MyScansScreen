package com.example.kropimagecropper.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.kropimagecropper.R
import java.io.File
import kotlin.math.abs

data class ImageItem(
    val id: String,
    val file: File,
    val originalIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageListScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onOrderChanged: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current

    // Convert paths to ImageItem objects
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
    var dragOffset by remember { mutableStateOf(0f) }

    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Item height for calculations
    val itemHeight = 120.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Image Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
                Text(
                    text = "No images found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(
                    items = images,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isDragged = draggedItem?.id == item.id

                    ImageListItem(
                        item = item,
                        index = index,
                        isDragged = isDragged,
                        dragOffset = if (isDragged) dragOffset else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .zIndex(if (isDragged) 1f else 0f) // Removed animateItemPlacement
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggedItem = item
                                        dragOffset = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dragOffset += dragAmount.y

                                        // Calculate target position
                                        val currentIndex = images.indexOf(item)
                                        val targetIndex = (currentIndex + (dragOffset / itemHeightPx).toInt())
                                            .coerceIn(0, images.size - 1)

                                        // Reorder if position changed
                                        if (targetIndex != currentIndex && targetIndex >= 0 && targetIndex < images.size) {
                                            val newList = images.toMutableList()
                                            newList.removeAt(currentIndex)
                                            newList.add(targetIndex, item)
                                            images = newList

                                            // Adjust drag offset for the new position
                                            dragOffset -= (targetIndex - currentIndex) * itemHeightPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItem = null
                                        dragOffset = 0f

                                        // Notify about order change
                                        val newOrder = images.map { it.id }
                                        onOrderChanged(newOrder)

                                        Toast.makeText(
                                            context,
                                            "Images reordered",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            },
                        onClick = { onImageClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageListItem(
    item: ImageItem,
    index: Int,
    isDragged: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .graphicsLayer {
                translationY = dragOffset
                alpha = if (isDragged) 0.8f else 1f
            }
            .then(
                if (isDragged) {
                    Modifier.shadow(8.dp, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragged) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier.padding(end = 12.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Image thumbnail
            val painter = rememberAsyncImagePainter(
                model = item.file,
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image)
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Image info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Position: ${index + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${item.file.length() / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}