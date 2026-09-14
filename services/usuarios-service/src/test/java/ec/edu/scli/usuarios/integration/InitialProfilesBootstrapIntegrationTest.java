package ec.edu.scli.usuarios.integration;

import ec.edu.scli.usuarios.infrastructure.bootstrap.InitialProfilesBootstrap;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.ContextoAcademicoEstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.infrastructure.security.HmacIdentificacionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.cockroachdb.CockroachContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class InitialProfilesBootstrapIntegrationTest {
    private static final CockroachContainer COCKROACH =
            new CockroachContainer("cockroachdb/cockroach:v24.3.5");
    private static final UUID ADMIN_ID = stableId("admin:adminpiso.03");
    private static final UUID TEACHER_ID = stableId("teacher:docente.03");
    private static final UUID STUDENT_ID = stableId("student:estudiante.software.01.02");
    private static final UUID PERIOD_ID = UUID.fromString("3f000000-0000-0000-0000-000000000001");
    private static final UUID CONTEXT_ID = stableId("context:" + STUDENT_ID + ":" + PERIOD_ID);

    static {
        COCKROACH.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", COCKROACH::getJdbcUrl);
        registry.add("spring.datasource.username", COCKROACH::getUsername);
        registry.add("spring.datasource.password", COCKROACH::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("app.initial-data.enabled", () -> false);
    }

    @Autowired private PerfilRepository perfiles;
    @Autowired private AdministradorRepository administradores;
    @Autowired private DocenteRepository docentes;
    @Autowired private EstudianteRepository estudiantes;
    @Autowired private ContextoAcademicoEstudianteRepository contextos;
    @Autowired private AdscripcionInstitucionalRepository adscripciones;
    @Autowired private EntityManager entityManager;
    @Autowired private HmacIdentificacionService hmac;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void insertaUuidDeterministasYRepetirNoDuplicaNiSobrescribe() {
        InitialProfilesBootstrap bootstrap = bootstrap();
        assertAffectedRows(0);

        bootstrap.run(null);
        entityManager.flush();
        entityManager.clear();
        assertAffectedRows(1);
        assertFloorCoverage();

        jdbc.update("UPDATE administradores SET cargo = ?, piso_id = ?, activo = false WHERE id = ?",
                "Dato existente", UUID.fromString("35000000-0000-0000-0000-000000000001"), ADMIN_ID);
        bootstrap = bootstrap();
        bootstrap.run(null);
        entityManager.flush();
        entityManager.clear();

        assertAffectedRows(1);
        assertFloorCoverage();
        assertEquals("Dato existente", jdbc.queryForObject(
                "SELECT cargo FROM administradores WHERE id = ?", String.class, ADMIN_ID));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM administradores WHERE codigo_administrador LIKE 'ADM-GLOBAL-%'"
                        + " AND piso_id IS NOT NULL", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM administradores WHERE activo = true AND piso_id IS NULL"
                        + " AND codigo_administrador IN ('ADM-0001', 'ADM-GLOBAL-01', 'ADM-GLOBAL-02')",
                Integer.class));
        assertEquals("ADM-0001", jdbc.queryForObject(
                "SELECT codigo_administrador FROM administradores WHERE activo = true AND piso_id IS NULL"
                        + " AND codigo_administrador IN ('ADM-0001', 'ADM-GLOBAL-01', 'ADM-GLOBAL-02')",
                String.class));
        for (int floor = 1; floor <= 4; floor++) {
            UUID floorId = UUID.fromString("35000000-0000-0000-0000-0000000000%02d".formatted(floor));
            assertEquals(1, administradores.findByPisoIdAndActivoTrue(floorId).size());
            assertEquals("ADM-PISO-%02d".formatted(floor),
                    administradores.findByPisoIdAndActivoTrue(floorId).getFirst().getCodigoAdministrador());
        }
    }

    private InitialProfilesBootstrap bootstrap() {
        return new InitialProfilesBootstrap(perfiles, administradores, docentes, estudiantes, contextos,
                adscripciones, entityManager, hmac,
                "35000000-0000-0000-0000-000000000001,35000000-0000-0000-0000-000000000002,"
                        + "35000000-0000-0000-0000-000000000003,35000000-0000-0000-0000-000000000004",
                "37000000-0000-0000-0000-000000000001,55584c85-0359-3f3f-8fa1-55000611374b",
                PERIOD_ID);
    }

    private void assertAffectedRows(int expected) {
        assertEquals(expected, count("administradores", ADMIN_ID));
        assertEquals(expected, count("docentes", TEACHER_ID));
        assertEquals(expected, count("estudiantes", STUDENT_ID));
        assertEquals(expected, count("contextos_academicos_estudiante", CONTEXT_ID));
    }

    private int count(String table, UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }

    private void assertFloorCoverage() {
        for (int floor = 1; floor <= 4; floor++) {
            String code = "ADM-PISO-%02d".formatted(floor);
            UUID floorId = UUID.fromString("35000000-0000-0000-0000-0000000000%02d".formatted(floor));
            assertEquals(floorId, jdbc.queryForObject(
                    "SELECT piso_id FROM administradores WHERE codigo_administrador = ? AND activo = true",
                    UUID.class, code));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM administradores WHERE codigo_administrador = ?",
                    Integer.class, code));
        }
    }

    private static UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(("scli-integral-test:" + value).getBytes(StandardCharsets.UTF_8));
    }
}
