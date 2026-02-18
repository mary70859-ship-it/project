package com.ar.education.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.ar.core.ArCoreApk

class LessonRepository(private val context: Context) {
    private val gson = Gson()
    private val lessonsCache = mutableListOf<Lesson>()

    private val lessonFiles = listOf(
        "lessons/physics_001.json",
        "lessons/physics_002.json",
        "lessons/biology_001.json",
        "lessons/biology_002.json",
        "lessons/chemistry_001.json",
        "lessons/chemistry_002.json"
    )

    suspend fun getAllLessons(): Result<List<Lesson>> = withContext(Dispatchers.IO) {
        try {
            if (lessonsCache.isEmpty()) loadFromAssets()
            Result.success(lessonsCache.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLessonById(lessonId: String): Result<Lesson?> = withContext(Dispatchers.IO) {
        try {
            if (lessonsCache.isEmpty()) loadFromAssets()
            Result.success(lessonsCache.find { it.id == lessonId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLesson(lessonId: String): Result<Lesson?> = getLessonById(lessonId)

    suspend fun getLessonsBySubject(subject: String): Result<List<Lesson>> = withContext(Dispatchers.IO) {
        try {
            if (lessonsCache.isEmpty()) loadFromAssets()
            Result.success(lessonsCache.filter { 
                it.subject.equals(subject, ignoreCase = true) 
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isARCoreSupported(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (e: Exception) {
            false
        }
    }

    fun hasSufficientHardware(): Boolean {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val maxMemory = Runtime.getRuntime().maxMemory()
        return availableProcessors >= 2 && maxMemory > 32 * 1024 * 1024
    }

    private fun loadFromAssets() {
        lessonsCache.clear()
        for (fileName in lessonFiles) {
            try {
                val json = context.assets.open(fileName).bufferedReader().readText()
                val lesson = gson.fromJson(json, Lesson::class.java)
                if (lesson != null) lessonsCache.add(lesson)
            } catch (e: Exception) {
                // Skip missing files gracefully
            }
        }
    }
}
