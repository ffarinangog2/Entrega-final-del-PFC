package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Tecnico;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.TecnicoRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.TecnicoRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PageMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilRelacionadoPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TecnicoRepositoryAdapter implements TecnicoRepositoryPort {

    private final TecnicoRepository repository;
    private final PerfilRelacionadoPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public TecnicoRepositoryAdapter(TecnicoRepository repository, PerfilRelacionadoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Tecnico save(Tecnico tecnico) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity =
                mapper.toEntity(tecnico);
        entity.setPerfil(entityManager.getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                tecnico.getPerfil().getId()
        ));
        return mapper.toDomain(repository.save(entity));
    }
    public PageResult<Tecnico> findAll(PageCriteria pageCriteria) { return PageMapper.toPageResult(repository.findAll(PageMapper.toPageable(pageCriteria)), mapper::toDomain); }
    public List<Tecnico> findAll() { return repository.findAll().stream().map(mapper::toDomain).toList(); }
    public Optional<Tecnico> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<Tecnico> findByPerfilId(UUID perfilId) { return repository.findByPerfilId(perfilId).map(mapper::toDomain); }
    public boolean existsByPerfilId(UUID perfilId) { return repository.existsByPerfilId(perfilId); }
    public boolean existsByCodigoTecnico(String codigoTecnico) { return repository.existsByCodigoTecnico(codigoTecnico); }
}
