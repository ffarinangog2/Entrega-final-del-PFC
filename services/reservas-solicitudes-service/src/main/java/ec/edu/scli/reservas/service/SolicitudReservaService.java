package ec.edu.scli.reservas.service;

import ec.edu.scli.reservas.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.enums.EstadoSolicitud;

import java.time.LocalDate;
import java.util.UUID;

/** Define las operaciones de negocio disponibles para solicitudes de reserva. */
public interface SolicitudReservaService {

    SolicitudReservaResponse crear(CrearSolicitudReservaRequest request, String claveIdempotencia,
                                   UUID usuarioAutenticadoId);

    PaginaResponse<SolicitudReservaResponse> listar(EstadoSolicitud estado, UUID solicitanteId,
                                                     UUID laboratorioId, LocalDate fecha, int pagina,
                                                     int tamanio);

    SolicitudReservaResponse buscarPorId(UUID id);

    PaginaResponse<SolicitudReservaResponse> listarPorSolicitante(UUID solicitanteId, int pagina, int tamanio);

    PaginaResponse<SolicitudReservaResponse> listarPorEstado(EstadoSolicitud estado, int pagina, int tamanio);

    SolicitudReservaResponse actualizar(UUID id, ActualizarSolicitudReservaRequest request,
                                        UUID usuarioAutenticadoId);

    SolicitudReservaResponse ponerEnRevision(UUID id, UUID usuarioAutenticadoId);

    ReservaResponse aprobar(UUID id, AprobarSolicitudRequest request, String claveIdempotencia,
                            UUID usuarioAutenticadoId);

    SolicitudReservaResponse rechazar(UUID id, RechazarSolicitudRequest request, UUID usuarioAutenticadoId);

    SolicitudReservaResponse cancelar(UUID id, CancelarSolicitudRequest request, UUID usuarioAutenticadoId);

    PaginaResponse<HistorialSolicitudResponse> obtenerHistorial(UUID solicitudId, int pagina, int tamanio);
}
