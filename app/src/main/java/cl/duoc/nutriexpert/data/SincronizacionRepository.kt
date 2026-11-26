package cl.duoc.nutriexpert.data

import android.util.Log
import cl.duoc.nutriexpert.data.remote.RegistroDto
import cl.duoc.nutriexpert.data.remote.RegistrosApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

// Envía a ms-registros los registros pendientes de Room (nuevos o editados) y los marca como sincronizados.
// Es seguro reintentar: el servidor no duplica un registro con el mismo usuarioId + idLocal.
class SincronizacionRepository(
    private val dao: RegistroTerrenoDao,
    private val api: RegistrosApi
) {

    // Devuelve cuántos registros quedaron sincronizados en esta pasada.
    suspend fun sincronizarPendientes(): Int = withContext(Dispatchers.IO) {
        var enviados = 0
        for (registro in dao.pendientes()) {
            val respuesta = try {
                enviar(registro)
            } catch (e: IOException) {
                // Se perdió la conexión: se corta y se reintenta cuando vuelva la señal.
                Log.i(TAG, "Sin conexión con ms-registros, se reintentará: ${e.message}")
                break
            }
            if (respuesta.isSuccessful) {
                dao.marcarSincronizado(registro.id, respuesta.body()?.id)
                enviados++
            } else {
                // 400: el servidor rechazó los datos. Queda pendiente para revisarlo.
                Log.w(TAG, "ms-registros rechazó el registro ${registro.id}: código ${respuesta.code()}")
            }
        }
        enviados
    }

    // Nuevo (sin idServidor) → POST. Editado (ya tiene idServidor) → PUT.
    // Si el PUT responde 404 (lo borraron en el servidor), se vuelve a crear con POST.
    private suspend fun enviar(registro: RegistroTerreno): Response<RegistroDto> {
        val idServidor = registro.idServidor ?: return api.guardar(registro.aDto())
        val respuesta = api.actualizar(idServidor, registro.aDto())
        return if (respuesta.code() == 404) api.guardar(registro.aDto()) else respuesta
    }

    // Si el registro ya estaba en el servidor, también se borra allá. Si no hay señal, queda allá.
    suspend fun borrarEnServidor(idServidor: Long) {
        withContext(Dispatchers.IO) {
            try {
                api.borrar(idServidor)
            } catch (e: IOException) {
                Log.i(TAG, "No se pudo borrar $idServidor en ms-registros: ${e.message}")
            }
        }
    }

    // La foto no se envía en esta etapa: queda solo en el teléfono.
    private fun RegistroTerreno.aDto() = RegistroDto(
        idLocal = id,
        usuarioId = usuarioId,
        cultivo = cultivo,
        lote = lote,
        metaRendimiento = metaRendimiento,
        eficienciaRiego = eficienciaRiego,
        n = n,
        p2o5 = p2o5,
        k2o = k2o,
        cao = cao,
        mgo = mgo,
        creadoEn = creadoEn
    )

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
