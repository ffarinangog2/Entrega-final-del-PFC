package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.presentation.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerAlternativaRequest;
import ec.edu.scli.reservas.presentation.dto.request.ResponderPropuestaRequest;
import ec.edu.scli.reservas.presentation.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

import java.time.LocalDate;
import java.util.UUID;

/** Define las operaciones de negocio disponibles para solicitudes de reserva. */
public interface SolicitudReservaService {

    SolicitudReservaResponse crear(CrearSolicitudReservaRequest request, String claveIdempotencia,
                                   UUID usuarioAutenticadoId);

    PaginaResponse<SolicitudReservaResponse> listar(EstadoSolicitud estado, UUID solicitanteId,
                                                     UUID laboratorioId, LocalDate fecha, int pagina,
                                                     int tamanio);

    PaginaResponse<SolicitudReservaResponse> listarAutorizado(EstadoSolicitud estado, UUID solicitanteId,
            UUID laboratorioId, LocalDate fecha, int pagina, int tamanio, UUID actorId);

    SolicitudReservaResponse buscarPorId(UUID id);
    SolicitudReservaResponse buscarPorIdAutorizado(UUID id, UUID actorId);

    PaginaResponse<SolicitudReservaResponse> listarPorSolicitante(UUID solicitanteId, int pagina, int tamanio);

    PaginaResponse<SolicitudReservaResponse> listarPorEstado(EstadoSolicitud estado, int pagina, int tamanio);

    SolicitudReservaResponse actualizar(UUID id, ActualizarSolicitudReservaRequest request,
                                        UUID usuarioAutenticadoId);

    SolicitudReservaResponse ponerEnRevision(UUID id, UUID usuarioAutenticadoId);

    ReservaResponse aprobar(UUID id, AprobarSolicitudRequest request, String claveIdempotencia,
                            UUID usuarioAutenticadoId);

    SolicitudReservaResponse rechazar(UUID id, RechazarSolicitudRequest request, UUID usuarioAutenticadoId);

    SolicitudReservaResponse cancelar(UUID id, CancelarSolicitudRequest request, UUID usuarioAutenticadoId);

    SolicitudReservaResponse proponerAlternativa(UUID id, ProponerAlternativaRequest request, UUID actorId);
    SolicitudReservaResponse aceptarPropuesta(UUID id, ResponderPropuestaRequest request, UUID actorId);
    SolicitudReservaResponse rechazarPropuesta(UUID id, ResponderPropuestaRequest request, UUID actorId);

    PaginaResponse<HistorialSolicitudResponse> obtenerHistorial(UUID solicitudId, int pagina, int tamanio);
}
