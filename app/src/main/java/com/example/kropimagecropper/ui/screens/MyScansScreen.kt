// File: app/src/main/java/com/example/kropimagecropper/ui/screens/MyScansScreen.kt

package com.example.kropimagecropper.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.core.content.FileProvider
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

    // String resources
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

    // Load scans on start
    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = ScanManager.loadScans(scanDir)
        val pdfDir = PdfCreator.getPdfDirectory(context)
        val pdfFiles = if (pdfDir.exists()) {
            pdfDir.listFiles { file -> file.isFile && file.extension.lowercase() == "pdf" }?.toList()
                ?: emptyList()
        } else emptyList()
        scans = (loaded + pdfFiles).sortedByDescending { it.lastModified() }
        isLoading = false
    }

    // Share function
    fun shareFiles(files: List<File>) {
        try {
            if (files.isEmpty()) return
            val shareIntent = Intent().apply {
                if (files.size == 1) {
                    action = Intent.ACTION_SEND
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        files[0]
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = if (files[0].extension.lowercase() == "pdf") "application/pdf" else "image/jpeg"
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
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share files"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Clear selection when leaving select mode
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
                        if (isSelectMode) isSelectMode = false else onScan()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isSelectMode) cancelText else "Scanner"
                        )
                    }
                },
                actions = {
                    if (isSelectMode) {
                        IconButton(
                            onClick = { shareFiles(selected.toList()) },
                            enabled = selected.isNotEmpty()
                        ) { Icon(Icons.Default.Share, contentDescription = "Share") }

                        IconButton(
                            onClick = { showPdfDialog = true },
                            enabled = selected.isNotEmpty() && selected.none { it.extension.lowercase() == "pdf" }
                        ) { Icon(Icons.Default.PictureAsPdf, contentDescription = createPdfText) }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = selected.isNotEmpty()
                        ) { Icon(Icons.Default.Delete, contentDescription = deleteText) }
                    } else {
                        if (scans.isNotEmpty()) {
                            IconButton(onClick = { isSelectMode = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            scans.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.DocumentScanner, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_scans_yet),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.no_scans_description),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(24.dp))
                Button(onClick = onScan) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.new_scan))
                }
            }

            else -> LazyVerticalGrid(
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
                        onSelect = { newSel ->
                            selected = if (newSel) selected + file else selected - file
                        },
                        onClick = {
                            if (isSelectMode) {
                                selected = if (file in selected) selected - file else selected + file
                            } else {
                                if (file.extension.lowercase() == "pdf") {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                                    }
                                } else onOpenScan(file.absolutePath)
                            }
                        }
                    )
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(deleteScansText) },
                text = { Text("$deleteConfirmationText ${selected.size} items") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                selected.forEach { if (it.exists()) it.delete() }
                                val loaded = ScanManager.loadScans(scanDir)
                                val pdfDir = PdfCreator.getPdfDirectory(context)
                                val pdfFiles = if (pdfDir.exists()) {
                                    pdfDir.listFiles { f -> f.isFile && f.extension.lowercase() == "pdf" }?.toList()
                                        ?: emptyList()
                                } else emptyList()
                                scans = (loaded + pdfFiles).sortedByDescending { it.lastModified() }
                                selected = emptySet(); isSelectMode = false; showDeleteDialog = false
                                Toast.makeText(context, scansDeletedText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text(deleteText) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(cancelText) }
                }
            )
        }

        // PDF dialog
        // PDF dialog
        if (showPdfDialog) {
            AlertDialog(
                onDismissRequest = { showPdfDialog = false },
                title = { Text(createPdfText) },
                text = { Text("$createPdfConfirmationText ${selected.size} images") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val imageFiles = selected.filter { it.extension.lowercase() != "pdf" }
                                val pdfFile = PdfCreator.createPdf(context, imageFiles)

                                if (pdfFile != null) {
                                    Toast.makeText(
                                        context,
                                        "$pdfSavedText ${pdfFile.absolutePath}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val loaded = ScanManager.loadScans(scanDir)
                                    val pdfDir = PdfCreator.getPdfDirectory(context)
                                    val pdfFiles = if (pdfDir.exists()) {
                                        pdfDir.listFiles { f -> f.isFile && f.extension.lowercase() == "pdf" }?.toList()
                                            ?: emptyList()
                                    } else emptyList()

                                    scans = (loaded + pdfFiles).sortedByDescending { it.lastModified() }
                                } else {
                                    Toast.makeText(context, pdfFailedText, Toast.LENGTH_SHORT).show()
                                }

                                showPdfDialog = false
                                isSelectMode = false
                                selected = emptySet()
                            }
                        }
                    ) { Text(createPdfText) }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfDialog = false }) { Text(cancelText) }
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
    val isPdf = file.extension.lowercase() == "pdf"
    Card(
        modifier = Modifier
            .widthIn(min = 120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (!isSelectMode) onSelect(true) }
            )
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = if (isSelected) Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) else Modifier
        ) {
            Column {
                Box {
                    if (isPdf) {
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, "PDF",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AsyncImage(
                            model = file,
                            contentDescription = "Scanned document",
                            modifier = Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isSelectMode) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd)
                                .padding(8.dp).size(24.dp)
                                .background(
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Column(Modifier.padding(8.dp)) {
                    Text(file.nameWithoutExtension,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${file.length() / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        if (isPdf) {
                            Text("PDF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
