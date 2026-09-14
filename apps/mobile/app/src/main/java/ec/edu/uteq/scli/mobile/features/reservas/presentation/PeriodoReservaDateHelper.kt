package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

sealed interface PeriodoReservaRango {
    data class Valido(
        val fechaMinima: LocalDate,
        val fechaMaxima: LocalDate,
    ) : PeriodoReservaRango

    sealed interface Invalido : PeriodoReservaRango {
        val mensaje: String
    }

    object SinPeriodo : Invalido {
        override val mensaje: String = "No hay un período lectivo activo para realizar reservas."
    }

    object FechasNoDeterminadas : Invalido {
        override val mensaje: String = "No se pudo determinar el rango de fechas del período lectivo."
    }

    object PeriodoVencido : Invalido {
        override val mensaje: String = "El período lectivo activo ha finalizado o no permite nuevas reservas."
    }
}

fun parsearFechaSegura(fechaStr: String?): LocalDate? {
    if (fechaStr.isNullOrBlank()) return null
    return try {
        LocalDate.parse(fechaStr.trim())
    } catch (_: Exception) {
        null
    }
}

fun utcMillisToLocalDate(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}

fun localDateToUtcMillis(localDate: LocalDate): Long {
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun evaluarRangoPeriodo(
    periodo: PeriodoDto?,
    hoy: LocalDate = LocalDate.now(ZoneId.systemDefault()),
): PeriodoReservaRango {
    if (periodo == null) {
        return PeriodoReservaRango.SinPeriodo
    }
    val inicioPeriodo = parsearFechaSegura(periodo.fechaInicio)
    val finPeriodo = parsearFechaSegura(periodo.fechaFin)
    if (inicioPeriodo == null || finPeriodo == null) {
        return PeriodoReservaRango.FechasNoDeterminadas
    }
    if (finPeriodo.isBefore(hoy)) {
        return PeriodoReservaRango.PeriodoVencido
    }
    val fechaMinima = if (inicioPeriodo.isAfter(hoy)) inicioPeriodo else hoy
    val fechaMaxima = finPeriodo
    if (fechaMinima.isAfter(fechaMaxima)) {
        return PeriodoReservaRango.PeriodoVencido
    }
    return PeriodoReservaRango.Valido(
        fechaMinima = fechaMinima,
        fechaMaxima = fechaMaxima,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
class ReservaSelectableDates(
    val fechaMinima: LocalDate,
    val fechaMaxima: LocalDate,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = utcMillisToLocalDate(utcTimeMillis)
        return !date.isBefore(fechaMinima) && !date.isAfter(fechaMaxima)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year in fechaMinima.year..fechaMaxima.year
    }
}
