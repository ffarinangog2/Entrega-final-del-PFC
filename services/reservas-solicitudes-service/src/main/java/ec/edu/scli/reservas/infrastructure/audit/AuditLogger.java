package ec.edu.scli.reservas.infrastructure.audit;

public interface AuditLogger {

    void registrarEvento(
            String tipoEvento,
            String usuario,
            String ip,
            String detalle);
}
