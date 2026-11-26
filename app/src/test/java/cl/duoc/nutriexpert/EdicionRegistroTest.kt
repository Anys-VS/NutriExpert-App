package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.domain.CalculoNutricional
import cl.duoc.nutriexpert.domain.EdicionRegistro
import cl.duoc.nutriexpert.domain.DatosEjemplo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdicionRegistroTest {

    @Test
    fun datosCorrectos_noHayErrores() {
        val errores = EdicionRegistro.validar(lote = "L-02", metaRendimiento = "14,5", eficienciaRiego = "85")
        assertFalse(errores.hayErrores)
    }

    @Test
    fun loteVacio_pideElLote() {
        val errores = EdicionRegistro.validar(lote = "  ", metaRendimiento = "14", eficienciaRiego = "85")
        assertEquals("Indica el lote o cuartel", errores.lote)
        assertNull(errores.metaRendimiento)
    }

    @Test
    fun metaEnCeroOTexto_esInvalida() {
        assertEquals("Debe ser mayor a 0", EdicionRegistro.validar("L-01", "0", "85").metaRendimiento)
        assertEquals("Ingresa un número válido", EdicionRegistro.validar("L-01", "doce", "85").metaRendimiento)
    }

    @Test
    fun eficienciaFueraDeRango_esInvalida() {
        assertEquals("Debe estar entre 1 y 100", EdicionRegistro.validar("L-01", "12", "150").eficienciaRiego)
        assertEquals("Debe estar entre 1 y 100", EdicionRegistro.validar("L-01", "12", "0,5").eficienciaRiego)
        assertTrue(EdicionRegistro.validar("L-01", "12", "").hayErrores)
    }

    @Test
    fun recalcular_daLoMismoQueCalcularDesdeLaTablaBase() {
        val palto = DatosEjemplo.buscar("Palto")!!
        val antes = CalculoNutricional.calcular(palto, metaRendimiento = 12.0, eficienciaRiego = 85.0)

        val recalculado = EdicionRegistro.recalcular(antes, 12.0, 85.0, metaNueva = 14.0, eficienciaNueva = 90.0)
        val desdeCero = CalculoNutricional.calcular(palto, metaRendimiento = 14.0, eficienciaRiego = 90.0)

        assertEquals(desdeCero.n, recalculado.n, 0.001)
        assertEquals(desdeCero.k2o, recalculado.k2o, 0.001)
        assertEquals(desdeCero.mgo, recalculado.mgo, 0.001)
    }

    @Test
    fun aNumero_aceptaComaYPunto() {
        assertEquals(12.5, EdicionRegistro.aNumero("12,5")!!, 0.0)
        assertEquals(12.5, EdicionRegistro.aNumero(" 12.5 ")!!, 0.0)
        assertNull(EdicionRegistro.aNumero("abc"))
    }
}
