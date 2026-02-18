package com.ar.education.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.education.data.LessonProgress
import kotlinx.coroutines.launch

class ProgressViewModel(private val progressRepository: ProgressRepository) : ViewModel() {

    private val _userProgress = MutableLiveData<List<LessonProgress>>()
    val userProgress: LiveData<List<LessonProgress>> = _userProgress

    private val _completedCount = MutableLiveData<Int>(0)
    val completedCount: LiveData<Int> = _completedCount

    private val _averageScore = MutableLiveData<Float>(0f)
    val averageScore: LiveData<Float> = _averageScore

    private val _bookmarkedCount = MutableLiveData<Int>(0)
    val bookmarkedCount: LiveData<Int> = _bookmarkedCount

    fun loadUserProgress(userId: String) {
        viewModelScope.launch {
            progressRepository.getAllProgress(userId).collect { progressList ->
                _userProgress.value = progressList
                _completedCount.value = progressList.count { it.isCompleted }
                _bookmarkedCount.value = progressList.count { it.bookmarked }
                val scores = progressList.filter { it.quizScore > 0 }.map { it.quizScore }
                _averageScore.value = if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            }
        }
    }

    fun getProgressForLesson(lessonId: String): LiveData<LessonProgress?> {
        val progressLiveData = MutableLiveData<LessonProgress?>()
        viewModelScope.launch {
            progressLiveData.value = progressRepository.getLessonProgress(lessonId)
        }
        return progressLiveData
    }
}

class ProgressViewModelFactory(private val progressRepository: ProgressRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(progressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
