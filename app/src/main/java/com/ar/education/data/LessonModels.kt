package com.ar.education.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Lesson(
    val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val difficulty: String,
    val estimatedDuration: Int,
    val markerId: String = "",
    val modelPath: String = "",
    val labSteps: List<LabStep> = emptyList(),
    val quiz: Quiz,
    val prerequisites: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    var bookmarked: Boolean = false,
    var completed: Boolean = false
) : Parcelable

@Parcelize
data class LabStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val modelHighlighting: ModelHighlighting? = null,
    val requiresInteraction: Boolean = false,
    val interactionType: String? = null,
    val expectedOutcome: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null
) : Parcelable

@Parcelize
data class ModelHighlighting(
    val objectId: String,
    val color: String,
    val highlightDuration: Int = 3000
) : Parcelable

@Parcelize
data class Quiz(
    val id: String,
    val title: String,
    val questions: List<QuizQuestion> = emptyList(),
    val passingScore: Int = 70,
    val timeLimit: Int = 0
) : Parcelable

@Parcelize
data class QuizQuestion(
    val id: String,
    val question: String,
    @SerializedName("questionType")
    val questionType: String = "multiple_choice",
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String? = null,
    val imageUrl: String? = null
) : Parcelable

data class LessonProgress(
    val lessonId: String,
    val userId: String,
    val completedSteps: List<Int> = emptyList(),
    val quizScore: Int = 0,
    val quizAttempts: Int = 0,
    val isCompleted: Boolean = false,
    val lastAccessed: Long = 0,
    val timeSpent: Long = 0,
    val bookmarked: Boolean = false
)
