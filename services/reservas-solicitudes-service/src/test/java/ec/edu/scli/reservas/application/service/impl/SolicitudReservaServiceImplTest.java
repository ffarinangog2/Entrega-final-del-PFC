package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.*;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.*;
import ec.edu.scli.reservas.mapper.*;
import ec.edu.scli.reservas.observability.BusinessEventMetrics;
import ec.edu.scli.reservas.presentation.dto.request.*;
import ec.edu.scli.reservas.presentation.dto.response.DisponibilidadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolicitudReservaServiceImplTest {
    private SolicitudReservaRepositoryPort solicitudes;
    private ReservaRepositoryPort reservas;
    private HistorialSolicitudRepositoryPort historiales;
    private IdempotenciaAprobacionRepositoryPort idempotenciaAprobaciones;
    private IdempotenciaCreacionSolicitudRepositoryPort idempotenciaCreaciones;
    private DocenteInstitucionalPort docentes;
    private AcademicoLaboratoriosClient academico;
    private DisponibilidadService disponibilidad;
    private SolicitudReservaServiceImpl service;
    private BusinessEventMetrics metrics;
    private PoliticaAmbitoLaboratorio politica;
    private AgendaMutexPort agendaMutex;

    private final UUID solicitanteId = UUID.randomUUID();
    private final UUID docenteId = UUID.randomUUID();
    private final UUID laboratorioId = UUID.randomUUID();
    private final UUID materiaId = UUID.randomUUID();
    private final UUID periodoId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        solicitudes = mock(SolicitudReservaRepositoryPort.class);
        reservas = mock(ReservaRepositoryPort.class);
        historiales = mock(HistorialSolicitudRepositoryPort.class);
        idempotenciaAprobaciones = mock(IdempotenciaAprobacionRepositoryPort.class);
        idempotenciaCreaciones = mock(IdempotenciaCreacionSolicitudRepositoryPort.class);
        docentes = mock(DocenteInstitucionalPort.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        disponibilidad = mock(DisponibilidadService.class);
        metrics = mock(BusinessEventMetrics.class);
        politica = mock(PoliticaAmbitoLaboratorio.class);
        agendaMutex = mock(AgendaMutexPort.class);
        service = new SolicitudReservaServiceImpl(
                solicitudes, reservas, historiales, idempotenciaAprobaciones, idempotenciaCreaciones,
                new SolicitudReservaMapper(), new ReservaMapper(), new HistorialSolicitudMapper(),
                docentes, academico, disponibilidad, metrics, politica, agendaMutex);

        when(politica.actor()).thenReturn(new ActorAutenticado(
                usuarioId, java.util.Set.of("ROLE_ADMINISTRADOR")));
        when(politica.obtenerPiso(any())).thenReturn(UUID.randomUUID());
        when(politica.validarGestion(any())).thenReturn(UUID.randomUUID());

        when(docentes.obtenerPorDocenteId(docenteId))
                .thenReturn(new DocenteInstitucional(docenteId, UUID.randomUUID(), true));
        when(academico.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(
                        laboratorioId, UUID.randomUUID(), true, true, "ACTIVO", 30));
        when(academico.verificarMateria(materiaId)).thenReturn(new ExisteExternoResponse(materiaId, true));
        when(academico.verificarPeriodoLectivo(periodoId)).thenReturn(new ExisteExternoResponse(periodoId, true));
        when(disponibilidad.consultar(any(), any(), any(), any()))
                .thenReturn(new DisponibilidadResponse(
                        laboratorioId, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0), true, null));
        when(solicitudes.guardar(any())).thenAnswer(i -> {
            SolicitudReserva solicitud = i.getArgument(0);
            if (solicitud.getId() == null) solicitud.setId(UUID.randomUUID());
            return solicitud;
        });
        when(reservas.guardar(any())).thenAnswer(i -> {
            Reserva reserva = i.getArgument(0);
            if (reserva.getId() == null) reserva.setId(UUID.randomUUID());
            return reserva;
        });
        when(historiales.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(idempotenciaCreaciones.buscarParaActualizar(anyString())).thenAnswer(i ->
                Optional.of(new IdempotenciaCreacionSolicitud(i.getArgument(0),
                        "CREAR_SOLICITUD", usuarioId, service.hashCreacion(crearRequest()), null)));
    }

    @Test
    void creaSolicitudYRegistraHistorial() {
        var respuesta = service.crear(crearRequest(), "clave", usuarioId);
        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
        assertEquals(laboratorioId, respuesta.laboratorioId());
        verify(historiales).guardar(argThat(h -> h.getEstadoNuevo() == EstadoSolicitud.PENDIENTE));
        verify(metrics).solicitudCreada();
        verify(idempotenciaCreaciones).completar("clave", respuesta.id());
    }

    @Test
    void docenteCreaSolicitudPropiaYNoPuedeSuplantarOtroDocente() {
        when(politica.actor()).thenReturn(new ActorAutenticado(
                usuarioId, java.util.Set.of("ROLE_DOCENTE", "SOLICITUD_CREAR")));
        when(docentes.obtenerPorPerfilId(usuarioId))
                .thenReturn(new DocenteInstitucional(docenteId, usuarioId, true));
        CrearSolicitudReservaRequest propia = new CrearSolicitudReservaRequest(
                UUID.randomUUID(), docenteId, laboratorioId, materiaId, periodoId,
                LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0), 10, "clase", null);
        when(idempotenciaCreaciones.buscarParaActualizar("propia")).thenReturn(Optional.of(
                new IdempotenciaCreacionSolicitud("propia", "CREAR_SOLICITUD", usuarioId,
                        service.hashCreacion(propia), null)));
        assertEquals(docenteId, service.crear(propia, "propia", usuarioId).docenteId());

        CrearSolicitudReservaRequest ajena = new CrearSolicitudReservaRequest(
                usuarioId, UUID.randomUUID(), laboratorioId, materiaId, periodoId,
                LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0), 10, "clase", null);
        when(idempotenciaCreaciones.buscarParaActualizar("ajena")).thenReturn(Optional.of(
                new IdempotenciaCreacionSolicitud("ajena", "CREAR_SOLICITUD", usuarioId,
                        service.hashCreacion(ajena), null)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.crear(ajena, "ajena", usuarioId));
    }

    @Test
    void perfilDocenteSinRegistroObtieneErrorFuncional() {
        when(politica.actor()).thenReturn(new ActorAutenticado(
                usuarioId, java.util.Set.of("ROLE_DOCENTE", "SOLICITUD_CREAR")));
        when(docentes.obtenerPorPerfilId(usuarioId)).thenReturn(null);
        assertThrows(ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException.class,
                () -> service.crear(crearRequest(), "sin-docente", usuarioId));
        verify(solicitudes, never()).guardar(any());
    }

    @Test
    void devuelveSolicitudExistentePorIdempotencia() {
        SolicitudReserva existente = solicitud(EstadoSolicitud.PENDIENTE);
        when(idempotenciaCreaciones.buscarParaActualizar("clave")).thenReturn(Optional.of(
                new IdempotenciaCreacionSolicitud("clave", "CREAR_SOLICITUD",
                        usuarioId, service.hashCreacion(crearRequest()), existente.getId())));
        when(solicitudes.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        assertEquals(existente.getId(), service.crear(crearRequest(), "clave", usuarioId).id());
        verify(solicitudes, never()).guardar(any());
        verify(metrics, never()).solicitudCreada();
    }

    @Test
    void rechazaClaveDeCreacionConActorDiferente() {
        when(idempotenciaCreaciones.buscarParaActualizar("clave")).thenReturn(Optional.of(
                new IdempotenciaCreacionSolicitud("clave", "CREAR_SOLICITUD",
                        UUID.randomUUID(), service.hashCreacion(crearRequest()), null)));
        assertThrows(IllegalStateException.class,
                () -> service.crear(crearRequest(), "clave", usuarioId));
        verify(solicitudes, never()).guardar(any());
    }

    @Test
    void rechazaClaveDeCreacionConPayloadDiferente() {
        when(idempotenciaCreaciones.buscarParaActualizar("clave")).thenReturn(Optional.of(
                new IdempotenciaCreacionSolicitud("clave", "CREAR_SOLICITUD",
                        usuarioId, "0".repeat(64), null)));
        assertThrows(IllegalStateException.class,
                () -> service.crear(crearRequest(), "clave", usuarioId));
        verify(solicitudes, never()).guardar(any());
    }


    @Test
    void actualizaSolicitudPermitida() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        solicitud.setSolicitanteId(usuarioId);
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        var respuesta = service.actualizar(solicitud.getId(), actualizarRequest(), usuarioId);
        assertEquals("actualizado", respuesta.motivo());
        verify(solicitudes).guardar(solicitud);
    }

    @Test
    void rechazaActualizacionEnEstadoFinal() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.RECHAZADA);
        solicitud.setSolicitanteId(usuarioId);
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        assertThrows(IllegalStateException.class,
                () -> service.actualizar(solicitud.getId(), actualizarRequest(), usuarioId));
    }

    @Test
    void poneSolicitudEnRevisionYRegistraHistorial() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        prepararBloqueo(solicitud);
        assertEquals(EstadoSolicitud.EN_REVISION,
                service.ponerEnRevision(solicitud.getId(), usuarioId).estado());
        verify(historiales).guardar(argThat(h -> h.getEstadoAnterior() == EstadoSolicitud.PENDIENTE
                && h.getEstadoNuevo() == EstadoSolicitud.EN_REVISION));
    }

    @Test
    void apruebaSolicitudCreaReservaEHistorial() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        prepararIdempotenciaPendiente("clave", solicitud.getId());
        when(reservas.existePorSolicitudId(solicitud.getId())).thenReturn(false);
        var respuesta = service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, "aprobada"), "clave", usuarioId);
        assertEquals(EstadoReserva.PROGRAMADA, respuesta.estado());
        assertEquals(EstadoSolicitud.APROBADA, solicitud.getEstado());
        assertNotNull(solicitud.getReservaId());
        verify(historiales).guardar(argThat(h -> h.getEstadoNuevo() == EstadoSolicitud.APROBADA));
        verify(metrics).solicitudAprobada();
        verify(metrics).reservaCreada();
        verify(idempotenciaAprobaciones).completar(eq("clave"), eq(respuesta.id()));
    }

    @Test
    void replayDeAprobacionDevuelveLaMismaReservaSinRepetirEfectos() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.APROBADA);
        Reserva reservaExistente = reserva(EstadoReserva.PROGRAMADA, solicitud.getId());
        when(idempotenciaAprobaciones.buscarParaActualizar("clave"))
                .thenReturn(Optional.of(new IdempotenciaAprobacion(
                        "clave", "APROBAR_SOLICITUD", solicitud.getId(), reservaExistente.getId())));
        when(reservas.buscarPorId(reservaExistente.getId())).thenReturn(Optional.of(reservaExistente));

        var respuesta = service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, "aprobada"),
                "clave", usuarioId);

        assertEquals(reservaExistente.getId(), respuesta.id());
        verify(reservas, never()).guardar(any());
        verify(solicitudes, never()).guardar(any());
        verify(historiales, never()).guardar(any());
        verify(metrics, never()).solicitudAprobada();
        verify(metrics, never()).reservaCreada();
        verify(idempotenciaAprobaciones, never()).completar(anyString(), any());
    }

    @Test
    void rechazaLaMismaClaveParaOtraSolicitud() {
        UUID otraSolicitudId = UUID.randomUUID();
        when(idempotenciaAprobaciones.buscarParaActualizar("clave"))
                .thenReturn(Optional.of(new IdempotenciaAprobacion(
                        "clave", "APROBAR_SOLICITUD", otraSolicitudId, null)));

        assertThrows(IllegalStateException.class, () -> service.aprobar(
                UUID.randomUUID(), new AprobarSolicitudRequest(usuarioId, null),
                "clave", usuarioId));

        verify(reservas, never()).guardar(any());
        verify(solicitudes, never()).buscarPorIdParaActualizar(any());
    }

    @Test
    void dosReplaysDeLaMismaClaveNoDuplicanReserva() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.APROBADA);
        Reserva reservaExistente = reserva(EstadoReserva.PROGRAMADA, solicitud.getId());
        var operacion = new IdempotenciaAprobacion(
                "clave", "APROBAR_SOLICITUD", solicitud.getId(), reservaExistente.getId());
        when(idempotenciaAprobaciones.buscarParaActualizar("clave"))
                .thenReturn(Optional.of(operacion));
        when(reservas.buscarPorId(reservaExistente.getId())).thenReturn(Optional.of(reservaExistente));

        var primera = service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, null),
                "clave", usuarioId);
        var segunda = service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, null),
                "clave", usuarioId);

        assertEquals(primera.id(), segunda.id());
        verify(reservas, never()).guardar(any());
        verify(reservas, times(2)).buscarPorId(reservaExistente.getId());
    }

    @Test
    void impideAprobacionDuplicadaConOtraClave() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        prepararIdempotenciaPendiente("otra-clave", solicitud.getId());
        when(reservas.existePorSolicitudId(solicitud.getId())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, null),
                "otra-clave", usuarioId));
        verify(reservas, never()).guardar(any());
    }

    @Test
    void rechazaSolicitudYRegistraHistorial() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        assertEquals(EstadoSolicitud.RECHAZADA,
                service.rechazar(solicitud.getId(), new RechazarSolicitudRequest("no cumple"), usuarioId).estado());
        verify(historiales).guardar(argThat(h -> "no cumple".equals(h.getComentario())));
        verify(metrics).solicitudRechazada();
    }

    @Test
    void cancelaSolicitudAprobadaYReservaProgramada() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.APROBADA);
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA, solicitud.getId());
        prepararBloqueo(solicitud);
        when(reservas.buscarPorSolicitudId(solicitud.getId())).thenReturn(Optional.of(reserva));
        when(reservas.buscarPorIdParaActualizar(reserva.getId())).thenReturn(Optional.of(reserva));
        var respuesta = service.cancelar(
                solicitud.getId(), new CancelarSolicitudRequest("cancelada"), usuarioId);
        assertEquals(EstadoSolicitud.CANCELADA, respuesta.estado());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(metrics).solicitudCancelada();
        verify(metrics).reservaCancelada();
    }

    @Test
    void noCancelaSolicitudAprobadaSiReservaEstaEnCursoOFinalizada() {
        for (EstadoReserva estado : List.of(EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA)) {
            SolicitudReserva solicitud = solicitud(EstadoSolicitud.APROBADA);
            solicitud.setSolicitanteId(usuarioId);
            Reserva reserva = reserva(estado, solicitud.getId());
            prepararBloqueo(solicitud);
            when(reservas.buscarPorSolicitudId(solicitud.getId())).thenReturn(Optional.of(reserva));
            when(reservas.buscarPorIdParaActualizar(reserva.getId())).thenReturn(Optional.of(reserva));
            assertThrows(IllegalStateException.class, () -> service.cancelar(
                    solicitud.getId(), new CancelarSolicitudRequest("fuera de estado"), usuarioId));
            assertEquals(EstadoSolicitud.APROBADA, solicitud.getEstado());
            assertEquals(estado, reserva.getEstado());
        }
    }

    @Test
    void propietarioCancelaPendienteYEnRevisionSinExigirReserva() {
        for (EstadoSolicitud estado : List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.EN_REVISION)) {
            SolicitudReserva solicitud = solicitud(estado);
            solicitud.setSolicitanteId(usuarioId);
            prepararBloqueo(solicitud);
            assertEquals(EstadoSolicitud.CANCELADA,
                    service.cancelar(solicitud.getId(), new CancelarSolicitudRequest("retiro"), usuarioId).estado());
        }
        verify(reservas, never()).buscarPorSolicitudId(any());
    }

    @Test
    void docenteNoCancelaSolicitudAjena() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        prepararBloqueo(solicitud);
        when(politica.actor()).thenReturn(new ActorAutenticado(
                usuarioId, java.util.Set.of("ROLE_DOCENTE", "SOLICITUD_CANCELAR")));
        doThrow(new org.springframework.security.access.AccessDeniedException("sin gestiÃ³n"))
                .when(politica).validarGestion(laboratorioId);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.cancelar(solicitud.getId(), new CancelarSolicitudRequest("no"), usuarioId));
    }

    @Test
    void propuestaValidaSeAceptaYAplicaSinCrearReserva() {
        UUID laboratorioAlternativo = UUID.randomUUID();
        LocalDate fecha = LocalDate.now().plusDays(2);
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        solicitud.setSolicitanteId(usuarioId);
        prepararBloqueo(solicitud);
        when(academico.obtenerLaboratorio(laboratorioAlternativo)).thenReturn(
                new LaboratorioExternoResponse(laboratorioAlternativo, UUID.randomUUID(), true, true, "ACTIVO", 30));
        when(disponibilidad.consultar(eq(laboratorioAlternativo), eq(fecha), any(), any()))
                .thenReturn(new DisponibilidadResponse(laboratorioAlternativo, fecha,
                        LocalTime.of(12, 0), LocalTime.of(14, 0), true, null));
        UUID pisoAlternativo = UUID.randomUUID();
        solicitud.setPisoId(UUID.randomUUID());
        when(politica.obtenerPiso(laboratorioAlternativo)).thenReturn(pisoAlternativo);

        var propuesta = service.proponerAlternativa(solicitud.getId(),
                new ProponerAlternativaRequest(fecha, LocalTime.of(12, 0), LocalTime.of(14, 0),
                        laboratorioAlternativo, "alternativa"), usuarioId);
        assertEquals(EstadoSolicitud.PROPUESTA, propuesta.estado());
        assertEquals(laboratorioAlternativo, propuesta.propuestaLaboratorioId());

        var aceptada = service.aceptarPropuesta(solicitud.getId(),
                new ResponderPropuestaRequest("acepto"), usuarioId);
        assertEquals(EstadoSolicitud.EN_REVISION, aceptada.estado());
        assertEquals(laboratorioAlternativo, aceptada.laboratorioId());
        assertEquals(pisoAlternativo, solicitud.getPisoId());
        assertEquals(fecha, aceptada.fechaReserva());
        assertEquals(LocalTime.of(12, 0), aceptada.horaInicio());
        assertEquals(LocalTime.of(14, 0), aceptada.horaFin());
        assertNull(aceptada.propuestaLaboratorioId());
        verify(reservas, never()).guardar(any());
        verify(historiales).guardar(argThat(h -> h.getEstadoAnterior() == EstadoSolicitud.EN_REVISION
                && h.getEstadoNuevo() == EstadoSolicitud.PROPUESTA));
        verify(historiales).guardar(argThat(h -> h.getEstadoAnterior() == EstadoSolicitud.PROPUESTA
                && h.getEstadoNuevo() == EstadoSolicitud.EN_REVISION));
    }

    @Test
    void soloPropietarioRespondePropuestaYPuedeRechazarla() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PROPUESTA);
        prepararBloqueo(solicitud);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.aceptarPropuesta(solicitud.getId(),
                        new ResponderPropuestaRequest(null), usuarioId));

        solicitud.setSolicitanteId(usuarioId);
        var respuesta = service.rechazarPropuesta(solicitud.getId(),
                new ResponderPropuestaRequest("prefiero original"), usuarioId);
        assertEquals(EstadoSolicitud.EN_REVISION, respuesta.estado());
    }

    @Test
    void gestorNoPuedeProponerLaboratorioFueraDeSuPiso() {
        UUID alternativo = UUID.randomUUID();
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        doThrow(new org.springframework.security.access.AccessDeniedException("otro piso"))
                .when(politica).validarGestion(alternativo);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.proponerAlternativa(solicitud.getId(),
                        new ProponerAlternativaRequest(LocalDate.now().plusDays(1),
                                LocalTime.NOON, LocalTime.of(14, 0), alternativo, null), usuarioId));
        verify(solicitudes, never()).guardar(solicitud);
    }

    @Test
    void listaBuscaFiltraYObtieneHistorial() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        Pagina<SolicitudReserva> pagina = new Pagina<>(List.of(solicitud), 0, 10, 1, 1, true, true);
        when(solicitudes.buscar(any(), eq(0), eq(10))).thenReturn(pagina);
        when(solicitudes.buscarPorSolicitante(solicitanteId, 0, 10)).thenReturn(pagina);
        when(solicitudes.buscarPorEstado(EstadoSolicitud.PENDIENTE, 0, 10)).thenReturn(pagina);
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        HistorialSolicitud historial = new HistorialSolicitud();
        historial.setId(UUID.randomUUID()); historial.setSolicitudId(solicitud.getId());
        historial.setEstadoNuevo(EstadoSolicitud.PENDIENTE); historial.setUsuarioAccionId(usuarioId);
        when(historiales.buscarPorSolicitudId(solicitud.getId(), 0, 10))
                .thenReturn(new Pagina<>(List.of(historial), 0, 10, 1, 1, true, true));
        assertEquals(1, service.listar(null, null, null, null, 0, 10).totalElementos());
        assertEquals(1, service.listarPorSolicitante(solicitanteId, 0, 10).totalElementos());
        assertEquals(1, service.listarPorEstado(EstadoSolicitud.PENDIENTE, 0, 10).totalElementos());
        assertEquals(solicitud.getId(), service.buscarPorId(solicitud.getId()).id());
        assertEquals(1, service.obtenerHistorial(solicitud.getId(), 0, 10).totalElementos());
    }

    private void prepararBloqueo(SolicitudReserva solicitud) {
        when(solicitudes.buscarPorIdParaActualizar(solicitud.getId())).thenReturn(Optional.of(solicitud));
    }

    private void prepararIdempotenciaPendiente(String clave, UUID solicitudId) {
        when(idempotenciaAprobaciones.buscarParaActualizar(clave))
                .thenReturn(Optional.of(new IdempotenciaAprobacion(
                        clave, "APROBAR_SOLICITUD", solicitudId, null)));
    }

    private CrearSolicitudReservaRequest crearRequest() {
        return new CrearSolicitudReservaRequest(solicitanteId, docenteId, laboratorioId, materiaId, periodoId,
                LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0), 10, "clase", "obs");
    }

    private ActualizarSolicitudReservaRequest actualizarRequest() {
        return new ActualizarSolicitudReservaRequest(docenteId, laboratorioId, materiaId, periodoId,
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0), 12, "actualizado", "obs2");
    }

    private SolicitudReserva solicitud(EstadoSolicitud estado) {
        SolicitudReserva s = new SolicitudReserva();
        s.setId(UUID.randomUUID()); s.setSolicitanteId(solicitanteId); s.setDocenteId(docenteId);
        s.setLaboratorioId(laboratorioId); s.setMateriaId(materiaId); s.setPeriodoLectivoId(periodoId);
        s.setFechaReserva(LocalDate.now()); s.setHoraInicio(LocalTime.of(8, 0)); s.setHoraFin(LocalTime.of(10, 0));
        s.setNumeroParticipantes(10); s.setMotivo("clase"); s.setObservacion("obs"); s.setEstado(estado); s.setVersion(0L);
        return s;
    }

    private Reserva reserva(EstadoReserva estado, UUID solicitudId) {
        Reserva r = new Reserva(); r.setId(UUID.randomUUID()); r.setSolicitudId(solicitudId);
        r.setLaboratorioId(laboratorioId); r.setResponsableId(usuarioId); r.setFechaReserva(LocalDate.now());
        r.setHoraInicio(LocalTime.of(8, 0)); r.setHoraFin(LocalTime.of(10, 0)); r.setEstado(estado);
        r.setCodigoReserva("RES-1"); r.setVersion(0L); return r;
    }
}
