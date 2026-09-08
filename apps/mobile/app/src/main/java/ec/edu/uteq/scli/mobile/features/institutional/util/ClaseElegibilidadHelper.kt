package ec.edu.uteq.scli.mobile.features.institutional.util

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ClaseElegibilidadHelper {
    val ZONA_INSTITUCIONAL: ZoneId = ZoneId.of("America/Guayaquil")

    fun esClaseElegible(
        diaSemana: String,
        horaInicioStr: String,
        horaFinStr: String,
        ahora: ZonedDateTime = ZonedDateTime.now(ZONA_INSTITUCIONAL),
    ): Boolean {
        val diaActual = when (ahora.dayOfWeek) {
            DayOfWeek.MONDAY -> "LUNES"
            DayOfWeek.TUESDAY -> "MARTES"
            DayOfWeek.WEDNESDAY -> "MIERCOLES"
            DayOfWeek.THURSDAY -> "JUEVES"
            DayOfWeek.FRIDAY -> "VIERNES"
            DayOfWeek.SATURDAY -> "SABADO"
            DayOfWeek.SUNDAY -> "DOMINGO"
            null -> ""
        }
        if (!diaSemana.equals(diaActual, ignoreCase = true)) {
            return false
        }

        val horaActual = ahora.toLocalTime()
        val horaInicio = runCatching { parseHora(horaInicioStr) }.getOrNull() ?: return false
        val horaFin = runCatching { parseHora(horaFinStr) }.getOrNull() ?: return false

        return !horaActual.isBefore(horaInicio) && !horaActual.isAfter(horaFin)
    }

    private fun parseHora(horaStr: String): LocalTime {
        val limpia = horaStr.trim()
        return if (limpia.count { it == ':' } == 1) {
            LocalTime.parse(limpia, DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            LocalTime.parse(limpia)
        }
    }
}
