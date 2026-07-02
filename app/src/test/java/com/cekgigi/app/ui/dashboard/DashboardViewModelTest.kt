package com.cekgigi.app.ui.dashboard

import com.cekgigi.app.data.ScreeningDao
import com.cekgigi.app.data.ScreeningEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var screeningDao: ScreeningDao
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        screeningDao = mock(ScreeningDao::class.java)
        viewModel = DashboardViewModel(screeningDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoading() = runTest {
        assertEquals(DashboardUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadDashboardData_updatesUiStateToSuccess() = runTest {
        // Arrange
        val fakeHistory = listOf(
            ScreeningEntity(1, "2026-07-01", "Sehat", "Healthy_Teeth", 99f, "Bagus", "path")
        )
        `when`(screeningDao.ambilRiwayatTerakhir()).thenReturn(fakeHistory.first())
        `when`(screeningDao.ambilSemuaRiwayat()).thenReturn(fakeHistory)

        // Act
        viewModel.loadDashboardData()

        // Assert
        val state = viewModel.uiState.value
        assert(state is DashboardUiState.Success)
        val successState = state as DashboardUiState.Success
        assertEquals(fakeHistory.first(), successState.riwayatTerakhir)
        assertEquals(fakeHistory, successState.semuaRiwayat)
    }
}
