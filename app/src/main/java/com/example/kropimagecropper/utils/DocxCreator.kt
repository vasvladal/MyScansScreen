// File: app/src/main/java/com/example/kropimagecropper/utils/DocxCreator.kt

package com.example.kropimagecropper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * DocxCreator - Creates DOCX documents with images
 *
 * This version is specifically designed to work with R8/ProGuard in release builds
 * by avoiding reflection and using direct API calls only.
 */
object DocxCreator {

    private const val TAG = "DocxCreator"
    private const val JPEG_QUALITY = 85
    private const val MAX_IMAGE_DIMENSION = 800 // pixels

    /**
     * Creates a DOCX document with the provided images
     */
    fun createDocx(
        context: Context,
        imageFiles: List<File>,
        onResult: (success: Boolean, path: String?) -> Unit
    ) {
        // Use IO dispatcher for file operations
        CoroutineScope(Dispatchers.IO).launch {

            var xwpfDocument: XWPFDocument? = null
            var fileOutputStream: FileOutputStream? = null

            try {
                Log.d(TAG, "Starting DOCX creation with ${imageFiles.size} images")

                // Initialize DOCX document
                xwpfDocument = XWPFDocument()

                // Add document title
                addDocumentTitle(xwpfDocument)

                // Process each image
                var processedCount = 0
                for ((index, imageFile) in imageFiles.withIndex()) {

                    if (!imageFile.exists() || !imageFile.canRead()) {
                        Log.w(TAG, "Skipping unreadable file: ${imageFile.name}")
                        continue
                    }

                    try {
                        Log.d(TAG, "Processing image ${index + 1}/${imageFiles.size}: ${imageFile.name}")

                        // Add spacing between images (except first)
                        if (processedCount > 0) {
                            addSpacing(xwpfDocument)
                        }

                        // Process and add image
                        val success = addImageToDocument(xwpfDocument, imageFile)
                        if (success) {
                            processedCount++
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image ${imageFile.name}", e)
                        // Continue with next image instead of failing completely
                    }
                }

                if (processedCount == 0) {
                    throw RuntimeException("No images could be processed")
                }

                // Save document
                val outputFile = createOutputFile(context)
                fileOutputStream = FileOutputStream(outputFile)
                xwpfDocument.write(fileOutputStream)
                fileOutputStream.flush()

                Log.d(TAG, "DOCX created successfully: ${outputFile.absolutePath}")

                // Return success on main thread
                withContext(Dispatchers.Main) {
                    onResult(true, outputFile.absolutePath)
                    Toast.makeText(
                        context,
                        "DOCX exported successfully ($processedCount images)",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create DOCX", e)

                withContext(Dispatchers.Main) {
                    onResult(false, null)
                    Toast.makeText(
                        context,
                        "Failed to create DOCX: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                // Clean up resources
                try {
                    fileOutputStream?.close()
                    xwpfDocument?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing resources", e)
                }
            }
        }
    }

    /**
     * Adds a title to the document
     */
    private fun addDocumentTitle(document: XWPFDocument) {
        val titleParagraph = document.createParagraph()
        titleParagraph.alignment = ParagraphAlignment.CENTER

        val titleRun = titleParagraph.createRun()
        titleRun.setText("Document Scan")
        titleRun.isBold = true
        titleRun.setFontSize(16)

        // Add timestamp
        val timestampParagraph = document.createParagraph()
        timestampParagraph.alignment = ParagraphAlignment.CENTER

        val timestampRun = timestampParagraph.createRun()
        val timestamp = SimpleDateFormat("MMMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(Date())
        timestampRun.setText(timestamp)
        timestampRun.setFontSize(12)
        timestampRun.setItalic(true)
    }

    /**
     * Adds spacing between elements
     */
    private fun addSpacing(document: XWPFDocument) {
        val spacingParagraph = document.createParagraph()
        spacingParagraph.createRun().addBreak()
    }

    /**
     * Adds an image to the document
     */
    private fun addImageToDocument(document: XWPFDocument, imageFile: File): Boolean {
        return try {
            Log.d(TAG, "Adding image: ${imageFile.name}")

            // Load and resize bitmap
            val bitmap = loadOptimizedBitmap(imageFile) ?: return false

            // Convert to JPEG byte array
            val imageBytes = bitmapToJpegBytes(bitmap) ?: run {
                bitmap.recycle()
                return false
            }

            // Create paragraph for image
            val imageParagraph = document.createParagraph()
            imageParagraph.alignment = ParagraphAlignment.CENTER

            val imageRun = imageParagraph.createRun()

            // Calculate dimensions in EMU (English Metric Units)
            val widthEMU = Units.toEMU(bitmap.width.coerceAtMost(MAX_IMAGE_DIMENSION).toDouble())
            val heightEMU = Units.toEMU(bitmap.height.coerceAtMost(MAX_IMAGE_DIMENSION).toDouble())

            // Add picture to document
            imageRun.addPicture(
                ByteArrayInputStream(imageBytes),
                XWPFDocument.PICTURE_TYPE_JPEG,
                imageFile.nameWithoutExtension,
                widthEMU,
                heightEMU
            )

            // Add image caption
            addImageCaption(document, imageFile.nameWithoutExtension)

            bitmap.recycle()
            Log.d(TAG, "Successfully added image: ${imageFile.name}")

            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add image: ${imageFile.name}", e)
            false
        }
    }

    /**
     * Loads and optimizes bitmap to prevent OutOfMemoryError
     */
    private fun loadOptimizedBitmap(imageFile: File): Bitmap? {
        return try {
            // First pass: get dimensions without loading the image
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions for: ${imageFile.name}")
                return null
            }

            // Calculate sample size to reduce memory usage
            val sampleSize = calculateSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                MAX_IMAGE_DIMENSION
            )

            // Second pass: load the scaled image
            val loadOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, loadOptions)

            if (bitmap != null) {
                Log.d(TAG, "Loaded bitmap: ${bitmap.width}x${bitmap.height} (sample size: $sampleSize)")
            }

            bitmap

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError loading: ${imageFile.name}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap: ${imageFile.name}", e)
            null
        }
    }

    /**
     * Calculates appropriate sample size for bitmap loading
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1

        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2

            while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        return sampleSize
    }

    /**
     * Converts bitmap to JPEG byte array
     */
    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            if (compressed) {
                outputStream.toByteArray()
            } else {
                Log.e(TAG, "Failed to compress bitmap to JPEG")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to bytes", e)
            null
        }
    }

    /**
     * Adds caption below image
     */
    private fun addImageCaption(document: XWPFDocument, caption: String) {
        try {
            val captionParagraph = document.createParagraph()
            captionParagraph.alignment = ParagraphAlignment.CENTER

            val captionRun = captionParagraph.createRun()
            captionRun.setText(caption)
            captionRun.setFontSize(10)
            captionRun.setItalic(true)
            captionRun.setColor("666666") // Gray color

        } catch (e: Exception) {
            Log.e(TAG, "Error adding caption: $caption", e)
        }
    }

    /**
     * Creates output file with timestamp
     */
    private fun createOutputFile(context: Context): File {
        val outputDir = getDocxDirectory(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(outputDir, "DocumentScan_$timestamp.docx")
    }

    /**
     * Gets or creates the directory for DOCX files
     */
    fun getDocxDirectory(context: Context): File {
        val documentsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - use app-specific directory
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        } else {
            // Android 9 and below - use public documents directory
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        }

        val scanDirectory = File(documentsDir, "DocumentScans")

        if (!scanDirectory.exists()) {
            val created = scanDirectory.mkdirs()
            Log.d(TAG, "Created directory: $created - ${scanDirectory.absolutePath}")
        }

        return scanDirectory
    }
}