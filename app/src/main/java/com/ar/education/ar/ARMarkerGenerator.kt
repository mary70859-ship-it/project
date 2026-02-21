package com.ar.education.ar

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ar.education.data.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs

class ARMarkerGenerator(private val context: Context) {

    companion object {
        private const val MARKER_SIZE = 1024
        private const val MARKER_BORDER_SIZE = 64
        private const val MARKER_DATA_SIZE = 24
        private const val CELL_SIZE = (MARKER_SIZE - 2 * MARKER_BORDER_SIZE) / MARKER_DATA_SIZE
    }

    data class MarkerResult(
        val success: Boolean,
        val filePath: String? = null,
        val message: String = ""
    )

    suspend fun generateMarker(lesson: Lesson): MarkerResult = withContext(Dispatchers.Default) {
        try {
            val bitmap = createMarkerBitmap(lesson.id, lesson.title)
            val fileName = "marker_${lesson.id}.png"
            val result = saveBitmapToStorage(bitmap, fileName)
            
            if (result.success) {
                MarkerResult(true, result.filePath, "Marker generated successfully")
            } else {
                MarkerResult(false, null, result.message)
            }
        } catch (e: Exception) {
            MarkerResult(false, null, "Failed to generate marker: ${e.message}")
        }
    }

    suspend fun generateAllMarkers(lessons: List<Lesson>): List<MarkerResult> = withContext(Dispatchers.Default) {
        lessons.map { lesson -> generateMarker(lesson) }
    }

    private fun createMarkerBitmap(lessonId: String, title: String): Bitmap {
        val bitmap = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val borderPaint = android.graphics.Paint().apply {
            color = Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, MARKER_SIZE.toFloat(), MARKER_BORDER_SIZE.toFloat(), borderPaint)
        canvas.drawRect(0f, (MARKER_SIZE - MARKER_BORDER_SIZE).toFloat(), MARKER_SIZE.toFloat(), MARKER_SIZE.toFloat(), borderPaint)
        canvas.drawRect(0f, 0f, MARKER_BORDER_SIZE.toFloat(), MARKER_SIZE.toFloat(), borderPaint)
        canvas.drawRect((MARKER_SIZE - MARKER_BORDER_SIZE).toFloat(), 0f, MARKER_SIZE.toFloat(), MARKER_SIZE.toFloat(), borderPaint)

        val dataPattern = generateDataPattern(lessonId)
        drawPattern(canvas, dataPattern)

        drawLessonInfo(canvas, lessonId, title)

        return bitmap
    }

    private fun generateDataPattern(lessonId: String): Array<IntArray> {
        val pattern = Array(MARKER_DATA_SIZE) { IntArray(MARKER_DATA_SIZE) }

        val hash1 = generateHash(lessonId, 0)
        val hash2 = generateHash(lessonId, 1)
        val hash3 = generateHash(lessonId, 2)

        for (y in 0 until MARKER_DATA_SIZE) {
            for (x in 0 until MARKER_DATA_SIZE) {
                if (isBorderCell(x, y)) {
                    pattern[y][x] = if ((x + y) % 2 == 0) 1 else 0
                } else if (isTimingCell(x, y)) {
                    pattern[y][x] = (x + y) % 2
                } else {
                    val hash = when {
                        x < MARKER_DATA_SIZE / 3 -> hash1
                        x < 2 * MARKER_DATA_SIZE / 3 -> hash2
                        else -> hash3
                    }
                    val index = (abs(hash) + x + y) % 2
                    pattern[y][x] = index
                }
            }
        }

        return pattern
    }

    private fun generateHash(input: String, seed: Int): Int {
        var hash = seed
        for (char in input) {
            hash = hash * 31 + char.code
        }
        return hash
    }

    private fun isBorderCell(x: Int, y: Int): Boolean {
        return x < 3 || x >= MARKER_DATA_SIZE - 3 || y < 3 || y >= MARKER_DATA_SIZE - 3
    }

