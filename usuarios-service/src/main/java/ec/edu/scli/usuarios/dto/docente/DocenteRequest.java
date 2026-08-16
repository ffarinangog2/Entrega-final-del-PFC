package ec.edu.scli.usuarios.dto.docente;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DocenteRequest(

        @NotNull(message = "El perfilId es obligatorio")
        UUID perfilId,

        @Size(max = 30, message = "El código docente no puede superar 30 caracteres")
        String codigoDocente,

        @Size(max = 100, message = "El título académico no puede superar 100 caracteres")
        String tituloAcademico,

        @Size(max = 100, message = "El departamento no puede superar 100 caracteres")
        String departamento,

        @Size(max = 30, message = "El tipo de contrato no puede superar 30 caracteres")
        String tipoContrato,

        @Size(max = 30, message = "La dedicación no puede superar 30 caracteres")
        String dedicacion,

        Boolean activo

) {
}