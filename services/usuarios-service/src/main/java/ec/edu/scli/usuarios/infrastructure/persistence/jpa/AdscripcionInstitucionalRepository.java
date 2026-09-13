package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdscripcionInstitucionalRepository
        extends JpaRepository<AdscripcionInstitucionalEntity, UUID> {
    List<AdscripcionInstitucionalEntity> findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(UUID perfilId);
}
