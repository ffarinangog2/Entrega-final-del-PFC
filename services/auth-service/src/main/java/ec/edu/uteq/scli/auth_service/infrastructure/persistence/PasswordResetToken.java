package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "password_reset_tokens") @Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
    @Id private UUID id;
    @Column(name="usuario_id", nullable=false) private UUID usuarioId;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(name="creado_en", nullable=false) private OffsetDateTime creadoEn;
    @Column(name="expira_en", nullable=false) private OffsetDateTime expiraEn;
    @Column(name="usado_en") private OffsetDateTime usadoEn;
    @Column(name="invalidado_en") private OffsetDateTime invalidadoEn;
    @Column(name="solicitado_ip", length=64) private String solicitadoIp;
}
