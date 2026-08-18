package ec.edu.scli.reservas;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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
    @Autowired
    private SolicitudReservaRepositoryPort solicitudRepository;
    @Autowired
    private ReservaRepositoryPort reservaRepository;
    @Autowired
    private EntityManager entityManager;

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

        Integer clavesForaneas = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_type = 'FOREIGN KEY'
                  AND constraint_name IN (
                    'fk_reservas_solicitud',
                    'fk_historial_solicitudes_solicitud'
                  )
                """,
                Integer.class);
        assertThat(clavesForaneas).isEqualTo(2);
    }

    @Test
    @Transactional
    void adapterPersisteRecuperaRelacionYVersionDeReserva() {
        UUID laboratorioId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();

        SolicitudReserva solicitud = nuevaSolicitud(laboratorioId);
        SolicitudReserva solicitudGuardada = solicitudRepository.guardar(solicitud);
        entityManager.flush();
        entityManager.clear();

        Reserva reserva = nuevaReserva(
                solicitudGuardada.getId(), laboratorioId, responsableId);
        Reserva reservaGuardada = reservaRepository.guardar(reserva);
        entityManager.flush();
        entityManager.clear();

        Reserva recuperada = reservaRepository.buscarPorId(reservaGuardada.getId()).orElseThrow();
        assertThat(recuperada.getId()).isEqualTo(reservaGuardada.getId());
        assertThat(recuperada.getEstado()).isEqualTo(EstadoReserva.PROGRAMADA);
        assertThat(recuperada.getLaboratorioId()).isEqualTo(laboratorioId);
        assertThat(recuperada.getResponsableId()).isEqualTo(responsableId);
        assertThat(recuperada.getSolicitudId()).isEqualTo(solicitudGuardada.getId());
        assertThat(reservaRepository.buscarPorSolicitudId(solicitudGuardada.getId()))
                .get().extracting(Reserva::getId).isEqualTo(recuperada.getId());

        Long versionInicial = recuperada.getVersion();
        recuperada.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.guardar(recuperada);
        entityManager.flush();
        entityManager.clear();

        Reserva actualizada = reservaRepository.buscarPorId(recuperada.getId()).orElseThrow();
        assertThat(actualizada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(actualizada.getVersion()).isGreaterThan(versionInicial);
    }

    private SolicitudReserva nuevaSolicitud(UUID laboratorioId) {
        SolicitudReserva solicitud = new SolicitudReserva();
        solicitud.setSolicitanteId(UUID.randomUUID());
        solicitud.setDocenteId(UUID.randomUUID());
        solicitud.setLaboratorioId(laboratorioId);
        solicitud.setMateriaId(UUID.randomUUID());
        solicitud.setPeriodoLectivoId(UUID.randomUUID());
        solicitud.setFechaReserva(LocalDate.now().plusDays(1));
        solicitud.setHoraInicio(LocalTime.of(8, 0));
        solicitud.setHoraFin(LocalTime.of(10, 0));
        solicitud.setNumeroParticipantes(20);
        solicitud.setMotivo("Prueba de integración de persistencia");
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setClaveIdempotencia("it-" + UUID.randomUUID());
        return solicitud;
    }

    private Reserva nuevaReserva(UUID solicitudId, UUID laboratorioId, UUID responsableId) {
        Reserva reserva = new Reserva();
        reserva.setSolicitudId(solicitudId);
        reserva.setLaboratorioId(laboratorioId);
        reserva.setResponsableId(responsableId);
        reserva.setFechaReserva(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(8, 0));
        reserva.setHoraFin(LocalTime.of(10, 0));
        reserva.setEstado(EstadoReserva.PROGRAMADA);
        reserva.setCodigoReserva("IT-" + UUID.randomUUID());
        return reserva;
    }
}
