package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PoliticaAmbitoLaboratorio {
    private final ActorActualPort actorActual;
    private final ContextoInstitucionalPort contextos;
    private final AcademicoLaboratoriosClient academico;

    public PoliticaAmbitoLaboratorio(ActorActualPort actorActual, ContextoInstitucionalPort contextos,
                                     AcademicoLaboratoriosClient academico) {
        this.actorActual = actorActual;
        this.contextos = contextos;
        this.academico = academico;
    }

    public ActorAutenticado actor() { return actorActual.obtener(); }

    public UUID pisoGestionado() {
        ActorAutenticado actor = actor();
        if (actor.tiene("ROLE_ADMINISTRADOR")) return null;
        if (!actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            throw new AccessDeniedException("El actor no posee gestión operativa");
        }
        var contexto = contextos.obtenerPorPerfilId(actor.perfilId());
        if (contexto == null || !contexto.perfilExiste() || !contexto.perfilActivo()
                || !contexto.administradorExiste() || !contexto.administradorActivo()
                || !contexto.administradorPisoOperativo() || contexto.pisoId() == null) {
            throw new AccessDeniedException("El administrador de piso no tiene adscripción válida");
        }
        return contexto.pisoId();
    }

    public UUID validarGestion(UUID laboratorioId) {
        UUID pisoGestionado = pisoGestionado();
        UUID pisoLaboratorio = obtenerPiso(laboratorioId);
        if (pisoGestionado != null && !pisoGestionado.equals(pisoLaboratorio)) {
            throw new AccessDeniedException("El laboratorio no pertenece al piso administrado");
        }
        return pisoLaboratorio;
    }

    public UUID obtenerPiso(UUID laboratorioId) {
        LaboratorioExternoResponse laboratorio = academico.obtenerLaboratorio(laboratorioId);
        if (laboratorio == null || !laboratorio.existe() || laboratorio.pisoId() == null) {
            throw new AccessDeniedException("No fue posible resolver el piso del laboratorio");
        }
        return laboratorio.pisoId();
    }
}
