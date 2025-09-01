// File: app/src/main/java/com/example/kropimagecropper/navigation/Navigation.kt

package com.example.kropimagecropper.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.kropimagecropper.ui.screens.MyScansScreen
import com.example.kropimagecropper.ui.screens.ScannerScreen
import com.example.kropimagecropper.ui.screens.PreviewScanScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun Navigation(navController: NavHostController) {
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
            ScannerScreen(onDone = {
                navController.popBackStack("my_scans", inclusive = false)
            })
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
    }
}