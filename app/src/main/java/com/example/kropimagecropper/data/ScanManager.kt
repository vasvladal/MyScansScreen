// File: app/src/main/java/com/example/kropimagecropper/data/ScanManager.kt

package com.example.kropimagecropper.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ScanManager {

    private const val SCANS_FOLDER = "DocumentScans"
    private const val TEMP_FOLDER = "temp"

    /**
     * Get the directory where scans are stored (public directory)
     */
    fun getScansDirectory(context: Context): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SCANS_FOLDER
        )
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    /**
     * Get the temporary directory for camera captures
     */
    private fun getTempDirectory(context: Context): File {
        val directory = File(context.cacheDir, TEMP_FOLDER)
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    /**
     * Create a temporary URI for camera capture
     */
    fun createTempImageUri(context: Context): Uri? {
        return try {
            val tempDir = getTempDirectory(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File(tempDir, "TEMP_$timeStamp.jpg")

            imageFile.parentFile?.mkdirs()
            if (!imageFile.exists()) imageFile.createNewFile()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save the cropped image to the public scans directory
     */
    suspend fun saveCroppedImage(context: Context, bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val scansDir = getScansDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SCAN_$timeStamp.jpg"
                val scanFile = File(scansDir, fileName)

                FileOutputStream(scanFile).use { out ->
                    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    if (success) addToMediaStore(context, scanFile)
                    success
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Add saved scan to MediaStore
     */
    private fun addToMediaStore(context: Context, file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/$SCANS_FOLDER"
                    )
                } else {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load all scans from the scans directory (suspend version)
     */
    suspend fun loadScans(scansDir: File): List<File> = withContext(Dispatchers.IO) {
        if (scansDir.exists()) {
            scansDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDir = getTempDirectory(context)
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("TEMP_")) {
                    if (System.currentTimeMillis() - file.lastModified() > 3600000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if a URI points to a valid image file
     */
    fun isValidImageUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream) != null
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
