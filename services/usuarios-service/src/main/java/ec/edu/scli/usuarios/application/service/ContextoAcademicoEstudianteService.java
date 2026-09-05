package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.infrastructure.client.AcademicoPeriodoClient;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.ContextoAcademicoEstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContextoAcademicoEstudianteService {

    private final EstudianteRepository estudiantes;
    private final ContextoAcademicoEstudianteRepository contextos;
    private final AcademicoPeriodoClient academico;

    public ContextoAcademicoEstudianteService(
            EstudianteRepository estudiantes,
            ContextoAcademicoEstudianteRepository contextos,
            AcademicoPeriodoClient academico) {
        this.estudiantes = estudiantes;
        this.contextos = contextos;
        this.academico = academico;
    }

    @Transactional(readOnly = true)
    public List<ContextoAcademicoEstudianteResponse> historial(UUID perfilId) {
        var estudiante = estudiantes.findByPerfilId(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe estudiante para el perfil autenticado"));
        return contextos.findByEstudianteIdOrderByCreadoEnDesc(estudiante.getId()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public ContextoAcademicoEstudianteResponse actual(UUID perfilId) {
        var estudiante = estudiantes.findByPerfilId(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe estudiante para el perfil autenticado"));
        return map(contextos.findFirstByEstudianteIdAndActivoTrueOrderByCreadoEnDesc(estudiante.getId())
                .orElseThrow(() -> new ResourceNotFoundException("El estudiante no tiene contexto académico vigente")));
    }

    @Transactional(readOnly = true)
    public ContextoAcademicoEstudianteResponse porPeriodo(UUID perfilId, UUID periodoId) {
        var estudiante = estudiantes.findByPerfilId(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe estudiante para el perfil autenticado"));
        return map(contextos.findByEstudianteIdAndPeriodoId(estudiante.getId(), periodoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contexto académico para el ciclo seleccionado")));
    }

    @Transactional
    public ContextoAcademicoEstudianteResponse asignar(UUID estudianteId, ContextoAcademicoEstudianteRequest request) {
        estudiantes.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        if (request.nivel() == null || request.nivel() < 1 || request.nivel() > 10) {
            throw new BusinessRuleException("El nivel académico debe estar comprendido entre 1 y 10");
        }
        contextos.findByEstudianteIdOrderByCreadoEnDesc(estudianteId).stream().filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .forEach(c -> {
                    c.setActivo(false);
                    contextos.save(c);
                });
        var contexto = contextos.findByEstudianteIdAndPeriodoId(estudianteId, request.periodoId()).orElseGet(ContextoAcademicoEstudianteEntity::new);
        contexto.setEstudianteId(estudianteId);
        contexto.setCarreraId(request.carreraId());
        contexto.setPeriodoId(request.periodoId());
        contexto.setNivel(request.nivel());
        contexto.setActivo(true);
        return map(contextos.save(contexto));
    }

    @Transactional
    public ContextoAcademicoEstudianteResponse asignarPorPerfil(UUID perfilId, ContextoAcademicoEstudianteRequest request) {
        var estudiante = estudiantes.findByPerfilId(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        return asignar(estudiante.getId(), request);
    }

    @Transactional
    public ContextoAcademicoEstudianteResponse autodeclarar(UUID perfilId, ContextoAcademicoEstudianteRequest request) {
        var estudiante = estudiantes.findByPerfilId(perfilId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe estudiante para el perfil autenticado"));

        if (request.carreraId() == null) {
            throw new BusinessRuleException("La carrera es obligatoria");
        }
        if (request.nivel() == null || request.nivel() < 1 || request.nivel() > 10) {
            throw new BusinessRuleException("El nivel académico debe estar comprendido entre 1 y 10");
        }

        var estadoCarrera = academico.estadoCarrera(request.carreraId());
        if (estadoCarrera == null || !estadoCarrera.existe()) {
            throw new ResourceNotFoundException("La carrera seleccionada no existe");
        }
        if (!estadoCarrera.activa()) {
            throw new BusinessRuleException("La carrera seleccionada se encuentra inactiva");
        }

        UUID periodoVigente = academico.periodoVigente();
        if (contextos.findByEstudianteIdAndPeriodoId(estudiante.getId(), periodoVigente).isPresent()) {
            throw new IllegalStateException("El contexto de este periodo ya fue confirmado; solicite una correccion administrativa");
        }
        return asignar(estudiante.getId(), new ContextoAcademicoEstudianteRequest(request.carreraId(), periodoVigente, request.nivel()));
    }

    private ContextoAcademicoEstudianteResponse map(ContextoAcademicoEstudianteEntity c) {
        return new ContextoAcademicoEstudianteResponse(
                c.getId(),
                c.getEstudianteId(),
                c.getCarreraId(),
                c.getPeriodoId(),
                c.getNivel(),
                Boolean.TRUE.equals(c.getActivo()),
                c.getCreadoEn());
    }
}
