package com.cekgigi.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DentalClassifier(context: Context) {

    private var interpreter: Interpreter? = null

    /**
     * URUTAN KELAS SESUAI TRAINING (ALFABETIS):
     * 0: Calculus
     * 1: Caries
     * 2: Discoloration
     * 3: Gingivitis
     * 4: Healthy_Teeth
     * 5: Hypodontia
     * 6: Non_Teeth
     */
    private val classLabels = arrayOf(
        "Calculus",
        "Caries",
        "Discoloration",
        "Gingivitis",
        "Healthy_Teeth",
        "Hypodontia",
        "Non_Teeth"
    )

    data class ClassificationResult(
        val isSehat: Boolean,
        val isNonTeeth: Boolean,
        val kelasDeteksi: String,
        val namaKelas: String,
        val deskripsi: String,
        val akurasi: Float
    )

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "model_cek_gigi.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    fun classifyImage(bitmap: Bitmap): ClassificationResult {
        // 1. AUTO-CROP ROI: Meniru pipeline CV2 (auto_crop_teeth_cv2)
        // Mencari area paling terang (gigi) dan memotong background yang tidak perlu.
        val roiBitmap = extractTeethROI(bitmap)
        
        // 2. RESIZE ke 224x224 (Input Model)
        val resizedBitmap = Bitmap.createScaledBitmap(roiBitmap, 224, 224, true)

        // 3. Alokasikan ByteBuffer
        val inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(224 * 224)
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        // 4. PREPROCESSING: Kirim Float 0-255 (EfficientNet handle rescaling internal)
        inputBuffer.rewind()
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF).toFloat()) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF).toFloat())  // G
            inputBuffer.putFloat((pixel and 0xFF).toFloat())         // B
        }

        // 5. Output Buffer [1, 7]
        val outputBuffer = Array(1) { FloatArray(7) }

        // 6. Inferensi
        interpreter?.run(inputBuffer, outputBuffer)

        // 7. Argmax
        val probabilities = outputBuffer[0]
        var maxIndex = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        val kelasDeteksi = if (maxIndex < classLabels.size) classLabels[maxIndex] else "Unknown"
        val isSehat = kelasDeteksi == "Healthy_Teeth"
        val isNonTeeth = kelasDeteksi == "Non_Teeth"

        return ClassificationResult(
            isSehat = isSehat,
            isNonTeeth = isNonTeeth,
            kelasDeteksi = kelasDeteksi,
            namaKelas = getNamaKelas(kelasDeteksi),
            deskripsi = getDeskripsi(kelasDeteksi),
            akurasi = maxProb * 100f
        )
    }

    /**
     * MENGGANTIKAN CV2 auto_crop_teeth_cv2
     * Mencari area gigi berdasarkan kontras cahaya (teeth pixels are bright)
     */
    private fun extractTeethROI(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Analisis pada resolusi rendah untuk kecepatan
        val scale = 0.2f
        val smallW = (width * scale).toInt()
        val smallH = (height * scale).toInt()
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, smallW, smallH, false)
        
        var minX = smallW
        var minY = smallH
        var maxX = 0
        var maxY = 0
        var foundTeeth = false

        for (y in 0 until smallH) {
            for (x in 0 until smallW) {
                val pixel = smallBitmap.getPixel(x, y)
                // Hitung Luminance (Kecerahan)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b)

                // Gigi biasanya memiliki brightness > 150 (putih/kuning terang)
                if (brightness > 140) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                    foundTeeth = true
                }
            }
        }

        if (!foundTeeth) return centerCrop(bitmap)

        // Kembalikan ke koordinat asli
        var finalMinX = (minX / scale).toInt()
        var finalMinY = (minY / scale).toInt()
        var finalMaxX = (maxX / scale).toInt()
        var finalMaxY = (maxY / scale).toInt()

        // Tambahkan padding agar tidak terlalu mepet
        val padding = 50
        finalMinX = (finalMinX - padding).coerceAtLeast(0)
        finalMinY = (finalMinY - padding).coerceAtLeast(0)
        finalMaxX = (finalMaxX + padding).coerceAtMost(width)
        finalMaxY = (finalMaxY + padding).coerceAtMost(height)

        val cropW = finalMaxX - finalMinX
        val cropH = finalMaxY - finalMinY
        
        return if (cropW > 0 && cropH > 0) {
            Bitmap.createBitmap(bitmap, finalMinX, finalMinY, cropW, cropH)
        } else {
            centerCrop(bitmap)
        }
    }

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    private fun getNamaKelas(kelas: String): String {
        return when (kelas) {
            "Calculus" -> "Karang Gigi (Calculus)"
            "Caries" -> "Karies / Gigi Berlubang (Caries)"
            "Discoloration" -> "Perubahan Warna (Discoloration)"
            "Gingivitis" -> "Radang Gusi (Gingivitis)"
            "Healthy_Teeth" -> "Gigi Sehat (Healthy Teeth)"
            "Hypodontia" -> "Gigi Kurang (Hypodontia)"
            "Non_Teeth" -> "Bukan Objek Gigi"
            else -> "Tidak Diketahui"
        }
    }

    private fun getDeskripsi(kelas: String): String {
        return when (kelas) {
            "Calculus" -> "Penumpukan plak yang mengeras pada gigi. Disarankan untuk melakukan scaling di dokter gigi."
            "Caries" -> "Ditemukan adanya lubang atau kerusakan pada struktur gigi. Segera hubungi dokter gigi untuk penambalan."
            "Discoloration" -> "Terjadi perubahan warna pada permukaan gigi. Bisa disebabkan oleh noda makanan atau faktor internal."
            "Gingivitis" -> "Gusi tampak merah, bengkak, atau mudah berdarah. Tingkatkan kebersihan mulut dan konsultasi ke dokter."
            "Healthy_Teeth" -> "Gigi dan mulut Anda tampak sehat. Tetap jaga kebersihan dengan menyikat gigi secara teratur."
            "Hypodontia" -> "Kondisi di mana terdapat kekurangan jumlah gigi secara bawaan. Konsultasikan dengan dokter spesialis ortodonti."
            "Non_Teeth" -> "Gambar yang Anda masukkan sepertinya bukan foto gigi. Pastikan foto terfokus pada gigi dengan pencahayaan cukup."
            else -> "Hasil analisis tidak memberikan deskripsi yang spesifik."
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
