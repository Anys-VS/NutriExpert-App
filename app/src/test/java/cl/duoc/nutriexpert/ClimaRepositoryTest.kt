package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.data.ClimaCache
import cl.duoc.nutriexpert.data.ClimaGuardado
import cl.duoc.nutriexpert.data.ClimaRepository
import cl.duoc.nutriexpert.data.model.ClimaActual
import cl.duoc.nutriexpert.data.model.ClimaDiario
import cl.duoc.nutriexpert.data.model.ClimaRespuesta
import cl.duoc.nutriexpert.data.remote.ClimaApiService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// Se simulan (mock) la API de Open-Meteo y el caché: la prueba no usa internet ni el teléfono.
class ClimaRepositoryTest {

    private val api = mockk<ClimaApiService>()
    private val cache = mockk<ClimaCache>(relaxed = true)
    private val repository = ClimaRepository(api, cache)

    private val clima = ClimaRespuesta(
        current = ClimaActual(temperatura = 18.4, humedad = 62, lluvia = 0.0),
        daily = ClimaDiario(evapotranspiracion = listOf(3.1), lluviaTotal = listOf(0.0))
    )

    @Test
    fun conSenal_devuelveElClimaNuevoYLoGuarda() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any(), any(), any()) } returns clima

        val resultado = repository.obtenerClima()

        assertSame(clima, resultado.clima)
        assertFalse(resultado.desdeCache)
        verify { cache.guardar(clima) }
    }

    @Test
    fun sinSenal_usaLoGuardadoHoy() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any(), any(), any()) } throws IOException("sin red")
        every { cache.leerDeHoy() } returns ClimaGuardado(clima, guardadoEn = 1_000L)

        val resultado = repository.obtenerClima()

        assertTrue(resultado.desdeCache)
        assertEquals(1_000L, resultado.actualizadoEn)
        assertEquals(18.4, resultado.clima.current.temperatura, 0.0)
    }

    @Test(expected = IOException::class)
    fun sinSenalYSinCache_lanzaElError() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any(), any(), any()) } throws IOException("sin red")
        every { cache.leerDeHoy() } returns null

        repository.obtenerClima()
    }
}
