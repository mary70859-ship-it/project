package com.ar.education.progress

import android.content.Context
import com.ar.education.data.LessonProgress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository private constructor(context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val progressDao = database.lessonProgressDao()
    private val gson = Gson()
    
    companion object {
        @Volatile private var INSTANCE: ProgressRepository? = null
        
        fun getInstance(context: Context): ProgressRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProgressRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
    
    suspend fun getLessonProgress(lessonId: String): LessonProgress? {
        val entity = progressDao.getProgress(lessonId)
        return entity?.toLessonProgress()
    }
    
    fun getAllProgress(userId: String): Flow<List<LessonProgress>> {
        return progressDao.getAllProgress(userId).map { entities ->
            entities.map { it.toLessonProgress() }
        }
    }
    
    fun getCompletedLessons(userId: String): Flow<List<LessonProgress>> {
        return progressDao.getCompletedLessons(userId).map { entities ->
            entities.map { it.toLessonProgress() }
        }
    }
    
    fun getBookmarkedLessons(): Flow<List<LessonProgress>> {
        return progressDao.getBookmarkedLessons().map { entities ->
            entities.map { it.toLessonProgress() }
        }
    }
    
    suspend fun saveProgress(progress: LessonProgress) {
        progressDao.insertProgress(progress.toEntity())
    }
    
    suspend fun markStepCompleted(lessonId: String, stepNumber: Int, userId: String) {
        val currentProgress = getLessonProgress(lessonId)
        val completedSteps = currentProgress?.completedSteps?.toMutableList() ?: mutableListOf()
        
        if (!completedSteps.contains(stepNumber)) {
            completedSteps.add(stepNumber)
            
            val newProgress = LessonProgress(
                lessonId = lessonId,
                userId = userId,
                completedSteps = completedSteps,
                quizScore = currentProgress?.quizScore ?: 0,
                quizAttempts = currentProgress?.quizAttempts ?: 0,
                isCompleted = currentProgress?.isCompleted ?: false,
                lastAccessed = System.currentTimeMillis(),
                timeSpent = currentProgress?.timeSpent ?: 0,
                bookmarked = currentProgress?.bookmarked ?: false
            )
            saveProgress(newProgress)
        }
    }
    
    suspend fun markQuizAsCompleted(lessonId: String, correctAnswers: Int, totalQuestions: Int) {
        val score = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        saveQuizResult(lessonId, score, "user_id_placeholder")
    }
    
    suspend fun saveQuizResult(lessonId: String, score: Int, userId: String) {
        val currentProgress = getLessonProgress(lessonId)
        val newAttempts = (currentProgress?.quizAttempts ?: 0) + 1
        
        val newProgress = LessonProgress(
            lessonId = lessonId,
            userId = userId,
            completedSteps = currentProgress?.completedSteps ?: emptyList(),
            quizScore = score,
            quizAttempts = newAttempts,
            isCompleted = score >= 70,
            lastAccessed = System.currentTimeMillis(),
            timeSpent = currentProgress?.timeSpent ?: 0,
            bookmarked = currentProgress?.bookmarked ?: false
        )
        saveProgress(newProgress)
    }
    
    suspend fun toggleBookmark(lessonId: String, userId: String) {
        val currentProgress = getLessonProgress(lessonId)
        
        val newProgress = LessonProgress(
            lessonId = lessonId,
            userId = userId,
            completedSteps = currentProgress?.completedSteps ?: emptyList(),
            quizScore = currentProgress?.quizScore ?: 0,
            quizAttempts = currentProgress?.quizAttempts ?: 0,
            isCompleted = currentProgress?.isCompleted ?: false,
            lastAccessed = System.currentTimeMillis(),
            timeSpent = currentProgress?.timeSpent ?: 0,
            bookmarked = !(currentProgress?.bookmarked ?: false)
        )
        saveProgress(newProgress)
    }
    
    fun getCompletedLessonCount(userId: String): Flow<Int> {
        return progressDao.getCompletedLessonCount(userId)
    }
    
    fun getAverageQuizScore(userId: String): Flow<Float?> {
        return progressDao.getAverageQuizScore(userId)
    }
    
    suspend fun resetProgress(lessonId: String) {
        progressDao.deleteProgressById(lessonId)
    }
    
    private fun LessonProgressEntity.toLessonProgress(): LessonProgress {
        val completedStepsList: List<Int> = try {
            gson.fromJson(completedSteps, object : TypeToken<List<Int>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
        
        return LessonProgress(
            lessonId = lessonId,
            userId = userId,
            completedSteps = completedStepsList,
            quizScore = quizScore,
            quizAttempts = quizAttempts,
            isCompleted = isCompleted,
            lastAccessed = lastAccessed,
            timeSpent = timeSpent,
            bookmarked = bookmarked
        )
    }
    
    private fun LessonProgress.toEntity(): LessonProgressEntity {
        return LessonProgressEntity(
            lessonId = lessonId,
            userId = userId,
            completedSteps = gson.toJson(completedSteps),
            quizScore = quizScore,
            quizAttempts = quizAttempts,
            isCompleted = isCompleted,
            lastAccessed = lastAccessed,
            timeSpent = timeSpent,
            bookmarked = bookmarked
        )
    }
}
