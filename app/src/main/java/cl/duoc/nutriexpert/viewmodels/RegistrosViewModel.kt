package cl.duoc.nutriexpert.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nutriexpert.data.AppDatabase
import cl.duoc.nutriexpert.data.RegistroTerreno
import cl.duoc.nutriexpert.data.SincronizacionRepository
import cl.duoc.nutriexpert.data.remote.Conectividad
import cl.duoc.nutriexpert.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// Lista de registros de terreno guardados en el teléfono. La usan el Home y el Historial.
// También los sincroniza con ms-registros: al abrir la app, al guardar uno nuevo
// y cada vez que vuelve la señal.
class RegistrosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).registroTerrenoDao()
    private val sincronizacion = SincronizacionRepository(dao, RetrofitInstance.registrosApi)
    private val conectividad = Conectividad(application)

    private val _registros = MutableStateFlow<List<RegistroTerreno>>(emptyList())
    val registros: StateFlow<List<RegistroTerreno>> = _registros.asStateFlow()

    val conectado: StateFlow<Boolean> = conectividad.conectado

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    // Evita dos sincronizaciones a la vez (por ejemplo, al guardar justo cuando vuelve la señal).
    private val enCurso = Mutex()

    init {
        conectividad.iniciar()
        viewModelScope.launch {
            dao.observar().collect { lista ->
                _registros.value = lista
                if (lista.any { !it.sincronizado }) sincronizar()
            }
        }
        viewModelScope.launch {
            conectividad.conectado.collect { hayRed ->
                if (hayRed) sincronizar()
            }
        }
    }

    fun sincronizar() {
        // Sin red no se intenta. Si hay red pero el servidor no responde, la pasada se corta sola.
        if (!conectividad.conectado.value && !conectividad.hayConexion()) return
        viewModelScope.launch {
            if (!enCurso.tryLock()) return@launch
            try {
                _sincronizando.value = true
                sincronizacion.sincronizarPendientes()
            } finally {
                _sincronizando.value = false
                enCurso.unlock()
            }
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch {
            val idServidor = dao.buscar(id)?.idServidor
            dao.borrar(id)
            if (idServidor != null) sincronizacion.borrarEnServidor(idServidor)
        }
    }

    override fun onCleared() {
        conectividad.detener()
        super.onCleared()
    }
}