    private fun isTimingCell(x: Int, y: Int): Boolean {
        return (x == 6 || x == MARKER_DATA_SIZE - 7 || y == 6 || y == MARKER_DATA_SIZE - 7)
    }

    private fun drawPattern(canvas: android.graphics.Canvas, pattern: Array<IntArray>) {
        val blackPaint = android.graphics.Paint().apply {
            color = Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
        val whitePaint = android.graphics.Paint().apply {
            color = Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        for (y in 0 until MARKER_DATA_SIZE) {
            for (x in 0 until MARKER_DATA_SIZE) {
                val paint = if (pattern[y][x] == 1) blackPaint else whitePaint
                val left = (MARKER_BORDER_SIZE + x * CELL_SIZE).toFloat()
                val top = (MARKER_BORDER_SIZE + y * CELL_SIZE).toFloat()
                canvas.drawRect(left, top, left + CELL_SIZE, top + CELL_SIZE, paint)
            }
        }
    }

    private fun drawLessonInfo(canvas: android.graphics.Canvas, lessonId: String, title: String) {
        val textPaint = android.graphics.Paint().apply {
            color = Color.BLACK
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        val displayId = lessonId.replace("_", " ").uppercase()
        canvas.drawText(displayId, MARKER_SIZE / 2f, MARKER_SIZE - 20f, textPaint)
    }

    private suspend fun saveBitmapToStorage(bitmap: Bitmap, fileName: String): MarkerResult = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ARMarkers")
                    put(MediaStore.Images.Media.DESCRIPTION, "AR Marker for ${fileName.removeSuffix(".png")}")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    MarkerResult(true, uri.toString(), "Marker saved to gallery")
                } ?: MarkerResult(false, null, "Failed to create file in gallery")
            } else {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "ARMarkers"
                )
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(directory, fileName)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                MarkerResult(true, file.absolutePath, "Marker saved to gallery")
            }
        } catch (e: Exception) {
            MarkerResult(false, null, "Error saving marker: ${e.message}")
        }
    }

    fun getMarkerFile(lessonId: String): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, use MediaStore to find the marker
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("marker_$lessonId.png", "${Environment.DIRECTORY_PICTURES}/ARMarkers/")

            val projection = arrayOf(MediaStore.MediaColumns._ID)
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    try {
                        // Create a temp file from the URI
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            val fileDescriptor = pfd.fileDescriptor
                            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                            // Create a temp file to return
                            val tempFile = File(context.cacheDir, "temp_marker_$lessonId.png")
                            tempFile.outputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            return tempFile
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                null
            }
        } else {
            // On Android 9 and below, use file-based approach
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ARMarkers"
            )
            val file = File(directory, "marker_$lessonId.png")
            return if (file.exists()) file else null
        }
        return null
    }

    fun getAllMarkerFiles(): List<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, use MediaStore to find all markers
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("${Environment.DIRECTORY_PICTURES}/ARMarkers/")

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            )

            val markerFiles = mutableListOf<File>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val fileName = cursor.getString(nameColumn)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            val fileDescriptor = pfd.fileDescriptor
                            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                            // Create a temp file for each marker
                            val tempFile = File(context.cacheDir, "temp_$fileName")
                            tempFile.outputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            markerFiles.add(tempFile)
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be read
                    }
                }
            }
            markerFiles
        } else {
            // On Android 9 and below, use file-based approach
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ARMarkers"
            )
            if (directory.exists()) {
                directory.listFiles { file -> file.extension == "png" }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    fun getMarkerBitmap(lessonId: String): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, use MediaStore to find the marker
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("marker_$lessonId.png", "${Environment.DIRECTORY_PICTURES}/ARMarkers/")

            val projection = arrayOf(MediaStore.MediaColumns._ID)
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            return BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                null
            }
        } else {
            // On Android 9 and below, use file-based approach
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ARMarkers"
            )
            val file = File(directory, "marker_$lessonId.png")
            return if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }
        return null
    }
}
