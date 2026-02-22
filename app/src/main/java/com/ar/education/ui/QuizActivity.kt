package com.ar.education.ui

import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ar.education.data.Quiz
import com.ar.education.databinding.ActivityQuizBinding

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private lateinit var viewModel: QuizViewModel
    private var quizData: Quiz? = null
    private var currentQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupOnBackPressedHandler()

        quizData = intent.getParcelableExtra(EXTRA_QUIZ_DATA, Quiz::class.java)

        val factory = QuizViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[QuizViewModel::class.java]

        setupViews()
        observeViewModel()

        if (quizData != null) {
            displayQuestion(0)
        } else {
            Toast.makeText(this, "Quiz data not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViews() {
        binding.btnNext.setOnClickListener { nextQuestion() }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupOnBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun observeViewModel() {
        viewModel.quizResult.observe(this) { result ->
            Toast.makeText(this, "Quiz submitted! Your score: ${result.score}%", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun displayQuestion(index: Int) {
        val question = quizData?.questions?.get(index) ?: return

        binding.tvQuestion.text = question.question
        binding.radioGroupOptions.removeAllViews()

        question.options.forEachIndexed { i, option ->
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.id = i
            binding.radioGroupOptions.addView(radioButton)
        }
    }

    private fun nextQuestion() {
        val selectedOptionId = binding.radioGroupOptions.checkedRadioButtonId
        if (selectedOptionId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedText = binding.radioGroupOptions.findViewById<RadioButton>(selectedOptionId)?.text?.toString() ?: return
        viewModel.submitAnswer(currentQuestionIndex, selectedText)

        currentQuestionIndex++
        if (currentQuestionIndex < quizData!!.questions.size) {
            displayQuestion(currentQuestionIndex)
        } else {
            quizData?.let { viewModel.submitQuiz(it, intent.getStringExtra(EXTRA_LESSON_ID) ?: "") }
        }
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
        const val EXTRA_QUIZ_DATA = "extra_quiz_data"
    }
}
