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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ScanManager {

    private const val SCANS_FOLDER = "DocumentScans"
    private const val TEMP_FOLDER = "temp"

    /**
     * Get the directory where scans are stored
     */
    fun getScansDirectory(context: Context): File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use app-specific external directory for Android 10+
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), SCANS_FOLDER)
        } else {
            // Use external storage for older versions
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SCANS_FOLDER)
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    /**
     * Get the temporary directory for camera captures
     */
    private fun getTempDirectory(context: Context): File {
        val directory = File(context.cacheDir, TEMP_FOLDER)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Create a temporary URI for camera capture - FIXED VERSION
     */
    fun createTempImageUri(context: Context): Uri? {
        return try {
            // Use external files directory instead of cache for better camera compatibility
            val tempDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TEMP_FOLDER)

            // Ensure temp directory exists and is writable
            if (!tempDir.exists()) {
                val created = tempDir.mkdirs()
                if (!created) {
                    println("DEBUG: Failed to create temp directory")
                    return null
                }
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File(tempDir, "TEMP_$timeStamp.jpg")

            // Don't pre-create the file - let the camera app create it
            // This avoids permission and corruption issues

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            // Debug logging
            println("DEBUG: Created temp URI: $uri")
            println("DEBUG: File path: ${imageFile.absolutePath}")
            println("DEBUG: Temp directory: ${tempDir.absolutePath}")
            println("DEBUG: Temp directory exists: ${tempDir.exists()}")
            println("DEBUG: Temp directory can write: ${tempDir.canWrite()}")

            uri
        } catch (e: Exception) {
            println("DEBUG: Error creating temp URI: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Save the cropped image to the scans directory
     */
    suspend fun saveCroppedImage(context: Context, croppedUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                val inputStream = context.contentResolver.openInputStream(croppedUri)
                    ?: return@withContext false

                // Decode the bitmap
                val options = BitmapFactory.Options()
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                if (bitmap == null) return@withContext false

                val scansDir = getScansDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SCAN_$timeStamp.jpg"
                val scanFile = File(scansDir, fileName)

                val outputStream = FileOutputStream(scanFile)
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()

                if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Add to MediaStore for visibility in gallery
                    addToMediaStore(context, scanFile)
                }

                success
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                // Always recycle the bitmap to free memory
                bitmap?.recycle()
            }
        }
    }

    /**
     * Add saved scan to MediaStore (Android 10+)
     */
    private fun addToMediaStore(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SCANS_FOLDER")
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }

                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load all scans from the scans directory
     */
    suspend fun loadScans(scansDir: File, onLoaded: (List<File>) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val scanFiles = if (scansDir.exists()) {
                    scansDir.listFiles { file ->
                        file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
                    }?.sortedByDescending { it.lastModified() } ?: emptyList()
                } else {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    onLoaded(scanFiles)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onLoaded(emptyList())
                }
            }
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
                    // Only delete files older than 1 hour
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
     * Check if a URI points to a valid image file - FIXED VERSION
     */
    fun isValidImageUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                options.outWidth > 0 && options.outHeight > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy URI content to a temporary file for better compatibility
     */
    fun copyUriToTempFile(context: Context, uri: Uri): Uri? {
        var bitmap: Bitmap? = null
        return try {
            val tempDir = getTempDirectory(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFile = File(tempDir, "COPY_$timeStamp.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Decode the bitmap with options to prevent memory issues
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Reduce memory usage by sampling
                }
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (bitmap != null) {
                FileOutputStream(tempFile).use { outputStream ->
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error copying URI to temp file: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            // Always recycle the bitmap to free memory
            bitmap?.recycle()
        }
    }

    suspend fun saveCorrectedImage(context: Context, bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val scansDir = getScansDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SCAN_$timeStamp.jpg"
                val scanFile = File(scansDir, fileName)

                FileOutputStream(scanFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addToMediaStore(context, scanFile)
                }

                scanFile.absolutePath
            } finally {
                // Recycle the bitmap if we're done with it
                // Note: Only recycle if this is the last use of this bitmap
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }
}