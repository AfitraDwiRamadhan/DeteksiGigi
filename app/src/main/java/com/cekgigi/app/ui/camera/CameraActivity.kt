package com.cekgigi.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cekgigi.app.databinding.ActivityCameraBinding
import com.cekgigi.app.ui.result.ResultActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    // 🔥 TAMBAHAN PERLUASAN OPSI 1: Melacak tahapan foto
    private var photoPath: String? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                // Simpan Uri ke file sementara agar bisa diproses DentalClassifier
                val tempFile = copyUriToTempFile(imageUri)
                if (tempFile != null) {
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra("photo_path", tempFile.absolutePath)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Gagal memproses gambar dari galeri", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Atur instruksi awal pada text view panduan di layout kamu
        // ⚠️ Pastikan di activity_camera.xml kamu ada TextView untuk panduan, misal namanya tvInstructions
        updateInstructionText()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        binding.btnFlipCamera.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.btnFlash.setOnClickListener {
            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    Toast.makeText(this, "Flash: HIDUP", Toast.LENGTH_SHORT).show()
                    ImageCapture.FLASH_MODE_ON
                }
                ImageCapture.FLASH_MODE_ON -> {
                    Toast.makeText(this, "Flash: OTOMATIS", Toast.LENGTH_SHORT).show()
                    ImageCapture.FLASH_MODE_AUTO
                }
                else -> {
                    Toast.makeText(this, "Flash: MATI", Toast.LENGTH_SHORT).show()
                    ImageCapture.FLASH_MODE_OFF
                }
            }
            imageCapture?.flashMode = flashMode
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    // 🔥 Fungsi untuk memperbarui teks panduan sudut foto agar user tidak bingung
    private fun updateInstructionText() {
        val text = "Posisikan gigi di dalam kotak dan ambil foto"
        // Menggunakan try-catch aman jika id TextView kamu berbeda di XML
        try {
            // Sesuai dengan id textview panduan di XML kamu (ganti jika namanya berbeda)
            binding.tvInstructions.text = text
        } catch (e: Exception) {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }

            val rotation = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
            } catch (e: Exception) {
                Surface.ROTATION_0
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(rotation)
                .setFlashMode(flashMode)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoDir = File(filesDir, "dental_photos")
        if (!photoDir.exists()) photoDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val photoFile = File(photoDir, "gigi_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Simpan path foto saat ini
                    photoPath = photoFile.absolutePath

                    // ✅ JIKA SUDAH FOTO: Lempar path foto ke ResultActivity
                    val intent = Intent(this@CameraActivity, ResultActivity::class.java)
                    intent.putExtra("photo_path", photoPath)
                    startActivity(intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Gagal: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val photoDir = File(filesDir, "dental_photos")
            if (!photoDir.exists()) photoDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
            val tempFile = File(photoDir, "gallery_$timestamp.jpg")

            tempFile.outputStream().use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}