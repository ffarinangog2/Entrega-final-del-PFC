package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    private UUID id;
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;
    @Column(name = "familia_token", nullable = false)
    private UUID familiaToken;
    @Column(name = "emitido_en", nullable = false)
    private OffsetDateTime emitidoEn;
    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;
    @Column(nullable = false)
    private Boolean revocado;
    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;
    @Column(name = "reemplazado_por")
    private UUID reemplazadoPor;
}
