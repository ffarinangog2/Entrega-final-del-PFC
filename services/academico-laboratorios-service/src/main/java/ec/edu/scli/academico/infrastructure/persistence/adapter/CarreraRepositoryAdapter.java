package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.domain.port.CarreraRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.CarreraEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.CarreraJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.CarreraJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CarreraRepositoryAdapter implements CarreraRepositoryPort {

    private final CarreraJpaRepository carreraJpaRepository;
    private final CarreraEntityMapper mapper;

    public CarreraRepositoryAdapter(CarreraJpaRepository carreraJpaRepository, CarreraEntityMapper mapper) {
        this.carreraJpaRepository = carreraJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Carrera guardar(Carrera carrera) {
        CarreraEntity entidad = mapper.aEntidad(carrera);
        CarreraEntity guardada = carreraJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Carrera> buscarPorId(UUID id) {
        return carreraJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Carrera> buscar(UUID facultadId, String codigo, String nombre, Boolean activo, Pageable pageable) {
        Specification<CarreraEntity> specification =
                CarreraJpaSpecification.tieneFacultad(facultadId)
                        .and(CarreraJpaSpecification.codigoContiene(codigo))
                        .and(CarreraJpaSpecification.nombreContiene(nombre))
                        .and(CarreraJpaSpecification.tieneEstado(activo));

        return carreraJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Carrera> buscarPorFacultad(UUID facultadId) {
        return carreraJpaRepository.findByFacultadId(facultadId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return carreraJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return carreraJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorFacultad(UUID facultadId) {
        return carreraJpaRepository.existsByFacultadId(facultadId);
    }
}
