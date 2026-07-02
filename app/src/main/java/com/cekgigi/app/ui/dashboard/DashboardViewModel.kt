package com.cekgigi.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cekgigi.app.data.ScreeningDao
import com.cekgigi.app.data.ScreeningEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val riwayatTerakhir: ScreeningEntity?,
        val semuaRiwayat: List<ScreeningEntity>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(private val screeningDao: ScreeningDao) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val terakhir = screeningDao.ambilRiwayatTerakhir()
                val semua = screeningDao.ambilSemuaRiwayat()
                _uiState.value = DashboardUiState.Success(terakhir, semua)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
