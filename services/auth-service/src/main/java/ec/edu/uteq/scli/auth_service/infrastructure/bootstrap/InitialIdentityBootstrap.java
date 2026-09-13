package ec.edu.uteq.scli.auth_service.infrastructure.bootstrap;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import ec.edu.uteq.scli.auth_service.application.service.RefreshSessionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.initial-data.enabled", havingValue = "true")
public class InitialIdentityBootstrap implements ApplicationRunner {
    private static final List<Account> BASE_ACCOUNTS = List.of(
            account(3, "adminpiso.01", "ADMINISTRADOR_PISO"),
            account(4, "adminpiso.02", "ADMINISTRADOR_PISO"),
            account(5, "coordinacion.carrera01", "COORDINADOR"),
            account(6, "coordinacion.carrera02", "COORDINADOR"),
            account(7, "docente.lab01", "DOCENTE"),
            account(8, "docente.lab02", "DOCENTE"),
            account(9, "estudiante.lab01", "ESTUDIANTE"),
            account(10, "estudiante.lab02", "ESTUDIANTE"));
    static final List<Account> ACCOUNTS = buildAccounts();

    private static List<Account> buildAccounts() {
        List<Account> accounts = new ArrayList<>(BASE_ACCOUNTS);
        accounts.add(namedAccount("adminpiso.03", "ADMINISTRADOR_PISO"));
        accounts.add(namedAccount("adminpiso.04", "ADMINISTRADOR_PISO"));
        for (int number = 3; number <= 20; number++) {
            accounts.add(namedAccount("docente.%02d".formatted(number), "DOCENTE"));
        }
        for (String career : List.of("software", "telematica")) {
            for (int level = 1; level <= 10; level++) {
                for (int student = 1; student <= 10; student++) {
                    if (level == 1 && student == 1) continue;
                    accounts.add(namedAccount("estudiante.%s.%02d.%02d".formatted(career, level, student), "ESTUDIANTE"));
                }
            }
        }
        return List.copyOf(accounts);
    }

    private final UsuarioAuthRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final RefreshSessionService refreshSessions;
    private final String password;

    public InitialIdentityBootstrap(UsuarioAuthRepository usuarios, RolRepository roles,
            PasswordEncoder encoder, RefreshSessionService refreshSessions,
            @Value("${app.initial-data.password:}") String password) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.refreshSessions = refreshSessions;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("INITIAL_USERS_PASSWORD es obligatorio cuando INITIAL_DATA_ENABLED=true");
        }
        normalizarAdministradoresGlobales();
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
            user.setEmail(account.username() + (account.authId().toString().startsWith("11000000") ? "@scli.local" : "@scli.edu.ec"));
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

    private void normalizarAdministradoresGlobales() {
        usuarios.findByUsernameIgnoreCase("admin").ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getActivo())) {
                user.setActivo(true);
                user.setActualizadoEn(OffsetDateTime.now());
                usuarios.save(user);
            }
        });
        desactivarAdministradorGlobalLegado("administrador.facultad01");
        desactivarAdministradorGlobalLegado("administrador.facultad02");
    }

    private void desactivarAdministradorGlobalLegado(String username) {
        usuarios.findByUsernameIgnoreCase(username).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getActivo())) {
                user.setActivo(false);
                user.setActualizadoEn(OffsetDateTime.now());
                usuarios.save(user);
            }
            refreshSessions.revocarActivasPorUsuario(user.getId());
        });
    }

    private static Account account(int suffix, String username, String role) {
        return new Account(UUID.fromString("11000000-0000-0000-0000-0000000000" + String.format("%02d", suffix)),
                UUID.fromString("22000000-0000-0000-0000-0000000000" + String.format("%02d", suffix)), username, role);
    }

    private static Account namedAccount(String username, String role) {
        return new Account(stableId("auth:" + username), stableId("profile:" + username), username, role);
    }

    private static UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(("scli-integral-test:" + value).getBytes(StandardCharsets.UTF_8));
    }

    record Account(UUID authId, UUID profileId, String username, String role) { }
}
