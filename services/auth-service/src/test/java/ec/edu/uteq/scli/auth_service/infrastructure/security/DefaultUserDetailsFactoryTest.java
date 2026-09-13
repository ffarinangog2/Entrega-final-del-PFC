package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultUserDetailsFactoryTest {

    @Test
    void creaUserDetailsConElUsuarioRecibido() {
        Usuario usuario = new Usuario(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "admin",
                "admin@scli.local",
                "hash",
                true,
                false,
                Set.of());

        CustomUserDetails result = new DefaultUserDetailsFactory().crear(usuario);

        assertNotNull(result);
        assertEquals(usuario.id(), result.getUsuarioId());
        assertEquals(usuario.username(), result.getUsername());
    }
}
