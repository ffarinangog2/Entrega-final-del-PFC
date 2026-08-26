package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PageMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilPersistenceMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.specification.PerfilSpecification;
import ec.edu.scli.usuarios.infrastructure.security.HmacIdentificacionService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PerfilRepositoryAdapter implements PerfilRepositoryPort {

    private final PerfilRepository repository;
    private final PerfilPersistenceMapper mapper;
    private final HmacIdentificacionService hmacService;

    public PerfilRepositoryAdapter(
            PerfilRepository repository,
            PerfilPersistenceMapper mapper,
            HmacIdentificacionService hmacService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.hmacService = hmacService;
    }

    @Override
    public Perfil save(Perfil perfil) {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil entity = mapper.toEntity(perfil);
        entity.setIdentificacionHash(hmacService.calcularHash(perfil.getIdentificacion()));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Perfil> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Perfil> findByIdentificacion(String identificacion) {
        return repository.findByIdentificacionHash(hmacService.calcularHash(identificacion))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Perfil> findByEmailInstitucional(String emailInstitucional) {
        return repository.findByEmailInstitucional(emailInstitucional).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentificacion(String identificacion) {
        return repository.existsByIdentificacionHash(hmacService.calcularHash(identificacion));
    }

    @Override
    public boolean existsByEmailInstitucional(String emailInstitucional) {
        return repository.existsByEmailInstitucional(emailInstitucional);
    }

    @Override
    public PageResult<Perfil> findAll(
            String identificacion,
            String nombre,
            String email,
            TipoPerfil tipoPerfil,
            Boolean activo,
            PageCriteria pageCriteria
    ) {
        String identificacionHash = (identificacion == null || identificacion.isBlank())
                ? null
                : hmacService.calcularHash(identificacion);

        Specification<ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil> specification =
                PerfilSpecification.identificacionHashIgual(identificacionHash)
                        .and(PerfilSpecification.nombreContiene(nombre))
                        .and(PerfilSpecification.emailInstitucionalContiene(email))
                        .and(PerfilSpecification.tieneTipoPerfil(tipoPerfil))
                        .and(PerfilSpecification.tieneEstado(activo));

        return PageMapper.toPageResult(
                repository.findAll(specification, PageMapper.toPageable(pageCriteria)),
                mapper::toDomain
        );
    }
}