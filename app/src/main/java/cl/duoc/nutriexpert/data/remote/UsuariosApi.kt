package cl.duoc.nutriexpert.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// ms-usuarios (puerto 8083)
interface UsuariosApi {
    // 201 + usuario · 409 correo existente · 400 datos inválidos
    @POST("api/usuarios/registro")
    suspend fun registrar(@Body body: RegistroUsuarioRequest): Response<UsuarioDto>

    // 200 + usuario · 401 "Correo o clave incorrectos"
    @POST("api/usuarios/login")
    suspend fun login(@Body body: LoginRequest): Response<UsuarioDto>
}
