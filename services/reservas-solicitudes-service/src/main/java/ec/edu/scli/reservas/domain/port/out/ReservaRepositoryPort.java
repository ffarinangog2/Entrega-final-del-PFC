package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.*;
import java.time.*;
import java.util.*;

public interface ReservaRepositoryPort {
    Pagina<Reserva> buscar(FiltroReserva filtro, int pagina, int tamanio);
    List<Reserva> buscarParaAgenda(UUID laboratorioId, LocalDate fechaDesde, LocalDate fechaHasta);
    Pagina<Reserva> buscarPorLaboratorio(UUID laboratorioId, int pagina, int tamanio);
    Pagina<Reserva> buscarPorResponsable(UUID responsableId, int pagina, int tamanio);
    Pagina<Reserva> buscarCalendario(UUID laboratorioId, LocalDate fechaDesde, LocalDate fechaHasta, int pagina, int tamanio);
    Optional<Reserva> buscarPorId(UUID id);
    Optional<Reserva> buscarPorIdParaActualizar(UUID id);
    Optional<Reserva> buscarPorSolicitudId(UUID solicitudId);
    boolean existePorSolicitudId(UUID solicitudId);
    long contarConflictosActivos(UUID laboratorioId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin);
    Reserva guardar(Reserva reserva);
}
