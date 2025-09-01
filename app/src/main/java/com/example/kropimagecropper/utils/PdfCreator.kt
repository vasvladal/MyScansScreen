// File: app/src/main/java/com/example/kropimagecropper/utils/PdfCreator.kt

package com.example.kropimagecropper.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfCreator {

    fun createPdf(
        context: Context,
        imageFiles: List<File>,
        onResult: (success: Boolean, path: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdfDocument = PdfDocument()
                var pageNumber = 1

                for (imageFile in imageFiles) {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        // Create page info
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            bitmap.width,
                            bitmap.height,
                            pageNumber
                        ).create()

                        // Start page
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas

                        // Draw bitmap on canvas
                        canvas.drawBitmap(bitmap, 0f, 0f, null)

                        // Finish page
                        pdfDocument.finishPage(page)
                        bitmap.recycle()
                        pageNumber++
                    }
                }

                // Save PDF to public Documents directory
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SCAN_$timeStamp.pdf"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/DocumentScans")
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        pdfDocument.close()

                        withContext(Dispatchers.Main) {
                            onResult(true, uri.toString())
                        }
                    } else {
                        pdfDocument.close()
                        withContext(Dispatchers.Main) {
                            onResult(false, null)
                        }
                    }
                } else {
                    // Use direct file access for older Android versions
                    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val pdfDir = File(documentsDir, "DocumentScans")

                    if (!pdfDir.exists()) {
                        pdfDir.mkdirs()
                    }

                    val pdfFile = File(pdfDir, fileName)
                    val outputStream = FileOutputStream(pdfFile)
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    pdfDocument.close()

                    withContext(Dispatchers.Main) {
                        onResult(true, pdfFile.absolutePath)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }
}