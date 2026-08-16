package ec.edu.scli.reservas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class CockroachFlywayIntegrationTests {

    @Container
    static final GenericContainer<?> COCKROACH =
            new GenericContainer<>(DockerImageName.parse("cockroachdb/cockroach:v24.3.5"))
                    .withCommand("start-single-node", "--insecure")
                    .withExposedPorts(26257);

    @DynamicPropertySource
    static void configurarAplicacion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://" + COCKROACH.getHost() + ":"
                        + COCKROACH.getMappedPort(26257)
                        + "/defaultdb?sslmode=disable");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
        registry.add("security.jwt.secret", () ->
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreaTodasLasTablasDelMicroservicio() {
        Integer tablas = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'solicitudes_reserva',
                    'reservas',
                    'historial_solicitudes',
                    'bloqueos_agenda',
                    'configuraciones_reserva'
                  )
                """,
                Integer.class);

        assertThat(tablas).isEqualTo(5);
    }
}
