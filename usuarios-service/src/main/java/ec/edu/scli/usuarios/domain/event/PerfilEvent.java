package ec.edu.scli.usuarios.domain.event;

import ec.edu.scli.usuarios.domain.model.Perfil;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PerfilEvent(
        UUID perfilId,
        String tipo,
        Boolean activo,
        OffsetDateTime ocurridoEn
) {

    public static PerfilEvent creado(Perfil perfil) {
        return new PerfilEvent(
                perfil.getId(),
                "PERFIL_CREADO",
                perfil.getActivo(),
                OffsetDateTime.now()
        );
    }

    public static PerfilEvent estadoCambiado(Perfil perfil) {
        return new PerfilEvent(
                perfil.getId(),
                perfil.getActivo() ? "PERFIL_ACTIVADO" : "PERFIL_DESACTIVADO",
                perfil.getActivo(),
                OffsetDateTime.now()
        );
    }
}
