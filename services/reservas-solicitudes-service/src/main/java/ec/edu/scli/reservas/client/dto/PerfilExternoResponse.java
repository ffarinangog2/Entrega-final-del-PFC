package ec.edu.scli.reservas.client.dto;

import java.util.List;
import java.util.UUID;

/** Respuesta externa con la existencia, estado y tipos de un perfil. */
public record PerfilExternoResponse(
        UUID perfilId,
        boolean existe,
        boolean activo,
        List<String> tiposPerfil
) {
}
