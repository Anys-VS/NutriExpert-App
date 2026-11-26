package cl.duoc.nutriexpert.model

import cl.duoc.nutriexpert.data.model.ClimaRespuesta

data class ClimaUiState(
    val clima: ClimaRespuesta? = null,
    val actualizadoEn: Long? = null, // hora en que se descargó el dato
    val desdeCache: Boolean = false, // true = sin señal, se muestran los datos guardados
    val isLoading: Boolean = false,
    val error: String? = null
)
