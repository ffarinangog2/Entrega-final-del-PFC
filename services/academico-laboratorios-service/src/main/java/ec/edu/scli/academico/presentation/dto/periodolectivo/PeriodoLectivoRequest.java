package ec.edu.scli.academico.presentation.dto.periodolectivo;

import ec.edu.scli.academico.enums.EstadoPeriodo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;

public record PeriodoLectivoRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        EstadoPeriodo estado,

        @Size(max = 40) String ppaCodigo,
        @Size(max = 120) String ppaNombre,
        @Min(1) @Max(2) Integer cicloAcademico

) {
    public PeriodoLectivoRequest(String codigo, String nombre, LocalDate fechaInicio,
            LocalDate fechaFin, EstadoPeriodo estado) {
        this(codigo, nombre, fechaInicio, fechaFin, estado, null, null, null);
    }
}
