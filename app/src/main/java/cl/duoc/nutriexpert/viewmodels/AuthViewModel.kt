package cl.duoc.nutriexpert.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nutriexpert.data.AppDatabase
import cl.duoc.nutriexpert.data.AuthRepository
import cl.duoc.nutriexpert.data.ResultadoLogin
import cl.duoc.nutriexpert.data.ResultadoRegistro
import cl.duoc.nutriexpert.data.Usuario
import cl.duoc.nutriexpert.data.remote.Conectividad
import cl.duoc.nutriexpert.data.remote.RetrofitInstance
import cl.duoc.nutriexpert.model.LoginUiState
import cl.duoc.nutriexpert.model.RegistroUiState
import cl.duoc.nutriexpert.model.SesionUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// El repositorio llega por constructor con un valor por defecto: la app usa el real
// (viewModel() sigue funcionando gracias a @JvmOverloads) y las pruebas pasan un mock.
class AuthViewModel @JvmOverloads constructor(
    application: Application,
    // Login y registro contra ms-usuarios, con Room como respaldo sin señal.
    private val repo: AuthRepository = AuthRepository(
        dao = AppDatabase.obtener(application).usuarioDao(),
        api = RetrofitInstance.usuariosApi,
        conectividad = Conectividad(application)
    )
) : AndroidViewModel(application) {

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _registro = MutableStateFlow(RegistroUiState())
    val registro: StateFlow<RegistroUiState> = _registro.asStateFlow()

    private val _sesion = MutableStateFlow<SesionUsuario?>(null)
    val sesion: StateFlow<SesionUsuario?> = _sesion.asStateFlow()

    // ---------- Login ----------

    fun onLoginCorreoChange(valor: String) =
        _login.update { it.copy(correo = valor, errorCorreo = null, errorGeneral = null) }

    fun onLoginClaveChange(valor: String) =
        _login.update { it.copy(clave = valor, errorClave = null, errorGeneral = null) }

    fun onLoginErrorMostrado() = _login.update { it.copy(errorGeneral = null) }

    fun validarLogin(): Boolean {
        val formulario = _login.value
        val errorCorreo = validarCorreo(formulario.correo)
        val errorClave = if (formulario.clave.isBlank()) "Ingresa tu contraseña" else null
        _login.update { it.copy(errorCorreo = errorCorreo, errorClave = errorClave) }
        return errorCorreo == null && errorClave == null
    }

    fun iniciarSesion(onExito: () -> Unit) {
        if (_login.value.cargando || !validarLogin()) return
        _login.update { it.copy(cargando = true, errorGeneral = null) }
        viewModelScope.launch {
            val formulario = _login.value
            val resultado = try {
                repo.login(formulario.correo, formulario.clave)
            } catch (e: Exception) {
                _login.update {
                    it.copy(cargando = false, errorGeneral = "No se pudo iniciar sesión. Intenta de nuevo.")
                }
                return@launch
            }
            when (resultado) {
                is ResultadoLogin.Ok -> {
                    abrirSesion(resultado.usuario)
                    _login.value = LoginUiState()
                    onExito()
                }
                // Mensaje genérico: no revela si el correo existe.
                ResultadoLogin.CredencialesInvalidas -> _login.update {
                    it.copy(cargando = false, clave = "", errorGeneral = "Correo o contraseña incorrectos")
                }
            }
        }
    }

    // ---------- Registro ----------

    fun onRegistroNombreChange(valor: String) =
        _registro.update { it.copy(nombre = valor, errorNombre = null) }

    fun onRegistroCorreoChange(valor: String) =
        _registro.update { it.copy(correo = valor, errorCorreo = null) }

    fun onRegistroRolChange(valor: String) =
        _registro.update { it.copy(rol = valor, errorRol = null) }

    fun onRegistroClaveChange(valor: String) =
        _registro.update { it.copy(clave = valor, errorClave = null) }

    fun onRegistroConfirmarChange(valor: String) =
        _registro.update { it.copy(confirmarClave = valor, errorConfirmar = null) }

    fun onRegistroConsentimientoChange(valor: Boolean) =
        _registro.update { it.copy(consentimiento = valor, errorConsentimiento = null) }

    fun onRegistroErrorMostrado() = _registro.update { it.copy(errorGeneral = null) }

    fun validarRegistro(): Boolean {
        val formulario = _registro.value
        val errorNombre = if (formulario.nombre.trim().length < 2) "Ingresa tu nombre (mínimo 2 letras)" else null
        val errorCorreo = validarCorreo(formulario.correo)
        val errorRol = if (formulario.rol == null) "Elige tu perfil" else null
        val errorClave = when {
            formulario.clave.length < 8 -> "Mínimo 8 caracteres"
            formulario.clave.none { it.isLetter() } || formulario.clave.none { it.isDigit() } -> "Debe tener letras y números"
            else -> null
        }
        val errorConfirmar = when {
            formulario.confirmarClave.isEmpty() -> "Repite tu contraseña"
            formulario.confirmarClave != formulario.clave -> "Las contraseñas no coinciden"
            else -> null
        }
        val errorConsentimiento =
            if (!formulario.consentimiento) "Debes aceptar para crear tu cuenta" else null

        _registro.update {
            it.copy(
                errorNombre = errorNombre,
                errorCorreo = errorCorreo,
                errorRol = errorRol,
                errorClave = errorClave,
                errorConfirmar = errorConfirmar,
                errorConsentimiento = errorConsentimiento
            )
        }
        return listOf(
            errorNombre, errorCorreo, errorRol, errorClave, errorConfirmar, errorConsentimiento
        ).all { it == null }
    }

    fun registrar(onExito: () -> Unit) {
        if (_registro.value.cargando || !validarRegistro()) return
        _registro.update { it.copy(cargando = true, errorGeneral = null) }
        viewModelScope.launch {
            val formulario = _registro.value
            try {
                when (val resultado = repo.registrar(formulario.nombre, formulario.correo, formulario.rol!!, formulario.clave)) {
                    is ResultadoRegistro.Ok -> {
                        // Cuenta creada en ms-usuarios: se inicia sesión de inmediato.
                        abrirSesion(resultado.usuario)
                        _registro.value = RegistroUiState()
                        onExito()
                    }
                    ResultadoRegistro.CorreoExistente -> _registro.update {
                        it.copy(cargando = false, errorCorreo = "Este correo ya está registrado")
                    }
                    ResultadoRegistro.SinConexion -> _registro.update {
                        it.copy(
                            cargando = false,
                            errorGeneral = "Para crear tu cuenta necesitas conexión a internet."
                        )
                    }
                    is ResultadoRegistro.Error -> _registro.update {
                        it.copy(cargando = false, errorGeneral = resultado.mensaje)
                    }
                }
            } catch (e: Exception) {
                _registro.update {
                    it.copy(cargando = false, errorGeneral = "No se pudo crear la cuenta. Intenta de nuevo.")
                }
            }
        }
    }

    // ---------- Sesión ----------

    fun cerrarSesion() {
        _sesion.value = null
    }

    private fun abrirSesion(usuario: Usuario) {
        _sesion.value = SesionUsuario(
            id = usuario.id,
            nombre = usuario.nombre,
            correo = usuario.correo,
            rol = usuario.rol
        )
    }

    private fun validarCorreo(correo: String): String? = when {
        correo.isBlank() -> "Ingresa tu correo"
        !FORMATO_CORREO.matches(correo.trim()) -> "Correo no válido"
        else -> null
    }

    private companion object {
        // usuario@dominio.cl — se usa Regex de Kotlin (y no Patterns de Android) para poder probarlo en la JVM.
        val FORMATO_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
