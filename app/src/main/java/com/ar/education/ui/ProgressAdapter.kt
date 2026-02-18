package com.ar.education.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ar.education.data.LessonProgress
import com.ar.education.databinding.ItemProgressBinding

class ProgressAdapter(private val onItemClicked: (LessonProgress) -> Unit) :
    ListAdapter<LessonProgress, ProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val progress = getItem(position)
        holder.bind(progress)
        holder.itemView.setOnClickListener { onItemClicked(progress) }
    }

    class ProgressViewHolder(private val binding: ItemProgressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(progress: LessonProgress) {
            binding.tvLessonTitle.text = progress.lessonId
            binding.tvProgress.text = "${progress.completedSteps.size} steps"
            binding.tvQuizScore.text = if (progress.quizScore > 0) "Quiz: ${progress.quizScore}%" else "Not taken"
            binding.tvStatus.text = if (progress.isCompleted) "Completed" else "In Progress"
        }
    }
}

class ProgressDiffCallback : DiffUtil.ItemCallback<LessonProgress>() {
    override fun areItemsTheSame(old: LessonProgress, new: LessonProgress) = old.lessonId == new.lessonId
    override fun areContentsTheSame(old: LessonProgress, new: LessonProgress) = old == new
}
