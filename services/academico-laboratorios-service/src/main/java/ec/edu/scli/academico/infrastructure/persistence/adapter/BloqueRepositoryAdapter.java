package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Bloque;
import ec.edu.scli.academico.domain.port.BloqueRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.BloqueEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.BloqueJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.BloqueJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BloqueRepositoryAdapter implements BloqueRepositoryPort {

    private final BloqueJpaRepository bloqueJpaRepository;
    private final BloqueEntityMapper mapper;

    public BloqueRepositoryAdapter(BloqueJpaRepository bloqueJpaRepository, BloqueEntityMapper mapper) {
        this.bloqueJpaRepository = bloqueJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Bloque guardar(Bloque bloque) {
        BloqueEntity entidad = mapper.aEntidad(bloque);
        BloqueEntity guardada = bloqueJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Bloque> buscarPorId(UUID id) {
        return bloqueJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Bloque> buscar(UUID campusId, String nombre, Boolean activo, Pageable pageable) {
        Specification<BloqueEntity> specification =
                BloqueJpaSpecification.tieneCampus(campusId)
                        .and(BloqueJpaSpecification.nombreContiene(nombre))
                        .and(BloqueJpaSpecification.tieneEstado(activo));

        return bloqueJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Bloque> buscarPorCampus(UUID campusId) {
        return bloqueJpaRepository.findByCampusId(campusId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeCodigoEnCampus(UUID campusId, String codigo) {
        return bloqueJpaRepository.existsByCampusIdAndCodigo(campusId, codigo);
    }

    @Override
    public boolean existeCodigoEnCampusParaOtroId(UUID campusId, String codigo, UUID id) {
        return bloqueJpaRepository.existsByCampusIdAndCodigoAndIdNot(campusId, codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return bloqueJpaRepository.existsById(id);
    }
}
