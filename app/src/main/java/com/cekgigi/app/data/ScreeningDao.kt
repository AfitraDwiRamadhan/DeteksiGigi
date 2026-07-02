package com.cekgigi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScreeningDao {
    @Insert
    suspend fun tambahRiwayat(screening: ScreeningEntity)

    @Query("SELECT * FROM riwayat_skrining ORDER BY id DESC")
    suspend fun ambilSemuaRiwayat(): List<ScreeningEntity>

    @Query("SELECT * FROM riwayat_skrining ORDER BY id DESC LIMIT 1")
    suspend fun ambilRiwayatTerakhir(): ScreeningEntity?

    @Query("DELETE FROM riwayat_skrining WHERE id = :id")
    suspend fun hapusRiwayat(id: Int)
}
