package ec.edu.scli.reservas.service;

import ec.edu.scli.reservas.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.enums.EstadoReserva;

import java.time.LocalDate;
import java.util.UUID;

/** Define las operaciones de negocio disponibles para reservas confirmadas. */
public interface ReservaService {

    PaginaResponse<ReservaResponse> listar(EstadoReserva estado, UUID laboratorioId, UUID responsableId,
                                           LocalDate fechaDesde, LocalDate fechaHasta, int pagina, int tamanio);

    ReservaResponse buscarPorId(UUID id);

    PaginaResponse<ReservaResponse> listarPorLaboratorio(UUID laboratorioId, int pagina, int tamanio);

    PaginaResponse<ReservaResponse> listarPorResponsable(UUID responsableId, int pagina, int tamanio);

    PaginaResponse<ReservaResponse> obtenerCalendario(UUID laboratorioId, LocalDate fechaDesde,
                                                      LocalDate fechaHasta, int pagina, int tamanio);

    ReservaResponse cancelar(UUID id, CancelarReservaRequest request, UUID usuarioAutenticadoId);

    ReservaResponse iniciar(UUID id, UUID usuarioAutenticadoId);

    ReservaResponse finalizar(UUID id, UUID usuarioAutenticadoId);

    ReservaResponse marcarNoAsistida(UUID id, UUID usuarioAutenticadoId);
}
