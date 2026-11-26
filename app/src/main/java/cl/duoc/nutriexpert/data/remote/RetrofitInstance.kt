package cl.duoc.nutriexpert.data.remote

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // Dónde corren los microservicios:
    //  - Emulador con los servicios en tu computador: "10.0.2.2"
    //  - EC2 de AWS Academy: la Elastic IP (agrégala también en res/xml/network_security_config.xml)
    const val HOST = "10.0.2.2"

    private const val URL_CATALOGO = "http://$HOST:8081/"
    private const val URL_REGISTROS = "http://$HOST:8082/"
    private const val URL_USUARIOS = "http://$HOST:8083/"

    // API externa (no es nuestra): clima de Open-Meteo, por HTTPS y sin clave.
    private const val URL_CLIMA = "https://api.open-meteo.com/"

    private val gson = Gson()

    // Tiempos cortos: si no hay señal o el servidor está apagado, la app cae rápido a Room.
    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                // BASIC: método, URL y código en Logcat. No registra cuerpos (ahí viaja la clave).
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private fun crear(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val catalogoApi: CatalogoApi by lazy { crear(URL_CATALOGO).create(CatalogoApi::class.java) }
    val registrosApi: RegistrosApi by lazy { crear(URL_REGISTROS).create(RegistrosApi::class.java) }
    val usuariosApi: UsuariosApi by lazy { crear(URL_USUARIOS).create(UsuariosApi::class.java) }
    val climaApi: ClimaApiService by lazy { crear(URL_CLIMA).create(ClimaApiService::class.java) }

    // Lee { "error": "..." } del cuerpo de una respuesta fallida.
    fun mensajeDeError(cuerpo: ResponseBody?): String? = try {
        cuerpo?.string()?.let { gson.fromJson(it, ErrorDto::class.java)?.error }
    } catch (e: Exception) {
        null
    }
}
