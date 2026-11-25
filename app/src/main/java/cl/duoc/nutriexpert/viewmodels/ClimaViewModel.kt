package cl.duoc.nutriexpert.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nutriexpert.data.ClimaRepository
import cl.duoc.nutriexpert.model.ClimaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// El repositorio llega por constructor: la app pasa el real y las pruebas un mock.
class ClimaViewModel(private val repository: ClimaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ClimaUiState())
    val uiState: StateFlow<ClimaUiState> = _uiState.asStateFlow()

    init {
        cargarClima()
    }

    fun cargarClima() {
        // Evita pedir el clima dos veces al mismo tiempo.
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resultado = repository.obtenerClima()
                _uiState.update {
                    it.copy(
                        clima = resultado.clima,
                        actualizadoEn = resultado.actualizadoEn,
                        desdeCache = resultado.desdeCache,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "No se pudo cargar el clima. Revisa tu conexión.")
                }
            }
        }
    }
}
