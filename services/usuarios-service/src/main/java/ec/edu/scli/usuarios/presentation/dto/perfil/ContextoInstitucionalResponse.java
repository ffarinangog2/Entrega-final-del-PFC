package ec.edu.scli.usuarios.presentation.dto.perfil;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;

import java.util.List;
import java.util.UUID;

public record ContextoInstitucionalResponse(
        UUID perfilId,
        boolean existe,
        boolean activo,
        List<TipoPerfil> tiposPerfil,
        AdministradorInstitucionalResponse administrador,
        List<AdscripcionResponse> adscripciones) {

    public record AdministradorInstitucionalResponse(
            boolean esAdministrador,
            boolean activo,
            UUID pisoId,
            String cargo,
            boolean administradorPisoOperativo) {
    }

    public record AdscripcionResponse(
            TipoAmbitoInstitucional tipoAmbito,
            UUID ambitoId,
            boolean activo) {
    }
}
