package ec.edu.scli.usuarios.presentation.dto.usuarios;

public record AuthUsuarioUpdateRequest(String username, String email, String rol, boolean activo) {
}
