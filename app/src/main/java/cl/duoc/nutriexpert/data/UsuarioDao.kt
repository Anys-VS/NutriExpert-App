package cl.duoc.nutriexpert.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    // Copia local del usuario que entregó ms-usuarios (mismo id). REPLACE la actualiza
    // en cada login con señal, así el login sin señal usa la clave más reciente.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): Usuario?
}