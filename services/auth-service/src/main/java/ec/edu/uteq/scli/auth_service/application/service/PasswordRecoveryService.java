package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.domain.service.InvalidPasswordResetTokenException;
import ec.edu.uteq.scli.auth_service.infrastructure.client.UsuariosClient;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    public static final String NEUTRAL_MESSAGE = "Si la cuenta existe, se enviaron instrucciones al correo registrado.";
    private final UsuarioAuthRepository users; private final PasswordResetTokenRepository tokens;
    private final UsuariosClient usuariosClient; private final PasswordResetMailService mail;
    private final PasswordPolicyValidator policy; private final PasswordEncoder encoder; private final Clock clock;
    private final Duration expiration; private final String baseUrl; private final SecureRandom random = new SecureRandom();
    private final PasswordResetTokenManager tokenManager;
    private final Map<String, Window> limits = new ConcurrentHashMap<>();

    public PasswordRecoveryService(UsuarioAuthRepository users, PasswordResetTokenRepository tokens,
            UsuariosClient usuariosClient, PasswordResetMailService mail, PasswordPolicyValidator policy,
            PasswordEncoder encoder, Clock clock,
            @Value("${app.password-reset.expiration-minutes:20}") long expirationMinutes,
            @Value("${app.password-reset.base-url}") String baseUrl,
            PasswordResetTokenManager tokenManager) {
        this.users=users; this.tokens=tokens; this.usuariosClient=usuariosClient; this.mail=mail; this.policy=policy;
        this.encoder=encoder; this.clock=clock; this.expiration=Duration.ofMinutes(expirationMinutes); this.baseUrl=baseUrl;
        this.tokenManager=tokenManager;
    }

    public void request(String identifier, String ipAddress) {
        String normalized=identifier.trim().toLowerCase(Locale.ROOT);
        log.info("security_event=PASSWORD_RESET_REQUEST_RECEIVED ip={}", safeIp(ipAddress));
        if (!allow((ipAddress == null ? "unknown" : ipAddress)+"|"+normalized)) {
            log.info("security_event=PASSWORD_RESET_REQUEST_RATE_LIMITED ip={}", safeIp(ipAddress)); return;
        }
        Optional<UsuarioAuth> found=users.findByUsernameIgnoreCase(normalized).or(() -> users.findByEmailIgnoreCase(normalized));
        if (found.isEmpty() || !Boolean.TRUE.equals(found.get().getActivo())) return;
        UsuarioAuth user=found.get(); OffsetDateTime now=now();
        try {
            var profile=usuariosClient.obtenerPerfil(user.getPerfilId());
            if (profile == null || !Boolean.TRUE.equals(profile.activo())) return;
            Optional<String> destination=validInstitutionalEmail(profile.emailInstitucional());
            if (destination.isEmpty()) return;
            log.info("security_event=PASSWORD_RESET_ACCOUNT_RESOLVED userId={} profileId={}", user.getId(), user.getPerfilId());
            byte[] bytes=new byte[32]; random.nextBytes(bytes);
            String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken row=new PasswordResetToken(); row.setId(UUID.randomUUID()); row.setUsuarioId(user.getId());
            row.setTokenHash(hash(raw)); row.setCreadoEn(now); row.setExpiraEn(now.plus(expiration)); row.setSolicitadoIp(limit(ipAddress,64));
            tokenManager.replaceActive(row);
            log.info("security_event=PASSWORD_RESET_MAIL_ATTEMPT userId={} profileId={}", user.getId(), user.getPerfilId());
            try {
                mail.sendResetLink(destination.get(), baseUrl+"?token="+raw);
            } catch (RuntimeException mailFailure) {
                try { tokenManager.invalidate(row.getId(), now()); }
                catch (RuntimeException invalidationFailure) {
                    log.error("security_event=PASSWORD_RESET_TOKEN_INVALIDATION_FAILURE userId={} exceptionType={}",
                            user.getId(), invalidationFailure.getClass().getSimpleName());
                }
                log.warn("security_event=PASSWORD_RESET_MAIL_FAILURE userId={} profileId={} exceptionType={}",
                        user.getId(), user.getPerfilId(), mailFailure.getClass().getSimpleName());
            }
        } catch (RuntimeException failure) {
            log.warn("security_event=PASSWORD_RESET_REQUEST_PROCESSING_FAILURE userId={} profileId={} exceptionType={}",
                    user.getId(), user.getPerfilId(), failure.getClass().getSimpleName());
        }
    }

    @Transactional
    public void reset(String rawToken, String newPassword, String confirmation) {
        if (!Objects.equals(newPassword, confirmation)) throw new IllegalArgumentException("Las contraseñas no coinciden");
        policy.validate(newPassword);
        OffsetDateTime now=now();
        PasswordResetToken token=tokens.findLockedByTokenHash(hash(rawToken)).orElseGet(() -> invalidToken("NOT_FOUND"));
        if (token.getUsadoEn()!=null) invalidToken("ALREADY_USED");
        if (token.getInvalidadoEn()!=null) invalidToken("INVALIDATED");
        if (!now.isBefore(token.getExpiraEn())) invalidToken("EXPIRED");
        UsuarioAuth user=users.findLockedById(token.getUsuarioId()).orElseThrow(InvalidPasswordResetTokenException::new);
        if (encoder.matches(newPassword, user.getPasswordHash())) throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        user.setPasswordHash(encoder.encode(newPassword)); user.setPasswordActualizadoEn(now);
        user.setIntentosFallidos(0); user.setCuentaBloqueada(false); user.setBloqueadoHasta(null); user.setActualizadoEn(now);
        token.setUsadoEn(now);
        tokens.invalidateActive(user.getId(), now);
        log.info("security_event=PASSWORD_RESET_COMPLETED userId={} profileId={}", user.getId(), user.getPerfilId());
    }

    private boolean allow(String key) {
        Instant now=clock.instant(); Window window=limits.compute(key,(k,current) -> current==null || !now.isBefore(current.until)
                ? new Window(now.plus(Duration.ofMinutes(15)),1) : new Window(current.until,current.count+1));
        return window.count<=5;
    }
    private OffsetDateTime now(){ return OffsetDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String limit(String value,int max){return value==null?null:value.substring(0,Math.min(max,value.length()));}
    private Optional<String> validInstitutionalEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String normalized=email.trim();
        try { InternetAddress address=new InternetAddress(normalized, true); address.validate(); return Optional.of(normalized); }
        catch (Exception invalid) { return Optional.empty(); }
    }
    private PasswordResetToken invalidToken(String reason) {
        log.warn("security_event=PASSWORD_RESET_INVALID_TOKEN reason={}", reason);
        throw new InvalidPasswordResetTokenException();
    }
    private static String safeIp(String ip) { return ip == null || ip.isBlank() ? "unknown" : limit(ip,64); }
    private record Window(Instant until,int count) { }
}
