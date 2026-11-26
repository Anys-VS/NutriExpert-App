package cl.duoc.nutriexpert

import android.app.Application
import cl.duoc.nutriexpert.data.Catalogo
import cl.duoc.nutriexpert.data.CatalogoRepository
import cl.duoc.nutriexpert.data.OrigenCatalogo
import cl.duoc.nutriexpert.data.RegistroTerrenoDao
import cl.duoc.nutriexpert.domain.DatosEjemplo
import cl.duoc.nutriexpert.viewmodels.CalculoNutricionalViewModel
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
import org.junit.Before
import org.junit.Test

// Formulario de nuevo registro: validación, cálculo y guardado en Room (simulado).
@OptIn(ExperimentalCoroutinesApi::class)
class CalculoNutricionalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = mockk<RegistroTerrenoDao>(relaxed = true)
    private val catalogo = mockk<CatalogoRepository>()
    private lateinit var viewModel: CalculoNutricionalViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        coEvery { catalogo.obtener() } returns Catalogo(DatosEjemplo.cultivos, OrigenCatalogo.SERVIDOR)
        viewModel = CalculoNutricionalViewModel(mockk<Application>(relaxed = true), dao, catalogo)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun llenar(cultivo: String = "Palto", lote: String = " L-01 ", meta: String = "12", eficiencia: String = "85") {
        viewModel.onCultivoChange(cultivo)
        viewModel.onLoteChange(lote)
        viewModel.onMetaRendimientoChange(meta)
        viewModel.onEficienciaRiegoChange(eficiencia)
    }

    @Test
    fun alIniciar_cargaElCatalogo() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals(OrigenCatalogo.SERVIDOR, viewModel.origenCatalogo.value)
        assertEquals(3, viewModel.cultivos.value.size)
    }

    @Test
    fun formularioVacio_marcaLosCuatroCampos() {
        assertFalse(viewModel.validar())
        val errores = viewModel.estado.value.errores
        assertEquals("Selecciona un cultivo", errores.cultivo)
        assertEquals("Indica el lote o cuartel", errores.lote)
        assertEquals("Campo obligatorio", errores.metaRendimiento)
        assertEquals("Campo obligatorio", errores.eficienciaRiego)
    }

    @Test
    fun numerosInvalidos_seRechazan() {
        llenar(meta = "doce", eficiencia = "150")
        assertFalse(viewModel.validar())
        assertEquals("Ingresa un número válido", viewModel.estado.value.errores.metaRendimiento)
        assertEquals("Debe estar entre 1 y 100", viewModel.estado.value.errores.eficienciaRiego)

        llenar(meta = "0")
        viewModel.validar()
        assertEquals("Debe ser mayor a 0", viewModel.estado.value.errores.metaRendimiento)
    }

    @Test
    fun calcular_entregaLaDemandaDeNutrientes() = runTest(dispatcher) {
        advanceUntilIdle()
        llenar(meta = "12,0") // acepta coma decimal

        viewModel.calcular()
        advanceUntilIdle()

        val resultado = viewModel.estado.value.resultado
        assertNotNull(resultado)
        assertEquals(84.7, resultado!!.n, 0.1) // 60 x (12/10) / 0,85
        assertFalse(viewModel.estado.value.calculando)
    }

    @Test
    fun cambiarUnDato_borraElResultadoAnterior() = runTest(dispatcher) {
        advanceUntilIdle()
        llenar()
        viewModel.calcular()
        advanceUntilIdle()

        viewModel.onMetaRendimientoChange("14")

        assertNull(viewModel.estado.value.resultado)
    }

    @Test
    fun guardar_insertaEnRoomConElLoteLimpio() = runTest(dispatcher) {
        advanceUntilIdle()
        llenar()
        viewModel.onFotoTomada("content://foto-prueba")
        viewModel.calcular()
        advanceUntilIdle()
        var guardado = false

        viewModel.guardar(usuarioId = 5) { guardado = true }
        advanceUntilIdle()

        assertEquals(true, guardado)
        coVerify {
            dao.insertar(match {
                it.usuarioId == 5L && it.lote == "L-01" && it.cultivo == "Palto" &&
                    it.fotoUri == "content://foto-prueba" && !it.sincronizado
            })
        }
    }

    @Test
    fun guardarSinCalcular_noHaceNada() = runTest(dispatcher) {
        llenar()

        viewModel.guardar(usuarioId = 5) {}
        advanceUntilIdle()

        coVerify(exactly = 0) { dao.insertar(any()) }
    }
}
