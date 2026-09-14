package ec.edu.scli.usuarios.integration;

import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.cockroachdb.CockroachContainer;

import java.util.Optional;

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

    @Autowired
    private PerfilRepositoryPort perfilRepositoryPort;

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

    @Test
    void debeAplicarMigracionDeAdscripcionesConRestriccionesEIndices() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success",
                Integer.class);
        Integer tabla = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() "
                        + "AND table_name = 'adscripciones_institucionales'",
                Integer.class);
        Integer indices = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = current_schema() "
                        + "AND tablename = 'adscripciones_institucionales'",
                Integer.class);

        assertEquals(1, version);
        assertEquals(1, tabla);
        assertNotNull(indices);
        assertTrue(indices >= 3);
    }

    @Test
    @Transactional
    void adaptadorPerfil_debeGuardarYLeerPerfilConCockroach() {

        Perfil perfil = new Perfil();
        perfil.setIdentificacion("INT-ADAPTER-001");
        perfil.setNombres("Integracion");
        perfil.setApellidos("Adapter");
        perfil.setEmailInstitucional("integracion.adapter@uteq.edu.ec");
        perfil.setActivo(true);

        Perfil guardado = perfilRepositoryPort.save(perfil);
        Optional<Perfil> encontrado = perfilRepositoryPort.findById(guardado.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(
                "integracion.adapter@uteq.edu.ec",
                encontrado.get().getEmailInstitucional()
        );
    }
}
