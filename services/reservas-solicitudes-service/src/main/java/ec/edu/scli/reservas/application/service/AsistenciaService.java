package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.*;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.*;
import ec.edu.scli.reservas.infrastructure.persistence.entity.*;
import ec.edu.scli.reservas.infrastructure.persistence.repository.*;
import ec.edu.scli.reservas.presentation.dto.request.*;
import ec.edu.scli.reservas.presentation.dto.response.*;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AsistenciaService {
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private final SesionAsistenciaJpaRepository sesiones;
    private final RegistroAsistenciaJpaRepository registros;
    private final ParticipanteUsoLaboratorioJpaRepository participantes;
    private final ReservaService reservas;
    private final long minutos;
    private final EstudianteInstitucionalPort estudiantes;
    private final ReservaRepositoryPort reservaRepository;
    private final SolicitudReservaRepositoryPort solicitudRepository;
    private final AcademicoLaboratoriosClient academico;
    private final PlanificacionJpaRepository bloques;
    private final PlanificacionAgregadaJpaRepository planes;
    private final UsuariosClient usuarios;
    private final NotificacionService notificaciones;
    private final PoliticaAmbitoLaboratorio ambito;
    private final SecureRandom random = new SecureRandom();

    public AsistenciaService(SesionAsistenciaJpaRepository sesiones, RegistroAsistenciaJpaRepository registros,
            ParticipanteUsoLaboratorioJpaRepository participantes, ReservaService reservas,
            EstudianteInstitucionalPort estudiantes, ReservaRepositoryPort reservaRepository,
            SolicitudReservaRepositoryPort solicitudRepository, AcademicoLaboratoriosClient academico,
            PlanificacionJpaRepository bloques, PlanificacionAgregadaJpaRepository planes,
            UsuariosClient usuarios, NotificacionService notificaciones, PoliticaAmbitoLaboratorio ambito,
            @Value("${app.asistencia.window-minutes:15}") long minutos) {
        this.sesiones=sesiones; this.registros=registros; this.participantes=participantes; this.reservas=reservas;
        this.estudiantes=estudiantes; this.reservaRepository=reservaRepository; this.solicitudRepository=solicitudRepository;
        this.academico=academico; this.bloques=bloques; this.planes=planes; this.usuarios=usuarios;
        this.notificaciones=notificaciones; this.ambito=ambito; this.minutos=minutos;
    }

    @Transactional
    public SesionAsistenciaResponse abrir(AbrirSesionAsistenciaRequest request, UUID actor) {
        if (request.bloqueId()!=null) return abrirBloque(request.bloqueId(),actor);
        if (request.reservaId()==null) throw new IllegalArgumentException("Debe seleccionar un uso planificado");
        var reserva=reservas.buscarPorId(request.reservaId());
        if(!actor.equals(reserva.responsableId())) throw new AccessDeniedException("La reserva no pertenece al docente");
        var abierta=sesiones.findFirstByReservaIdAndEstado(request.reservaId(),EstadoSesionAsistencia.ABIERTA);
        if(abierta.isPresent()) throw new IllegalStateException("La reserva ya tiene una sesión abierta");
        Instant now=Instant.now(); String token=tokenSeguro(); var entity=new SesionAsistenciaJpaEntity();
        entity.setReservaId(request.reservaId()); entity.setDocenteId(actor); entity.setAbiertaEn(now);
        entity.setExpiraEn(now.plusSeconds(minutos*60)); entity.setTokenHash(hash(token));
        return response(sesiones.save(entity),token);
    }

    private SesionAsistenciaResponse abrirBloque(UUID bloqueId,UUID actor){
        var bloque=bloques.findById(bloqueId).orElseThrow(()->new ResourceNotFoundException("Bloque no encontrado"));
        var plan=planes.findById(bloque.getPlanificacionId()).orElseThrow(()->new ResourceNotFoundException("Planificación no encontrada"));
        if(plan.getEstado()!=EstadoPlanificacionAgregada.APROBADA) throw new IllegalStateException("La planificación no está aprobada");
        if(bloque.getEstado()==EstadoPlanificacion.CANCELADA) throw new IllegalStateException("El bloque está cancelado");
        var docente=usuarios.obtenerDocentePorId(bloque.getDocenteId());
        if(docente==null||!docente.activo()||!actor.equals(docente.perfilId()))
            throw new AccessDeniedException("El bloque no pertenece al docente autenticado");
        LocalDate fecha=LocalDate.now(ZONA); LocalTime hora=LocalTime.now(ZONA);
        var periodo=academico.obtenerPeriodo(plan.getPeriodoId());
        if(periodo==null||periodo.fechaInicio()==null||periodo.fechaFin()==null
                ||fecha.isBefore(periodo.fechaInicio())||fecha.isAfter(periodo.fechaFin()))
            throw new IllegalStateException("El período de la planificación no está vigente");
        if(!dia(fecha).equalsIgnoreCase(bloque.getDiaSemana())) throw new IllegalStateException("El uso no corresponde al día actual");
        if(hora.isBefore(bloque.getHoraInicio())||hora.isAfter(bloque.getHoraFin()))
            throw new IllegalStateException("El uso no está dentro de su horario");
        var existente=sesiones.findByBloquePlanificacionIdAndFechaClase(bloqueId,fecha);
        if(existente.isPresent()) return response(existente.get(),null);
        var laboratorio=academico.obtenerLaboratorio(bloque.getLaboratorioId());
        if(laboratorio==null||!laboratorio.existe()||laboratorio.pisoId()==null)
            throw new IllegalStateException("No fue posible resolver el laboratorio planificado");
        Instant now=Instant.now(); String token=tokenSeguro(); var entity=new SesionAsistenciaJpaEntity();
        entity.setBloquePlanificacionId(bloqueId); entity.setFechaClase(fecha); entity.setDocenteId(actor);
        entity.setAbiertaEn(now); entity.setExpiraEn(now.plusSeconds(minutos*60)); entity.setTokenHash(hash(token));
        entity.setCarreraIdSnapshot(plan.getCarreraId()); entity.setPeriodoIdSnapshot(plan.getPeriodoId());
        entity.setNivelSnapshot(bloque.getNivel()); entity.setMateriaIdSnapshot(bloque.getMateriaId());
        entity.setDocenteIdSnapshot(bloque.getDocenteId()); entity.setLaboratorioIdSnapshot(bloque.getLaboratorioId());
        entity.setPisoIdSnapshot(laboratorio.pisoId()); entity.setDiaSemanaSnapshot(bloque.getDiaSemana());
        entity.setHoraInicioSnapshot(bloque.getHoraInicio()); entity.setHoraFinSnapshot(bloque.getHoraFin());
        entity=sesiones.saveAndFlush(entity);
        List<UUID> perfiles=usuarios.obtenerEstudiantesCompatibles(plan.getCarreraId(),plan.getPeriodoId(),bloque.getNivel())
                .stream().distinct().toList();
        for(UUID perfil:perfiles){var p=new ParticipanteUsoLaboratorioJpaEntity();p.setSesionId(entity.getId());
            p.setEstudiantePerfilId(perfil);participantes.save(p);notificaciones.notificarPerfilIdempotente(perfil,
                    "USO_LABORATORIO:"+entity.getId()+":"+perfil,"Registro de presencia disponible",
                    "Registro de presencia disponible para el laboratorio planificado",
                    Map.of("tipo","USO_LABORATORIO_ABIERTO","referenciaId",entity.getId().toString()));}
        return response(entity,token);
    }

    @Transactional
    public RegistroAsistenciaResponse registrar(UUID id,RegistrarAsistenciaRequest request,UUID perfil){
        estudiantes.resolverEstudianteActivo(perfil); var sesion=obtener(id); Instant now=Instant.now();
        if(request.token()==null||!MessageDigest.isEqual(hash(request.token()).getBytes(StandardCharsets.US_ASCII),
                sesion.getTokenHash().getBytes(StandardCharsets.US_ASCII))) throw new AccessDeniedException("Token inválido");
        validarAbierta(sesion,now);
        var encontrado=participantes.findBySesionIdAndEstudiantePerfilId(id,perfil);
        if(encontrado.isEmpty()&&sesion.getBloquePlanificacionId()==null){UUID estudiante=estudiantes.resolverEstudianteActivo(perfil);
            if(registros.existsBySesionIdAndEstudianteId(id,estudiante))throw new IllegalStateException("La asistencia ya fue registrada");
            var antiguo=new RegistroAsistenciaJpaEntity();antiguo.setSesionId(id);antiguo.setEstudianteId(estudiante);antiguo.setRegistradaEn(now);
            return mapAntiguo(registros.save(antiguo));}
        var participante=encontrado.orElseThrow(()->new AccessDeniedException("El estudiante no pertenece al padrón de este uso"));
        if(participante.getEstado()==EstadoParticipanteUso.PRESENTE) return mapRegistro(participante);
        participante.setEstado(EstadoParticipanteUso.PRESENTE); participante.setRegistradoEn(now);
        participante.setRegistradoPorPerfilId(perfil); participantes.save(participante);
        return mapRegistro(participante);
    }

    @Transactional
    public RegistroAsistenciaResponse registrarPropia(UUID id,UUID perfil){
        throw new IllegalArgumentException("El registro de presencia requiere el token temporal del QR");
    }

    @Transactional
    public SesionAsistenciaResponse completar(UUID id,CompletarUsoLaboratorioRequest request,UUID actor){
        var sesion=sesionPropia(id,actor); if(sesion.getEstado()!=EstadoSesionAsistencia.ABIERTA)
            throw new IllegalStateException("El registro de uso ya está cerrado");
        sesion.setTemaActividad(request.temaActividad().trim()); sesion.setObservacionUso(limpiar(request.observacionUso()));
        return response(sesiones.save(sesion),null);
    }

    @Transactional
    public ParticipanteUsoResponse ajustar(UUID id,UUID perfilEstudiante,AjustarPresenciaRequest request,UUID actor){
        var sesion=sesionPropia(id,actor); if(sesion.getEstado()!=EstadoSesionAsistencia.ABIERTA)
            throw new IllegalStateException("El registro de uso ya está cerrado");
        var p=participantes.findBySesionIdAndEstudiantePerfilId(id,perfilEstudiante)
                .orElseThrow(()->new ResourceNotFoundException("Participante no encontrado"));
        p.setEstado(EstadoParticipanteUso.valueOf(request.estado())); p.setRegistradoEn(Instant.now());
        p.setRegistradoPorPerfilId(actor); p.setObservacion(request.observacion().trim());
        return mapParticipante(participantes.save(p));
    }

    @Transactional
    public void cerrar(UUID id,UUID actor){var s=sesionPropia(id,actor);if(s.getEstado()==EstadoSesionAsistencia.CERRADA)return;
        if(s.getEstado()==EstadoSesionAsistencia.VENCIDA)throw new IllegalStateException("La sesión está vencida");
        if(s.getBloquePlanificacionId()!=null&&(s.getTemaActividad()==null||s.getTemaActividad().isBlank()))throw new IllegalStateException("Debe registrar el tema o actividad");
        s.setEstado(EstadoSesionAsistencia.CERRADA);s.setCerradaEn(Instant.now());sesiones.save(s);
        var items=participantes.findBySesionIdOrderByEstudiantePerfilId(id);items.stream()
                .filter(p->p.getEstado()==EstadoParticipanteUso.PENDIENTE).forEach(p->p.setEstado(EstadoParticipanteUso.AUSENTE));
        participantes.saveAll(items);}

    @Transactional(readOnly=true) public SesionAsistenciaResponse consultar(UUID id,UUID actor){return response(sesionPropia(id,actor),null);}
    @Transactional(readOnly=true) public List<ParticipanteUsoResponse> participantes(UUID id,UUID actor){sesionPropia(id,actor);
        return participantes.findBySesionIdOrderByEstudiantePerfilId(id).stream().map(this::mapParticipante).toList();}
    @Transactional(readOnly=true) public List<RegistroAsistenciaResponse> listar(UUID id,UUID actor){return participantes(id,actor).stream()
        .filter(p->"PRESENTE".equals(p.estado())).map(p->new RegistroAsistenciaResponse(p.id(),p.sesionId(),p.estudiantePerfilId(),
                obtener(id).getBloquePlanificacionId(),p.registradoEn(),p.estado())).toList();}

    @Transactional(readOnly=true)
    public List<SesionAsistenciaResponse> usosPiso(LocalDate fecha,UUID periodoId,UUID laboratorioId,UUID materiaId,
            UUID docenteId,String estado){UUID piso=ambito.pisoGestionado();return sesiones.findByPisoIdSnapshot(piso).stream()
        .filter(s->fecha==null||fecha.equals(s.getFechaClase())).filter(s->periodoId==null||periodoId.equals(s.getPeriodoIdSnapshot()))
        .filter(s->laboratorioId==null||laboratorioId.equals(s.getLaboratorioIdSnapshot()))
        .filter(s->materiaId==null||materiaId.equals(s.getMateriaIdSnapshot()))
        .filter(s->docenteId==null||docenteId.equals(s.getDocenteIdSnapshot()))
        .filter(s->estado==null||estado.equalsIgnoreCase(s.getEstado().name())).map(s->response(s,null)).toList();}
    @Transactional(readOnly=true) public SesionAsistenciaResponse usoPiso(UUID id){UUID piso=ambito.pisoGestionado();var s=obtener(id);
        if(!piso.equals(s.getPisoIdSnapshot()))throw new AccessDeniedException("El uso no pertenece al piso administrado");return response(s,null);}
    @Transactional(readOnly=true) public List<ParticipanteUsoResponse> participantesPiso(UUID id){usoPiso(id);
        return participantes.findBySesionIdOrderByEstudiantePerfilId(id).stream().map(this::mapParticipante).toList();}

    @Transactional(readOnly=true) public List<SesionAsistenciaResponse> sesionesAbiertas(UUID perfil){
        var contexto=estudiantes.resolverContextoActivo(perfil);Instant now=Instant.now();return sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA).stream()
                .filter(s->!now.isAfter(s.getExpiraEn())).filter(s->participantes.findBySesionIdAndEstudiantePerfilId(s.getId(),perfil).isPresent()||perteneceContextoAntiguo(s,contexto))
                .map(s->response(s,null)).toList();}
    @Transactional(readOnly=true) public List<RegistroAsistenciaResponse> historial(UUID perfil){var nuevos=participantes.findAll().stream()
        .filter(p->perfil.equals(p.getEstudiantePerfilId())&&p.getEstado()!=EstadoParticipanteUso.PENDIENTE).map(this::mapRegistro).toList();
        if(!nuevos.isEmpty())return nuevos;UUID estudiante=estudiantes.resolverEstudianteActivo(perfil);return registros.findByEstudianteId(estudiante).stream().map(this::mapAntiguo).toList();}
    @Transactional(readOnly=true) public List<RegistroAsistenciaResponse> historial(UUID perfil,UUID periodo){if(periodo==null)return historial(perfil);return historial(perfil).stream()
        .filter(r->sesiones.findById(r.sesionId()).map(s->periodo.equals(s.getPeriodoIdSnapshot())
                ||perteneceContextoAntiguo(s,estudiantes.resolverContexto(perfil,periodo))).orElse(false)).toList();}

    @Transactional(readOnly=true) public List<PlanificacionResponse> horario(UUID perfil,UUID periodo){var contexto=periodo==null
        ?estudiantes.resolverContextoActivo(perfil):estudiantes.resolverContexto(perfil,periodo);var plan=planes
        .findByCarreraIdAndPeriodoId(contexto.carreraId(),contexto.periodoId()).orElse(null);if(!horarioDefinitivo(plan))return List.of();
        return bloques.findByPlanificacionId(plan.getId()).stream().filter(b->b.getEstado()!=EstadoPlanificacion.CANCELADA)
                .filter(b->contexto.nivel().equals(b.getNivel())).map(this::mapBloque).toList();}
    @Transactional(readOnly=true) public List<PlanificacionResponse> clasesDocenteHoy(UUID perfil){var d=docente(perfil);
        return bloques.findByDocenteIdAndDiaSemana(d.docenteId(),dia(LocalDate.now(ZONA))).stream()
                .filter(b->b.getEstado()!=EstadoPlanificacion.CANCELADA).filter(b->planes.findById(b.getPlanificacionId()).map(this::horarioDefinitivo).orElse(false))
                .map(this::mapBloque).toList();}
    @Transactional(readOnly=true) public List<PlanificacionResponse> horarioDocente(UUID perfil,UUID periodo){var d=docente(perfil);
        return bloques.findAll().stream().filter(b->d.docenteId().equals(b.getDocenteId()))
                .filter(b->b.getEstado()!=EstadoPlanificacion.CANCELADA).filter(b->periodo==null||periodo.equals(b.getPeriodoId()))
                .filter(b->b.getPlanificacionId()!=null&&planes.findById(b.getPlanificacionId()).map(this::horarioDefinitivo).orElse(false))
                .map(this::mapBloque).toList();}

    private boolean horarioDefinitivo(PlanificacionAgregadaJpaEntity p){return p!=null&&(p.getEstado()==EstadoPlanificacionAgregada.APROBADA
            ||p.getEstado()==EstadoPlanificacionAgregada.FINALIZADA);}
    private ec.edu.scli.reservas.client.dto.DocenteExternoResponse docente(UUID perfil){var d=usuarios.obtenerDocentePorPerfil(perfil);
        if(d==null||!d.activo())throw new AccessDeniedException("No existe docente activo para el perfil autenticado");return d;}
    private void validarAbierta(SesionAsistenciaJpaEntity s,Instant now){if(now.isAfter(s.getExpiraEn()))throw new IllegalStateException("La sesión está vencida");
        if(s.getEstado()!=EstadoSesionAsistencia.ABIERTA)throw new IllegalStateException("La sesión no está abierta");}
    private SesionAsistenciaJpaEntity sesionPropia(UUID id,UUID actor){return sesiones.findByIdAndDocenteId(id,actor)
            .orElseThrow(()->new AccessDeniedException("El registro no pertenece al docente"));}
    private SesionAsistenciaJpaEntity obtener(UUID id){return sesiones.findById(id).orElseThrow(()->new ResourceNotFoundException("Registro no encontrado"));}
    private SesionAsistenciaResponse response(SesionAsistenciaJpaEntity s,String token){var ps=participantes.findBySesionIdOrderByEstudiantePerfilId(s.getId());
        long presentes=ps.stream().filter(p->p.getEstado()==EstadoParticipanteUso.PRESENTE).count();
        long ausentes=ps.stream().filter(p->p.getEstado()==EstadoParticipanteUso.AUSENTE).count();
        return new SesionAsistenciaResponse(s.getId(),s.getReservaId(),s.getBloquePlanificacionId(),s.getFechaClase(),s.getAbiertaEn(),
                s.getExpiraEn(),s.getCerradaEn(),s.getEstado().name(),token,s.getTemaActividad(),s.getObservacionUso(),s.getCarreraIdSnapshot(),
                s.getPeriodoIdSnapshot(),s.getNivelSnapshot(),s.getMateriaIdSnapshot(),s.getDocenteIdSnapshot(),s.getLaboratorioIdSnapshot(),
                s.getPisoIdSnapshot(),s.getDiaSemanaSnapshot(),s.getHoraInicioSnapshot(),s.getHoraFinSnapshot(),ps.size(),presentes,ausentes);}
    private ParticipanteUsoResponse mapParticipante(ParticipanteUsoLaboratorioJpaEntity p){return new ParticipanteUsoResponse(p.getId(),p.getSesionId(),
            p.getEstudiantePerfilId(),p.getEstado().name(),p.getRegistradoEn(),p.getRegistradoPorPerfilId(),p.getObservacion());}
    private RegistroAsistenciaResponse mapRegistro(ParticipanteUsoLaboratorioJpaEntity p){UUID bloque=sesiones.findById(p.getSesionId())
            .map(SesionAsistenciaJpaEntity::getBloquePlanificacionId).orElse(null);return new RegistroAsistenciaResponse(p.getId(),p.getSesionId(),
                    p.getEstudiantePerfilId(),bloque,p.getRegistradoEn(),p.getEstado().name());}
    private RegistroAsistenciaResponse mapAntiguo(RegistroAsistenciaJpaEntity r){UUID bloque=sesiones.findById(r.getSesionId())
            .map(SesionAsistenciaJpaEntity::getBloquePlanificacionId).orElse(null);return new RegistroAsistenciaResponse(r.getId(),r.getSesionId(),
                    r.getEstudianteId(),bloque,r.getRegistradaEn(),r.getEstado());}
    private boolean perteneceContextoAntiguo(SesionAsistenciaJpaEntity s,EstudianteInstitucionalPort.Contexto c){
        if(s.getBloquePlanificacionId()!=null){var b=bloques.findById(s.getBloquePlanificacionId()).orElse(null);if(b==null||!c.nivel().equals(b.getNivel()))return false;
            var p=planes.findById(b.getPlanificacionId()).orElse(null);return p!=null&&p.getEstado()==EstadoPlanificacionAgregada.APROBADA
                    &&c.carreraId().equals(p.getCarreraId())&&c.periodoId().equals(p.getPeriodoId());}
        var reserva=reservaRepository.buscarPorId(s.getReservaId()).orElse(null);if(reserva==null)return false;var solicitud=solicitudRepository.buscarPorId(reserva.getSolicitudId()).orElse(null);
        if(solicitud==null)return false;var materia=academico.obtenerContextoMateria(solicitud.getMateriaId());return materia!=null&&materia.existe()&&c.carreraId().equals(materia.carreraId());}
    private PlanificacionResponse mapBloque(PlanificacionJpaEntity b){return new PlanificacionResponse(b.getId(),b.getPlanificacionId(),b.getNivel(),
        b.getPeriodoId(),b.getCarreraId(),b.getMateriaId(),b.getDocenteId(),b.getLaboratorioId(),b.getDiaSemana(),b.getHoraInicio(),b.getHoraFin(),
        b.getEstado().name(),b.getObservacion(),b.getCreadoPorPerfilId(),b.getCreadaEn(),b.getActualizadaEn(),b.getVersion());}
    private String limpiar(String v){return v==null||v.isBlank()?null:v.trim();}
    private String tokenSeguro(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 no disponible",e);}}
    private String dia(LocalDate f){return switch(f.getDayOfWeek()){case MONDAY->"LUNES";case TUESDAY->"MARTES";case WEDNESDAY->"MIERCOLES";
        case THURSDAY->"JUEVES";case FRIDAY->"VIERNES";case SATURDAY->"SABADO";case SUNDAY->"DOMINGO";};}
}
