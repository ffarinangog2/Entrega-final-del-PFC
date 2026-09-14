package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinacionStateTest {
    @Test
    fun `resume estados reales con etiquetas de coordinacion`() {
        assertEquals("SIN INICIAR", estadoGeneral(emptyList()))
        assertEquals("BORRADOR", estadoGeneral(listOf(plan("BORRADOR"))))
        assertEquals("EN REVISIÓN", estadoGeneral(listOf(plan("ENVIADA"))))
        assertEquals("APROBADA", estadoGeneral(listOf(plan("CONFIRMADA"))))
        assertEquals("DEVUELTA CON OBSERVACIONES", estadoGeneral(listOf(plan("PROPUESTA_CAMBIO"))))
        assertEquals("Rechazada", etiquetaEstado("RECHAZADA"))
    }

    private fun plan(estado: String) = PlanificacionDto(
        "plan", "periodo", "carrera", "materia", "docente", "laboratorio",
        "LUNES", "07:30", "09:30", estado, null,
    )
}
