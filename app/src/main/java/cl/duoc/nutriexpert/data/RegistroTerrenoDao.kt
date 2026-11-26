package cl.duoc.nutriexpert.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroTerrenoDao {
    @Insert
    suspend fun insertar(registro: RegistroTerreno): Long

    @Query("SELECT * FROM registros_terreno ORDER BY creadoEn DESC")
    fun observar(): Flow<List<RegistroTerreno>>

    // Los que faltan por enviar a ms-registros (solo de usuarios con id del servidor).
    @Query("SELECT * FROM registros_terreno WHERE sincronizado = 0 AND usuarioId > 0 ORDER BY creadoEn")
    suspend fun pendientes(): List<RegistroTerreno>

    @Query("UPDATE registros_terreno SET sincronizado = 1, idServidor = :idServidor WHERE id = :id")
    suspend fun marcarSincronizado(id: Long, idServidor: Long?)

    // Edición: cambia los datos y lo deja pendiente. Conserva idServidor para que
    // la sincronización sepa que debe actualizarlo (PUT) y no crear otro (POST).
    @Query(
        "UPDATE registros_terreno SET lote = :lote, metaRendimiento = :meta, eficienciaRiego = :eficiencia, " +
            "n = :n, p2o5 = :p2o5, k2o = :k2o, cao = :cao, mgo = :mgo, sincronizado = 0 WHERE id = :id"
    )
    suspend fun actualizar(
        id: Long, lote: String, meta: Double, eficiencia: Double,
        n: Double, p2o5: Double, k2o: Double, cao: Double, mgo: Double
    )

    @Query("SELECT * FROM registros_terreno WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Long): RegistroTerreno?

    @Query("DELETE FROM registros_terreno WHERE id = :id")
    suspend fun borrar(id: Long)
}
