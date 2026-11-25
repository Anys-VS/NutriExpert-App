package cl.duoc.nutriexpert.data

import cl.duoc.nutriexpert.data.remote.CatalogoApi
import cl.duoc.nutriexpert.data.remote.CultivoDto
import cl.duoc.nutriexpert.domain.CultivoReferencia
import cl.duoc.nutriexpert.domain.DatosEjemplo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class OrigenCatalogo { SERVIDOR, COPIA_LOCAL, DATOS_EJEMPLO }

data class Catalogo(val cultivos: List<CultivoReferencia>, val origen: OrigenCatalogo)

// Lista de cultivos: primero ms-catalogo; si no responde, la última copia guardada en Room;
// si nunca hubo conexión, los datos de ejemplo que trae la app.
class CatalogoRepository(
    private val dao: CultivoDao,
    private val api: CatalogoApi
) {

    suspend fun obtener(): Catalogo = withContext(Dispatchers.IO) {
        try {
            val respuesta = api.listar()
            val locales = respuesta.body().orEmpty().mapNotNull { it.aLocal() }
            if (respuesta.isSuccessful && locales.isNotEmpty()) {
                dao.reemplazar(locales)
                return@withContext Catalogo(locales.map { it.aDominio() }, OrigenCatalogo.SERVIDOR)
            }
        } catch (e: IOException) {
            // Sin señal o servidor apagado: se usa el respaldo.
        }
        val copia = dao.todos()
        if (copia.isNotEmpty()) {
            Catalogo(copia.map { it.aDominio() }, OrigenCatalogo.COPIA_LOCAL)
        } else {
            Catalogo(DatosEjemplo.cultivos, OrigenCatalogo.DATOS_EJEMPLO)
        }
    }

    // Descarta cultivos incompletos: con un valor nulo el cálculo no tendría sentido.
    private fun CultivoDto.aLocal(): CultivoLocal? {
        val nombre = nombre?.takeIf { it.isNotBlank() } ?: return null
        val base = rendimientoBase?.takeIf { it > 0.0 } ?: return null
        return CultivoLocal(
            nombre = nombre,
            rendimientoBase = base,
            n = n ?: return null,
            p2o5 = p2o5 ?: return null,
            k2o = k2o ?: return null,
            cao = cao ?: return null,
            mgo = mgo ?: return null
        )
    }
}
