package ec.edu.uteq.scli.auth_service.domain.repository;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para consultar usuarios autenticables. */
public interface UsuarioRepository {
    Optional<Usuario> buscarConRolesPorIdentificador(String identificador);
    Optional<Usuario> buscarConRolesPorId(UUID id);
}
