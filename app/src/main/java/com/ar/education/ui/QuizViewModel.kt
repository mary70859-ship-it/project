package com.ar.education.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ar.education.data.Quiz
import com.ar.education.progress.ProgressRepository
import kotlinx.coroutines.launch

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = ProgressRepository.getInstance(application)

    private val _quizResult = MutableLiveData<QuizResult>()
    val quizResult: LiveData<QuizResult> = _quizResult

    private val userAnswers = mutableMapOf<Int, String>()

    fun submitAnswer(questionIndex: Int, selectedOption: String) {
        userAnswers[questionIndex] = selectedOption
    }

    fun submitQuiz(quiz: Quiz, lessonId: String) {
        val questions = quiz.questions
        var correct = 0
        for ((idx, answer) in userAnswers) {
            if (idx < questions.size && questions[idx].correctAnswer == answer) {
                correct++
            }
        }
        val total = questions.size
        val percentage = if (total > 0) (correct * 100) / total else 0
        viewModelScope.launch {
            progressRepository.saveQuizResult(lessonId, percentage, "user_id_placeholder")
        }
        _quizResult.value = QuizResult(correct, total, percentage, quiz.passingScore)
    }
}

data class QuizResult(val correct: Int, val total: Int, val score: Int, val passingScore: Int) {
    val passed get() = score >= passingScore
}

class QuizViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
