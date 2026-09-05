package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Facultad;
import ec.edu.scli.academico.domain.port.FacultadRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.FacultadEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.FacultadEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.FacultadJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.FacultadJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FacultadRepositoryAdapter implements FacultadRepositoryPort {

    private final FacultadJpaRepository facultadJpaRepository;
    private final FacultadEntityMapper mapper;

    public FacultadRepositoryAdapter(FacultadJpaRepository facultadJpaRepository, FacultadEntityMapper mapper) {
        this.facultadJpaRepository = facultadJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Facultad guardar(Facultad facultad) {
        FacultadEntity entidad = mapper.aEntidad(facultad);
        FacultadEntity guardada = facultadJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Facultad> buscarPorId(UUID id) {
        return facultadJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Facultad> buscar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        Specification<FacultadEntity> specification =
                FacultadJpaSpecification.codigoContiene(codigo)
                        .and(FacultadJpaSpecification.nombreContiene(nombre))
                        .and(FacultadJpaSpecification.tieneEstado(activo));

        return facultadJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return facultadJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return facultadJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return facultadJpaRepository.existsById(id);
    }
}
