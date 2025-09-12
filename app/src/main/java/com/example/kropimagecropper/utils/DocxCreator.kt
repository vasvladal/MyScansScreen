// File: app/src/main/java/com/example/kropimagecropper/utils/DocxCreator.kt

package com.example.kropimagecropper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DocxCreator {

    fun createDocx(
        context: Context,
        imageFiles: List<File>,
        onResult: (success: Boolean, path: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a new DOCX document
                val document = XWPFDocument()

                for (imageFile in imageFiles) {
                    // Add a paragraph for spacing
                    document.createParagraph().createRun().addBreak()

                    // Add image to document
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val run = document.createParagraph().createRun()

                        // Convert bitmap to byte array
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        val imageData = stream.toByteArray()

                        // Add image to document
                        run.addPicture(
                            java.io.ByteArrayInputStream(imageData),
                            XWPFDocument.PICTURE_TYPE_JPEG,
                            imageFile.name,
                            Units.toEMU(bitmap.width.toDouble()),
                            Units.toEMU(bitmap.height.toDouble())
                        )

                        bitmap.recycle()
                    }

                    // Add caption with file name
                    val caption = document.createParagraph()
                    caption.alignment = ParagraphAlignment.CENTER
                    val captionRun = caption.createRun()
                    captionRun.setText(imageFile.nameWithoutExtension)  // Fixed: use setText()
                    captionRun.setFontSize(10)  // Fixed: use setFontSize()
                    captionRun.setItalic(true)  // Fixed: use setItalic()
                }

                // Save DOCX file
                val docxDir = getDocxDirectory(context)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val docxFile = File(docxDir, "SCAN_$timeStamp.docx")

                val outputStream = FileOutputStream(docxFile)
                document.write(outputStream)
                outputStream.close()
                document.close()

                withContext(Dispatchers.Main) {
                    onResult(true, docxFile.absolutePath)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                    Toast.makeText(context, "Failed to export DOCX", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getDocxDirectory(context: Context): File {
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