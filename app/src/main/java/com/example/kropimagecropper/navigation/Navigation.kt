package com.example.kropimagecropper.navigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.kropimagecropper.data.ScanManager
import com.example.kropimagecropper.ui.screens.MyScansScreen
import com.example.kropimagecropper.ui.screens.ScannerScreen
import com.example.kropimagecropper.ui.screens.PreviewScanScreen
import com.example.kropimagecropper.ui.screens.PerspectiveCorrectionScreen
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun Navigation(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "my_scans"
    ) {
        composable("my_scans") {
            MyScansScreen(
                onScan = { navController.navigate("scanner") },
                onOpenScan = { scanPath ->
                    val encodedPath = URLEncoder.encode(scanPath, StandardCharsets.UTF_8.toString())
                    navController.navigate("preview/$encodedPath")
                }
            )
        }

        composable("scanner") {
            ScannerScreen(
                onDone = {
                    navController.popBackStack("my_scans", inclusive = false)
                },
//                onNavigateToPerspective = { bitmap ->
//                    // Save bitmap to temporary file and navigate to perspective correction
//                    scope.launch {
//                        val tempFile = File.createTempFile("temp_perspective", ".jpg", context.cacheDir)
//                        val outputStream = FileOutputStream(tempFile)
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
//                        outputStream.close()
//
//                        val encodedPath = URLEncoder.encode(tempFile.absolutePath, StandardCharsets.UTF_8.toString())
//                        navController.navigate("perspective/$encodedPath")
//                    }
//                }
            )
        }

        composable("preview/{scanPath}") { backStackEntry ->
            val encodedScanPath = backStackEntry.arguments?.getString("scanPath")
            val scanPath = if (encodedScanPath != null) {
                java.net.URLDecoder.decode(encodedScanPath, StandardCharsets.UTF_8.toString())
            } else {
                ""
            }

            PreviewScanScreen(
                scanPath = scanPath,
                onBack = { navController.popBackStack() }
            )
        }

        composable("perspective/{imagePath}") { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            val bitmap = remember {
                BitmapFactory.decodeFile(imagePath)
            }

            PerspectiveCorrectionScreen(
                originalBitmap = bitmap,
                onCorrected = { correctedBitmap ->
                    scope.launch {
                        // Save corrected bitmap and navigate back
                        val savedPath = ScanManager.saveCorrectedImage(context, correctedBitmap)
                        navController.popBackStack()
                        val encodedPath = URLEncoder.encode(savedPath, StandardCharsets.UTF_8.toString())
                        navController.navigate("preview/$encodedPath")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}