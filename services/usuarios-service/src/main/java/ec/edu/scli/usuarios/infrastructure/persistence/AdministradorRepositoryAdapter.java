package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PageMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilRelacionadoPersistenceMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdministradorRepositoryAdapter implements AdministradorRepositoryPort {

    private final AdministradorRepository repository;
    private final PerfilRelacionadoPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public AdministradorRepositoryAdapter(AdministradorRepository repository, PerfilRelacionadoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Administrador save(Administrador administrador) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador entity =
                mapper.toEntity(administrador);
        entity.setPerfil(entityManager.getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                administrador.getPerfil().getId()
        ));
        return mapper.toDomain(repository.save(entity));
    }
    public PageResult<Administrador> findAll(PageCriteria pageCriteria) { return PageMapper.toPageResult(repository.findAll(PageMapper.toPageable(pageCriteria)), mapper::toDomain); }
    public List<Administrador> findAll() { return repository.findAll().stream().map(mapper::toDomain).toList(); }
    public Optional<Administrador> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<Administrador> findByPerfilId(UUID perfilId) { return repository.findByPerfilId(perfilId).map(mapper::toDomain); }
    public boolean existsByPerfilId(UUID perfilId) { return repository.existsByPerfilId(perfilId); }
    public boolean existsByCodigoAdministrador(String codigoAdministrador) { return repository.existsByCodigoAdministrador(codigoAdministrador); }
}
