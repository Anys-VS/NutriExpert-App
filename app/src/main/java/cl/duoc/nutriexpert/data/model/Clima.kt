package cl.duoc.nutriexpert.data.model

import com.google.gson.annotations.SerializedName

// Respuesta de Open-Meteo. Solo se declaran los campos que usa la app.
// @SerializedName une el nombre del JSON con el nombre en español de Kotlin.
data class ClimaRespuesta(
    val current: ClimaActual,
    val daily: ClimaDiario
)

data class ClimaActual(
    @SerializedName("temperature_2m") val temperatura: Double,
    @SerializedName("relative_humidity_2m") val humedad: Int,
    @SerializedName("precipitation") val lluvia: Double
)

// Open-Meteo entrega listas (una posición por día). Pedimos solo 1 día.
data class ClimaDiario(
    @SerializedName("et0_fao_evapotranspiration") val evapotranspiracion: List<Double>,
    @SerializedName("precipitation_sum") val lluviaTotal: List<Double>
)
