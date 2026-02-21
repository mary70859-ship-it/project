package com.ar.education.ar

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ar.education.databinding.ItemMarkerBinding
import java.io.File

class MarkerAdapter(
    private val markerFiles: List<File>,
    private val onShareClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val binding = ItemMarkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MarkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        holder.bind(markerFiles[position])
    }

    override fun getItemCount(): Int = markerFiles.size

    inner class MarkerViewHolder(
        private val binding: ItemMarkerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            binding.ivMarker.setImageBitmap(bitmap)

            val lessonTitle = file.name
                .replace("marker_", "")
                .replace(".png", "")
                .replace("_", " ")
                .uppercase()

            binding.tvLessonTitle.text = lessonTitle

            binding.btnShare.setOnClickListener {
                onShareClick(file)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(file)
            }
        }
    }
}
