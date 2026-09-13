package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.AdscripcionInstitucional;

import java.util.List;
import java.util.UUID;

public interface AdscripcionInstitucionalRepositoryPort {
    List<AdscripcionInstitucional> findByPerfilId(UUID perfilId);
}
