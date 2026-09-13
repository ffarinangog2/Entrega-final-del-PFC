package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.IdempotenciaAprobacionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IdempotenciaAprobacionSpringDataRepository
        extends JpaRepository<IdempotenciaAprobacionJpaEntity, String> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotencia_aprobaciones (clave, operacion, solicitud_id)
            VALUES (:clave, 'APROBAR_SOLICITUD', :solicitudId)
            ON CONFLICT (clave) DO NOTHING
            """, nativeQuery = true)
    int insertarSiAusente(
            @Param("clave") String clave,
            @Param("solicitudId") UUID solicitudId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IdempotenciaAprobacion i WHERE i.clave = :clave")
    Optional<IdempotenciaAprobacionJpaEntity> findByClaveForUpdate(
            @Param("clave") String clave);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE idempotencia_aprobaciones
            SET reserva_id = :reservaId, completada_en = CURRENT_TIMESTAMP
            WHERE clave = :clave AND reserva_id IS NULL
            """, nativeQuery = true)
    int completar(
            @Param("clave") String clave,
            @Param("reservaId") UUID reservaId);
}
