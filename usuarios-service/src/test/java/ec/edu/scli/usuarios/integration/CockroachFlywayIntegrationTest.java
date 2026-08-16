package ec.edu.scli.usuarios.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cockroachdb.CockroachContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CockroachFlywayIntegrationTest {

    private static final CockroachContainer COCKROACH =
            new CockroachContainer("cockroachdb/cockroach:v24.3.5");

    static {
        COCKROACH.start();
    }

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", COCKROACH::getJdbcUrl);
        registry.add("spring.datasource.username", COCKROACH::getUsername);
        registry.add("spring.datasource.password", COCKROACH::getPassword);

        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void debeLevantarCockroachYAplicarMigracionesFlyway() {

        assertTrue(COCKROACH.isRunning());

        Integer cantidadPerfiles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perfiles",
                Integer.class
        );

        assertNotNull(cantidadPerfiles);
        assertEquals(11, cantidadPerfiles);
    }

    @Test
    void debeExistirAdministradorDelSistema() {

        Integer cantidadAdministradores = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM perfiles
                WHERE id = 'a0000000-0000-0000-0000-000000000001'
                """,
                Integer.class
        );

        assertNotNull(cantidadAdministradores);
        assertEquals(1, cantidadAdministradores);
    }
}