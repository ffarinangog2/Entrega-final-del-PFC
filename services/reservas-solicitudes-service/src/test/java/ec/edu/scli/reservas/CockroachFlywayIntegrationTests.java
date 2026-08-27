package ec.edu.scli.reservas;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaAprobacionRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaCreacionSolicitudRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.AgendaMutexPort;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.presentation.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CancelarSolicitudRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class CockroachFlywayIntegrationTests {

    private final UUID actorId = UUID.randomUUID();

    @MockitoBean
    private PoliticaAmbitoLaboratorio politicaAmbito;
    @MockitoBean
    private AcademicoLaboratoriosClient academicoClient;

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
    private IdempotenciaAprobacionRepositoryPort idempotenciaAprobacionRepository;
    @Autowired
    private IdempotenciaCreacionSolicitudRepositoryPort idempotenciaCreacionRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AgendaMutexPort agendaMutex;
    @Autowired
    private SolicitudReservaService solicitudService;

    @BeforeEach
    void prepararDependenciasExternas() {
        when(politicaAmbito.actor()).thenReturn(new ActorAutenticado(
                actorId, Set.of("ROLE_ADMINISTRADOR", "SOLICITUD_APROBAR")));
        when(politicaAmbito.validarGestion(any())).thenReturn(UUID.randomUUID());
    }

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
                    'configuraciones_reserva',
                    'idempotencia_aprobaciones',
                    'idempotencia_creacion_solicitudes',
                    'mutex_agenda'
                  )
                """,
                Integer.class);

        assertThat(tablas).isEqualTo(8);

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
    void mutexAgendaSerializaDosTransaccionesDelMismoLaboratorioYFecha() throws Exception {
        UUID laboratorioId = UUID.randomUUID();
        LocalDate fecha = LocalDate.now().plusDays(3);
        CountDownLatch primeraBloqueada = new CountDownLatch(1);
        CountDownLatch liberarPrimera = new CountDownLatch(1);
        CountDownLatch segundaTermino = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primera = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                agendaMutex.bloquear(laboratorioId, fecha);
                primeraBloqueada.countDown();
                try {
                    assertThat(liberarPrimera.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            assertThat(primeraBloqueada.await(10, TimeUnit.SECONDS)).isTrue();
            var segunda = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(
                        status -> agendaMutex.bloquear(laboratorioId, fecha));
                segundaTermino.countDown();
            });

            assertThat(segundaTermino.await(300, TimeUnit.MILLISECONDS)).isFalse();
            liberarPrimera.countDown();
            primera.get(10, TimeUnit.SECONDS);
            segunda.get(10, TimeUnit.SECONDS);
            assertThat(segundaTermino.getCount()).isZero();
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mutex_agenda WHERE laboratorio_id = ? AND fecha = ?",
                Integer.class, laboratorioId, fecha)).isEqualTo(1);
    }

    @Test
    void dosSolicitudesSolapadasAprobadasConcurrentementeCreanComoMaximoUnaReserva() throws Exception {
        UUID laboratorioId = UUID.randomUUID();
        UUID pisoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.now().plusDays(4);
        when(academicoClient.obtenerLaboratorio(laboratorioId)).thenReturn(
                new LaboratorioExternoResponse(laboratorioId, pisoId, true, true, "ACTIVO", 40));

        SolicitudReserva primeraSolicitud = nuevaSolicitud(laboratorioId);
        primeraSolicitud.setEstado(EstadoSolicitud.EN_REVISION);
        primeraSolicitud.setPisoId(pisoId);
        primeraSolicitud.setFechaReserva(fecha);
        SolicitudReserva segundaSolicitud = nuevaSolicitud(laboratorioId);
        segundaSolicitud.setEstado(EstadoSolicitud.EN_REVISION);
        segundaSolicitud.setPisoId(pisoId);
        segundaSolicitud.setFechaReserva(fecha);
        SolicitudReserva primeraParaGuardar = primeraSolicitud;
        SolicitudReserva segundaParaGuardar = segundaSolicitud;
        primeraSolicitud = transactionTemplate.execute(
                status -> solicitudRepository.guardar(primeraParaGuardar));
        segundaSolicitud = transactionTemplate.execute(
                status -> solicitudRepository.guardar(segundaParaGuardar));

        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        var resultados = new java.util.concurrent.CopyOnWriteArrayList<Object>();
        SolicitudReserva solicitudUno = primeraSolicitud;
        SolicitudReserva solicitudDos = segundaSolicitud;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var aprobar = (java.util.function.Consumer<SolicitudReserva>) solicitud -> {
                preparadas.countDown();
                try {
                    assertThat(iniciar.await(10, TimeUnit.SECONDS)).isTrue();
                    resultados.add(solicitudService.aprobar(solicitud.getId(),
                            new AprobarSolicitudRequest(actorId, "concurrente"),
                            "aprobacion-" + solicitud.getId(), actorId));
                } catch (Exception exception) {
                    resultados.add(exception);
                }
            };
            var primera = executor.submit(() -> aprobar.accept(solicitudUno));
            var segunda = executor.submit(() -> aprobar.accept(solicitudDos));
            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            primera.get(30, TimeUnit.SECONDS);
            segunda.get(30, TimeUnit.SECONDS);
        }

        Long reservas = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM reservas
                WHERE laboratorio_id = ? AND fecha_reserva = ?
                  AND estado <> 'CANCELADA'
                """, Long.class, laboratorioId, fecha);
        assertThat(resultados).hasSize(2);
        assertThat(resultados.stream().filter(
                ec.edu.scli.reservas.presentation.dto.response.ReservaResponse.class::isInstance).count())
                .withFailMessage("Resultados concurrentes: %s", resultados)
                .isEqualTo(1L);
        assertThat(reservas).isEqualTo(1L);
    }

    @Test
    void idempotenciaAprobacionConservaSolicitudYResultadoSinDuplicarClave() {
        SolicitudReserva solicitud = transactionTemplate.execute(status ->
                solicitudRepository.guardar(nuevaSolicitud(UUID.randomUUID())));
        UUID reservaId = transactionTemplate.execute(status -> {
            Reserva reserva = reservaRepository.guardar(nuevaReserva(
                    solicitud.getId(), solicitud.getLaboratorioId(), UUID.randomUUID()));
            idempotenciaAprobacionRepository.registrarSiAusente("aprobacion-it", solicitud.getId());
            idempotenciaAprobacionRepository.registrarSiAusente("aprobacion-it", solicitud.getId());
            idempotenciaAprobacionRepository.completar("aprobacion-it", reserva.getId());
            return reserva.getId();
        });

        var registro = transactionTemplate.execute(status ->
                idempotenciaAprobacionRepository.buscarParaActualizar("aprobacion-it")
                        .orElseThrow());

        assertThat(registro.operacion()).isEqualTo("APROBAR_SOLICITUD");
        assertThat(registro.solicitudId()).isEqualTo(solicitud.getId());
        assertThat(registro.reservaId()).isEqualTo(reservaId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotencia_aprobaciones WHERE clave = ?",
                Integer.class,
                "aprobacion-it")).isEqualTo(1);
    }

    @Test
    void dosTransaccionesConLaMismaClaveConservanUnSoloRegistro() throws Exception {
        SolicitudReserva solicitud = transactionTemplate.execute(status ->
                solicitudRepository.guardar(nuevaSolicitud(UUID.randomUUID())));
        String clave = "aprobacion-concurrente-" + UUID.randomUUID();
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarea = (java.util.concurrent.Callable<UUID>) () -> {
                preparados.countDown();
                assertThat(iniciar.await(10, TimeUnit.SECONDS)).isTrue();
                return transactionTemplate.execute(status -> {
                    idempotenciaAprobacionRepository.registrarSiAusente(
                            clave, solicitud.getId());
                    return idempotenciaAprobacionRepository.buscarParaActualizar(clave)
                            .orElseThrow()
                            .solicitudId();
                });
            };

            var primera = executor.submit(tarea);
            var segunda = executor.submit(tarea);
            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            assertThat(primera.get(20, TimeUnit.SECONDS)).isEqualTo(solicitud.getId());
            assertThat(segunda.get(20, TimeUnit.SECONDS)).isEqualTo(solicitud.getId());
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotencia_aprobaciones WHERE clave = ?",
                Integer.class,
                clave)).isEqualTo(1);
    }

    @Test
    void idempotenciaCreacionPersisteActorPayloadYResultado() {
        UUID actorId = UUID.randomUUID();
        String clave = "creacion-it-" + UUID.randomUUID();
        String hash = "a".repeat(64);
        SolicitudReserva solicitud = transactionTemplate.execute(status -> {
            idempotenciaCreacionRepository.registrarSiAusente(clave, actorId, hash);
            SolicitudReserva creada = solicitudRepository.guardar(nuevaSolicitud(UUID.randomUUID()));
            idempotenciaCreacionRepository.completar(clave, creada.getId());
            return creada;
        });

        var registro = transactionTemplate.execute(status ->
                idempotenciaCreacionRepository.buscarParaActualizar(clave).orElseThrow());
        assertThat(registro.actorId()).isEqualTo(actorId);
        assertThat(registro.payloadHash()).isEqualTo(hash);
        assertThat(registro.solicitudId()).isEqualTo(solicitud.getId());
    }

    @Test
    void dosTransaccionesDeCreacionConMismaClaveCompartenUnSoloClaim() throws Exception {
        String clave = "creacion-concurrente-" + UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String hash = "b".repeat(64);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarea = (java.util.concurrent.Callable<UUID>) () -> {
                preparados.countDown();
                assertThat(iniciar.await(10, TimeUnit.SECONDS)).isTrue();
                return ejecutarConRetrySerializable(() -> transactionTemplate.execute(status -> {
                    idempotenciaCreacionRepository.registrarSiAusente(clave, actorId, hash);
                    return idempotenciaCreacionRepository.buscarParaActualizar(clave)
                            .orElseThrow().actorId();
                }));
            };
            var primera = executor.submit(tarea);
            var segunda = executor.submit(tarea);
            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(primera.get(20, TimeUnit.SECONDS)).isEqualTo(actorId);
            assertThat(segunda.get(20, TimeUnit.SECONDS)).isEqualTo(actorId);
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotencia_creacion_solicitudes WHERE clave = ?",
                Integer.class, clave)).isEqualTo(1);
    }

    @Test
    void cancelacionPropietariaAprobadaEsAtomicaParaSolicitudYReserva() {
        SolicitudReserva solicitud = transactionTemplate.execute(status ->
                solicitudRepository.guardar(nuevaSolicitud(UUID.randomUUID())));
        Reserva reserva = transactionTemplate.execute(status -> {
            Reserva creada = reservaRepository.guardar(nuevaReserva(
                    solicitud.getId(), solicitud.getLaboratorioId(), actorId));
            solicitud.setReservaId(creada.getId());
            solicitudRepository.guardar(solicitud);
            return creada;
        });

        solicitudService.cancelar(solicitud.getId(),
                new CancelarSolicitudRequest("Retiro docente"), solicitud.getSolicitanteId());

        SolicitudReserva solicitudFinal = transactionTemplate.execute(status ->
                solicitudRepository.buscarPorId(solicitud.getId()).orElseThrow());
        Reserva reservaFinal = transactionTemplate.execute(status ->
                reservaRepository.buscarPorId(reserva.getId()).orElseThrow());
        assertThat(solicitudFinal.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(reservaFinal.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    private <T> T ejecutarConRetrySerializable(Supplier<T> operacion) {
        TransientDataAccessException ultimo = null;
        for (int intento = 1; intento <= 5; intento++) {
            try {
                return operacion.get();
            } catch (TransientDataAccessException exception) {
                ultimo = exception;
                if (intento < 5) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(25L << (intento - 1));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                }
            }
        }
        throw ultimo;
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
