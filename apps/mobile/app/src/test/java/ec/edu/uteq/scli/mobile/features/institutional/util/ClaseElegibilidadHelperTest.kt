package ec.edu.uteq.scli.mobile.features.institutional.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class ClaseElegibilidadHelperTest {

    // Lunes 7 de Septiembre 2026
    private val fechaLunes = LocalDate.of(2026, 9, 7)

    @Test
    fun `mismo dia dentro del horario resulta elegible`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(9, 30), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "LUNES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertTrue(elegible)
    }

    @Test
    fun `dia diferente no es elegible aunque coincida la hora`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(9, 30), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "MARTES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertFalse(elegible)
    }

    @Test
    fun `antes de horaInicio no es elegible`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(7, 59, 59), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "LUNES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertFalse(elegible)
    }

    @Test
    fun `despues de horaFin no es elegible`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(10, 0, 1), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "LUNES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertFalse(elegible)
    }

    @Test
    fun `exactamente en limite horaInicio es elegible`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(8, 0, 0), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "LUNES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertTrue(elegible)
    }

    @Test
    fun `exactamente en limite horaFin es elegible`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(10, 0, 0), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        val elegible = ClaseElegibilidadHelper.esClaseElegible(
            diaSemana = "LUNES",
            horaInicioStr = "08:00",
            horaFinStr = "10:00",
            ahora = ahora,
        )
        assertTrue(elegible)
    }

    @Test
    fun `soporta formatos con segundos y maneja entradas invalidas con seguridad`() {
        val ahora = ZonedDateTime.of(fechaLunes, LocalTime.of(8, 30, 0), ClaseElegibilidadHelper.ZONA_INSTITUCIONAL)
        assertTrue(ClaseElegibilidadHelper.esClaseElegible("LUNES", "08:00:00", "10:00:00", ahora))
        assertFalse(ClaseElegibilidadHelper.esClaseElegible("LUNES", "invalido", "10:00", ahora))
    }
}
