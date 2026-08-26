package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "intentos_login") @Getter @Setter @NoArgsConstructor
public class IntentoLogin {
    @Id private UUID id;
    @Column(name = "usuario_id") private UUID usuarioId;
    @Column(name = "username_ingresado", length = 160) private String usernameIngresado;
    @Column(nullable = false) private Boolean exitoso;
    @Column(length = 150) private String motivo;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "fecha_hora", nullable = false) private OffsetDateTime fechaHora;
}
