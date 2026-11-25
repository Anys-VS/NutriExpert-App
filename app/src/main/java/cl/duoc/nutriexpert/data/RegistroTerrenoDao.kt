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

    @Query("SELECT * FROM registros_terreno WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Long): RegistroTerreno?

    @Query("DELETE FROM registros_terreno WHERE id = :id")
    suspend fun borrar(id: Long)
}
