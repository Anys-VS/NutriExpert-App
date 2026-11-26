package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.data.ClimaRepository
import cl.duoc.nutriexpert.data.ResultadoClima
import cl.duoc.nutriexpert.data.model.ClimaActual
import cl.duoc.nutriexpert.data.model.ClimaDiario
import cl.duoc.nutriexpert.data.model.ClimaRespuesta
import cl.duoc.nutriexpert.viewmodels.ClimaViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// El ViewModel usa viewModelScope (hilo Main de Android). En la JVM se reemplaza Main
// por un dispatcher de prueba que avanzamos a mano con advanceUntilIdle().
@OptIn(ExperimentalCoroutinesApi::class)
class ClimaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ClimaRepository>()

    private val clima = ClimaRespuesta(
        current = ClimaActual(temperatura = 18.4, humedad = 62, lluvia = 0.0),
        daily = ClimaDiario(evapotranspiracion = listOf(3.1), lluviaTotal = listOf(0.0))
    )

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun alCrearse_quedaCargandoYLuegoMuestraElClima() = runTest(dispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns ResultadoClima(clima, 1_000L, desdeCache = false)

        val viewModel = ClimaViewModel(repository)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertEquals(clima, estado.clima)
        assertEquals(1_000L, estado.actualizadoEn)
        assertFalse(estado.desdeCache)
        assertNull(estado.error)
    }

    @Test
    fun conDatosGuardados_loIndicaEnElEstado() = runTest(dispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns ResultadoClima(clima, 1_000L, desdeCache = true)

        val viewModel = ClimaViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.desdeCache)
    }

    @Test
    fun siFalla_muestraUnMensajeDeError() = runTest(dispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } throws IOException("sin red")

        val viewModel = ClimaViewModel(repository)
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertNull(estado.clima)
        assertEquals("No se pudo cargar el clima. Revisa tu conexión.", estado.error)
    }

    @Test
    fun reintentarDespuesDeUnError_cargaElClima() = runTest(dispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } throws IOException("sin red")
        val viewModel = ClimaViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.obtenerClima(any(), any()) } returns ResultadoClima(clima, 2_000L, desdeCache = false)
        viewModel.cargarClima()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.clima)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun mientrasCarga_noPideElClimaDosVeces() = runTest(dispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns ResultadoClima(clima, 1_000L, desdeCache = false)

        val viewModel = ClimaViewModel(repository)
        viewModel.cargarClima() // todavía está cargando el del init
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.obtenerClima(any(), any()) }
    }
}
