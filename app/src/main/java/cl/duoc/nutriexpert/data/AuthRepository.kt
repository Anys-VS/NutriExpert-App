package cl.duoc.nutriexpert.data

import cl.duoc.nutriexpert.data.remote.Conectividad
import cl.duoc.nutriexpert.data.remote.LoginRequest
import cl.duoc.nutriexpert.data.remote.RegistroUsuarioRequest
import cl.duoc.nutriexpert.data.remote.RetrofitInstance
import cl.duoc.nutriexpert.data.remote.UsuarioDto
import cl.duoc.nutriexpert.data.remote.UsuariosApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class ResultadoRegistro {
    data class Ok(val usuario: Usuario) : ResultadoRegistro()
    data object CorreoExistente : ResultadoRegistro()
    data object SinConexion : ResultadoRegistro()
    data class Error(val mensaje: String) : ResultadoRegistro()
}

sealed class ResultadoLogin {
    // enLinea = false: se validó contra Room porque no hubo respuesta del servidor.
    data class Ok(val usuario: Usuario, val enLinea: Boolean) : ResultadoLogin()
    data object CredencialesInvalidas : ResultadoLogin()
}

// Registro y login contra ms-usuarios, con Room como respaldo:
//  - Registro: necesita conexión. El usuario queda en Room con el id del servidor.
//  - Login con señal: lo valida ms-usuarios y se actualiza la copia en Room.
//  - Login sin señal (o servidor apagado): se valida contra Room, como antes.
class AuthRepository(
    private val dao: UsuarioDao,
    private val api: UsuariosApi,
    private val conectividad: Conectividad
) {

    suspend fun registrar(
        nombre: String,
        correo: String,
        rol: String,
        clave: String
    ): ResultadoRegistro = withContext(Dispatchers.IO) {
        if (!conectividad.hayConexion()) return@withContext ResultadoRegistro.SinConexion
        val correoNorm = correo.trim().lowercase()
        val respuesta = try {
            api.registrar(RegistroUsuarioRequest(nombre.trim(), correoNorm, rol, clave))
        } catch (e: IOException) {
            return@withContext ResultadoRegistro.SinConexion
        }
        val dto = respuesta.body()
        when {
            respuesta.isSuccessful && dto?.id != null ->
                ResultadoRegistro.Ok(guardarCopiaLocal(dto, correoNorm, clave))
            respuesta.code() == 409 -> ResultadoRegistro.CorreoExistente
            else -> ResultadoRegistro.Error(
                RetrofitInstance.mensajeDeError(respuesta.errorBody())
                    ?: "El servidor no pudo crear la cuenta (código ${respuesta.code()})"
            )
        }
    }

    suspend fun login(correo: String, clave: String): ResultadoLogin = withContext(Dispatchers.IO) {
        val correoNorm = correo.trim().lowercase()
        if (conectividad.hayConexion()) {
            try {
                val respuesta = api.login(LoginRequest(correoNorm, clave))
                val dto = respuesta.body()
                if (respuesta.isSuccessful && dto?.id != null) {
                    return@withContext ResultadoLogin.Ok(guardarCopiaLocal(dto, correoNorm, clave), enLinea = true)
                }
                if (respuesta.code() == 401) {
                    return@withContext ResultadoLogin.CredencialesInvalidas
                }
                // Otro error del servidor (500, 503...): se intenta con Room.
            } catch (e: IOException) {
                // Sin respuesta del servidor (lab apagado, mala señal): se intenta con Room.
            }
        }
        loginLocal(correoNorm, clave)
    }

    private suspend fun loginLocal(correo: String, clave: String): ResultadoLogin {
        val usuario = dao.buscarPorCorreo(correo)
            ?: return ResultadoLogin.CredencialesInvalidas
        return if (Contrasenas.verificar(clave, usuario.sal, usuario.hashClave)) {
            ResultadoLogin.Ok(usuario, enLinea = false)
        } else {
            ResultadoLogin.CredencialesInvalidas
        }
    }

    // Guarda (o actualiza) el usuario en Room con el id del servidor y un hash local de la clave,
    // para poder iniciar sesión sin señal la próxima vez.
    private suspend fun guardarCopiaLocal(dto: UsuarioDto, correo: String, clave: String): Usuario {
        val anterior = dao.buscarPorCorreo(correo)
        val sal = Contrasenas.nuevaSal()
        val usuario = Usuario(
            id = dto.id ?: 0,
            nombre = dto.nombre ?: anterior?.nombre.orEmpty(),
            correo = dto.correo ?: correo,
            rol = dto.rol ?: anterior?.rol.orEmpty(),
            hashClave = Contrasenas.aBase64(Contrasenas.hash(clave, sal)),
            sal = Contrasenas.aBase64(sal),
            consentimientoEn = anterior?.consentimientoEn ?: System.currentTimeMillis()
        )
        dao.guardar(usuario)
        return usuario
    }
}
