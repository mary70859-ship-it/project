package com.ar.education.ar

import android.app.Application
import androidx.lifecycle.*
import com.ar.education.data.Lesson
import com.ar.education.data.LessonRepository
import com.ar.education.progress.ProgressRepository
import com.ar.education.data.LessonProgress
import kotlinx.coroutines.launch

class ARViewerViewModel(application: Application, private val lessonId: String, private val progressRepository: ProgressRepository) : AndroidViewModel(application) {

    private val _currentLesson = MutableLiveData<Lesson>()
    val currentLesson: LiveData<Lesson> = _currentLesson

    private val _currentStep = MutableLiveData<Int>()
    val currentStep: LiveData<Int> = _currentStep

    private val _progress = MutableLiveData<LessonProgress?>()
    val progress: LiveData<LessonProgress?> = _progress

    private val lessonRepository = LessonRepository(application)

    init {
        loadLesson()
        loadProgress()
    }

    fun loadLesson() {
        viewModelScope.launch {
            lessonRepository.getLesson(lessonId).onSuccess {
                _currentLesson.value = it
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            _progress.value = progressRepository.getLessonProgress(lessonId)
        }
    }

    fun nextStep() {
        _currentStep.value = (_currentStep.value ?: 0) + 1
    }

    fun previousStep() {
        _currentStep.value = (_currentStep.value ?: 0) - 1
    }

    fun markStepCompleted(stepNumber: Int, userId: String) {
        viewModelScope.launch {
            progressRepository.markStepCompleted(lessonId, stepNumber, userId)
            loadProgress() // Refresh progress
        }
    }

    fun toggleBookmark(userId: String) {
        viewModelScope.launch {
            progressRepository.toggleBookmark(lessonId, userId)
            loadProgress()
        }
    }
}

class ARViewerViewModelFactory(private val application: Application, private val lessonId: String, private val progressRepository: ProgressRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ARViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ARViewerViewModel(application, lessonId, progressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
