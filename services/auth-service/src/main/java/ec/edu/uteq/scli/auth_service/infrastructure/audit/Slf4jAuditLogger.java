package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jAuditLogger implements AuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    @Override
    public void registrarEvento(
            String tipoEvento,
            String usuario,
            String ip,
            String detalle) {

        AUDIT_LOG.atInfo()
                .addKeyValue("evento", tipoEvento)
                .addKeyValue("usuario", usuario)
                .addKeyValue("ip", ip)
                .addKeyValue("detalle", detalle)
                .log("auditoria");
    }
}
