package ec.edu.scli.usuarios.dto.perfil;

import java.util.List;
import java.util.UUID;

public record PerfilExistsResponse(

        UUID perfilId,

        boolean existe,

        boolean activo,

        List<String> tiposPerfil

) {
}