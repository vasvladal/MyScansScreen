// File: app/src/main/java/com/example/kropimagecropper/utils/PdfCreator.kt

package com.example.kropimagecropper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.widget.Toast
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

                // Save PDF
                val pdfDir = getPdfDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(pdfDir, "SCAN_$timeStamp.pdf")

                val outputStream = FileOutputStream(pdfFile)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    onResult(true, pdfFile.absolutePath)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                    Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getPdfDirectory(context: Context): File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "DocumentScans")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DocumentScans")
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }
}