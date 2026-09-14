package ec.edu.scli.usuarios.infrastructure.audit;

public interface AuditLogger {

    void registrarEvento(
            String tipoEvento,
            String usuario,
            String ip,
            String detalle);
}
