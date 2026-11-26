package cl.duoc.nutriexpert

import android.util.Log
import cl.duoc.nutriexpert.data.RegistroTerreno
import cl.duoc.nutriexpert.data.RegistroTerrenoDao
import cl.duoc.nutriexpert.data.SincronizacionRepository
import cl.duoc.nutriexpert.data.remote.RegistroDto
import cl.duoc.nutriexpert.data.remote.RegistrosApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

// Se simulan Room (dao) y ms-registros (api). Se prueba la decisión de qué enviar y cómo.
class SincronizacionRepositoryTest {

    private val dao = mockk<RegistroTerrenoDao>(relaxed = true)
    private val api = mockk<RegistrosApi>()
    private val repository = SincronizacionRepository(dao, api)

    private fun registro(id: Long, idServidor: Long? = null) = RegistroTerreno(
        id = id, usuarioId = 1, cultivo = "Palto", lote = "L-01",
        metaRendimiento = 12.0, eficienciaRiego = 85.0,
        n = 84.7, p2o5 = 28.2, k2o = 112.9, cao = 42.4, mgo = 21.2,
        fotoUri = "", creadoEn = 1_000L, idServidor = idServidor
    )

    private fun respuestaOk(idServidor: Long) = Response.success(
        RegistroDto(
            id = idServidor, idLocal = 1, usuarioId = 1, cultivo = "Palto", lote = "L-01",
            metaRendimiento = 12.0, eficienciaRiego = 85.0,
            n = 84.7, p2o5 = 28.2, k2o = 112.9, cao = 42.4, mgo = 21.2, creadoEn = 1_000L
        )
    )

    private fun respuestaError(codigo: Int): Response<RegistroDto> =
        Response.error(codigo, """{"error":"x"}""".toResponseBody(null))

    // android.util.Log no existe en la JVM: se reemplaza por uno que no hace nada.
    @Before
    fun preparar() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun limpiar() = unmockkStatic(Log::class)

    @Test
    fun registroNuevo_seEnviaConPostYQuedaSincronizado() = runTest {
        coEvery { dao.pendientes() } returns listOf(registro(id = 7))
        coEvery { api.guardar(any()) } returns respuestaOk(idServidor = 15)

        val enviados = repository.sincronizarPendientes()

        assertEquals(1, enviados)
        coVerify { dao.marcarSincronizado(7, 15) }
        coVerify(exactly = 0) { api.actualizar(any(), any()) }
    }

    @Test
    fun registroEditado_seEnviaConPut() = runTest {
        coEvery { dao.pendientes() } returns listOf(registro(id = 7, idServidor = 15))
        coEvery { api.actualizar(15, any()) } returns respuestaOk(idServidor = 15)

        repository.sincronizarPendientes()

        coVerify { api.actualizar(15, match { it.idLocal == 7L && it.lote == "L-01" }) }
        coVerify(exactly = 0) { api.guardar(any()) }
        coVerify { dao.marcarSincronizado(7, 15) }
    }

    @Test
    fun siElPutDa404_seVuelveACrearConPost() = runTest {
        coEvery { dao.pendientes() } returns listOf(registro(id = 7, idServidor = 15))
        coEvery { api.actualizar(15, any()) } returns respuestaError(404)
        coEvery { api.guardar(any()) } returns respuestaOk(idServidor = 22)

        repository.sincronizarPendientes()

        coVerify { dao.marcarSincronizado(7, 22) }
    }

    @Test
    fun siElServidorRechazaLosDatos_quedaPendiente() = runTest {
        coEvery { dao.pendientes() } returns listOf(registro(id = 7))
        coEvery { api.guardar(any()) } returns respuestaError(400)

        val enviados = repository.sincronizarPendientes()

        assertEquals(0, enviados)
        coVerify(exactly = 0) { dao.marcarSincronizado(any(), any()) }
    }

    @Test
    fun siSePierdeLaConexion_seDetieneSinIntentarLosDemas() = runTest {
        coEvery { dao.pendientes() } returns listOf(registro(id = 7), registro(id = 8))
        coEvery { api.guardar(any()) } throws IOException("sin red")

        val enviados = repository.sincronizarPendientes()

        assertEquals(0, enviados)
        coVerify(exactly = 1) { api.guardar(any()) }
    }

    @Test
    fun borrarEnServidor_llamaAlDelete() = runTest {
        coEvery { api.borrar(15) } returns Response.success(Unit)

        repository.borrarEnServidor(15)

        coVerify { api.borrar(15) }
    }

    @Test
    fun borrarEnServidorSinConexion_noRevientaLaApp() = runTest {
        coEvery { api.borrar(15) } throws IOException("sin red")

        repository.borrarEnServidor(15) // no debe lanzar excepción
    }
}
