package cl.duoc.nutriexpert.domain

// Errores del formulario "Editar registro". null = el campo está bien.
data class ErroresEdicion(
    val lote: String? = null,
    val metaRendimiento: String? = null,
    val eficienciaRiego: String? = null
) {
    val hayErrores: Boolean get() = lote != null || metaRendimiento != null || eficienciaRiego != null
}

// Reglas para editar un registro ya guardado. Son funciones puras (sin Android),
// así se prueban con JUnit sin emulador.
object EdicionRegistro {

    // Mismas reglas que el formulario de nuevo registro.
    fun validar(lote: String, metaRendimiento: String, eficienciaRiego: String): ErroresEdicion {
        val meta = aNumero(metaRendimiento)
        val eficiencia = aNumero(eficienciaRiego)
        return ErroresEdicion(
            lote = if (lote.isBlank()) "Indica el lote o cuartel" else null,
            metaRendimiento = when {
                metaRendimiento.isBlank() -> "Campo obligatorio"
                meta == null -> "Ingresa un número válido"
                meta <= 0.0 -> "Debe ser mayor a 0"
                else -> null
            },
            eficienciaRiego = when {
                eficienciaRiego.isBlank() -> "Campo obligatorio"
                eficiencia == null -> "Ingresa un número válido"
                eficiencia < 1.0 || eficiencia > 100.0 -> "Debe estar entre 1 y 100"
                else -> null
            }
        )
    }

    // Recalcula los nutrientes con la nueva meta y eficiencia, para el mismo cultivo.
    // La demanda es proporcional a la meta e inversa a la eficiencia, así que basta con
    // escalar el resultado anterior: da lo mismo que volver a calcular desde la tabla base.
    fun recalcular(
        anterior: Nutrientes,
        metaAnterior: Double,
        eficienciaAnterior: Double,
        metaNueva: Double,
        eficienciaNueva: Double
    ): Nutrientes {
        val factor = (metaNueva / metaAnterior) * (eficienciaAnterior / eficienciaNueva)
        return Nutrientes(
            n = anterior.n * factor,
            p2o5 = anterior.p2o5 * factor,
            k2o = anterior.k2o * factor,
            cao = anterior.cao * factor,
            mgo = anterior.mgo * factor
        )
    }

    // Acepta coma o punto decimal ("12,5" o "12.5").
    fun aNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()
}
