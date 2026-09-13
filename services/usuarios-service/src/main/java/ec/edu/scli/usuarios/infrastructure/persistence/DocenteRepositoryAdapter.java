package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Docente;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PageMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilRelacionadoPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocenteRepositoryAdapter implements DocenteRepositoryPort {

    private final DocenteRepository repository;
    private final PerfilRelacionadoPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public DocenteRepositoryAdapter(DocenteRepository repository, PerfilRelacionadoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Docente save(Docente docente) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity =
                mapper.toEntity(docente);
        entity.setPerfil(entityManager.getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                docente.getPerfil().getId()
        ));
        return mapper.toDomain(repository.save(entity));
    }
    public PageResult<Docente> findAll(PageCriteria pageCriteria) { return PageMapper.toPageResult(repository.findAll(PageMapper.toPageable(pageCriteria)), mapper::toDomain); }
    public List<Docente> findAll() { return repository.findAll().stream().map(mapper::toDomain).toList(); }
    public Optional<Docente> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<Docente> findByPerfilId(UUID perfilId) { return repository.findByPerfilId(perfilId).map(mapper::toDomain); }
    public boolean existsByPerfilId(UUID perfilId) { return repository.existsByPerfilId(perfilId); }
    public boolean existsByCodigoDocente(String codigoDocente) { return repository.existsByCodigoDocente(codigoDocente); }
}
