package com.cekgigi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "riwayat_skrining")
data class ScreeningEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: String,
    val statusGigi: String,
    val kelasDeteksi: String,
    val akurasi: Float,
    val deskripsi: String,
    val pathFoto: String
)
