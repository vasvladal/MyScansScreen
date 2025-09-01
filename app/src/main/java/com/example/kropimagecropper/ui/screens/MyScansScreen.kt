// File: app/src/main/java/com/example/kropimagecropper/ui/screens/MyScansScreen.kt

package com.example.kropimagecropper.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kropimagecropper.R
import com.example.kropimagecropper.data.ScanManager
import com.example.kropimagecropper.utils.PdfCreator
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScansScreen(
    onScan: () -> Unit,
    onOpenScan: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanDir = ScanManager.getScansDirectory(context)
    var scans by remember { mutableStateOf(emptyList<File>()) }
    var selected by remember { mutableStateOf(setOf<File>()) }
    var isSelectMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

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

    // Load scans
    LaunchedEffect(Unit) {
        isLoading = true
        ScanManager.loadScans(scanDir) { loaded ->
            scans = loaded
            isLoading = false
        }
    }

    // Clear selection when exiting select mode
    LaunchedEffect(isSelectMode) {
        if (!isSelectMode) selected = emptySet()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
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
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectMode) {
                            isSelectMode = false
                        } else {
                            // Safe back navigation
                            try {
                                onBack()
                            } catch (e: Exception) {
                                // Fallback to system back if onBack fails
                                (context as? Activity)?.finish()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isSelectMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSelectMode) cancelText else "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectMode) {
                        IconButton(
                            onClick = { showPdfDialog = true },
                            enabled = selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = createPdfText)
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = deleteText)
                        }
                    } else {
                        if (scans.isNotEmpty()) {
                            IconButton(onClick = { isSelectMode = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(!isSelectMode) {
                FloatingActionButton(onClick = onScan) {
                    Icon(Icons.Default.Add, contentDescription = "Add new scan")
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (scans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_scans_yet),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_scans_description),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onScan) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.new_scan))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(scans, key = { it.absolutePath }) { file ->
                    ScanItem(
                        file = file,
                        isSelected = file in selected,
                        isSelectMode = isSelectMode,
                        onSelect = { newSelected ->
                            selected = if (newSelected) {
                                selected + file
                            } else {
                                selected - file
                            }
                        },
                        onClick = {
                            if (isSelectMode) {
                                // Toggle selection
                                selected = if (file in selected) {
                                    selected - file
                                } else {
                                    selected + file
                                }
                            } else {
                                onOpenScan(file.absolutePath)
                            }
                        }
                    )
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(deleteScansText) },
                text = { Text("$deleteConfirmationText ${selected.size}") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    selected.forEach { file ->
                                        if (file.exists()) {
                                            file.delete()
                                        }
                                    }
                                    // Update the scans list
                                    scans = scans.filter { it !in selected && it.exists() }
                                    selected = emptySet()
                                    isSelectMode = false
                                    showDeleteDialog = false
                                    Toast.makeText(context, scansDeletedText, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to delete scans", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(deleteText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(cancelText)
                    }
                }
            )
        }

        // PDF Dialog
        if (showPdfDialog) {
            AlertDialog(
                onDismissRequest = { showPdfDialog = false },
                title = { Text(createPdfText) },
                text = { Text("$createPdfConfirmationText ${selected.size}") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    PdfCreator.createPdf(context, selected.toList()) { success, path ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "$pdfSavedText${if (path != null) " $path" else ""}",
                                                Toast.LENGTH_LONG
                                            ).show()
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
                        }
                    ) {
                        Text(createPdfText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfDialog = false }) {
                        Text(cancelText)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanItem(
    file: File,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(min = 120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!isSelectMode) {
                        onSelect(true)
                    }
                }
            )
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = if (isSelected) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Modifier
            }
        ) {
            Column {
                Box {
                    AsyncImage(
                        model = file,
                        contentDescription = "Scanned document",
                        modifier = Modifier
                            .height(120.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (isSelectMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.8f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = file.nameWithoutExtension,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${file.length() / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}