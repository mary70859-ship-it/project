package com.ar.education.ar

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ar.education.R
import com.ar.education.data.*
import com.ar.education.databinding.ActivityArViewerBinding
import com.ar.education.progress.ProgressRepository
import com.ar.education.ui.QuizActivity
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.ar.ArSceneView
import kotlinx.coroutines.launch

class ARViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArViewerBinding
    private lateinit var arSceneView: ArSceneView
    private lateinit var viewModel: ARViewerViewModel
    private var currentLesson: Lesson? = null
    private var currentStepIndex = 0
    private var modelNode: ArModelNode? = null
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityArViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        arSceneView = binding.arSceneView

        setupViews()
        setupViewModel()
        setupAR()
        loadLessonData()
        checkArCoreAvailability()
    }

    private fun setupViews() {
        binding.apply {
            btnPrevious.setOnClickListener { previousStep() }
            btnNext.setOnClickListener { nextStep() }
            btnTakeQuiz.setOnClickListener { startQuiz() }
            btnHome.setOnClickListener { finish() }
            btnBookmark.setOnClickListener { toggleBookmark() }
        }
    }

    private fun setupViewModel() {
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: return
        val progressRepository = ProgressRepository.getInstance(this)
        val factory = ARViewerViewModelFactory(application, lessonId, progressRepository)
        viewModel = ViewModelProvider(this, factory)[ARViewerViewModel::class.java]

        viewModel.currentLesson.observe(this) { lesson ->
            currentLesson = lesson
            updateUIForCurrentStep()
            if (modelNode == null) {
                loadModel()
            }
        }

        viewModel.currentStep.observe(this) { stepIndex ->
            currentStepIndex = stepIndex
            updateUIForCurrentStep()
        }

        viewModel.progress.observe(this) { progress ->
            binding.btnBookmark.isSelected = progress?.bookmarked ?: false
        }
    }

    private fun setupAR() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (modelNode == null) {
                    val hitResult = arSceneView.hitTest(e.x, e.y)
                    hitResult?.let { loadModel(it.anchor) }
                }
                return true
            }
        })

        arSceneView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun loadLessonData() {
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID)
        if (lessonId != null) {
            viewModel.loadLesson()
        }
    }

    private fun checkArCoreAvailability() {
        val lessonRepo = LessonRepository(this)
        if (!lessonRepo.isARCoreSupported()) {
            binding.arSceneView.visibility = View.GONE
            binding.fallbackMessage.visibility = View.VISIBLE
        }
    }

    private fun loadModel(anchor: Anchor? = null) {
        val lesson = currentLesson ?: return

        modelNode?.anchor = anchor

        if (modelNode == null) {
            lifecycleScope.launch {
                try {
                    val newModelNode = ArModelNode(arSceneView.engine, PlacementMode.INSTANT).apply {
                        loadModelGlb(
                            context = this@ARViewerActivity,
                            glbFileLocation = lesson.modelPath
                        )
                        anchor?.let { this.anchor = it }
                    }
                    modelNode = newModelNode
                    arSceneView.addChild(newModelNode)

                    val currentStep = currentLesson?.labSteps?.get(currentStepIndex)
                    currentStep?.modelHighlighting?.let {
                        applyModelHighlighting(it)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ARViewerActivity, "Failed to load model: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyModelHighlighting(highlighting: ModelHighlighting) {
        val step = currentLesson?.labSteps?.get(currentStepIndex)
        Toast.makeText(this, step?.title ?: "", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIForCurrentStep() {
        val lesson = currentLesson ?: return

        binding.apply {
            tvLessonTitle.text = lesson.title
            tvStepInfo.text = "Step ${currentStepIndex + 1} of ${lesson.labSteps.size}"

            if (currentStepIndex < lesson.labSteps.size) {
                val step = lesson.labSteps[currentStepIndex]
                tvStepTitle.text = step.title
                tvStepInstruction.text = step.instruction
                tvExpectedOutcome.text = step.expectedOutcome ?: ""
            }

            btnPrevious.isEnabled = currentStepIndex > 0
            btnNext.isEnabled = currentStepIndex < lesson.labSteps.size - 1

            btnTakeQuiz.visibility = if (currentStepIndex == lesson.labSteps.size - 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun previousStep() {
        if (currentStepIndex > 0) {
            viewModel.previousStep()
        }
    }

    private fun nextStep() {
        val lesson = currentLesson ?: return
        if (currentStepIndex < lesson.labSteps.size - 1) {
            viewModel.markStepCompleted(currentStepIndex + 1, "user_id_placeholder")
            viewModel.nextStep()
        }
    }

    private fun startQuiz() {
        val lesson = currentLesson ?: return
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra(QuizActivity.EXTRA_LESSON_ID, lesson.id)
        intent.putExtra(QuizActivity.EXTRA_QUIZ_DATA, lesson.quiz)
        startActivity(intent)
    }

    private fun toggleBookmark() {
        viewModel.toggleBookmark("user_id_placeholder")
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
        const val EXTRA_QUIZ_DATA = "extra_quiz_data"
    }
}
