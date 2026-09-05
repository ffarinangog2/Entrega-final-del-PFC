package ec.edu.scli.academico.presentation.dto.periodolectivo;

import ec.edu.scli.academico.enums.EstadoPeriodo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PeriodoLectivoResponse(

        UUID id,

        String codigo,

        String nombre,

        LocalDate fechaInicio,

        LocalDate fechaFin,

        EstadoPeriodo estado,

        String ppaCodigo,

        String ppaNombre,

        Integer cicloAcademico,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
    public PeriodoLectivoResponse(UUID id, String codigo, String nombre, LocalDate fechaInicio,
            LocalDate fechaFin, EstadoPeriodo estado, OffsetDateTime creadoEn, OffsetDateTime actualizadoEn) {
        this(id, codigo, nombre, fechaInicio, fechaFin, estado, null, null, null, creadoEn, actualizadoEn);
    }
}
