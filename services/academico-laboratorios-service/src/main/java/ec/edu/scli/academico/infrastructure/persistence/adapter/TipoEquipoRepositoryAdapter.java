package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.TipoEquipo;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.TipoEquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.TipoEquipoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.TipoEquipoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.TipoEquipoJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TipoEquipoRepositoryAdapter implements TipoEquipoRepositoryPort {

    private final TipoEquipoJpaRepository tipoEquipoJpaRepository;
    private final TipoEquipoEntityMapper mapper;

    public TipoEquipoRepositoryAdapter(
            TipoEquipoJpaRepository tipoEquipoJpaRepository,
            TipoEquipoEntityMapper mapper
    ) {
        this.tipoEquipoJpaRepository = tipoEquipoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TipoEquipo guardar(TipoEquipo tipoEquipo) {
        TipoEquipoEntity entidad = mapper.aEntidad(tipoEquipo);
        TipoEquipoEntity guardada = tipoEquipoJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<TipoEquipo> buscarPorId(UUID id) {
        return tipoEquipoJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<TipoEquipo> buscar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        Specification<TipoEquipoEntity> specification =
                TipoEquipoJpaSpecification.codigoContiene(codigo)
                        .and(TipoEquipoJpaSpecification.nombreContiene(nombre))
                        .and(TipoEquipoJpaSpecification.tieneEstado(activo));

        return tipoEquipoJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return tipoEquipoJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return tipoEquipoJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return tipoEquipoJpaRepository.existsById(id);
    }
}
