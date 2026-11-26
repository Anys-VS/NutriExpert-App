package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.data.CatalogoRepository
import cl.duoc.nutriexpert.data.CultivoDao
import cl.duoc.nutriexpert.data.CultivoLocal
import cl.duoc.nutriexpert.data.OrigenCatalogo
import cl.duoc.nutriexpert.data.remote.CatalogoApi
import cl.duoc.nutriexpert.data.remote.CultivoDto
import cl.duoc.nutriexpert.domain.DatosEjemplo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

// Orden de respaldo del catálogo: ms-catalogo → copia en Room → datos de ejemplo de la app.
class CatalogoRepositoryTest {

    private val dao = mockk<CultivoDao>(relaxed = true)
    private val api = mockk<CatalogoApi>()
    private val repository = CatalogoRepository(dao, api)

    private val paltoDto = CultivoDto(
        id = 1, nombre = "Palto", rendimientoBase = 10.0,
        n = 60.0, p2o5 = 20.0, k2o = 80.0, cao = 30.0, mgo = 15.0
    )
    private val paltoLocal = CultivoLocal("Palto", 10.0, 60.0, 20.0, 80.0, 30.0, 15.0)

    @Test
    fun conServidor_usaSuListaYActualizaLaCopia() = runTest {
        coEvery { api.listar() } returns Response.success(listOf(paltoDto))

        val catalogo = repository.obtener()

        assertEquals(OrigenCatalogo.SERVIDOR, catalogo.origen)
        assertEquals("Palto", catalogo.cultivos.single().nombre)
        coVerify { dao.reemplazar(listOf(paltoLocal)) }
    }

    @Test
    fun sinServidor_usaLaUltimaCopiaGuardada() = runTest {
        coEvery { api.listar() } throws IOException("sin red")
        coEvery { dao.todos() } returns listOf(paltoLocal)

        val catalogo = repository.obtener()

        assertEquals(OrigenCatalogo.COPIA_LOCAL, catalogo.origen)
        assertEquals(10.0, catalogo.cultivos.single().rendimientoBase, 0.0)
    }

    @Test
    fun sinServidorNiCopia_usaLosDatosDeEjemplo() = runTest {
        coEvery { api.listar() } throws IOException("sin red")
        coEvery { dao.todos() } returns emptyList()

        val catalogo = repository.obtener()

        assertEquals(OrigenCatalogo.DATOS_EJEMPLO, catalogo.origen)
        assertEquals(DatosEjemplo.cultivos, catalogo.cultivos)
    }

    @Test
    fun cultivosIncompletosDelServidor_seDescartan() = runTest {
        val sinNitrogeno = paltoDto.copy(nombre = "Tomate", n = null)
        coEvery { api.listar() } returns Response.success(listOf(paltoDto, sinNitrogeno))

        val catalogo = repository.obtener()

        assertEquals(listOf("Palto"), catalogo.cultivos.map { it.nombre })
    }

    @Test
    fun servidorConListaVacia_noBorraLaCopia() = runTest {
        coEvery { api.listar() } returns Response.success(emptyList())
        coEvery { dao.todos() } returns listOf(paltoLocal)

        val catalogo = repository.obtener()

        assertEquals(OrigenCatalogo.COPIA_LOCAL, catalogo.origen)
        coVerify(exactly = 0) { dao.reemplazar(any()) }
    }
}
