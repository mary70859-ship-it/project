package com.ar.education.ar

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ar.education.R
import com.ar.education.databinding.ActivityMarkerGalleryBinding
import java.io.File

class MarkerGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarkerGalleryBinding
    private lateinit var markerGenerator: ARMarkerGenerator
    private lateinit var adapter: MarkerAdapter
    private var markerFiles: MutableList<File> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkerGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        markerGenerator = ARMarkerGenerator(this)

        setupToolbar()
        setupRecyclerView()
        loadMarkers()
    }

    override fun onResume() {
        super.onResume()
        loadMarkers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = MarkerAdapter(
            markerFiles = markerFiles,
            onShareClick = { file -> shareMarker(file) },
            onDeleteClick = { file -> deleteMarker(file) }
        )

        binding.rvMarkers.layoutManager = GridLayoutManager(this, 2)
        binding.rvMarkers.adapter = adapter
    }

    private fun loadMarkers() {
        markerFiles.clear()
        markerFiles.addAll(markerGenerator.getAllMarkerFiles())

        adapter.notifyDataSetChanged()

        binding.tvEmpty.visibility = if (markerFiles.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMarkers.visibility = if (markerFiles.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun shareMarker(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_marker)))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share marker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteMarker(file: File) {
        if (file.delete()) {
            Toast.makeText(this, R.string.marker_deleted, Toast.LENGTH_SHORT).show()
            loadMarkers()
        } else {
            Toast.makeText(this, "Failed to delete marker", Toast.LENGTH_SHORT).show()
        }
    }
}
