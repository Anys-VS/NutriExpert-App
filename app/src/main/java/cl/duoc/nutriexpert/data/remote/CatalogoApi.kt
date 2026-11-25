package cl.duoc.nutriexpert.data.remote

import retrofit2.Response
import retrofit2.http.GET

// ms-catalogo (puerto 8081)
interface CatalogoApi {
    @GET("api/cultivos")
    suspend fun listar(): Response<List<CultivoDto>>
}
