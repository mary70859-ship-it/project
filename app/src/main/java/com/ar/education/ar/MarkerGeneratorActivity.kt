package com.ar.education.ar

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ar.education.R
import com.ar.education.data.Lesson
import com.ar.education.data.LessonRepository
import com.ar.education.databinding.ActivityMarkerGeneratorBinding
import kotlinx.coroutines.launch

class MarkerGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarkerGeneratorBinding
    private lateinit var markerGenerator: ARMarkerGenerator
    private lateinit var lessonRepository: LessonRepository
    private var lessons: List<Lesson> = emptyList()
    private var selectedLesson: Lesson? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkerGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        markerGenerator = ARMarkerGenerator(this)
        lessonRepository = LessonRepository(this)

        setupToolbar()
        setupViews()
        loadLessons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        binding.spinnerLessons.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < lessons.size) {
                    selectedLesson = lessons[position]
                    updateMarkerPreview()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLesson = null
            }
        }

        binding.btnGenerate.setOnClickListener {
            selectedLesson?.let { lesson ->
                generateMarker(lesson)
            } ?: Toast.makeText(this, R.string.select_lesson_first, Toast.LENGTH_SHORT).show()
        }

        binding.btnGenerateAll.setOnClickListener {
            generateAllMarkers()
        }

        binding.btnViewAll.setOnClickListener {
            viewAllMarkers()
        }
    }

    private fun loadLessons() {
        lifecycleScope.launch {
            val result = lessonRepository.getAllLessons()
            result.onSuccess { lessonList ->
                lessons = lessonList
                setupLessonSpinner()
            }.onFailure { error ->
                Toast.makeText(this@MarkerGeneratorActivity, 
                    "Failed to load lessons: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLessonSpinner() {
        val lessonTitles = lessons.map { "${it.subject.uppercase()} - ${it.title}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lessonTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLessons.adapter = adapter

        if (lessons.isNotEmpty()) {
            selectedLesson = lessons[0]
            updateMarkerPreview()
        }
    }

    private fun updateMarkerPreview() {
        selectedLesson?.let { lesson ->
            binding.tvMarkerTitle.text = lesson.title
            val existingFile = markerGenerator.getMarkerFile(lesson.id)
            if (existingFile != null) {
                loadMarkerBitmap(existingFile.absolutePath)
            } else {
                binding.ivMarkerPreview.setImageResource(R.drawable.ic_marker_placeholder)
            }
        }
    }

    private fun loadMarkerBitmap(filePath: String) {
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(filePath)
                binding.ivMarkerPreview.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            binding.ivMarkerPreview.setImageResource(R.drawable.ic_marker_placeholder)
        }
    }

    private fun generateMarker(lesson: Lesson) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false

        lifecycleScope.launch {
            val result = markerGenerator.generateMarker(lesson)
            
            binding.progressBar.visibility = View.GONE
            binding.btnGenerate.isEnabled = true

            if (result.success) {
                Toast.makeText(this@MarkerGeneratorActivity, 
                    "Marker saved: ${result.filePath}", 
                    Toast.LENGTH_LONG).show()
                updateMarkerPreview()
            } else {
                Toast.makeText(this@MarkerGeneratorActivity, 
                    result.message, 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateAllMarkers() {
        if (lessons.isEmpty()) {
            Toast.makeText(this, R.string.no_lessons_available, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerateAll.isEnabled = false

        lifecycleScope.launch {
            val results = markerGenerator.generateAllMarkers(lessons)
            
            binding.progressBar.visibility = View.GONE
            binding.btnGenerateAll.isEnabled = true

            val successCount = results.count { it.success }
            Toast.makeText(this@MarkerGeneratorActivity,
                "Generated $successCount of ${lessons.size} markers",
                Toast.LENGTH_LONG).show()

            if (successCount > 0) {
                updateMarkerPreview()
            }
        }
    }

    private fun viewAllMarkers() {
        val markerFiles = markerGenerator.getAllMarkerFiles()
        if (markerFiles.isEmpty()) {
            Toast.makeText(this, R.string.no_markers_generated, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MarkerGalleryActivity::class.java)
        startActivity(intent)
    }
}
