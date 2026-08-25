package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.cockroachdb.CockroachContainer;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class UsuarioAuthRepositoryCockroachIntegrationTest {

    private static final CockroachContainer COCKROACH =
            new CockroachContainer("cockroachdb/cockroach:v24.3.5");

    static {
        COCKROACH.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", COCKROACH::getJdbcUrl);
        registry.add("spring.datasource.username", COCKROACH::getUsername);
        registry.add("spring.datasource.password", COCKROACH::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private UsuarioAuthRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndReadsUsuarioUsingRealCockroachRepository() {
        UUID id = UUID.randomUUID();
        UUID perfilId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setId(id);
        usuario.setPerfilId(perfilId);
        usuario.setUsername("integration-user");
        usuario.setEmail("integration-user@scli.test");
        usuario.setPasswordHash("{noop}integration-password");
        usuario.setActivo(true);
        usuario.setCuentaBloqueada(false);
        usuario.setIntentosFallidos(0);
        usuario.setPasswordActualizadoEn(now);
        usuario.setCreadoEn(now);
        usuario.setActualizadoEn(now);

        repository.saveAndFlush(usuario);
        entityManager.clear();

        UsuarioAuth persisted = repository.findByUsernameIgnoreCase("INTEGRATION-USER")
                .orElseThrow();

        assertEquals(id, persisted.getId());
        assertEquals(perfilId, persisted.getPerfilId());
        assertEquals("integration-user@scli.test", persisted.getEmail());
        assertTrue(persisted.getActivo());
    }
}
