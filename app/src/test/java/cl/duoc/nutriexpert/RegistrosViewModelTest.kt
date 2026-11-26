package cl.duoc.nutriexpert

import android.app.Application
import cl.duoc.nutriexpert.data.RegistroTerreno
import cl.duoc.nutriexpert.data.RegistroTerrenoDao
import cl.duoc.nutriexpert.data.SincronizacionRepository
import cl.duoc.nutriexpert.data.remote.Conectividad
import cl.duoc.nutriexpert.viewmodels.RegistrosViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Se simulan Room, la sincronización con ms-registros y el estado de la red.
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrosViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = mockk<RegistroTerrenoDao>(relaxed = true)
    private val sincronizacion = mockk<SincronizacionRepository>(relaxed = true)
    private val conectividad = mockk<Conectividad>(relaxed = true)
    private val hayRed = MutableStateFlow(false)

    private fun registro(id: Long, sincronizado: Boolean, idServidor: Long? = null) = RegistroTerreno(
        id = id, usuarioId = 1, cultivo = "Palto", lote = "L-01",
        metaRendimiento = 12.0, eficienciaRiego = 85.0,
        n = 84.7, p2o5 = 28.2, k2o = 112.9, cao = 42.4, mgo = 21.2,
        fotoUri = "", creadoEn = 1_000L, sincronizado = sincronizado, idServidor = idServidor
    )

    private fun crear(registros: List<RegistroTerreno>) : RegistrosViewModel {
        every { dao.observar() } returns flowOf(registros)
        return RegistrosViewModel(mockk<Application>(relaxed = true), dao, sincronizacion, conectividad)
    }

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        every { conectividad.conectado } returns hayRed
        every { conectividad.hayConexion() } answers { hayRed.value }
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun muestraLosRegistrosDeRoom() = runTest(dispatcher) {
        val viewModel = crear(listOf(registro(1, sincronizado = true), registro(2, sincronizado = true)))
        advanceUntilIdle()

        assertEquals(2, viewModel.registros.value.size)
    }

    @Test
    fun sinRed_noIntentaSincronizar() = runTest(dispatcher) {
        crear(listOf(registro(1, sincronizado = false)))
        advanceUntilIdle()

        coVerify(exactly = 0) { sincronizacion.sincronizarPendientes() }
    }

    @Test
    fun conRedYPendientes_sincronizaSolo() = runTest(dispatcher) {
        hayRed.value = true
        crear(listOf(registro(1, sincronizado = false)))
        advanceUntilIdle()

        coVerify(atLeast = 1) { sincronizacion.sincronizarPendientes() }
    }

    @Test
    fun alVolverLaSenal_sincroniza() = runTest(dispatcher) {
        crear(listOf(registro(1, sincronizado = true)))
        advanceUntilIdle()

        hayRed.value = true
        advanceUntilIdle()

        coVerify(atLeast = 1) { sincronizacion.sincronizarPendientes() }
    }

    @Test
    fun actualizar_recalculaYGuardaEnRoom() = runTest(dispatcher) {
        coEvery { dao.buscar(1) } returns registro(1, sincronizado = true, idServidor = 15)
        val viewModel = crear(emptyList())

        // Doble meta con la misma eficiencia → doble demanda de nutrientes.
        viewModel.actualizar(1, " L-02 ", metaRendimiento = 24.0, eficienciaRiego = 85.0)
        advanceUntilIdle()

        coVerify {
            dao.actualizar(
                id = 1, lote = "L-02", meta = 24.0, eficiencia = 85.0,
                n = match { kotlin.math.abs(it - 169.4) < 0.001 },
                p2o5 = any(), k2o = any(), cao = any(), mgo = any()
            )
        }
    }

    @Test
    fun borrarUnoYaEnviado_tambienLoBorraEnElServidor() = runTest(dispatcher) {
        coEvery { dao.buscar(1) } returns registro(1, sincronizado = true, idServidor = 15)
        val viewModel = crear(emptyList())

        viewModel.borrar(1)
        advanceUntilIdle()

        coVerify { dao.borrar(1) }
        coVerify { sincronizacion.borrarEnServidor(15) }
    }

    @Test
    fun borrarUnoPendiente_soloLoBorraDelTelefono() = runTest(dispatcher) {
        coEvery { dao.buscar(2) } returns registro(2, sincronizado = false)
        val viewModel = crear(emptyList())

        viewModel.borrar(2)
        advanceUntilIdle()

        coVerify { dao.borrar(2) }
        coVerify(exactly = 0) { sincronizacion.borrarEnServidor(any()) }
    }
}
