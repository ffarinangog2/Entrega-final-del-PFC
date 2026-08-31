package ec.edu.scli.academico.infrastructure.client;

import java.util.List;
import java.util.UUID;

public record ContextoInstitucionalResponse(
        UUID perfilId, boolean existe, boolean activo, List<String> tiposPerfil,
        Administrador administrador, List<Adscripcion> adscripciones) {
    public record Administrador(boolean esAdministrador, boolean activo, UUID pisoId,
                                String cargo, boolean administradorPisoOperativo) { }
    public record Adscripcion(String tipoAmbito, UUID ambitoId, boolean activo) { }
}
