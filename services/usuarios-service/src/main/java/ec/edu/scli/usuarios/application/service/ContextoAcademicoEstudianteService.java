package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.*;
import ec.edu.scli.usuarios.presentation.dto.estudiante.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ContextoAcademicoEstudianteService {
    private final EstudianteRepository estudiantes;
    private final ContextoAcademicoEstudianteRepository contextos;
    public ContextoAcademicoEstudianteService(EstudianteRepository estudiantes, ContextoAcademicoEstudianteRepository contextos) {
        this.estudiantes=estudiantes; this.contextos=contextos;
    }
    @Transactional(readOnly=true) public List<ContextoAcademicoEstudianteResponse> historial(UUID perfilId) {
        var estudiante=estudiantes.findByPerfilId(perfilId).orElseThrow(() -> new EntityNotFoundException("No existe estudiante para el perfil autenticado"));
        return contextos.findByEstudianteIdOrderByCreadoEnDesc(estudiante.getId()).stream().map(this::map).toList();
    }
    @Transactional(readOnly=true) public ContextoAcademicoEstudianteResponse actual(UUID perfilId) {
        var estudiante=estudiantes.findByPerfilId(perfilId).orElseThrow(() -> new EntityNotFoundException("No existe estudiante para el perfil autenticado"));
        return map(contextos.findFirstByEstudianteIdAndActivoTrueOrderByCreadoEnDesc(estudiante.getId())
                .orElseThrow(() -> new EntityNotFoundException("El estudiante no tiene contexto académico vigente")));
    }
    @Transactional(readOnly=true) public ContextoAcademicoEstudianteResponse porPeriodo(UUID perfilId, UUID periodoId) {
        var estudiante=estudiantes.findByPerfilId(perfilId).orElseThrow(() -> new EntityNotFoundException("No existe estudiante para el perfil autenticado"));
        return map(contextos.findByEstudianteIdAndPeriodoId(estudiante.getId(), periodoId)
                .orElseThrow(() -> new EntityNotFoundException("No existe contexto académico para el ciclo seleccionado")));
    }
    @Transactional public ContextoAcademicoEstudianteResponse asignar(UUID estudianteId, ContextoAcademicoEstudianteRequest request) {
        estudiantes.findById(estudianteId).orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId).stream().filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .forEach(c -> { c.setActivo(false); contextos.save(c); });
        var contexto=contextos.findByEstudianteIdAndPeriodoId(estudianteId, request.periodoId()).orElseGet(ContextoAcademicoEstudianteEntity::new);
        contexto.setEstudianteId(estudianteId); contexto.setCarreraId(request.carreraId()); contexto.setPeriodoId(request.periodoId());
        contexto.setNivel(request.nivel()); contexto.setActivo(true);
        return map(contextos.save(contexto));
    }
    @Transactional public ContextoAcademicoEstudianteResponse asignarPorPerfil(UUID perfilId, ContextoAcademicoEstudianteRequest request) {
        var estudiante=estudiantes.findByPerfilId(perfilId).orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        return asignar(estudiante.getId(),request);
    }
    private ContextoAcademicoEstudianteResponse map(ContextoAcademicoEstudianteEntity c) {
        return new ContextoAcademicoEstudianteResponse(c.getId(),c.getEstudianteId(),c.getCarreraId(),c.getPeriodoId(),c.getNivel(),Boolean.TRUE.equals(c.getActivo()),c.getCreadoEn());
    }
}
