package cl.duoc.nutriexpert

import android.app.Application
import cl.duoc.nutriexpert.data.AuthRepository
import cl.duoc.nutriexpert.data.ResultadoLogin
import cl.duoc.nutriexpert.data.ResultadoRegistro
import cl.duoc.nutriexpert.data.Usuario
import cl.duoc.nutriexpert.model.LoginUiState
import cl.duoc.nutriexpert.viewmodels.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Se prueba la validación de los formularios y qué muestra la pantalla según lo que
// responda el repositorio (simulado). Datos ficticios.
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<AuthRepository>()
    private lateinit var viewModel: AuthViewModel

    private val usuario = Usuario(
        id = 5, nombre = "Usuario Prueba", correo = "prueba@nutriexpert.cl", rol = "AGRICULTOR",
        hashClave = "hash", sal = "sal", consentimientoEn = 1_000L
    )

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        viewModel = AuthViewModel(mockk<Application>(relaxed = true), repo)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun llenarLogin(correo: String = "prueba@nutriexpert.cl", clave: String = "Clave1234") {
        viewModel.onLoginCorreoChange(correo)
        viewModel.onLoginClaveChange(clave)
    }

    private fun llenarRegistro(
        nombre: String = "Usuario Prueba",
        correo: String = "prueba@nutriexpert.cl",
        rol: String? = "AGRICULTOR",
        clave: String = "Clave1234",
        confirmar: String = "Clave1234",
        consentimiento: Boolean = true
    ) {
        viewModel.onRegistroNombreChange(nombre)
        viewModel.onRegistroCorreoChange(correo)
        rol?.let { viewModel.onRegistroRolChange(it) }
        viewModel.onRegistroClaveChange(clave)
        viewModel.onRegistroConfirmarChange(confirmar)
        viewModel.onRegistroConsentimientoChange(consentimiento)
    }

    // ---------- Login ----------

    @Test
    fun loginVacio_muestraErroresYNoLlamaAlRepositorio() = runTest(dispatcher) {
        viewModel.iniciarSesion {}
        advanceUntilIdle()

        assertEquals("Ingresa tu correo", viewModel.login.value.errorCorreo)
        assertEquals("Ingresa tu contraseña", viewModel.login.value.errorClave)
        coVerify(exactly = 0) { repo.login(any(), any()) }
    }

    @Test
    fun correoMalEscrito_esInvalido() {
        llenarLogin(correo = "prueba@nutriexpert")
        assertFalse(viewModel.validarLogin())
        assertEquals("Correo no válido", viewModel.login.value.errorCorreo)
    }

    @Test
    fun alEscribir_seBorraElErrorDelCampo() {
        viewModel.validarLogin()
        viewModel.onLoginCorreoChange("p")
        assertNull(viewModel.login.value.errorCorreo)
    }

    @Test
    fun loginCorrecto_abreLaSesionYAvanza() = runTest(dispatcher) {
        coEvery { repo.login(any(), any()) } returns ResultadoLogin.Ok(usuario, enLinea = true)
        llenarLogin()
        var avanzo = false

        viewModel.iniciarSesion { avanzo = true }
        assertTrue(viewModel.login.value.cargando)
        advanceUntilIdle()

        assertTrue(avanzo)
        assertEquals(5L, viewModel.sesion.value?.id)
        assertEquals(LoginUiState(), viewModel.login.value) // formulario limpio
    }

    @Test
    fun loginIncorrecto_muestraMensajeGenericoYBorraLaClave() = runTest(dispatcher) {
        coEvery { repo.login(any(), any()) } returns ResultadoLogin.CredencialesInvalidas
        llenarLogin()

        viewModel.iniciarSesion {}
        advanceUntilIdle()

        val estado = viewModel.login.value
        assertEquals("Correo o contraseña incorrectos", estado.errorGeneral)
        assertEquals("", estado.clave)
        assertFalse(estado.cargando)
        assertNull(viewModel.sesion.value)
    }

    @Test
    fun loginConFallaInesperada_noSeCae() = runTest(dispatcher) {
        coEvery { repo.login(any(), any()) } throws IllegalStateException("falla")
        llenarLogin()

        viewModel.iniciarSesion {}
        advanceUntilIdle()

        assertEquals("No se pudo iniciar sesión. Intenta de nuevo.", viewModel.login.value.errorGeneral)
        viewModel.onLoginErrorMostrado()
        assertNull(viewModel.login.value.errorGeneral)
    }

    // ---------- Registro ----------

    @Test
    fun registroVacio_marcaTodosLosCampos() {
        assertFalse(viewModel.validarRegistro())
        val estado = viewModel.registro.value
        assertEquals("Ingresa tu nombre (mínimo 2 letras)", estado.errorNombre)
        assertEquals("Ingresa tu correo", estado.errorCorreo)
        assertEquals("Elige tu perfil", estado.errorRol)
        assertEquals("Mínimo 8 caracteres", estado.errorClave)
        assertEquals("Repite tu contraseña", estado.errorConfirmar)
        assertEquals("Debes aceptar para crear tu cuenta", estado.errorConsentimiento)
    }

    @Test
    fun claveSinNumeros_yConfirmacionDistinta_sonInvalidas() {
        llenarRegistro(clave = "SoloLetras", confirmar = "OtraCosa1")
        assertFalse(viewModel.validarRegistro())
        assertEquals("Debe tener letras y números", viewModel.registro.value.errorClave)
        assertEquals("Las contraseñas no coinciden", viewModel.registro.value.errorConfirmar)
    }

    @Test
    fun registroCorrecto_abreLaSesion() = runTest(dispatcher) {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns ResultadoRegistro.Ok(usuario)
        llenarRegistro()
        var avanzo = false

        viewModel.registrar { avanzo = true }
        advanceUntilIdle()

        assertTrue(avanzo)
        assertEquals("Usuario Prueba", viewModel.sesion.value?.nombre)
    }

    @Test
    fun registroConCorreoRepetido_loMarcaEnElCampo() = runTest(dispatcher) {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns ResultadoRegistro.CorreoExistente
        llenarRegistro()

        viewModel.registrar {}
        advanceUntilIdle()

        assertEquals("Este correo ya está registrado", viewModel.registro.value.errorCorreo)
    }

    @Test
    fun registroSinConexion_avisaQueNecesitaInternet() = runTest(dispatcher) {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns ResultadoRegistro.SinConexion
        llenarRegistro()

        viewModel.registrar {}
        advanceUntilIdle()

        assertEquals("Para crear tu cuenta necesitas conexión a internet.", viewModel.registro.value.errorGeneral)
    }

    @Test
    fun registroConErrorDelServidor_muestraSuMensaje() = runTest(dispatcher) {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns ResultadoRegistro.Error("rol inválido")
        llenarRegistro()

        viewModel.registrar {}
        advanceUntilIdle()

        assertEquals("rol inválido", viewModel.registro.value.errorGeneral)
        viewModel.onRegistroErrorMostrado()
        assertNull(viewModel.registro.value.errorGeneral)
    }

    @Test
    fun registroConFallaInesperada_noSeCae() = runTest(dispatcher) {
        coEvery { repo.registrar(any(), any(), any(), any()) } throws IllegalStateException("falla")
        llenarRegistro()

        viewModel.registrar {}
        advanceUntilIdle()

        assertEquals("No se pudo crear la cuenta. Intenta de nuevo.", viewModel.registro.value.errorGeneral)
        assertFalse(viewModel.registro.value.cargando)
    }

    @Test
    fun cerrarSesion_quitaLaSesion() = runTest(dispatcher) {
        coEvery { repo.login(any(), any()) } returns ResultadoLogin.Ok(usuario, enLinea = false)
        llenarLogin()
        viewModel.iniciarSesion {}
        advanceUntilIdle()

        viewModel.cerrarSesion()

        assertNull(viewModel.sesion.value)
    }
}
