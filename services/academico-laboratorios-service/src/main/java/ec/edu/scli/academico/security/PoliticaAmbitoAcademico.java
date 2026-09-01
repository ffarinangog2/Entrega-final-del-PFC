package ec.edu.scli.academico.security;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.infrastructure.client.ContextoInstitucionalResponse;
import ec.edu.scli.academico.infrastructure.client.UsuariosContextoClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.List;

import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;

@Component
public class PoliticaAmbitoAcademico {
    private final UsuariosContextoClient contextos;
    private final LaboratorioService laboratorios;
    private final MateriaService materias;

    public PoliticaAmbitoAcademico(UsuariosContextoClient contextos,
            LaboratorioService laboratorios, MateriaService materias) {
        this.contextos = contextos; this.laboratorios = laboratorios; this.materias = materias;
    }

    public void validarPiso(UUID pisoId) {
        Authentication authentication = autenticacion();
        if (tiene(authentication, "ROLE_ADMINISTRADOR")) {
            return;
        }
        if (!tiene(authentication, "ROLE_ADMINISTRADOR_PISO")) {
            throw denegado();
        }
        ContextoInstitucionalResponse contexto = contexto(authentication);
        var administrador = contexto.administrador();
        if (!contexto.existe() || !contexto.activo() || administrador == null
                || !administrador.administradorPisoOperativo()
                || administrador.pisoId() == null || !administrador.pisoId().equals(pisoId)) {
            throw denegado();
        }
    }

    public void validarLaboratorio(UUID laboratorioId) {
        validarPiso(laboratorios.obtenerPorId(laboratorioId).pisoId());
    }

    public void validarCarrera(UUID carreraId) {
        Authentication authentication = autenticacion();
        if (tiene(authentication, "ROLE_ADMINISTRADOR")) {
            return;
        }
        if (!tiene(authentication, "ROLE_COORDINADOR")) {
            throw denegado();
        }
        ContextoInstitucionalResponse contexto = contexto(authentication);
        boolean pertenece = contexto.existe() && contexto.activo() && contexto.adscripciones() != null
                && contexto.adscripciones().stream().anyMatch(value -> value.activo()
                && "CARRERA".equals(value.tipoAmbito()) && carreraId.equals(value.ambitoId()));
        if (!pertenece) {
            throw denegado();
        }
    }

    public void validarMateria(UUID materiaId) {
        validarCarrera(materias.obtenerPorId(materiaId).carreraId());
    }

    public UUID aplicarCarreraLectura(UUID carreraSolicitada) {
        Authentication authentication = autenticacion();
        if (!tiene(authentication, "ROLE_COORDINADOR")) {
            return carreraSolicitada;
        }
        UUID carreraAsignada = carreraCoordinador(authentication);
        if (carreraSolicitada != null && !carreraAsignada.equals(carreraSolicitada)) {
            throw denegado();
        }
        return carreraAsignada;
    }

    public void validarMateriaLectura(UUID materiaId) {
        Authentication authentication = autenticacion();
        if (!tiene(authentication, "ROLE_COORDINADOR")) {
            return;
        }
        UUID carreraId = materias.obtenerPorId(materiaId).carreraId();
        if (!carreraCoordinador(authentication).equals(carreraId)) {
            throw denegado();
        }
    }

    public List<HorarioAcademicoResponse> filtrarHorariosLectura(List<HorarioAcademicoResponse> horarios) {
        Authentication authentication = autenticacion();
        if (!tiene(authentication, "ROLE_COORDINADOR")) {
            return horarios;
        }
        UUID carreraId = carreraCoordinador(authentication);
        return horarios.stream()
                .filter(horario -> carreraId.equals(materias.obtenerPorId(horario.materiaId()).carreraId()))
                .toList();
    }

    private UUID carreraCoordinador(Authentication authentication) {
        ContextoInstitucionalResponse contexto = contexto(authentication);
        if (!contexto.existe() || !contexto.activo() || contexto.adscripciones() == null) {
            throw denegado();
        }
        return contexto.adscripciones().stream()
                .filter(value -> value.activo() && "CARRERA".equals(value.tipoAmbito()))
                .map(ContextoInstitucionalResponse.Adscripcion::ambitoId)
                .findFirst()
                .orElseThrow(this::denegado);
    }

    private ContextoInstitucionalResponse contexto(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw denegado();
        }
        ContextoInstitucionalResponse contexto = contextos.obtener(principal.perfilId());
        if (contexto == null) {
            throw denegado();
        }
        return contexto;
    }

    private Authentication autenticacion() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw denegado();
        }
        return authentication;
    }

    private boolean tiene(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }

    private AccessDeniedException denegado() {
        return new AccessDeniedException("El recurso está fuera del ámbito institucional del usuario");
    }
}
