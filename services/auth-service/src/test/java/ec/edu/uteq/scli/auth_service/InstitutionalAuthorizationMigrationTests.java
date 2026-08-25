package ec.edu.uteq.scli.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InstitutionalAuthorizationMigrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesTheSevenInstitutionalRoles() {
        assertThat(codes("SELECT codigo FROM roles ORDER BY codigo"))
                .containsExactlyInAnyOrder(
                        "ADMINISTRADOR",
                        "DOCENTE",
                        "TECNICO",
                        "ESTUDIANTE",
                        "COORDINADOR",
                        "ADMINISTRADOR_PISO",
                        "DECANO");
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
                        "AGENDA_GESTIONAR")
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
                        "LABORATORIO_LEER")
                .doesNotContain(
                        "SOLICITUD_APROBAR",
                        "SOLICITUD_RECHAZAR",
                        "RESERVA_CANCELAR",
                        "AGENDA_GESTIONAR");
    }

    @Test
    void deanHasReadOnlySupervisionPermissions() {
        assertThat(permissions("DECANO"))
                .containsExactlyInAnyOrder(
                        "ACADEMICO_LEER",
                        "LABORATORIO_LEER",
                        "RESERVA_LEER",
                        "REPORTE_LEER")
                .doesNotContain(
                        "SOLICITUD_APROBAR",
                        "SOLICITUD_RECHAZAR",
                        "RESERVA_CANCELAR",
                        "AGENDA_GESTIONAR",
                        "PLANIFICACION_GESTIONAR");
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
