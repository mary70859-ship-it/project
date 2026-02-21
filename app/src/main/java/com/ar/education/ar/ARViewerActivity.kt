package com.ar.education.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ar.education.R
import com.ar.education.data.*
import com.ar.education.databinding.ActivityArViewerBinding
import com.ar.education.progress.ProgressRepository
import com.ar.education.ui.QuizActivity
import com.google.ar.core.*
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
    private var isMarkerMode = false
    private var augmentedImageDatabase: AugmentedImageDatabase? = null
    private val trackedImages = mutableMapOf<String, AugmentedImage>()
    private var markerGenerator: ARMarkerGenerator? = null
    
    // Camera permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            setupAR()
        } else {
            Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityArViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        arSceneView = binding.arSceneView

        markerGenerator = ARMarkerGenerator(this)

        // Check if we're in marker mode (no lesson ID provided)
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID)
        isMarkerMode = lessonId == null

        setupViews()
        setupViewModel()
        
        // Check for camera permissions before setting up AR
        checkCameraPermission()
        
        if (isMarkerMode) {
            setupMarkerMode()
        } else {
            loadLessonData()
        }
        
        checkArCoreAvailability()
    }

    private fun setupMarkerMode() {
        // Show scan overlay in marker mode
        binding.scanOverlay.visibility = View.VISIBLE
        binding.bottomCard.visibility = View.GONE
        binding.topControls.visibility = View.GONE
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        } else {
            // Camera permission already granted, set up AR
            setupAR()
        }
    }

    private fun configureAugmentedImages(session: Session) {
        try {
            val database = AugmentedImageDatabase(session)
            val markerFiles = markerGenerator?.getAllMarkerFiles() ?: emptyList()
            
            for (file in markerFiles) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        // Extract lesson ID from filename (marker_lessonId.png)
                        val lessonId = file.nameWithoutExtension.replace("marker_", "")
                        database.addImage(lessonId, bitmap, 0.1f) // 10cm estimated width
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    // Skip problematic marker files
                }
            }
            
            augmentedImageDatabase = database
            
            // Configure the session with augmented image database
            val config = Config(session)
            config.augmentedImageDatabase = database
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            session.configure(config)
            
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Failed to configure markers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
        // In marker mode, we don't have a lesson yet, so skip setup
        if (isMarkerMode) return
        
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: return
        val progressRepository = ProgressRepository.getInstance(this)
        val factory = ARViewerViewModelFactory(application, lessonId, progressRepository)
        viewModel = ViewModelProvider(this, factory)[ARViewerViewModel::class.java]

        setupViewModelObservers()
    }

    private fun setupAR() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (modelNode == null && !isMarkerMode) {
                    val hitResult = arSceneView.hitTest(e.x, e.y)
                    hitResult?.let {
                        val session = arSceneView.arSession
                        val anchor = session?.createAnchor(it.hitPose)
                        loadModel(anchor)
                    }
                }
                return true
            }
        })

        arSceneView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // Set up AR frame update listener for marker tracking
        if (isMarkerMode) {
            arSceneView.onSessionUpdated = { session: Session, frame: Frame ->
                // Configure augmented images when session is first available
                if (augmentedImageDatabase == null) {
                    configureAugmentedImages(session)
                }
                updateAugmentedImages(session, frame)
            }
        }
    }

    private fun updateAugmentedImages(session: Session, frame: Frame) {
        val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        
        for (augmentedImage in updatedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> {
                    val imageIndex = augmentedImage.index
                    // Get the name we assigned to the image in the database
                    val name = augmentedImageDatabase?.let { db ->
                        // The name is associated with the image index
                        getImageNameByIndex(imageIndex)
                    }
                    
                    if (name != null && !trackedImages.containsKey(name)) {
                        trackedImages[name] = augmentedImage
                        runOnUiThread {
                            onMarkerDetected(name)
                        }
                    }
                }
                TrackingState.STOPPED -> {
                    // Find and remove the stopped image
                    val keysToRemove = mutableListOf<String>()
                    for ((key, value) in trackedImages) {
                        if (value == augmentedImage) {
                            keysToRemove.add(key)
                        }
                    }
                    for (key in keysToRemove) {
                        trackedImages.remove(key)
                    }
                }
                TrackingState.PAUSED -> {
                    // Handle paused state if needed
                }
            }
        }
    }

    private fun getImageNameByIndex(index: Int): String? {
        // We need to map index back to the lesson ID
        // Since we can't easily get this from the database, we'll use a different approach
        // by iterating through all markers
        val markerFiles = markerGenerator?.getAllMarkerFiles() ?: return null
        if (index >= 0 && index < markerFiles.size) {
            return markerFiles[index].nameWithoutExtension.replace("marker_", "")
        }
        return null
    }

    private fun onMarkerDetected(lessonId: String) {
        // Transition from marker mode to lesson mode
        isMarkerMode = false
        
        // Hide scan overlay
        binding.scanOverlay.visibility = View.GONE
        binding.bottomCard.visibility = View.VISIBLE
        binding.topControls.visibility = View.VISIBLE
        
        // Set the lesson ID in intent and load the lesson
        intent.putExtra(EXTRA_LESSON_ID, lessonId)
        
        // Initialize the view model for this lesson
        val progressRepository = ProgressRepository.getInstance(this)
        val factory = ARViewerViewModelFactory(application, lessonId, progressRepository)
        viewModel = ViewModelProvider(this, factory)[ARViewerViewModel::class.java]
        
        setupViewModelObservers()
        loadLessonData()
        
        Toast.makeText(this, "Marker detected! Loading lesson: $lessonId", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupViewModelObservers() {
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
