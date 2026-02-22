package com.ar.education.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ar.education.ar.ARViewerActivity
import com.ar.education.data.Lesson
import com.ar.education.databinding.ActivityLessonDetailBinding
import com.ar.education.databinding.ItemLabStepBinding
import com.ar.education.databinding.ItemQuizQuestionPreviewBinding

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonDetailBinding
    private val viewModel: LessonDetailViewModel by viewModels {
        LessonDetailViewModelFactory(application, intent.getStringExtra(EXTRA_LESSON_ID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupOnBackPressedHandler()

        viewModel.lesson.observe(this) { lesson ->
            lesson?.let { displayLesson(it) }
        }
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

    private fun displayLesson(lesson: Lesson) {
        binding.tvLessonTitle.text = lesson.title
        binding.tvLessonDescription.text = lesson.description

        binding.btnViewInAr.setOnClickListener {
            val intent = Intent(this, ARViewerActivity::class.java)
            intent.putExtra(ARViewerActivity.EXTRA_LESSON_ID, lesson.id)
            startActivity(intent)
        }

        binding.btnTakeQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra(QuizActivity.EXTRA_LESSON_ID, lesson.id)
            intent.putExtra(QuizActivity.EXTRA_QUIZ_DATA, lesson.quiz)
            startActivity(intent)
        }

        // Dynamically add lab steps
        binding.llLabSteps.removeAllViews()
        for (step in lesson.labSteps) {
            val stepBinding = ItemLabStepBinding.inflate(LayoutInflater.from(this), binding.llLabSteps, false)
            stepBinding.tvStepTitle.text = step.title
            stepBinding.tvStepInstruction.text = step.instruction
            binding.llLabSteps.addView(stepBinding.root)
        }

        // Dynamically add quiz questions
        binding.llQuizQuestions.removeAllViews()
        for (question in lesson.quiz.questions) {
            val questionBinding = ItemQuizQuestionPreviewBinding.inflate(LayoutInflater.from(this), binding.llQuizQuestions, false)
            questionBinding.tvQuestion.text = question.question
            binding.llQuizQuestions.addView(questionBinding.root)
        }
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
    }
}