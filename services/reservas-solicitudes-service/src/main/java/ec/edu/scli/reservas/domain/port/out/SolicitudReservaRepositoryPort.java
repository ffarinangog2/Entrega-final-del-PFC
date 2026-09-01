package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.*;
import java.util.*;
import java.time.Instant;

public interface SolicitudReservaRepositoryPort {
    Pagina<SolicitudReserva> buscar(FiltroSolicitudReserva filtro, int pagina, int tamanio);
    Pagina<SolicitudReserva> buscarPorSolicitante(UUID solicitanteId, int pagina, int tamanio);
    Pagina<SolicitudReserva> buscarPorEstado(EstadoSolicitud estado, int pagina, int tamanio);
    Optional<SolicitudReserva> buscarPorId(UUID id);
    Optional<SolicitudReserva> buscarPorIdParaActualizar(UUID id);
    Optional<SolicitudReserva> buscarPorClaveIdempotencia(String clave);
    SolicitudReserva guardar(SolicitudReserva solicitud);
    List<SolicitudReserva> buscarPendientesAnterioresA(Instant limite);
}
