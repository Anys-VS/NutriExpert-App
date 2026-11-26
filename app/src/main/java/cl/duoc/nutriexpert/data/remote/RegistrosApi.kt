package cl.duoc.nutriexpert.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// ms-registros (puerto 8082)
interface RegistrosApi {
    // 201 nuevo · 200 si ya existía (mismo usuarioId + idLocal) · 400 datos inválidos
    @POST("api/registros")
    suspend fun guardar(@Body body: RegistroDto): Response<RegistroDto>

    // 200 actualizado · 400 datos inválidos · 404 no existe
    @PUT("api/registros/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body body: RegistroDto): Response<RegistroDto>

    // 204 borrado · 404 no existe
    @DELETE("api/registros/{id}")
    suspend fun borrar(@Path("id") id: Long): Response<Unit>
}
