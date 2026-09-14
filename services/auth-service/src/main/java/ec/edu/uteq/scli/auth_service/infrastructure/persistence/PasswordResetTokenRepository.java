package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findLockedByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken t set t.invalidadoEn=:now where t.usuarioId=:userId and t.usadoEn is null and t.invalidadoEn is null and t.expiraEn>:now")
    int invalidateActive(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("update PasswordResetToken t set t.invalidadoEn=:now where t.id=:tokenId and t.usadoEn is null and t.invalidadoEn is null")
    int invalidateById(@Param("tokenId") UUID tokenId, @Param("now") OffsetDateTime now);
}
