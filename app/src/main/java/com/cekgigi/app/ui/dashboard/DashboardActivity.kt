package com.cekgigi.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cekgigi.app.R
import com.cekgigi.app.data.AppDatabase
import com.cekgigi.app.databinding.ActivityDashboardBinding
import com.cekgigi.app.ui.camera.CameraActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var riwayatAdapter: RiwayatAdapter
    private lateinit var database: AppDatabase

    private val viewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(database.screeningDao()) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupUI()
        observeViewModel()
        viewModel.loadDashboardData()
    }

    private fun setupUI() {
        // Setup RecyclerView
        riwayatAdapter = RiwayatAdapter(emptyList())
        binding.recyclerRiwayat.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = riwayatAdapter
        }

        // Tombol Ambil Foto
        binding.cardAmbilFoto.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // Bottom Navigation Placeholder
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    Toast.makeText(this, "Fitur Riwayat Lengkap akan segera hadir!", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_tips -> {
                    Toast.makeText(this, "Kumpulan Tips akan segera hadir!", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardUiState.Loading -> {
                        // Show progress if needed
                    }
                    is DashboardUiState.Success -> {
                        updateDashboardUI(state)
                    }
                    is DashboardUiState.Error -> {
                        Toast.makeText(this@DashboardActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateDashboardUI(state: DashboardUiState.Success) {
        val terakhir = state.riwayatTerakhir
        if (terakhir != null) {
            binding.layoutRiwayatTerakhir.visibility = View.VISIBLE
            binding.txtEmptyState.visibility = View.GONE
            binding.txtTanggalTerakhir.text = terakhir.tanggal
            binding.txtStatusTerakhir.text = if (terakhir.statusGigi == "Sehat") {
                "Gigi Bersih & Sehat"
            } else {
                terakhir.kelasDeteksi
            }
            binding.txtStatusTerakhir.setTextColor(
                if (terakhir.statusGigi == "Sehat") {
                    getColor(R.color.success_green)
                } else {
                    getColor(R.color.warning_amber)
                }
            )
        } else {
            binding.layoutRiwayatTerakhir.visibility = View.GONE
            binding.txtEmptyState.visibility = View.VISIBLE
        }

        riwayatAdapter.updateData(state.semuaRiwayat)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
    }
}
