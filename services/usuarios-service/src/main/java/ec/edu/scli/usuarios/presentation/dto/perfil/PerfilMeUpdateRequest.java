package ec.edu.scli.usuarios.presentation.dto.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PerfilMeUpdateRequest(
        @Email(message = "El email personal no tiene un formato válido")
        @Size(max = 150) String emailPersonal,
        @Size(max = 20) String telefono,
        @Size(max = 255) String direccion,
        @Size(max = 500) String fotoUrl
) {
}
