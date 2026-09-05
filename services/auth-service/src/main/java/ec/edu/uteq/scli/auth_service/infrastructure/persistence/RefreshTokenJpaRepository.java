package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revocado = true, token.revocadoEn = :ahora
             where token.tokenHash = :hash
               and token.revocado = false
               and token.expiraEn > :ahora
            """)
    int revocarSiActiva(@Param("hash") String hash, @Param("ahora") OffsetDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.reemplazadoPor = :reemplazoId where token.tokenHash = :hash")
    int registrarReemplazo(@Param("hash") String hash, @Param("reemplazoId") UUID reemplazoId);

    @Query("select count(token) from RefreshToken token where token.revocado = false and token.expiraEn > :ahora")
    long contarActivas(@Param("ahora") OffsetDateTime ahora);
}
