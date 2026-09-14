package ec.edu.uteq.scli.auth_service.application;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ec.edu.uteq.scli.auth_service.application.service.*;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidPasswordResetTokenException;
import ec.edu.uteq.scli.auth_service.infrastructure.client.UsuariosClient;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.*;
import ec.edu.uteq.scli.auth_service.presentation.dto.PerfilAuthResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
    @Mock UsuarioAuthRepository users; @Mock PasswordResetTokenRepository tokens;
    @Mock UsuariosClient profiles; @Mock PasswordResetMailService mail; @Mock PasswordResetTokenManager manager;
    private PasswordRecoveryService service; private UsuarioAuth user; private BCryptPasswordEncoder encoder;
    @BeforeEach void setup() {
        encoder=new BCryptPasswordEncoder(12);
        service=new PasswordRecoveryService(users,tokens,profiles,mail,new PasswordPolicyValidator(),encoder,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC),20,"http://web/reset",manager);
        user=new UsuarioAuth(); user.setId(UUID.randomUUID()); user.setPerfilId(UUID.randomUUID());
        user.setActivo(true); user.setPasswordHash(encoder.encode("ClaveAnterior1!"));
    }
    @Test void cuentaInexistenteNoEnviaCorreo() {
        when(users.findByUsernameIgnoreCase("nadie")).thenReturn(Optional.empty()); when(users.findByEmailIgnoreCase("nadie")).thenReturn(Optional.empty());
        service.request("nadie","ip"); verifyNoInteractions(mail,manager);
    }
    @ParameterizedTest @ValueSource(strings={"", "   "}) @NullSource
    void emailInstitucionalAusenteOVacioNoEnviaCorreo(String email) { validAccount(email); service.request("user","ip"); verifyNoInteractions(mail,manager); }
    @Test void emailValidoNormalizadoInvocaSmtpYGuardaSoloHash() {
        validAccount("  a@uteq.edu.ec  "); service.request("user","ip");
        ArgumentCaptor<PasswordResetToken> row=ArgumentCaptor.forClass(PasswordResetToken.class); ArgumentCaptor<String> link=ArgumentCaptor.forClass(String.class);
        verify(manager).replaceActive(row.capture()); verify(mail).sendResetLink(eq("a@uteq.edu.ec"),link.capture());
        String raw=link.getValue().substring(link.getValue().indexOf("token=")+6);
        assertNotEquals(raw,row.getValue().getTokenHash()); assertEquals(43,raw.length()); assertEquals(sha(raw),row.getValue().getTokenHash());
    }
    @Test void falloSmtpSeOcultaEInvalidaElTokenEmitido() {
        validAccount("a@uteq.edu.ec"); doThrow(new IllegalStateException("smtp unavailable")).when(mail).sendResetLink(any(),any());
        assertDoesNotThrow(()->service.request("user","ip"));
        ArgumentCaptor<PasswordResetToken> row=ArgumentCaptor.forClass(PasswordResetToken.class); verify(manager).replaceActive(row.capture());
        verify(manager).invalidate(eq(row.getValue().getId()),any());
    }
    @Test void resetValidoAlmacenaBcryptYMarcaSoloUsado() {
        String raw="token-seguro"; PasswordResetToken token=activeToken(raw);
        when(tokens.findLockedByTokenHash(sha(raw))).thenReturn(Optional.of(token)); when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));
        service.reset(raw,"ClaveNuevaSegura2!","ClaveNuevaSegura2!");
        assertTrue(encoder.matches("ClaveNuevaSegura2!",user.getPasswordHash())); assertFalse(encoder.matches("ClaveAnterior1!",user.getPasswordHash()));
        assertNotNull(token.getUsadoEn()); assertNull(token.getInvalidadoEn());
    }
    @Test void tokenUsadoNoPuedeReutilizarse() {
        PasswordResetToken token=activeToken("x"); token.setUsadoEn(OffsetDateTime.parse("2025-12-31T23:59:00Z")); when(tokens.findLockedByTokenHash(sha("x"))).thenReturn(Optional.of(token));
        assertThrows(InvalidPasswordResetTokenException.class,()->service.reset("x","ClaveNuevaSegura2!","ClaveNuevaSegura2!"));
    }
    @Test void tokenInvalidadoNoPuedeUtilizarse() {
        PasswordResetToken token=activeToken("x"); token.setInvalidadoEn(OffsetDateTime.parse("2025-12-31T23:59:00Z")); when(tokens.findLockedByTokenHash(sha("x"))).thenReturn(Optional.of(token));
        assertThrows(InvalidPasswordResetTokenException.class,()->service.reset("x","ClaveNuevaSegura2!","ClaveNuevaSegura2!"));
    }
    @Test void logsNoContienenTokenPasswordCorreoNiSecretos() {
        Logger logger=(Logger)LoggerFactory.getLogger(PasswordRecoveryService.class); ListAppender<ILoggingEvent> appender=new ListAppender<>(); appender.start(); logger.addAppender(appender);
        try { validAccount("a@uteq.edu.ec"); service.request("user","ip"); ArgumentCaptor<String> link=ArgumentCaptor.forClass(String.class); verify(mail).sendResetLink(any(),link.capture());
            String raw=link.getValue().substring(link.getValue().indexOf("token=")+6); PasswordResetToken token=activeToken(raw);
            when(tokens.findLockedByTokenHash(sha(raw))).thenReturn(Optional.of(token)); when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));
            service.reset(raw,"ClaveNuevaSegura2!","ClaveNuevaSegura2!");
            String logs=appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+b);
            assertFalse(logs.contains(raw)); assertFalse(logs.contains("ClaveAnterior1!")); assertFalse(logs.contains("ClaveNuevaSegura2!"));
            assertFalse(logs.contains("MAIL_PASSWORD")); assertFalse(logs.contains("a@uteq.edu.ec"));
        } finally { logger.detachAppender(appender); }
    }
    private void validAccount(String email) { when(users.findByUsernameIgnoreCase("user")).thenReturn(Optional.of(user)); when(profiles.obtenerPerfil(user.getPerfilId())).thenReturn(new PerfilAuthResponse(user.getPerfilId(),"A","B",email,true,List.of())); }
    private PasswordResetToken activeToken(String raw) { PasswordResetToken token=new PasswordResetToken(); token.setUsuarioId(user.getId()); token.setTokenHash(sha(raw)); token.setExpiraEn(OffsetDateTime.parse("2026-01-01T00:20:00Z")); return token; }
    private static String sha(String value) { try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);} }
}
