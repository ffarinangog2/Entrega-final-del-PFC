package ec.edu.uteq.scli.auth_service.infrastructure.audit;

public interface AuditLogger {

    void registrarEvento(
            String tipoEvento,
            String usuario,
            String ip,
            String detalle);
}
