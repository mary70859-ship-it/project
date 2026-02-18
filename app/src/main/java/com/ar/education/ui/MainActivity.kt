package com.ar.education.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ar.education.R
import com.ar.education.data.Lesson
import com.ar.education.databinding.ActivityMainBinding
import com.ar.education.ui.LessonAdapter
import com.ar.education.ui.MainViewModel
import com.ar.education.ui.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application)
    }
    private lateinit var lessonAdapter: LessonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilterButtons()
        observeViewModel()

        viewModel.loadLessons()
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(this::onLessonClicked)
        binding.recyclerViewLessons.apply {
            adapter = lessonAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupFilterButtons() {
        binding.btnAllSubjects.setOnClickListener { viewModel.filterBySubject("all") }
        binding.btnPhysics.setOnClickListener { viewModel.filterBySubject("physics") }
        binding.btnBiology.setOnClickListener { viewModel.filterBySubject("biology") }
        binding.btnChemistry.setOnClickListener { viewModel.filterBySubject("chemistry") }
        binding.btnProgress.setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }
    }

    private fun observeViewModel() {
        viewModel.filteredLessons.observe(this) { lessons ->
            lessonAdapter.submitList(lessons)
        }
    }

    private fun onLessonClicked(lesson: Lesson) {
        val intent = Intent(this, LessonDetailActivity::class.java)
        intent.putExtra(LessonDetailActivity.EXTRA_LESSON_ID, lesson.id)
        startActivity(intent)
    }
}
