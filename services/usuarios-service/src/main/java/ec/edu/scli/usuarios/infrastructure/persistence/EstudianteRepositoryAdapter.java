package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Estudiante;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PageMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilRelacionadoPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EstudianteRepositoryAdapter implements EstudianteRepositoryPort {

    private final EstudianteRepository repository;
    private final PerfilRelacionadoPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public EstudianteRepositoryAdapter(EstudianteRepository repository, PerfilRelacionadoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Estudiante save(Estudiante estudiante) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity =
                mapper.toEntity(estudiante);
        entity.setPerfil(entityManager.getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                estudiante.getPerfil().getId()
        ));
        return mapper.toDomain(repository.save(entity));
    }
    public PageResult<Estudiante> findAll(PageCriteria pageCriteria) { return PageMapper.toPageResult(repository.findAll(PageMapper.toPageable(pageCriteria)), mapper::toDomain); }
    public List<Estudiante> findAll() { return repository.findAll().stream().map(mapper::toDomain).toList(); }
    public Optional<Estudiante> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<Estudiante> findByPerfilId(UUID perfilId) { return repository.findByPerfilId(perfilId).map(mapper::toDomain); }
    public boolean existsByPerfilId(UUID perfilId) { return repository.existsByPerfilId(perfilId); }
    public boolean existsByMatricula(String matricula) { return repository.existsByMatricula(matricula); }
}
