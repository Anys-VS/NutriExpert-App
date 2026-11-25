package cl.duoc.nutriexpert.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Avisa si el teléfono tiene red. Con iniciar() escucha los cambios: así la app
// sincroniza sola cuando vuelve la señal.
class Conectividad(context: Context) {

    private val cm: ConnectivityManager? =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _conectado = MutableStateFlow(hayConexion())
    val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    private var escuchando = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _conectado.value = true
        }

        override fun onLost(network: Network) {
            _conectado.value = false
        }
    }

    fun hayConexion(): Boolean {
        val manager = cm ?: return false
        val red = manager.activeNetwork ?: return false
        val capacidades = manager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun iniciar() {
        val manager = cm ?: return
        if (escuchando) return
        manager.registerDefaultNetworkCallback(callback)
        escuchando = true
    }

    fun detener() {
        if (!escuchando) return
        runCatching { cm?.unregisterNetworkCallback(callback) }
        escuchando = false
    }
}
