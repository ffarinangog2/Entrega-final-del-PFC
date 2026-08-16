package ec.edu.scli.academico.dto.horario;

import ec.edu.scli.academico.enums.DiaSemana;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HorarioAcademicoResponse(

        UUID id,

        UUID materiaId,

        UUID periodoLectivoId,

        UUID laboratorioId,

        UUID docenteId,

        DiaSemana diaSemana,

        LocalTime horaInicio,

        LocalTime horaFin,

        String paralelo,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
