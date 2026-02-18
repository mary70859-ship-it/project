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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val lessonRepository = LessonRepository(application)

    private val _lessons = MutableLiveData<List<Lesson>>()
    val lessons: LiveData<List<Lesson>> = _lessons

    private val _filteredLessons = MutableLiveData<List<Lesson>>()
    val filteredLessons: LiveData<List<Lesson>> = _filteredLessons

    private val _selectedSubject = MutableLiveData<String>("all")
    val selectedSubject: LiveData<String> = _selectedSubject

    fun loadLessons() {
        viewModelScope.launch {
            lessonRepository.getAllLessons().onSuccess { 
                _lessons.value = it
                filterBySubject(_selectedSubject.value ?: "all")
            }
        }
    }

    fun filterBySubject(subject: String) {
        _selectedSubject.value = subject
        val all = _lessons.value ?: return
        _filteredLessons.value = if (subject == "all") all 
                                 else all.filter { it.subject.equals(subject, ignoreCase = true) }
    }

    fun searchLessons(query: String) {
        val all = _lessons.value ?: return
        _filteredLessons.value = if (query.isEmpty()) all
            else all.filter { it.title.contains(query, ignoreCase = true) || 
                              it.description.contains(query, ignoreCase = true) }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
