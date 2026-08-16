package ec.edu.scli.usuarios.dto.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PerfilUpdateRequest(

        @Size(max = 20)
        String identificacion,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100)
        String apellidos,

        @NotBlank(message = "El email institucional es obligatorio")
        @Email(message = "El email institucional no tiene un formato válido")
        @Size(max = 150)
        String emailInstitucional,

        @Email(message = "El email personal no tiene un formato válido")
        @Size(max = 150)
        String emailPersonal,

        @Size(max = 20)
        String telefono,

        @Size(max = 255)
        String direccion,

        @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual")
        LocalDate fechaNacimiento,

        String fotoUrl

) {
}