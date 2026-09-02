package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {

    Optional<Docente> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoDocente(String codigoDocente);

    @Query("""
            select d from Docente d
            join AdscripcionInstitucionalEntity a on a.perfil.id = d.perfil.id
            where a.tipoAmbito = ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional.CARRERA
              and a.ambitoId = :carreraId and a.activo = true and d.activo = true
            order by d.codigoDocente
            """)
    List<Docente> findActivosByCarreraId(@Param("carreraId") UUID carreraId);
}
