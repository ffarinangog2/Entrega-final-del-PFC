package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.LaboratorioEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.LaboratorioJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.LaboratorioJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LaboratorioRepositoryAdapter implements LaboratorioRepositoryPort {

    private final LaboratorioJpaRepository laboratorioJpaRepository;
    private final LaboratorioEntityMapper mapper;

    public LaboratorioRepositoryAdapter(
            LaboratorioJpaRepository laboratorioJpaRepository,
            LaboratorioEntityMapper mapper
    ) {
        this.laboratorioJpaRepository = laboratorioJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Laboratorio guardar(Laboratorio laboratorio) {
        LaboratorioEntity entidad = mapper.aEntidad(laboratorio);
        LaboratorioEntity guardada = laboratorioJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Laboratorio> buscarPorId(UUID id) {
        return laboratorioJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Laboratorio> buscar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable) {
        Specification<LaboratorioEntity> specification =
                LaboratorioJpaSpecification.nombreOCodigoContiene(texto)
                        .and(LaboratorioJpaSpecification.tieneEstadoLaboratorio(estado))
                        .and(LaboratorioJpaSpecification.tieneEstado(activo));

        return laboratorioJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Laboratorio> buscarDisponibles() {
        return laboratorioJpaRepository.findByEstadoAndActivoTrue(EstadoLaboratorio.DISPONIBLE)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return laboratorioJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return laboratorioJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return laboratorioJpaRepository.existsById(id);
    }
}
