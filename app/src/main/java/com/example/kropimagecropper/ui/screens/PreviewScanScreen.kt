// File: app/src/main/java/com/example/kropimagecropper/ui/screens/PreviewScanScreen.kt

package com.example.kropimagecropper.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.kropimagecropper.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScanScreen(
    scanPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file = File(scanPath)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Get string resources in composable context
    val scanNotFoundText = stringResource(R.string.scan_not_found)
    val scanDeletedText = stringResource(R.string.scan_deleted)
    val deleteFailedText = stringResource(R.string.delete_failed)

    // Check if file exists
    if (!file.exists()) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, scanNotFoundText, Toast.LENGTH_SHORT).show()
            onBack()
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.preview_scan)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_scan)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Image
            val painter = rememberAsyncImagePainter(
                model = file,
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image)
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit
            )

            // Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${file.length() / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_scan)) },
                text = { Text(stringResource(R.string.delete_scan_confirmation)) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (file.delete()) {
                                Toast.makeText(context, scanDeletedText, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, deleteFailedText, Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                            onBack()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}