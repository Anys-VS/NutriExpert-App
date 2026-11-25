package cl.duoc.nutriexpert.data

import android.content.Context
import cl.duoc.nutriexpert.data.model.ClimaRespuesta
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Último clima descargado y la hora en que se descargó.
data class ClimaGuardado(
    val clima: ClimaRespuesta,
    val guardadoEn: Long
)

// Guarda el último clima en SharedPreferences como texto JSON (con Gson).
// Así, si se pierde la señal, el Home puede mostrar los datos del día.
class ClimaCache(context: Context) {

    private val prefs = context.getSharedPreferences("clima_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun guardar(clima: ClimaRespuesta) {
        prefs.edit()
            .putString("clima", gson.toJson(clima))
            .putLong("guardadoEn", System.currentTimeMillis())
            .apply()
    }

    // Devuelve lo guardado solo si es de hoy. El clima de otro día no sirve para regar.
    fun leerDeHoy(): ClimaGuardado? {
        val json = prefs.getString("clima", null) ?: return null
        val guardadoEn = prefs.getLong("guardadoEn", 0L)
        if (dia(guardadoEn) != dia(System.currentTimeMillis())) return null
        return ClimaGuardado(gson.fromJson(json, ClimaRespuesta::class.java), guardadoEn)
    }

    private fun dia(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
}
