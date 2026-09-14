package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.AdscripcionInstitucional;
import ec.edu.scli.usuarios.domain.port.AdscripcionInstitucionalRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AdscripcionInstitucionalRepositoryAdapter
        implements AdscripcionInstitucionalRepositoryPort {
    private final AdscripcionInstitucionalRepository repository;

    public AdscripcionInstitucionalRepositoryAdapter(AdscripcionInstitucionalRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AdscripcionInstitucional> findByPerfilId(UUID perfilId) {
        return repository.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId)
                .stream().map(this::toDomain).toList();
    }

    private AdscripcionInstitucional toDomain(AdscripcionInstitucionalEntity entity) {
        return new AdscripcionInstitucional(
                entity.getId(), entity.getPerfil().getId(), entity.getTipoAmbito(),
                entity.getAmbitoId(), entity.isActivo(), entity.getCreadoEn(),
                entity.getActualizadoEn());
    }
}
