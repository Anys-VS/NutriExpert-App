package cl.duoc.nutriexpert.data.remote

// Objetos JSON de los microservicios. Los nombres deben ser EXACTAMENTE los del contrato
// (camelCase): Gson los lee por nombre y uno distinto deja el valor en null.
// Las respuestas usan tipos nullables porque Gson no respeta la nulabilidad de Kotlin.

// ---------- ms-usuarios ----------
data class RegistroUsuarioRequest(
    val nombre: String,
    val correo: String,
    val rol: String,
    val clave: String
)

data class LoginRequest(
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Long?,
    val nombre: String?,
    val correo: String?,
    val rol: String?
)

// ---------- ms-catalogo ----------
data class CultivoDto(
    val id: Long?,
    val nombre: String?,
    val rendimientoBase: Double?,
    val n: Double?,
    val p2o5: Double?,
    val k2o: Double?,
    val cao: Double?,
    val mgo: Double?
)

// ---------- ms-registros ----------
data class RegistroDto(
    val id: Long? = null,
    val idLocal: Long,
    val usuarioId: Long,
    val cultivo: String,
    val lote: String,
    val metaRendimiento: Double,
    val eficienciaRiego: Double,
    val n: Double,
    val p2o5: Double,
    val k2o: Double,
    val cao: Double,
    val mgo: Double,
    val creadoEn: Long
)

// Cuerpo de todos los errores: { "error": "mensaje" }
data class ErrorDto(val error: String?)
