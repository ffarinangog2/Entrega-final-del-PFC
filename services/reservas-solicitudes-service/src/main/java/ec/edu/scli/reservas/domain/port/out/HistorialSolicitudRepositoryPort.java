package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.*;
import java.util.UUID;

public interface HistorialSolicitudRepositoryPort {
    HistorialSolicitud guardar(HistorialSolicitud historial);
    Pagina<HistorialSolicitud> buscarPorSolicitudId(UUID solicitudId, int pagina, int tamanio);
}
