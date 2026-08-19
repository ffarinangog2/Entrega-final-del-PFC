package ec.edu.scli.academico.dto.horario;

import ec.edu.scli.academico.enums.DiaSemana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioAcademicoRequest(

        @NotNull(message = "La materia es obligatoria")
        UUID materiaId,

        @NotNull(message = "El periodo lectivo es obligatorio")
        UUID periodoLectivoId,

        UUID laboratorioId,

        @NotNull(message = "El docente es obligatorio")
        UUID docenteId,

        @NotNull(message = "El día de la semana es obligatorio")
        DiaSemana diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotBlank(message = "El paralelo es obligatorio")
        @Size(max = 10, message = "El paralelo no puede superar 10 caracteres")
        String paralelo

) {
}
