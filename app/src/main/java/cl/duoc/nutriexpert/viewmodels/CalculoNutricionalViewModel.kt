package cl.duoc.nutriexpert.viewmodels

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nutriexpert.data.AppDatabase
import cl.duoc.nutriexpert.data.CatalogoRepository
import cl.duoc.nutriexpert.data.OrigenCatalogo
import cl.duoc.nutriexpert.data.RegistroTerreno
import cl.duoc.nutriexpert.data.RegistroTerrenoDao
import cl.duoc.nutriexpert.domain.CalculoNutricional
import cl.duoc.nutriexpert.data.remote.RetrofitInstance
import cl.duoc.nutriexpert.domain.CultivoReferencia
import cl.duoc.nutriexpert.domain.DatosEjemplo
import cl.duoc.nutriexpert.model.CalculoNutricionalErrores
import cl.duoc.nutriexpert.model.CalculoNutricionalUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

// Room y el catálogo llegan por constructor con valores por defecto: la app usa los reales
// y las pruebas pasan mocks.
class CalculoNutricionalViewModel @JvmOverloads constructor(
    application: Application,
    private val dao: RegistroTerrenoDao = AppDatabase.obtener(application).registroTerrenoDao(),
    private val catalogo: CatalogoRepository = CatalogoRepository(
        AppDatabase.obtener(application).cultivoDao(),
        RetrofitInstance.catalogoApi
    )
) : AndroidViewModel(application) {
    private val _estado = MutableStateFlow(CalculoNutricionalUiState())
    val estado: StateFlow<CalculoNutricionalUiState> = _estado.asStateFlow()

    // Cultivos de ms-catalogo (o su copia en Room). Mientras carga se muestran los de ejemplo.
    private val _cultivos = MutableStateFlow(DatosEjemplo.cultivos)
    val cultivos: StateFlow<List<CultivoReferencia>> = _cultivos.asStateFlow()

    private val _origenCatalogo = MutableStateFlow(OrigenCatalogo.DATOS_EJEMPLO)
    val origenCatalogo: StateFlow<OrigenCatalogo> = _origenCatalogo.asStateFlow()

    init {
        viewModelScope.launch {
            val resultado = catalogo.obtener()
            _cultivos.value = resultado.cultivos
            _origenCatalogo.value = resultado.origen
        }
    }

    fun onCultivoChange(valor: String) {
        _estado.update {
            it.copy(cultivo = valor, resultado = null, errores = it.errores.copy(cultivo = null))
        }
    }

    fun onLoteChange(valor: String) {
        _estado.update {
            it.copy(lote = valor, resultado = null, errores = it.errores.copy(lote = null))
        }
    }

    fun onMetaRendimientoChange(valor: String) {
        _estado.update {
            it.copy(metaRendimiento = valor, resultado = null, errores = it.errores.copy(metaRendimiento = null))
        }
    }

    fun onEficienciaRiegoChange(valor: String) {
        _estado.update {
            it.copy(eficienciaRiego = valor, resultado = null, errores = it.errores.copy(eficienciaRiego = null))
        }
    }

    // TODO confirmar con el cliente: rangos validos de rendimiento y eficiencia
    fun validar(): Boolean {
        val actual = _estado.value
        
        val meta = actual.metaRendimiento.replace(',', '.').toDoubleOrNull()
        val eficiencia = actual.eficienciaRiego.replace(',', '.').toDoubleOrNull()

        val errores = CalculoNutricionalErrores(
            cultivo = if (actual.cultivo.isBlank()) "Selecciona un cultivo" else null,
            lote = if (actual.lote.isBlank()) "Indica el lote o cuartel" else null,
            metaRendimiento = when {
                actual.metaRendimiento.isBlank() -> "Campo obligatorio"
                meta == null -> "Ingresa un número válido"
                meta <= 0.0 -> "Debe ser mayor a 0"
                else -> null
            },
            eficienciaRiego = when {
                actual.eficienciaRiego.isBlank() -> "Campo obligatorio"
                eficiencia == null -> "Ingresa un número válido"
                eficiencia <= 0.0 || eficiencia > 100.0 -> "Debe estar entre 1 y 100"
                else -> null
            }
        )
        _estado.update { it.copy(errores = errores) }
        return errores == CalculoNutricionalErrores()
    }

    fun calcular() {
        if (_estado.value.calculando || !validar()) return
        val actual = _estado.value
        val cultivo = _cultivos.value.find { it.nombre == actual.cultivo } ?: return
        val meta = actual.metaRendimiento.replace(',', '.').toDouble()
        val eficiencia = actual.eficienciaRiego.replace(',', '.').toDouble()

        _estado.update { it.copy(calculando = true, resultado = null) }
        viewModelScope.launch {
            delay(800)
            val resultado = CalculoNutricional.calcular(cultivo, meta, eficiencia)
            _estado.update { it.copy(calculando = false, resultado = resultado) }
        }
    }

    // Guarda en Room el registro con su resultado. Solo se puede después de calcular.
    // Queda "Pendiente"; RegistrosViewModel lo envía a ms-registros apenas haya señal.
    fun guardar(usuarioId: Long, onGuardado: () -> Unit) {
        val actual = _estado.value
        val resultado = actual.resultado ?: return
        if (actual.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            dao.insertar(
                RegistroTerreno(
                    usuarioId = usuarioId,
                    cultivo = actual.cultivo,
                    lote = actual.lote.trim(),
                    metaRendimiento = actual.metaRendimiento.replace(',', '.').toDouble(),
                    eficienciaRiego = actual.eficienciaRiego.replace(',', '.').toDouble(),
                    n = resultado.n,
                    p2o5 = resultado.p2o5,
                    k2o = resultado.k2o,
                    cao = resultado.cao,
                    mgo = resultado.mgo,
                    fotoUri = actual.fotoUri,
                    creadoEn = System.currentTimeMillis()
                )
            )
            _estado.update { it.copy(guardando = false) }
            onGuardado()
        }
    }

    fun crearFoto(): Uri {
        val aplicacion = getApplication<Application>()
        val directorio = File(aplicacion.filesDir, "fotos")
        if (!directorio.exists()) {
            directorio.mkdirs()
        }
        val archivo = File(directorio, "foliar-${System.currentTimeMillis()}.jpg")
        archivo.createNewFile()
        return FileProvider.getUriForFile(
            aplicacion,
            "cl.duoc.nutriexpert.fileprovider",
            archivo
        )
    }

    // Solo se llama si la cámara confirma que guardó la foto.
    fun onFotoTomada(uri: String) {
        _estado.update { it.copy(fotoUri = uri) }
    }
}
