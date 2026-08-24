package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.IdempotenciaCreacionSolicitudJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IdempotenciaCreacionSolicitudSpringDataRepository
        extends JpaRepository<IdempotenciaCreacionSolicitudJpaEntity, String> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO idempotencia_creacion_solicitudes
                (clave, operacion, actor_id, payload_hash)
            VALUES (:clave, 'CREAR_SOLICITUD', :actorId, :payloadHash)
            ON CONFLICT (clave) DO NOTHING
            """, nativeQuery = true)
    void insertarSiAusente(@Param("clave") String clave,
                           @Param("actorId") UUID actorId,
                           @Param("payloadHash") String payloadHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IdempotenciaCreacionSolicitudJpaEntity i where i.clave = :clave")
    Optional<IdempotenciaCreacionSolicitudJpaEntity> buscarParaActualizar(@Param("clave") String clave);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE idempotencia_creacion_solicitudes
               SET solicitud_id = :solicitudId, completada_en = current_timestamp
             WHERE clave = :clave AND solicitud_id IS NULL
            """, nativeQuery = true)
    int completar(@Param("clave") String clave, @Param("solicitudId") UUID solicitudId);
}
