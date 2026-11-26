package cl.duoc.nutriexpert.data

import cl.duoc.nutriexpert.data.model.ClimaRespuesta
import cl.duoc.nutriexpert.data.remote.ClimaApiService

// Recibe el ApiService y el caché por constructor: así en las pruebas se pueden usar mocks.
class ClimaRepository(
    private val api: ClimaApiService,
    private val cache: ClimaCache
) {

    // Coordenadas de ejemplo: Quillota, zona de paltos (dato ficticio de demostración).
    // Con señal: consulta Open-Meteo y guarda la respuesta.
    // Sin señal: devuelve lo guardado hoy; si no hay nada, lanza el error.
    suspend fun obtenerClima(
        latitud: Double = -32.88,
        longitud: Double = -71.25
    ): ResultadoClima {
        return try {
            val clima = api.obtenerClima(latitud, longitud)
            cache.guardar(clima)
            ResultadoClima(clima, System.currentTimeMillis(), desdeCache = false)
        } catch (e: Exception) {
            val guardado = cache.leerDeHoy() ?: throw e
            ResultadoClima(guardado.clima, guardado.guardadoEn, desdeCache = true)
        }
    }
}

data class ResultadoClima(
    val clima: ClimaRespuesta,
    val actualizadoEn: Long,
    val desdeCache: Boolean
)
