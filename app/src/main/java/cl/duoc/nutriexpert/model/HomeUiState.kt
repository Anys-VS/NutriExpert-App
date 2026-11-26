package cl.duoc.nutriexpert.model

// Lo que muestra el Home. La pantalla no sabe de dónde vienen los datos:
// el nombre y el rol salen de la sesión y los registros, de Room.
// conectado y sincronizando vienen de RegistrosViewModel.
data class HomeUiState(
    val nombre: String = "",
    val rol: String = "",
    val pendientes: Int = 0,
    val conectado: Boolean = false,      // hay red en el teléfono
    val sincronizando: Boolean = false,  // enviando pendientes a ms-registros
    val ultimosRegistros: List<RegistroResumen> = emptyList()
)

data class RegistroResumen(
    val id: Long,
    val cultivo: String,
    val lote: String,
    val fecha: String,
    val sincronizado: Boolean
)
