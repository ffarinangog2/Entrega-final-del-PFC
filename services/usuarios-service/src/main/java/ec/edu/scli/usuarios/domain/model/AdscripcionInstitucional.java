package ec.edu.scli.usuarios.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdscripcionInstitucional(
        UUID id,
        UUID perfilId,
        TipoAmbitoInstitucional tipoAmbito,
        UUID ambitoId,
        boolean activo,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {
}
