package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.domain.port.CampusRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.CampusEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.CampusJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.CampusJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador (patrón Repository, lado infraestructura): implementa el
 * puerto de dominio CampusRepositoryPort apoyándose en Spring Data JPA.
 * Es el único lugar donde domain.model.Campus se traduce hacia/desde
 * infrastructure.persistence.entity.CampusEntity.
 */
@Component
public class CampusRepositoryAdapter implements CampusRepositoryPort {

    private final CampusJpaRepository campusJpaRepository;
    private final CampusEntityMapper mapper;

    public CampusRepositoryAdapter(CampusJpaRepository campusJpaRepository, CampusEntityMapper mapper) {
        this.campusJpaRepository = campusJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Campus guardar(Campus campus) {
        CampusEntity entidad = mapper.aEntidad(campus);
        CampusEntity guardada = campusJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Campus> buscarPorId(UUID id) {
        return campusJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Campus> buscar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        Specification<CampusEntity> specification =
                CampusJpaSpecification.codigoContiene(codigo)
                        .and(CampusJpaSpecification.nombreContiene(nombre))
                        .and(CampusJpaSpecification.tieneEstado(activo));

        return campusJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return campusJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return campusJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }
}
