package ec.edu.scli.academico.dto.periodolectivo;

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

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
