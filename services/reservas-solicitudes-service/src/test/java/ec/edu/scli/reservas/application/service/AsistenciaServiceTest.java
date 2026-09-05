package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.model.EstadoSesionAsistencia;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SesionAsistenciaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RegistroAsistenciaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SesionAsistenciaJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.AbrirSesionAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.domain.port.out.EstudianteInstitucionalPort;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import java.util.List;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.client.dto.MateriaContextoExternoResponse;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.client.dto.DocenteExternoResponse;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RegistroAsistenciaJpaEntity;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsistenciaServiceTest {
    private SesionAsistenciaJpaRepository sesiones;
    private RegistroAsistenciaJpaRepository registros;
    private ReservaService reservas;
    private AsistenciaService service;
    private EstudianteInstitucionalPort estudiantes;
    private ReservaRepositoryPort reservaRepository;
    private SolicitudReservaRepositoryPort solicitudRepository;
    private AcademicoLaboratoriosClient academico;
    private PlanificacionJpaRepository bloques;
    private PlanificacionAgregadaJpaRepository planes;
    private UsuariosClient usuarios;
    private NotificacionService notificaciones;

    @BeforeEach void preparar() {
        sesiones = mock(SesionAsistenciaJpaRepository.class);
        registros = mock(RegistroAsistenciaJpaRepository.class);
        reservas = mock(ReservaService.class);
        estudiantes = mock(EstudianteInstitucionalPort.class);
        reservaRepository = mock(ReservaRepositoryPort.class);
        solicitudRepository = mock(SolicitudReservaRepositoryPort.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        bloques = mock(PlanificacionJpaRepository.class); planes = mock(PlanificacionAgregadaJpaRepository.class); usuarios=mock(UsuariosClient.class);notificaciones=mock(NotificacionService.class);
        when(estudiantes.resolverEstudianteActivo(any())).thenAnswer(i -> i.getArgument(0));
        service = new AsistenciaService(sesiones, registros, reservas, estudiantes,
                reservaRepository, solicitudRepository, academico, bloques, planes, usuarios, notificaciones, 15);
        when(sesiones.save(any())).thenAnswer(i -> { SesionAsistenciaJpaEntity value=i.getArgument(0); if(value.getId()==null)value.setId(UUID.randomUUID()); return value; });
        when(registros.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void propietarioAbreYAjenoNoPuede() {
        UUID reservaId = UUID.randomUUID(), docente = UUID.randomUUID();
        when(reservas.buscarPorId(reservaId)).thenReturn(reserva(reservaId, docente));
        assertThat(service.abrir(new AbrirSesionAsistenciaRequest(reservaId), docente).token()).isNotBlank();
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(reservaId), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void registraSoloUnaVezConTokenValido() throws Exception {
        UUID sesionId = UUID.randomUUID(), estudiante = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = sesion(sesionId, "secreto", Instant.now().plusSeconds(60));
        when(sesiones.findById(sesionId)).thenReturn(Optional.of(sesion));
        assertThat(service.registrar(sesionId, new RegistrarAsistenciaRequest("secreto"), estudiante)).isNotNull();
        when(registros.existsBySesionIdAndEstudianteId(sesionId, estudiante)).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(sesionId, new RegistrarAsistenciaRequest("secreto"), estudiante))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("registrada");
    }

    @Test void rechazaTokenInvalidoVencidoYCerrado() throws Exception {
        UUID id = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = sesion(id, "correcto", Instant.now().plusSeconds(60));
        when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("incorrecto"), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        sesion = sesion(id, "correcto", Instant.now().minusSeconds(1)); when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("correcto"), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("vencida");
        sesion = sesion(id, "correcto", Instant.now().plusSeconds(60)); sesion.setEstado(EstadoSesionAsistencia.CERRADA);
        when(sesiones.findById(id)).thenReturn(Optional.of(sesion));
        assertThatThrownBy(() -> service.registrar(id, new RegistrarAsistenciaRequest("correcto"), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("abierta");
    }

    @Test void soloPropietarioConsultaListaYCierraEHistorialEsPropio() {
        UUID id = UUID.randomUUID(), docente = UUID.randomUUID(), estudiante = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = new SesionAsistenciaJpaEntity(); sesion.setId(id); sesion.setDocenteId(docente);
        sesion.setReservaId(UUID.randomUUID()); sesion.setEstado(EstadoSesionAsistencia.ABIERTA);
        when(sesiones.findByIdAndDocenteId(id, docente)).thenReturn(Optional.of(sesion));
        service.consultar(id, docente); service.listar(id, docente); service.cerrar(id, docente); service.historial(estudiante);
        verify(registros).findByEstudianteId(estudiante);
        assertThatThrownBy(() -> service.consultar(id, UUID.randomUUID())).isInstanceOf(AccessDeniedException.class);
    }

    @Test void estudianteDescubreYRegistraSesionDeSuCarreraSinIdsTecnicos() throws Exception {
        UUID perfil = UUID.randomUUID(), estudiante = UUID.randomUUID(), carrera = UUID.randomUUID();
        UUID sesionId = UUID.randomUUID(), reservaId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID(), materiaId = UUID.randomUUID();
        SesionAsistenciaJpaEntity sesion = sesion(sesionId, "interno", Instant.now().plusSeconds(60));
        sesion.setReservaId(reservaId);
        Reserva reserva = new Reserva(); reserva.setId(reservaId); reserva.setSolicitudId(solicitudId);
        SolicitudReserva solicitud = new SolicitudReserva(); solicitud.setId(solicitudId); solicitud.setMateriaId(materiaId);
        when(estudiantes.resolverContextoActivo(perfil)).thenReturn(new EstudianteInstitucionalPort.Contexto(
                estudiante, perfil, carrera, UUID.randomUUID(), 7));
        when(estudiantes.resolverEstudianteActivo(perfil)).thenReturn(estudiante);
        when(sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA)).thenReturn(List.of(sesion));
        when(sesiones.findById(sesionId)).thenReturn(Optional.of(sesion));
        when(reservaRepository.buscarPorId(reservaId)).thenReturn(Optional.of(reserva));
        when(solicitudRepository.buscarPorId(solicitudId)).thenReturn(Optional.of(solicitud));
        when(academico.obtenerContextoMateria(materiaId))
                .thenReturn(new MateriaContextoExternoResponse(materiaId, carrera, true, true));

        assertThat(service.sesionesAbiertas(perfil)).hasSize(1);
        assertThat(service.registrarPropia(sesionId, perfil)).isNotNull();
        verify(registros).save(any());
    }

    @Test void sesionPlanificadaExigeMismaCarreraNivelYCiclo() throws Exception {
        UUID perfil=UUID.randomUUID(), estudiante=UUID.randomUUID(), carrera=UUID.randomUUID(), periodo=UUID.randomUUID();
        UUID bloqueId=UUID.randomUUID(), planId=UUID.randomUUID(), sesionId=UUID.randomUUID();
        var bloque=new PlanificacionJpaEntity();bloque.setId(bloqueId);bloque.setPlanificacionId(planId);bloque.setNivel(7);
        var plan=new PlanificacionAgregadaJpaEntity();plan.setId(planId);plan.setCarreraId(carrera);plan.setPeriodoId(periodo);plan.setEstado(EstadoPlanificacionAgregada.APROBADA);
        var sesion=sesion(sesionId,"interno",Instant.now().plusSeconds(60));sesion.setBloquePlanificacionId(bloqueId);
        when(sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA)).thenReturn(List.of(sesion));when(sesiones.findById(sesionId)).thenReturn(Optional.of(sesion));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(estudiantes.resolverContextoActivo(perfil)).thenReturn(new EstudianteInstitucionalPort.Contexto(estudiante,perfil,carrera,periodo,7));
        when(estudiantes.resolverEstudianteActivo(perfil)).thenReturn(estudiante);
        assertThat(service.sesionesAbiertas(perfil)).hasSize(1);assertThat(service.registrarPropia(sesionId,perfil)).isNotNull();
        UUID otro=UUID.randomUUID();when(estudiantes.resolverContextoActivo(otro)).thenReturn(new EstudianteInstitucionalPort.Contexto(UUID.randomUUID(),otro,carrera,periodo,8));
        assertThat(service.sesionesAbiertas(otro)).isEmpty();assertThatThrownBy(()->service.registrarPropia(sesionId,otro)).isInstanceOf(AccessDeniedException.class);
        UUID otraCarrera=UUID.randomUUID();when(estudiantes.resolverContextoActivo(otro)).thenReturn(new EstudianteInstitucionalPort.Contexto(UUID.randomUUID(),otro,otraCarrera,periodo,7));
        assertThat(service.sesionesAbiertas(otro)).isEmpty();
    }

    @Test void abrirReservaValidaSeleccionResponsableYDuplicado() {
        UUID reservaId = UUID.randomUUID(), docente = UUID.randomUUID();
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, null), docente))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("planificada");
        when(reservas.buscarPorId(reservaId)).thenReturn(reserva(reservaId, docente));
        when(sesiones.findFirstByReservaIdAndEstado(reservaId, EstadoSesionAsistencia.ABIERTA))
                .thenReturn(Optional.of(new SesionAsistenciaJpaEntity()));
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(reservaId), docente))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("sesion abierta");
    }

    @Test void abrirBloqueValidaPlanDocenteHorarioYDuplicado() {
        UUID bloqueId = UUID.randomUUID(), planId = UUID.randomUUID(), docenteId = UUID.randomUUID(), perfil = UUID.randomUUID();
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil))
                .isInstanceOf(ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException.class);

        PlanificacionJpaEntity bloque = bloque(bloqueId, planId, 7);
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil))
                .isInstanceOf(ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException.class);

        PlanificacionAgregadaJpaEntity plan = plan(planId, UUID.randomUUID(), UUID.randomUUID(), EstadoPlanificacionAgregada.BORRADOR);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("aprobada");

        plan.setEstado(EstadoPlanificacionAgregada.APROBADA);
        bloque.setDocenteId(docenteId);
        when(usuarios.obtenerDocentePorId(docenteId)).thenReturn(new DocenteExternoResponse(docenteId, UUID.randomUUID(), true));
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil))
                .isInstanceOf(AccessDeniedException.class);

        when(usuarios.obtenerDocentePorId(docenteId)).thenReturn(new DocenteExternoResponse(docenteId, perfil, true));
        bloque.setDiaSemana(diaActual()); bloque.setHoraInicio(LocalTime.MIN); bloque.setHoraFin(LocalTime.MAX);
        when(sesiones.findFirstByBloquePlanificacionIdAndFechaClaseAndEstado(eq(bloqueId), any(), eq(EstadoSesionAsistencia.ABIERTA)))
                .thenReturn(Optional.of(new SesionAsistenciaJpaEntity()));
        assertThatThrownBy(() -> service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("abierta");

        when(sesiones.findFirstByBloquePlanificacionIdAndFechaClaseAndEstado(eq(bloqueId), any(), eq(EstadoSesionAsistencia.ABIERTA)))
                .thenReturn(Optional.empty());
        UUID estudiantePerfil=UUID.randomUUID();when(usuarios.obtenerEstudiantesCompatibles(plan.getCarreraId(),plan.getPeriodoId(),bloque.getNivel())).thenReturn(List.of(estudiantePerfil));
        assertThat(service.abrir(new AbrirSesionAsistenciaRequest(null, bloqueId), perfil).token()).isNotBlank();
        verify(notificaciones).notificarPerfilIdempotente(eq(estudiantePerfil),contains("ASISTENCIA:"),eq("Asistencia disponible"),anyString(),anyMap());
    }

    @Test void sesionesAbiertasDescartanVencidasYContextosIncompletos() throws Exception {
        UUID perfil = UUID.randomUUID(), carrera = UUID.randomUUID(), periodo = UUID.randomUUID();
        var contexto = new EstudianteInstitucionalPort.Contexto(UUID.randomUUID(), perfil, carrera, periodo, 7);
        when(estudiantes.resolverContextoActivo(perfil)).thenReturn(contexto);
        var vencida = sesion(UUID.randomUUID(), "a", Instant.now().minusSeconds(1));
        var sinReserva = sesion(UUID.randomUUID(), "b", Instant.now().plusSeconds(60)); sinReserva.setReservaId(UUID.randomUUID());
        var sinBloque = sesion(UUID.randomUUID(), "c", Instant.now().plusSeconds(60)); sinBloque.setBloquePlanificacionId(UUID.randomUUID());
        when(sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA)).thenReturn(List.of(vencida, sinReserva, sinBloque));
        assertThat(service.sesionesAbiertas(perfil)).isEmpty();
    }

    @Test void cerrarRespetaEstadosYOwnership() {
        UUID id = UUID.randomUUID(), docente = UUID.randomUUID();
        var sesion = new SesionAsistenciaJpaEntity(); sesion.setId(id); sesion.setDocenteId(docente);
        when(sesiones.findByIdAndDocenteId(id, docente)).thenReturn(Optional.of(sesion));
        sesion.setEstado(EstadoSesionAsistencia.CERRADA);
        service.cerrar(id, docente);
        verify(sesiones, never()).save(any());
        sesion.setEstado(EstadoSesionAsistencia.VENCIDA);
        assertThatThrownBy(() -> service.cerrar(id, docente)).isInstanceOf(IllegalStateException.class).hasMessageContaining("vencida");
        sesion.setEstado(EstadoSesionAsistencia.ABIERTA);
        service.cerrar(id, docente);
        assertThat(sesion.getEstado()).isEqualTo(EstadoSesionAsistencia.CERRADA);
        assertThat(sesion.getCerradaEn()).isNotNull();
        verify(sesiones).save(sesion);
    }

    @Test void historialPorPeriodoFiltraSesionesDelContextoYMapeaBloque() {
        UUID perfil = UUID.randomUUID(), estudiante = UUID.randomUUID(), carrera = UUID.randomUUID(), periodo = UUID.randomUUID();
        UUID sesionValidaId = UUID.randomUUID(), sesionAjenaId = UUID.randomUUID(), bloqueId = UUID.randomUUID(), planId = UUID.randomUUID();
        when(estudiantes.resolverContexto(perfil, periodo)).thenReturn(new EstudianteInstitucionalPort.Contexto(estudiante, perfil, carrera, periodo, 7));
        when(estudiantes.resolverEstudianteActivo(perfil)).thenReturn(estudiante);
        var valido = registro(UUID.randomUUID(), sesionValidaId, estudiante);
        var ajeno = registro(UUID.randomUUID(), sesionAjenaId, estudiante);
        when(registros.findByEstudianteId(estudiante)).thenReturn(List.of(valido, ajeno));
        var sesionValida = new SesionAsistenciaJpaEntity(); sesionValida.setId(sesionValidaId); sesionValida.setBloquePlanificacionId(bloqueId);
        when(sesiones.findById(sesionValidaId)).thenReturn(Optional.of(sesionValida));
        when(sesiones.findById(sesionAjenaId)).thenReturn(Optional.empty());
        var bloque = bloque(bloqueId, planId, 7); when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(planes.findById(planId)).thenReturn(Optional.of(plan(planId, carrera, periodo, EstadoPlanificacionAgregada.APROBADA)));
        assertThat(service.historial(perfil, periodo)).singleElement().extracting(r -> r.bloqueId()).isEqualTo(bloqueId);
        service.historial(perfil, null);
        verify(registros, times(2)).findByEstudianteId(estudiante);
    }

    @Test void horarioSoloExponePlanAprobadoYNivelDelContexto() {
        UUID perfil = UUID.randomUUID(), carrera = UUID.randomUUID(), periodo = UUID.randomUUID(), planId = UUID.randomUUID();
        when(estudiantes.resolverContexto(perfil, periodo)).thenReturn(new EstudianteInstitucionalPort.Contexto(UUID.randomUUID(), perfil, carrera, periodo, 7));
        when(planes.findByCarreraIdAndPeriodoId(carrera, periodo)).thenReturn(Optional.empty());
        assertThat(service.horario(perfil, periodo)).isEmpty();
        var plan = plan(planId, carrera, periodo, EstadoPlanificacionAgregada.BORRADOR);
        when(planes.findByCarreraIdAndPeriodoId(carrera, periodo)).thenReturn(Optional.of(plan));
        assertThat(service.horario(perfil, periodo)).isEmpty();
        plan.setEstado(EstadoPlanificacionAgregada.APROBADA);
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(bloque(UUID.randomUUID(), planId, 7), bloque(UUID.randomUUID(), planId, 8)));
        assertThat(service.horario(perfil, periodo)).hasSize(1).allMatch(b -> b.nivel() == 7);
    }

    @Test void clasesDocenteHoyExigeDocenteActivoYPlanAprobado() {
        UUID perfil = UUID.randomUUID(), docenteId = UUID.randomUUID(), planId = UUID.randomUUID();
        assertThatThrownBy(() -> service.clasesDocenteHoy(perfil)).isInstanceOf(AccessDeniedException.class);
        when(usuarios.obtenerDocentePorPerfil(perfil)).thenReturn(new DocenteExternoResponse(docenteId, perfil, false));
        assertThatThrownBy(() -> service.clasesDocenteHoy(perfil)).isInstanceOf(AccessDeniedException.class);
        when(usuarios.obtenerDocentePorPerfil(perfil)).thenReturn(new DocenteExternoResponse(docenteId, perfil, true));
        var sinPlan = bloque(UUID.randomUUID(), null, 7);
        var noAprobado = bloque(UUID.randomUUID(), UUID.randomUUID(), 7);
        var aprobado = bloque(UUID.randomUUID(), planId, 7);
        when(bloques.findByDocenteIdAndDiaSemana(docenteId, diaActual())).thenReturn(List.of(sinPlan, noAprobado, aprobado));
        when(planes.findById(noAprobado.getPlanificacionId())).thenReturn(Optional.of(plan(noAprobado.getPlanificacionId(), UUID.randomUUID(), UUID.randomUUID(), EstadoPlanificacionAgregada.BORRADOR)));
        when(planes.findById(planId)).thenReturn(Optional.of(plan(planId, UUID.randomUUID(), UUID.randomUUID(), EstadoPlanificacionAgregada.APROBADA)));
        assertThat(service.clasesDocenteHoy(perfil)).singleElement().extracting(r -> r.id()).isEqualTo(aprobado.getId());
    }

    @Test void registrarPropiaRechazaSesionCerradaYDuplicada() throws Exception {
        UUID perfil = UUID.randomUUID(), estudiante = UUID.randomUUID(), carrera = UUID.randomUUID(), periodo = UUID.randomUUID();
        UUID sesionId = UUID.randomUUID(), bloqueId = UUID.randomUUID(), planId = UUID.randomUUID();
        var contexto = new EstudianteInstitucionalPort.Contexto(estudiante, perfil, carrera, periodo, 7);
        when(estudiantes.resolverContextoActivo(perfil)).thenReturn(contexto); when(estudiantes.resolverEstudianteActivo(perfil)).thenReturn(estudiante);
        var sesion = sesion(sesionId, "x", Instant.now().plusSeconds(60)); sesion.setBloquePlanificacionId(bloqueId);
        when(sesiones.findById(sesionId)).thenReturn(Optional.of(sesion));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque(bloqueId, planId, 7)));
        when(planes.findById(planId)).thenReturn(Optional.of(plan(planId, carrera, periodo, EstadoPlanificacionAgregada.APROBADA)));
        sesion.setEstado(EstadoSesionAsistencia.CERRADA);
        assertThatThrownBy(() -> service.registrarPropia(sesionId, perfil)).isInstanceOf(IllegalStateException.class).hasMessageContaining("abierta");
        sesion.setEstado(EstadoSesionAsistencia.ABIERTA); when(registros.existsBySesionIdAndEstudianteId(sesionId, estudiante)).thenReturn(true);
        assertThatThrownBy(() -> service.registrarPropia(sesionId, perfil)).isInstanceOf(IllegalStateException.class).hasMessageContaining("registrada");
    }

    private PlanificacionJpaEntity bloque(UUID id, UUID planId, int nivel) {
        var b = new PlanificacionJpaEntity(); b.setId(id); b.setPlanificacionId(planId); b.setNivel(nivel);
        b.setPeriodoId(UUID.randomUUID()); b.setCarreraId(UUID.randomUUID()); b.setMateriaId(UUID.randomUUID());
        b.setDocenteId(UUID.randomUUID()); b.setLaboratorioId(UUID.randomUUID()); b.setDiaSemana(diaActual());
        b.setHoraInicio(LocalTime.of(8, 0)); b.setHoraFin(LocalTime.of(10, 0)); b.setEstado(EstadoPlanificacion.BORRADOR);
        return b;
    }

    private PlanificacionAgregadaJpaEntity plan(UUID id, UUID carrera, UUID periodo, EstadoPlanificacionAgregada estado) {
        var p = new PlanificacionAgregadaJpaEntity(); p.setId(id); p.setCarreraId(carrera); p.setPeriodoId(periodo); p.setEstado(estado); return p;
    }

    private RegistroAsistenciaJpaEntity registro(UUID id, UUID sesionId, UUID estudiante) {
        var r = new RegistroAsistenciaJpaEntity(); r.setId(id); r.setSesionId(sesionId); r.setEstudianteId(estudiante); r.setRegistradaEn(Instant.now()); return r;
    }

    private String diaActual() {
        return switch (LocalDate.now(ZoneId.of("America/Guayaquil")).getDayOfWeek()) {
            case MONDAY -> "LUNES"; case TUESDAY -> "MARTES"; case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES"; case FRIDAY -> "VIERNES"; case SATURDAY -> "SABADO"; case SUNDAY -> "DOMINGO";
        };
    }

    private SesionAsistenciaJpaEntity sesion(UUID id, String token, Instant expira) throws Exception {
        SesionAsistenciaJpaEntity s = new SesionAsistenciaJpaEntity(); s.setId(id); s.setEstado(EstadoSesionAsistencia.ABIERTA);
        s.setExpiraEn(expira); s.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)))); return s;
    }

    private ReservaResponse reserva(UUID id, UUID responsable) {
        return new ReservaResponse(id, UUID.randomUUID(), UUID.randomUUID(), responsable,
                null, null, null, null, null, null, null, null);
    }
}
