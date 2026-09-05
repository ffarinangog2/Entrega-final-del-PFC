package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.HorarioAcademico;
import ec.edu.scli.academico.domain.port.HorarioAcademicoRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.HorarioAcademicoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.HorarioAcademicoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HorarioAcademicoRepositoryAdapter implements HorarioAcademicoRepositoryPort {

    private final HorarioAcademicoJpaRepository horarioAcademicoJpaRepository;
    private final HorarioAcademicoEntityMapper mapper;

    public HorarioAcademicoRepositoryAdapter(
            HorarioAcademicoJpaRepository horarioAcademicoJpaRepository,
            HorarioAcademicoEntityMapper mapper
    ) {
        this.horarioAcademicoJpaRepository = horarioAcademicoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public HorarioAcademico guardar(HorarioAcademico horario) {
        HorarioAcademicoEntity entidad = mapper.aEntidad(horario);
        HorarioAcademicoEntity guardado = horarioAcademicoJpaRepository.save(entidad);
        return mapper.aDominio(guardado);
    }

    @Override
    public Optional<HorarioAcademico> buscarPorId(UUID id) {
        return horarioAcademicoJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public List<HorarioAcademico> buscarTodos() {
        return horarioAcademicoJpaRepository.findAll()
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public List<HorarioAcademico> buscarPorDocente(UUID docenteId) {
        return horarioAcademicoJpaRepository.findByDocenteId(docenteId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public List<HorarioAcademico> buscarPorLaboratorio(UUID laboratorioId) {
        return horarioAcademicoJpaRepository.findByLaboratorioId(laboratorioId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }
}
