package com.example.kropimagecropper.utils

import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted

object PermissionUtils {
    fun getMediaPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun handleCameraPermission(
        cameraPermission: PermissionState,
        onPermissionGranted: () -> Unit
    ) {
        if (cameraPermission.status.isGranted) {
            onPermissionGranted()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun handleMediaPermissions(
        mediaPermissionsState: MultiplePermissionsState,
        onPermissionGranted: () -> Unit
    ) {
        if (mediaPermissionsState.allPermissionsGranted) {
            onPermissionGranted()
        } else {
            mediaPermissionsState.launchMultiplePermissionRequest()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun requestWritePermission(writePermissionState: PermissionState) {
        writePermissionState.launchPermissionRequest()
    }
}