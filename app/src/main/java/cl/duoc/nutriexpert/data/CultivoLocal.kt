package cl.duoc.nutriexpert.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import cl.duoc.nutriexpert.domain.CultivoReferencia
import cl.duoc.nutriexpert.domain.Nutrientes

// Copia local del catálogo de ms-catalogo: si no hay señal se calcula con la última lista recibida.
@Entity(tableName = "cultivos")
data class CultivoLocal(
    @PrimaryKey val nombre: String,
    val rendimientoBase: Double,
    val n: Double,
    val p2o5: Double,
    val k2o: Double,
    val cao: Double,
    val mgo: Double
) {
    fun aDominio() = CultivoReferencia(
        nombre = nombre,
        rendimientoBase = rendimientoBase,
        requerimientoBase = Nutrientes(n = n, p2o5 = p2o5, k2o = k2o, cao = cao, mgo = mgo)
    )
}

@Dao
abstract class CultivoDao {
    @Query("SELECT * FROM cultivos ORDER BY nombre")
    abstract suspend fun todos(): List<CultivoLocal>

    @Query("DELETE FROM cultivos")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(cultivos: List<CultivoLocal>)

    // Cambia la copia completa de una vez: nunca queda a medias.
    @Transaction
    open suspend fun reemplazar(cultivos: List<CultivoLocal>) {
        borrarTodos()
        insertar(cultivos)
    }
}
