package ec.edu.uteq.scli.auth_service.infrastructure.bootstrap;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialIdentityBootstrap implements ApplicationRunner {
    static final List<Account> ACCOUNTS = List.of(
            account(1, "administrador.facultad01", "ADMINISTRADOR"),
            account(2, "administrador.facultad02", "ADMINISTRADOR"),
            account(3, "adminpiso.01", "ADMINISTRADOR_PISO"),
            account(4, "adminpiso.02", "ADMINISTRADOR_PISO"),
            account(5, "coordinacion.carrera01", "COORDINADOR"),
            account(6, "coordinacion.carrera02", "COORDINADOR"),
            account(7, "docente.lab01", "DOCENTE"),
            account(8, "docente.lab02", "DOCENTE"),
            account(9, "estudiante.lab01", "ESTUDIANTE"),
            account(10, "estudiante.lab02", "ESTUDIANTE"));

    private final UsuarioAuthRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final String password;

    public InitialIdentityBootstrap(UsuarioAuthRepository usuarios, RolRepository roles,
            PasswordEncoder encoder, @Value("${app.initial-data.password:}") String password) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("INITIAL_USERS_PASSWORD es obligatorio cuando INITIAL_DATA_ENABLED=true");
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (Account account : ACCOUNTS) {
            if (usuarios.existsByUsernameIgnoreCase(account.username())) continue;
            var role = roles.findByCodigoIgnoreCase(account.role())
                    .filter(value -> Boolean.TRUE.equals(value.getActivo()))
                    .orElseThrow(() -> new IllegalStateException("Rol funcional no disponible: " + account.role()));
            UsuarioAuth user = new UsuarioAuth();
            user.setId(account.authId());
            user.setPerfilId(account.profileId());
            user.setUsername(account.username());
            user.setEmail(account.username() + "@scli.local");
            user.setPasswordHash(encoder.encode(password));
            user.setActivo(true);
            user.setCuentaBloqueada(false);
            user.setIntentosFallidos(0);
            user.setPasswordActualizadoEn(now);
            user.setCreadoEn(now);
            user.setActualizadoEn(now);
            user.getRoles().add(role);
            usuarios.save(user);
        }
    }

    private static Account account(int suffix, String username, String role) {
        return new Account(UUID.fromString("11000000-0000-0000-0000-0000000000" + String.format("%02d", suffix)),
                UUID.fromString("22000000-0000-0000-0000-0000000000" + String.format("%02d", suffix)), username, role);
    }

    record Account(UUID authId, UUID profileId, String username, String role) { }
}
