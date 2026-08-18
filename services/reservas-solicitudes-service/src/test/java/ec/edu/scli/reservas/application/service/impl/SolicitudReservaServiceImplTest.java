package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.*;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.*;
import ec.edu.scli.reservas.mapper.*;
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
    private UsuariosClient usuarios;
    private AcademicoLaboratoriosClient academico;
    private DisponibilidadService disponibilidad;
    private SolicitudReservaServiceImpl service;

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
        usuarios = mock(UsuariosClient.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        disponibilidad = mock(DisponibilidadService.class);
        service = new SolicitudReservaServiceImpl(
                solicitudes, reservas, historiales,
                new SolicitudReservaMapper(), new ReservaMapper(), new HistorialSolicitudMapper(),
                usuarios, academico, disponibilidad);

        when(usuarios.obtenerPerfil(docenteId))
                .thenReturn(new PerfilExternoResponse(docenteId, true, true, List.of("DOCENTE")));
        when(academico.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(laboratorioId, true, true, "ACTIVO", 30));
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
    }

    @Test
    void creaSolicitudYRegistraHistorial() {
        when(solicitudes.buscarPorClaveIdempotencia("clave")).thenReturn(Optional.empty());
        var respuesta = service.crear(crearRequest(), "clave", usuarioId);
        assertEquals(EstadoSolicitud.PENDIENTE, respuesta.estado());
        assertEquals(laboratorioId, respuesta.laboratorioId());
        verify(historiales).guardar(argThat(h -> h.getEstadoNuevo() == EstadoSolicitud.PENDIENTE));
    }

    @Test
    void devuelveSolicitudExistentePorIdempotencia() {
        SolicitudReserva existente = solicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudes.buscarPorClaveIdempotencia("clave")).thenReturn(Optional.of(existente));
        assertEquals(existente.getId(), service.crear(crearRequest(), "clave", usuarioId).id());
        verify(solicitudes, never()).guardar(any());
    }

    @Test
    void actualizaSolicitudPermitida() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        var respuesta = service.actualizar(solicitud.getId(), actualizarRequest(), usuarioId);
        assertEquals("actualizado", respuesta.motivo());
        verify(solicitudes).guardar(solicitud);
    }

    @Test
    void rechazaActualizacionEnEstadoFinal() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.RECHAZADA);
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
        when(reservas.existePorSolicitudId(solicitud.getId())).thenReturn(false);
        var respuesta = service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, "aprobada"), "clave", usuarioId);
        assertEquals(EstadoReserva.PROGRAMADA, respuesta.estado());
        assertEquals(EstadoSolicitud.APROBADA, solicitud.getEstado());
        assertNotNull(solicitud.getReservaId());
        verify(historiales).guardar(argThat(h -> h.getEstadoNuevo() == EstadoSolicitud.APROBADA));
    }

    @Test
    void impideAprobacionDuplicada() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        when(reservas.existePorSolicitudId(solicitud.getId())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.aprobar(
                solicitud.getId(), new AprobarSolicitudRequest(usuarioId, null), "clave", usuarioId));
        verify(reservas, never()).guardar(any());
    }

    @Test
    void rechazaSolicitudYRegistraHistorial() {
        SolicitudReserva solicitud = solicitud(EstadoSolicitud.EN_REVISION);
        prepararBloqueo(solicitud);
        assertEquals(EstadoSolicitud.RECHAZADA,
                service.rechazar(solicitud.getId(), new RechazarSolicitudRequest("no cumple"), usuarioId).estado());
        verify(historiales).guardar(argThat(h -> "no cumple".equals(h.getComentario())));
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
