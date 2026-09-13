package ec.edu.scli.usuarios.domain.model;

import java.util.List;
import java.util.UUID;

public record ContextoInstitucional(
        UUID perfilId,
        boolean existe,
        boolean activo,
        List<TipoPerfil> tiposPerfil,
        ContextoAdministrador administrador,
        List<AdscripcionInstitucional> adscripciones) {

    public record ContextoAdministrador(
            boolean esAdministrador,
            boolean activo,
            UUID pisoId,
            String cargo,
            boolean administradorPisoOperativo) {
    }
}
