package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.domain.port.EquipoRepositoryPort;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.EquipoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.EquipoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.EquipoJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EquipoRepositoryAdapter implements EquipoRepositoryPort {

    private final EquipoJpaRepository equipoJpaRepository;
    private final EquipoEntityMapper mapper;

    public EquipoRepositoryAdapter(EquipoJpaRepository equipoJpaRepository, EquipoEntityMapper mapper) {
        this.equipoJpaRepository = equipoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Equipo guardar(Equipo equipo) {
        EquipoEntity entidad = mapper.aEntidad(equipo);
        EquipoEntity guardada = equipoJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Equipo> buscarPorId(UUID id) {
        return equipoJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Equipo> buscar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable) {
        Specification<EquipoEntity> specification =
                EquipoJpaSpecification.tieneLaboratorio(laboratorioId)
                        .and(EquipoJpaSpecification.tieneEstadoEquipo(estado))
                        .and(EquipoJpaSpecification.tieneEstado(activo));

        return equipoJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Equipo> buscarPorLaboratorio(UUID laboratorioId) {
        return equipoJpaRepository.findByLaboratorioId(laboratorioId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeCodigoInventario(String codigoInventario) {
        return equipoJpaRepository.existsByCodigoInventario(codigoInventario);
    }

    @Override
    public boolean existeCodigoInventarioParaOtroId(String codigoInventario, UUID id) {
        return equipoJpaRepository.existsByCodigoInventarioAndIdNot(codigoInventario, id);
    }

    @Override
    public boolean existeNumeroSerie(String numeroSerie) {
        return equipoJpaRepository.existsByNumeroSerie(numeroSerie);
    }

    @Override
    public boolean existeNumeroSerieParaOtroId(String numeroSerie, UUID id) {
        return equipoJpaRepository.existsByNumeroSerieAndIdNot(numeroSerie, id);
    }
}
