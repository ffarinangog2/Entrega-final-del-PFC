package ec.edu.scli.usuarios.application.usecase;

import ec.edu.scli.usuarios.domain.model.ContextoInstitucional;

import java.util.UUID;

public interface ContextoInstitucionalService {
    ContextoInstitucional obtenerPorPerfilId(UUID perfilId);
}
