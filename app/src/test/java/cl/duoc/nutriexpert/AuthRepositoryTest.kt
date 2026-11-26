package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.data.AuthRepository
import cl.duoc.nutriexpert.data.Contrasenas
import cl.duoc.nutriexpert.data.ResultadoLogin
import cl.duoc.nutriexpert.data.ResultadoRegistro
import cl.duoc.nutriexpert.data.Usuario
import cl.duoc.nutriexpert.data.UsuarioDao
import cl.duoc.nutriexpert.data.remote.Conectividad
import cl.duoc.nutriexpert.data.remote.UsuarioDto
import cl.duoc.nutriexpert.data.remote.UsuariosApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

// Se simulan ms-usuarios (api), Room (dao) y el estado de la red (conectividad).
// Datos ficticios: ningún correo ni clave es real.
class AuthRepositoryTest {

    private val dao = mockk<UsuarioDao>(relaxed = true)
    private val api = mockk<UsuariosApi>()
    private val conectividad = mockk<Conectividad>()
    private val repository = AuthRepository(dao, api, conectividad)

    private val correo = "prueba@nutriexpert.cl"
    private val clave = "Clave1234"
    private val dto = UsuarioDto(id = 5, nombre = "Usuario Prueba", correo = correo, rol = "AGRICULTOR")

    private fun error(codigo: Int, cuerpo: String = """{"error":"x"}"""): Response<UsuarioDto> =
        Response.error(codigo, cuerpo.toResponseBody(null))

    // Usuario guardado en Room con la clave en hash, como lo deja un login anterior.
    private fun usuarioLocal(claveGuardada: String): Usuario {
        val sal = Contrasenas.nuevaSal()
        return Usuario(
            id = 5, nombre = "Usuario Prueba", correo = correo, rol = "AGRICULTOR",
            hashClave = Contrasenas.aBase64(Contrasenas.hash(claveGuardada, sal)),
            sal = Contrasenas.aBase64(sal), consentimientoEn = 1_000L
        )
    }

    // ---------- Registro ----------

    @Test
    fun registrarSinConexion_noLlamaAlServidor() = runTest {
        every { conectividad.hayConexion() } returns false

        val resultado = repository.registrar("Usuario Prueba", correo, "AGRICULTOR", clave)

        assertEquals(ResultadoRegistro.SinConexion, resultado)
        coVerify(exactly = 0) { api.registrar(any()) }
    }

    @Test
    fun registrarOk_guardaElUsuarioConElIdDelServidor() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.registrar(any()) } returns Response.success(201, dto)
        coEvery { dao.buscarPorCorreo(correo) } returns null

        val resultado = repository.registrar("Usuario Prueba", " Prueba@NutriExpert.cl ", "AGRICULTOR", clave)

        assertTrue(resultado is ResultadoRegistro.Ok)
        assertEquals(5L, (resultado as ResultadoRegistro.Ok).usuario.id)
        // El correo se envía limpio y en minúsculas.
        coVerify { api.registrar(match { it.correo == correo }) }
        coVerify { dao.guardar(match { it.id == 5L && it.hashClave != clave }) }
    }

    @Test
    fun registrarCorreoRepetido_avisaQueYaExiste() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.registrar(any()) } returns error(409)

        val resultado = repository.registrar("Usuario Prueba", correo, "AGRICULTOR", clave)

        assertEquals(ResultadoRegistro.CorreoExistente, resultado)
    }

    @Test
    fun registrarConServidorApagado_esSinConexion() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.registrar(any()) } throws IOException("servidor apagado")

        assertEquals(
            ResultadoRegistro.SinConexion,
            repository.registrar("Usuario Prueba", correo, "AGRICULTOR", clave)
        )
    }

    @Test
    fun registrarConErrorDelServidor_muestraSuMensaje() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.registrar(any()) } returns error(400, """{"error":"rol inválido"}""")

        val resultado = repository.registrar("Usuario Prueba", correo, "OTRO", clave)

        assertEquals(ResultadoRegistro.Error("rol inválido"), resultado)
    }

    // ---------- Login ----------

    @Test
    fun loginConSenal_loValidaElServidor() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.login(any()) } returns Response.success(dto)
        coEvery { dao.buscarPorCorreo(correo) } returns null

        val resultado = repository.login(correo, clave)

        assertTrue(resultado is ResultadoLogin.Ok)
        assertTrue((resultado as ResultadoLogin.Ok).enLinea)
        coVerify { dao.guardar(any()) } // copia para entrar sin señal la próxima vez
    }

    @Test
    fun loginConClaveIncorrecta_esRechazado() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.login(any()) } returns error(401)

        assertEquals(ResultadoLogin.CredencialesInvalidas, repository.login(correo, "otraClave1"))
    }

    @Test
    fun loginConServidorApagado_usaLaCopiaDeRoom() = runTest {
        every { conectividad.hayConexion() } returns true
        coEvery { api.login(any()) } throws IOException("servidor apagado")
        coEvery { dao.buscarPorCorreo(correo) } returns usuarioLocal(clave)

        val resultado = repository.login(correo, clave)

        assertTrue(resultado is ResultadoLogin.Ok)
        assertFalse((resultado as ResultadoLogin.Ok).enLinea)
    }

    @Test
    fun loginSinSenalConClaveIncorrecta_esRechazado() = runTest {
        every { conectividad.hayConexion() } returns false
        coEvery { dao.buscarPorCorreo(correo) } returns usuarioLocal(clave)

        assertEquals(ResultadoLogin.CredencialesInvalidas, repository.login(correo, "otraClave1"))
        coVerify(exactly = 0) { api.login(any()) }
    }

    @Test
    fun loginSinSenalDeUsuarioDesconocido_esRechazado() = runTest {
        every { conectividad.hayConexion() } returns false
        coEvery { dao.buscarPorCorreo(any()) } returns null

        assertEquals(ResultadoLogin.CredencialesInvalidas, repository.login("nadie@nutriexpert.cl", clave))
    }
}
