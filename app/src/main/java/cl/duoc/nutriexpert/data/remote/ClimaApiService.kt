package cl.duoc.nutriexpert.data.remote

import cl.duoc.nutriexpert.data.model.ClimaRespuesta
import retrofit2.http.GET
import retrofit2.http.Query

// Endpoints de Open-Meteo (https://open-meteo.com). Es gratis y no pide clave.
interface ClimaApiService {

    // Ejemplo de URL final:
    // https://api.open-meteo.com/v1/forecast?latitude=-32.88&longitude=-71.25&current=...
    @GET("v1/forecast")
    suspend fun obtenerClima(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") actual: String = "temperature_2m,relative_humidity_2m,precipitation",
        @Query("daily") diario: String = "et0_fao_evapotranspiration,precipitation_sum",
        @Query("timezone") zonaHoraria: String = "auto",
        @Query("forecast_days") dias: Int = 1
    ): ClimaRespuesta
}
