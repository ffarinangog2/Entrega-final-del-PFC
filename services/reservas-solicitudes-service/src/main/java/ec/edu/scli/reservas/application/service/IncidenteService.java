package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.model.EstadoIncidente;
import ec.edu.scli.reservas.infrastructure.persistence.entity.IncidenteJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.IncidenteSpringDataRepository;
import ec.edu.scli.reservas.presentation.dto.request.CrearIncidenteRequest;
import ec.edu.scli.reservas.presentation.dto.response.*;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class IncidenteService {
    private final IncidenteSpringDataRepository repository;
    private final NotificacionService notificaciones;
    public IncidenteService(IncidenteSpringDataRepository repository, NotificacionService notificaciones) {
        this.repository=repository; this.notificaciones=notificaciones;
    }

    @Transactional
    public IncidenteResponse crear(CrearIncidenteRequest request, UUID reportanteId) {
        var entity = new IncidenteJpaEntity();
        entity.setReportanteId(reportanteId);
        entity.setLaboratorioEquipo(request.laboratorioEquipo().trim());
        entity.setDescripcion(request.descripcion().trim());
        entity.setPrioridad(request.prioridad()); entity.setFecha(request.fecha());
        return response(repository.saveAndFlush(entity));
    }

    @Transactional(readOnly=true)
    public PaginaResponse<IncidenteResponse> listar(UUID actorId, boolean gestor, int pagina, int tamanio) {
        Pageable pageable=PageRequest.of(pagina,tamanio,Sort.by(Sort.Direction.DESC,"creadoEn"));
        Page<IncidenteJpaEntity> page=gestor ? repository.findAll(pageable) : repository.findByReportanteId(actorId,pageable);
        return new PaginaResponse<>(page.map(this::response).getContent(), pagina, tamanio,
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Transactional(readOnly=true)
    public IncidenteResponse obtener(UUID id, UUID actorId, boolean gestor) {
        var entity=buscar(id); verificarLectura(entity,actorId,gestor); return response(entity);
    }

    @Transactional
    public IncidenteResponse cambiarEstado(UUID id, EstadoIncidente destino) {
        var entity=buscar(id);
        if (!entity.getEstado().puedeTransicionarA(destino))
            throw new IllegalStateException("La transición de estado del incidente no es válida");
        entity.setEstado(destino); var actualizado=repository.saveAndFlush(entity);
        notificaciones.notificarPerfil(actualizado.getReportanteId(), "Incidente actualizado",
                "El incidente ahora está " + destino.name(), java.util.Map.of(
                        "tipo", "INCIDENTE_ACTUALIZADO", "incidenteId", actualizado.getId().toString()));
        return response(actualizado);
    }

    private IncidenteJpaEntity buscar(UUID id) { return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el incidente solicitado")); }
    private void verificarLectura(IncidenteJpaEntity e, UUID actor, boolean gestor) {
        if (!gestor && !e.getReportanteId().equals(actor)) throw new AccessDeniedException("No puede consultar este incidente");
    }
    private IncidenteResponse response(IncidenteJpaEntity e) { return new IncidenteResponse(e.getId(),e.getReportanteId(),
            e.getLaboratorioEquipo(),e.getDescripcion(),e.getPrioridad(),e.getFecha(),e.getEstado(),
            e.getCreadoEn(),e.getActualizadoEn(),e.getVersion()); }
}
