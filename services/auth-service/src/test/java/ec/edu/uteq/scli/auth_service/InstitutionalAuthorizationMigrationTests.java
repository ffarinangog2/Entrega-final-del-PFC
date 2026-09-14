package ec.edu.uteq.scli.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cockroachdb.CockroachContainer;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class InstitutionalAuthorizationMigrationTests {

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
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesTheInstitutionalRoles() {
        assertThat(codes("SELECT codigo FROM roles ORDER BY codigo"))
                .containsExactlyInAnyOrder(
                        "ADMINISTRADOR",
                        "DOCENTE",
                        "ESTUDIANTE",
                        "COORDINADOR",
                        "ADMINISTRADOR_PISO");
    }

    @Test
    void administratorKeepsAllPermissionsAndReceivesTheNewOnes() {
        Set<String> allPermissions = codes("SELECT codigo FROM permisos");
        Set<String> administratorPermissions = permissions("ADMINISTRADOR");

        assertThat(administratorPermissions)
                .containsExactlyInAnyOrderElementsOf(allPermissions)
                .contains("ACADEMICO_LEER", "PLANIFICACION_GESTIONAR");
    }

    @Test
    void floorAdministratorHasOperationalPermissionsWithoutGlobalUserAccess() {
        assertThat(permissions("ADMINISTRADOR_PISO"))
                .containsExactlyInAnyOrder(
                        "ACADEMICO_LEER",
                        "SOLICITUD_LEER",
                        "SOLICITUD_APROBAR",
                        "SOLICITUD_RECHAZAR",
                        "RESERVA_LEER",
                        "RESERVA_CANCELAR",
                        "LABORATORIO_LEER",
                        "LABORATORIO_GESTIONAR",
                        "EQUIPO_LEER",
                        "EQUIPO_GESTIONAR",
                        "AGENDA_GESTIONAR",
                        "INCIDENTE_CREAR",
                        "INCIDENTE_LEER",
                        "INCIDENTE_GESTIONAR",
                        "NOTIFICACION_DISPOSITIVO")
                .doesNotContain(
                        "USUARIO_LEER",
                        "USUARIO_CREAR",
                        "USUARIO_EDITAR",
                        "USUARIO_DESACTIVAR",
                        "REPORTE_LEER",
                        "REPORTE_GENERAR");
    }

    @Test
    void coordinatorPlansButDoesNotOperateDailyReservations() {
        assertThat(permissions("COORDINADOR"))
                .containsExactlyInAnyOrder(
                        "ACADEMICO_LEER",
                        "PLANIFICACION_GESTIONAR",
                        "LABORATORIO_LEER",
                        "INCIDENTE_CREAR",
                        "INCIDENTE_LEER",
                        "NOTIFICACION_DISPOSITIVO")
                .doesNotContain(
                        "SOLICITUD_APROBAR",
                        "SOLICITUD_RECHAZAR",
                        "RESERVA_CANCELAR",
                        "AGENDA_GESTIONAR");
    }

    @Test
    void teacherCanCancelOnlyThroughReservationOwnershipPolicy() {
        assertThat(permissions("DOCENTE"))
                .contains("RESERVA_CANCELAR");
    }

    private Set<String> permissions(String roleCode) {
        return Set.copyOf(jdbcTemplate.queryForList(
                """
                SELECT p.codigo
                FROM permisos p
                JOIN roles_permisos rp ON rp.permiso_id = p.id
                JOIN roles r ON r.id = rp.rol_id
                WHERE r.codigo = ?
                """,
                String.class,
                roleCode));
    }

    private Set<String> codes(String sql) {
        List<String> results = jdbcTemplate.queryForList(sql, String.class);
        return Set.copyOf(results);
    }
}
