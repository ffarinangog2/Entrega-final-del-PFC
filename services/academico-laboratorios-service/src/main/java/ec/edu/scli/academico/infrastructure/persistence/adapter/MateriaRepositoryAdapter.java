package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Materia;
import ec.edu.scli.academico.domain.port.MateriaRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.MateriaEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.MateriaEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.MateriaJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.MateriaJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MateriaRepositoryAdapter implements MateriaRepositoryPort {

    private final MateriaJpaRepository materiaJpaRepository;
    private final MateriaEntityMapper mapper;

    public MateriaRepositoryAdapter(MateriaJpaRepository materiaJpaRepository, MateriaEntityMapper mapper) {
        this.materiaJpaRepository = materiaJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Materia guardar(Materia materia) {
        MateriaEntity entidad = mapper.aEntidad(materia);
        MateriaEntity guardada = materiaJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Materia> buscarPorId(UUID id) {
        return materiaJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Materia> buscar(UUID carreraId, String codigo, String nombre, Boolean activo, Pageable pageable) {
        Specification<MateriaEntity> specification =
                MateriaJpaSpecification.tieneCarrera(carreraId)
                        .and(MateriaJpaSpecification.codigoContiene(codigo))
                        .and(MateriaJpaSpecification.nombreContiene(nombre))
                        .and(MateriaJpaSpecification.tieneEstado(activo));

        return materiaJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Materia> buscarPorCarrera(UUID carreraId) {
        return materiaJpaRepository.findByCarreraId(carreraId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return materiaJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return materiaJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return materiaJpaRepository.existsById(id);
    }
}
