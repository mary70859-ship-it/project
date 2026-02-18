package com.ar.education.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ar.education.data.Lesson
import com.ar.education.data.LessonRepository
import kotlinx.coroutines.launch

class LessonDetailViewModel(application: Application, private val lessonId: String) : AndroidViewModel(application) {

    private val lessonRepository = LessonRepository(application)

    private val _lesson = MutableLiveData<Lesson?>()
    val lesson: LiveData<Lesson?> = _lesson

    init {
        loadLesson()
    }

    private fun loadLesson() {
        viewModelScope.launch {
            _lesson.value = lessonRepository.getLesson(lessonId).getOrNull()
        }
    }
}

class LessonDetailViewModelFactory(private val application: Application, private val lessonId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LessonDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LessonDetailViewModel(application, lessonId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}