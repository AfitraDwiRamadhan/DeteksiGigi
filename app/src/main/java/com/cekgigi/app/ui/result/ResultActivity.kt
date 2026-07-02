package com.cekgigi.app.ui.result

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cekgigi.app.databinding.ActivityResultBinding
import com.cekgigi.app.ml.DentalClassifier
import androidx.lifecycle.lifecycleScope
import com.cekgigi.app.R
import com.cekgigi.app.data.AppDatabase
import com.cekgigi.app.data.ScreeningEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var dentalClassifier: DentalClassifier
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        // Tampilkan loading saat inisialisasi model
        binding.tvResultStatus.text = getString(R.string.skrining_sekarang)
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dentalClassifier = DentalClassifier(this@ResultActivity)
                }
                
                // 1. Tangkap path foto dari CameraActivity
                val photoPath = intent.getStringExtra("photo_path")

                if (photoPath != null) {
                    val mainBitmap = BitmapFactory.decodeFile(photoPath)
                    binding.ivResultPreview.setImageBitmap(mainBitmap)

                    // 2. Jalankan proses diagnosis
                    prosesDiagnosa(photoPath)
                } else {
                    binding.tvResultStatus.text = getString(R.string.belum_ada_riwayat)
                }
            } catch (e: Exception) {
                binding.tvResultStatus.text = "Error"
                binding.tvResultDesc.text = e.message ?: "Gagal memuat model"
            }
        }

        binding.btnBackToHome.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dentalClassifier.isInitialized) {
            dentalClassifier.close()
        }
    }

    private fun prosesDiagnosa(path: String) {
        lifecycleScope.launch {
            val file = File(path)
            if (!file.exists()) return@launch

            binding.tvResultStatus.text = "Analisis..."

            val hasil = withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    dentalClassifier.classifyImage(bitmap)
                } else {
                    null
                }
            }

            if (hasil == null) {
                binding.tvResultStatus.text = "Gagal"
                return@launch
            }

            // ✅ JIKA BUKAN GIGI (Non_Teeth)
            if (hasil.isNonTeeth) {
                binding.tvResultStatus.text = "Bukan Gigi"
                binding.tvResultDesc.text = "AI mendeteksi bahwa ini bukan foto gigi yang valid."
                binding.tvResultAccuracy.text = String.format(Locale.getDefault(), "Keyakinan: %.1f%%", hasil.akurasi)
                binding.viewStatusIndicator.setBackgroundColor(getColor(R.color.text_muted))
                return@launch
            }

            // ✅ PENYARINGAN OOD: Jika akurasi sangat rendah (di bawah 15%)
            if (hasil.akurasi < 15.0f) {
                binding.tvResultStatus.text = "Tidak Dikenali"
                binding.tvResultDesc.text = "Sistem kurang yakin dengan gambar ini. Pastikan foto gigi terlihat jelas."
                binding.tvResultAccuracy.text = String.format(Locale.getDefault(), "Keyakinan: %.1f%%", hasil.akurasi)
                binding.viewStatusIndicator.setBackgroundColor(getColor(R.color.text_muted))
                return@launch
            }

            // 3. TAMPILKAN HASIL KE LAYOUT
            binding.tvResultStatus.text = hasil.namaKelas
            binding.tvResultDesc.text = hasil.deskripsi
            binding.tvResultAccuracy.text = String.format(Locale.getDefault(), "Keyakinan AI: %.1f%%", hasil.akurasi)

            if (hasil.isSehat) {
                binding.viewStatusIndicator.setBackgroundColor(getColor(R.color.success_green))
            } else {
                binding.viewStatusIndicator.setBackgroundColor(getColor(R.color.warning_amber))
            }

            // Simpan ke database
            withContext(Dispatchers.IO) {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val currentDate = sdf.format(Date())
                
                val entity = ScreeningEntity(
                    tanggal = currentDate,
                    statusGigi = if (hasil.isSehat) "Sehat" else "Bermasalah",
                    kelasDeteksi = hasil.kelasDeteksi,
                    akurasi = hasil.akurasi,
                    deskripsi = hasil.deskripsi,
                    pathFoto = path
                )
                database.screeningDao().tambahRiwayat(entity)
            }
        }
    }
}
