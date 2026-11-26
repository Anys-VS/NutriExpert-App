package cl.duoc.nutriexpert.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Usuario::class, RegistroTerreno::class, CultivoLocal::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun registroTerrenoDao(): RegistroTerrenoDao
    abstract fun cultivoDao(): CultivoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriexpert.db"
                )
                    // v2 agrega los registros; v3 los ids del servidor y la copia del catálogo.
                    // En el MVP solo hay datos ficticios, así que al cambiar de versión se borra
                    // la base en vez de migrarla (los usuarios deben volver a iniciar sesión con señal).
                    .fallbackToDestructiveMigration(true)
                    .build().also { instancia = it }
            }
        }
    }
}