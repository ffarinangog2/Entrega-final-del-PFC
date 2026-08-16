package ec.edu.scli.usuarios.dto.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PerfilCreateRequest(

        @Size(max = 20, message = "La identificación no puede superar 20 caracteres")
        String identificacion,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
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