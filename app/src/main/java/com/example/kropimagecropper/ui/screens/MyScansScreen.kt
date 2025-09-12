// MyScansScreen.kt (Modernized UI with DOCX support)
package com.example.kropimagecropper.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.kropimagecropper.R
import com.example.kropimagecropper.data.ScanManager
import com.example.kropimagecropper.utils.DocxCreator
import com.example.kropimagecropper.utils.PdfCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyScansScreen(
    onScan: () -> Unit,
    onOpenScan: (String) -> Unit,
    onManageScans: () -> Unit,
    onManageScansAsList: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val scanDir = ScanManager.getScansDirectory(context)

    var scans by remember { mutableStateOf(emptyList<File>()) }
    var selected by remember { mutableStateOf(setOf<File>()) }
    var isSelectMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var showDocxDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showViewOptions by remember { mutableStateOf(false) }

    // Animation states
    val fabScale by animateFloatAsState(
        targetValue = if (!isSelectMode && scans.isNotEmpty()) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_scale"
    )

    // Pre-fetch string resources
    val selectedCountText = stringResource(R.string.selected_count)
    val selectScansText = stringResource(R.string.select_scans)
    val myScansText = stringResource(R.string.my_scans)
    val cancelText = stringResource(R.string.cancel)
    val scansDeletedText = stringResource(R.string.scans_deleted)
    val deleteScansText = stringResource(R.string.delete_scans)
    val deleteConfirmationText = stringResource(R.string.delete_confirmation)
    val deleteText = stringResource(R.string.delete)
    val createPdfText = stringResource(R.string.create_pdf)
    val createPdfConfirmationText = stringResource(R.string.create_pdf_confirmation)
    val pdfSavedText = stringResource(R.string.pdf_saved)
    val pdfFailedText = stringResource(R.string.pdf_failed)
    val createDocxText = stringResource(R.string.create_docx)
    val createDocxConfirmationText = stringResource(R.string.create_docx_confirmation)
    val docxSavedText = stringResource(R.string.docx_saved)
    val docxFailedText = stringResource(R.string.docx_failed)
    val itemsText = stringResource(R.string.items)
    val loadingScansText = stringResource(R.string.loading_image)
    val failedToDeleteText = stringResource(R.string.delete_failed)
    val clearText = stringResource(R.string.clear)
    val noPdfViewerText = stringResource(R.string.operation_failed)
    val gridViewText = stringResource(R.string.grid_view)
    val listViewText = stringResource(R.string.list_view)

    // Load scans including PDFs and DOCXs
    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            if (scanDir.exists()) {
                scanDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            } else {
                emptyList()
            }
        }

        val pdfDir = PdfCreator.getPdfDirectory(context)
        val pdfFiles = withContext(Dispatchers.IO) {
            if (pdfDir.exists()) {
                pdfDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() == "pdf"
                }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }

        val docxDir = DocxCreator.getDocxDirectory(context)
        val docxFiles = withContext(Dispatchers.IO) {
            if (docxDir.exists()) {
                docxDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() == "docx"
                }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }

        scans = (loaded + pdfFiles + docxFiles).sortedByDescending { it.lastModified() }
        isLoading = false
    }

    // Share files function
    fun shareFiles(files: List<File>) {
        try {
            if (files.isEmpty()) return
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

            val shareIntent = Intent().apply {
                if (files.size == 1) {
                    action = Intent.ACTION_SEND
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        files[0]
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = when (files[0].extension.lowercase()) {
                        "pdf" -> "application/pdf"
                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        else -> "image/jpeg"
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    val uris = files.map { file ->
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    type = "*/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(Intent.createChooser(shareIntent,
                context.getString(R.string.share_files)))
        } catch (e: Exception) {
            Toast.makeText(context,
                context.getString(R.string.failed_to_share_files, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    // Clear selection when exiting select mode
    LaunchedEffect(isSelectMode) {
        if (!isSelectMode) selected = emptySet()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSelectMode) {
                                if (selected.isNotEmpty()) {
                                    "$selectedCountText ${selected.size}"
                                } else {
                                    selectScansText
                                }
                            } else {
                                myScansText
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (!isSelectMode && scans.isNotEmpty()) {
                            Text(
                                text = "${scans.size} $itemsText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSelectMode) {
                                isSelectMode = false
                            } else {
                                onScan()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSelectMode) cancelText else stringResource(R.string.document_scanner)
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isSelectMode,
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it })
                    ) {
                        Row {
                            ModernActionButton(
                                icon = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                enabled = selected.isNotEmpty(),
                                onClick = { shareFiles(selected.toList()) }
                            )
                            ModernActionButton(
                                icon = Icons.Default.PictureAsPdf,
                                contentDescription = createPdfText,
                                enabled = selected.isNotEmpty() && selected.none { it.extension.lowercase() == "pdf" },
                                onClick = { showPdfDialog = true }
                            )
                            ModernActionButton(
                                icon = Icons.Default.Description,
                                contentDescription = createDocxText,
                                enabled = selected.isNotEmpty() && selected.none { it.extension.lowercase() == "docx" },
                                onClick = { showDocxDialog = true }
                            )
                            ModernActionButton(
                                icon = Icons.Default.Delete,
                                contentDescription = deleteText,
                                enabled = selected.isNotEmpty(),
                                onClick = { showDeleteDialog = true }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !isSelectMode && scans.isNotEmpty(),
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it })
                    ) {
                        Box {
                            IconButton(onClick = { showViewOptions = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                            }

                            DropdownMenu(
                                expanded = showViewOptions,
                                onDismissRequest = { showViewOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(gridViewText) },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) },
                                    onClick = {
                                        showViewOptions = false
                                        onManageScans()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(listViewText) },
                                    leadingIcon = { Icon(Icons.Default.ViewList, null) },
                                    onClick = {
                                        showViewOptions = false
                                        onManageScansAsList()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(createPdfText) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                    onClick = {
                                        showViewOptions = false
                                        isSelectMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(createDocxText) },
                                    leadingIcon = { Icon(Icons.Default.Description, null) },
                                    onClick = {
                                        showViewOptions = false
                                        isSelectMode = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectMode && scans.isNotEmpty(),
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = scaleOut(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSelectMode = true
                    },
                    modifier = Modifier.scale(fabScale),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.PictureAsPdf, createPdfText)
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "content_animation"
        ) { loading ->
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            loadingScansText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (scans.isEmpty()) {
                EmptyState(onScan = onScan, padding = padding)
            } else {
                ScansContent(
                    scans = scans,
                    selected = selected,
                    isSelectMode = isSelectMode,
                    onSelect = { file, newSelected ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        selected = if (newSelected) {
                            selected + file
                        } else {
                            selected - file
                        }
                    },
                    onItemClick = { file ->
                        if (isSelectMode) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            selected = if (file in selected) {
                                selected - file
                            } else {
                                selected + file
                            }
                        } else {
                            when (file.extension.lowercase()) {
                                "pdf" -> openPdfFile(context, file)
                                "docx" -> openDocxFile(context, file)
                                else -> onOpenScan(file.absolutePath)
                            }
                        }
                    },
                    onLongPress = { file ->
                        if (!isSelectMode) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            selected = setOf(file)
                            isSelectMode = true
                        }
                    },
                    onClearSelection = { selected = emptySet() },
                    padding = padding
                )
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            ModernAlertDialog(
                title = deleteScansText,
                message = "$deleteConfirmationText ${selected.size} $itemsText",
                confirmText = deleteText,
                onConfirm = {
                    scope.launch {
                        deleteSelectedFiles(
                            selected = selected,
                            context = context,
                            onComplete = { newScans ->
                                scans = newScans
                                selected = emptySet()
                                isSelectMode = false
                                showDeleteDialog = false
                                Toast.makeText(context, scansDeletedText, Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, failedToDeleteText, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onDismiss = { showDeleteDialog = false },
                isDestructive = true
            )
        }

        // PDF Dialog
        if (showPdfDialog) {
            ModernAlertDialog(
                title = createPdfText,
                message = "$createPdfConfirmationText ${selected.size} $itemsText",
                confirmText = createPdfText,
                onConfirm = {
                    scope.launch {
                        try {
                            val imageFiles = selected.filter { it.extension.lowercase() != "pdf" }
                            PdfCreator.createPdf(context, imageFiles) { success, path ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "$pdfSavedText${if (path != null) " $path" else ""}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Reload scans to include new PDF
                                    scope.launch {
                                        scans = reloadScans(context)
                                    }
                                } else {
                                    Toast.makeText(context, pdfFailedText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, pdfFailedText, Toast.LENGTH_SHORT).show()
                        }
                        showPdfDialog = false
                        isSelectMode = false
                        selected = emptySet()
                    }
                },
                onDismiss = { showPdfDialog = false }
            )
        }

        // DOCX Dialog
        if (showDocxDialog) {
            ModernAlertDialog(
                title = createDocxText,
                message = "$createDocxConfirmationText ${selected.size} $itemsText",
                confirmText = createDocxText,
                onConfirm = {
                    scope.launch {
                        try {
                            val imageFiles = selected.filter {
                                it.extension.lowercase() != "docx" &&
                                        it.extension.lowercase() != "pdf"
                            }
                            DocxCreator.createDocx(context, imageFiles) { success, path ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "$docxSavedText${if (path != null) " $path" else ""}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Reload scans to include new DOCX
                                    scope.launch {
                                        scans = reloadScans(context)
                                    }
                                } else {
                                    Toast.makeText(context, docxFailedText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, docxFailedText, Toast.LENGTH_SHORT).show()
                        }
                        showDocxDialog = false
                        isSelectMode = false
                        selected = emptySet()
                    }
                },
                onDismiss = { showDocxDialog = false }
            )
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}

@Composable
private fun EmptyState(
    onScan: () -> Unit,
    padding: PaddingValues
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Gradient background for icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.no_scans_yet),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.no_scans_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        FilledTonalButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onScan()
            },
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.new_scan))
        }
    }
}

@Composable
private fun ScansContent(
    scans: List<File>,
    selected: Set<File>,
    isSelectMode: Boolean,
    onSelect: (File, Boolean) -> Unit,
    onItemClick: (File) -> Unit,
    onLongPress: (File) -> Unit,
    onClearSelection: () -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(scans, key = { it.absolutePath }) { file ->
                ModernScanItem(
                    file = file,
                    isSelected = file in selected,
                    isSelectMode = isSelectMode,
                    onSelect = { newSelected -> onSelect(file, newSelected) },
                    onClick = { onItemClick(file) },
                    onLongPress = { onLongPress(file) }
                )
            }
        }

        // Modern selection bar
        AnimatedVisibility(
            visible = isSelectMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.selected_1, selected.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (selected.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = onClearSelection
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernScanItem(
    file: File,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isPdf = file.extension.lowercase() == "pdf"
    val isDocx = file.extension.lowercase() == "docx"
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = stringResource(R.string.item_scale)
    )

    Card(
        modifier = Modifier
            .scale(scale)
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column {
            Box {
                if (isPdf) {
                    // Modern PDF display
                    Box(
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.pdf_document),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isDocx) {
                    // Modern DOCX display
                    Box(
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = stringResource(R.string.docx_document),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    // Enhanced image preview
                    AsyncImage(
                        model = file,
                        contentDescription = stringResource(R.string.scanned_document),
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Modern selection indicator
                if (isSelectMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(28.dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                },
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        this@Column.AnimatedVisibility(
                            visible = isSelected,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.selected_1),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Enhanced file info section
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = file.nameWithoutExtension,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(file.length()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    if (isPdf) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isDocx) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DOCX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = formatDate(file.lastModified()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Helper functions
private fun formatFileSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "${sizeInBytes} B"
        sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
        else -> "${"%.1f".format(sizeInBytes / (1024.0 * 1024.0))} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun openPdfFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Grant temporary permissions to all apps that can handle the intent
        val resolvedIntentActivities = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolvedIntentInfo in resolvedIntentActivities) {
            val packageName = resolvedIntentInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun openDocxFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Grant temporary permissions to all apps that can handle the intent
        val resolvedIntentActivities = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolvedIntentInfo in resolvedIntentActivities) {
            val packageName = resolvedIntentInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
    }
}

private suspend fun deleteSelectedFiles(
    selected: Set<File>,
    context: android.content.Context,
    onComplete: (List<File>) -> Unit,
    onError: () -> Unit
) {
    try {
        selected.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }

        val newScans = reloadScans(context)
        onComplete(newScans)
    } catch (e: Exception) {
        onError()
    }
}

private suspend fun reloadScans(context: android.content.Context): List<File> {
    return withContext(Dispatchers.IO) {
        val scanDir = ScanManager.getScansDirectory(context)
        val pdfDir = PdfCreator.getPdfDirectory(context)
        val docxDir = DocxCreator.getDocxDirectory(context)

        val imageFiles = if (scanDir.exists()) {
            scanDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }

        val pdfFiles = if (pdfDir.exists()) {
            pdfDir.listFiles { file ->
                file.isFile && file.extension.lowercase() == "pdf"
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }

        val docxFiles = if (docxDir.exists()) {
            docxDir.listFiles { file ->
                file.isFile && file.extension.lowercase() == "docx"
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }

        (imageFiles + pdfFiles + docxFiles).sortedByDescending { it.lastModified() }
    }
}