package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.PeriodoLectivoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.PeriodoLectivoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.PeriodoLectivoJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

@Component
public class PeriodoLectivoRepositoryAdapter implements PeriodoLectivoRepositoryPort {

    private final PeriodoLectivoJpaRepository periodoLectivoJpaRepository;
    private final PeriodoLectivoEntityMapper mapper;

    public PeriodoLectivoRepositoryAdapter(
            PeriodoLectivoJpaRepository periodoLectivoJpaRepository,
            PeriodoLectivoEntityMapper mapper
    ) {
        this.periodoLectivoJpaRepository = periodoLectivoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PeriodoLectivo guardar(PeriodoLectivo periodoLectivo) {
        PeriodoLectivoEntity entidad = mapper.aEntidad(periodoLectivo);
        PeriodoLectivoEntity guardado = periodoLectivoJpaRepository.save(entidad);
        return mapper.aDominio(guardado);
    }

    @Override
    public Optional<PeriodoLectivo> buscarPorId(UUID id) {
        return periodoLectivoJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<PeriodoLectivo> buscar(String codigo, Pageable pageable) {
        Specification<PeriodoLectivoEntity> specification =
                PeriodoLectivoJpaSpecification.codigoContiene(codigo);

        return periodoLectivoJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public Optional<PeriodoLectivo> buscarActualPorEstado(EstadoPeriodo estado) {
        return periodoLectivoJpaRepository
                .findFirstByEstadoOrderByFechaInicioDesc(estado)
                .map(mapper::aDominio);
    }

    @Override
    public Optional<PeriodoLectivo> buscarVigente(LocalDate fecha) {
        return periodoLectivoJpaRepository
                .buscarVigente(fecha)
                .map(mapper::aDominio);
    }

    @Override
    public boolean existeCodigo(String codigo) {
        return periodoLectivoJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public boolean existeCodigoParaOtroId(String codigo, UUID id) {
        return periodoLectivoJpaRepository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return periodoLectivoJpaRepository.existsById(id);
    }
}
