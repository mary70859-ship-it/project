package com.ar.education.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ar.education.databinding.ActivityProgressBinding
import com.ar.education.progress.ProgressRepository
import com.ar.education.progress.ProgressViewModel
import com.ar.education.progress.ProgressViewModelFactory

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private lateinit var viewModel: ProgressViewModel
    private lateinit var adapter: ProgressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val progressRepository = ProgressRepository.getInstance(this)
        val factory = ProgressViewModelFactory(progressRepository)
        viewModel = ViewModelProvider(this, factory)[ProgressViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        viewModel.loadUserProgress("user_id_placeholder")
    }

    private fun setupRecyclerView() {
        adapter = ProgressAdapter { progress ->
            // Handle progress item click
        }
        binding.rvProgress.adapter = adapter
        binding.rvProgress.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.userProgress.observe(this) { progressList ->
            adapter.submitList(progressList)
        }
        viewModel.completedCount.observe(this) { 
            binding.tvCompletedLessons.text = it.toString() 
        }
        viewModel.averageScore.observe(this) { 
            binding.tvAverageScore.text = if (it > 0f) "${it.toInt()}%" else "N/A"
        }
        viewModel.bookmarkedCount.observe(this) { 
            binding.tvBookmarked.text = it.toString() 
        }
    }
}
