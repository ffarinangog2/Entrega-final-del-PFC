package ec.edu.scli.reservas.service;

import ec.edu.scli.reservas.dto.request.CrearBloqueoAgendaRequest;
import ec.edu.scli.reservas.dto.response.AgendaItemResponse;
import ec.edu.scli.reservas.dto.response.BloqueoAgendaResponse;
import ec.edu.scli.reservas.dto.response.PaginaResponse;

import java.time.LocalDate;
import java.util.UUID;

/** Define las operaciones de consulta y bloqueo de agenda. */
public interface AgendaService {

    PaginaResponse<AgendaItemResponse> listar(UUID laboratorioId, LocalDate fechaDesde, LocalDate fechaHasta,
                                              int pagina, int tamanio);

    PaginaResponse<AgendaItemResponse> listarPorLaboratorio(UUID laboratorioId, LocalDate fechaDesde,
                                                            LocalDate fechaHasta, int pagina, int tamanio);

    BloqueoAgendaResponse crearBloqueo(CrearBloqueoAgendaRequest request, UUID usuarioAutenticadoId);

    void eliminarBloqueo(UUID bloqueoId, UUID usuarioAutenticadoId);
}
