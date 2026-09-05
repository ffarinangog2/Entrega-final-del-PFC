package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import ec.edu.scli.usuarios.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/docentes/planificacion")
public class DocentePlanificacionController {
    private final DocenteRepository docentes;
    private final AdscripcionInstitucionalRepository adscripciones;

    public DocentePlanificacionController(DocenteRepository docentes,
            AdscripcionInstitucionalRepository adscripciones) {
        this.docentes = docentes;
        this.adscripciones = adscripciones;
    }

    @GetMapping
    public ResponseEntity<List<DocenteResponse>> listar(Authentication authentication) {
        UUID perfilId = principal(authentication).perfilId();
        List<UUID> carreras = adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId).stream()
                .filter(item -> item.isActivo() && item.getTipoAmbito() == TipoAmbitoInstitucional.CARRERA)
                .map(item -> item.getAmbitoId())
                .distinct()
                .toList();
        if (carreras.size() != 1) {
            throw new AccessDeniedException("El coordinador no posee una carrera institucional unica y activa");
        }
        return ResponseEntity.ok(docentes.findActivosByCarreraId(carreras.getFirst()).stream()
                .map(docente -> new DocenteResponse(docente.getId(), docente.getPerfil().getId(),
                        docente.getCodigoDocente(), docente.getTituloAcademico(), docente.getDepartamento(),
                        docente.getTipoContrato(), docente.getDedicacion(), docente.getActivo(),
                        docente.getCreadoEn(), docente.getActualizadoEn()))
                .toList());
    }

    private JwtPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No existe una identidad institucional autenticada");
        }
        return principal;
    }
}
