package ec.edu.uteq.scli.auth_service.dto;

import java.util.List;
import java.util.UUID;

public record PerfilAuthResponse(
        UUID id,
        String nombres,
        String apellidos,
        String emailInstitucional,
        Boolean activo,
        List<String> tiposPerfil) {
}