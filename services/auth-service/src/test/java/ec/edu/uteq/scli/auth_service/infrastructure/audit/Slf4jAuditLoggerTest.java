package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Slf4jAuditLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger auditLogger;

    @BeforeEach
    void configurarAppender() {
        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void limpiarAppender() {
        auditLogger.detachAppender(appender);
    }

    @Test
    void registraEventoConCamposEstructurados() {
        Slf4jAuditLogger logger = new Slf4jAuditLogger();

        logger.registrarEvento(
                "login_fallido",
                "jperez",
                "10.0.0.5",
                "BadCredentialsException");

        assertEquals(1, appender.list.size());

        Map<String, String> campos = appender.list.get(0)
                .getKeyValuePairs()
                .stream()
                .collect(Collectors.toMap(kv -> kv.key, kv -> String.valueOf(kv.value)));

        assertEquals("login_fallido", campos.get("evento"));
        assertEquals("jperez", campos.get("usuario"));
        assertEquals("10.0.0.5", campos.get("ip"));
        assertEquals("BadCredentialsException", campos.get("detalle"));
    }

    @Test
    void aceptaDetalleNulo() {
        Slf4jAuditLogger logger = new Slf4jAuditLogger();

        logger.registrarEvento("login_exitoso", "jperez", "10.0.0.5", null);

        List<KeyValuePair> campos = appender.list.get(0).getKeyValuePairs();
        boolean tieneDetalleNulo = campos.stream()
                .anyMatch(kv -> kv.key.equals("detalle") && kv.value == null);

        assertEquals(true, tieneDetalleNulo);
    }
}
