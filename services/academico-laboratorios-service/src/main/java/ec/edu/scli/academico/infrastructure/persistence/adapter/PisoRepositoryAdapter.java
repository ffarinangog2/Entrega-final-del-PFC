package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Piso;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.PisoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.PisoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.specification.PisoJpaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PisoRepositoryAdapter implements PisoRepositoryPort {

    private final PisoJpaRepository pisoJpaRepository;
    private final PisoEntityMapper mapper;

    public PisoRepositoryAdapter(PisoJpaRepository pisoJpaRepository, PisoEntityMapper mapper) {
        this.pisoJpaRepository = pisoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Piso guardar(Piso piso) {
        PisoEntity entidad = mapper.aEntidad(piso);
        PisoEntity guardada = pisoJpaRepository.save(entidad);
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Piso> buscarPorId(UUID id) {
        return pisoJpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Page<Piso> buscar(UUID bloqueId, Boolean activo, Pageable pageable) {
        Specification<PisoEntity> specification =
                PisoJpaSpecification.tieneBloque(bloqueId)
                        .and(PisoJpaSpecification.tieneEstado(activo));

        return pisoJpaRepository.findAll(specification, pageable).map(mapper::aDominio);
    }

    @Override
    public List<Piso> buscarPorBloque(UUID bloqueId) {
        return pisoJpaRepository.findByBloqueId(bloqueId)
                .stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeNumeroEnBloque(UUID bloqueId, Integer numero) {
        return pisoJpaRepository.existsByBloqueIdAndNumero(bloqueId, numero);
    }

    @Override
    public boolean existeNumeroEnBloqueParaOtroId(UUID bloqueId, Integer numero, UUID id) {
        return pisoJpaRepository.existsByBloqueIdAndNumeroAndIdNot(bloqueId, numero, id);
    }
}
