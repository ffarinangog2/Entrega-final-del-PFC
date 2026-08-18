package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

public final class SolicitudReservaStates {
    private static final SolicitudReservaState PENDIENTE = new PendienteState();
    private static final SolicitudReservaState EN_REVISION = new EnRevisionState();
    private static final SolicitudReservaState APROBADA = new AprobadaState();
    private static final SolicitudReservaState RECHAZADA = new RechazadaState();
    private static final SolicitudReservaState CANCELADA = new CanceladaState();
    private static final SolicitudReservaState EXPIRADA = new ExpiradaState();

    private SolicitudReservaStates() { }

    public static SolicitudReservaState desde(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE -> PENDIENTE;
            case EN_REVISION -> EN_REVISION;
            case APROBADA -> APROBADA;
            case RECHAZADA -> RECHAZADA;
            case CANCELADA -> CANCELADA;
            case EXPIRADA -> EXPIRADA;
        };
    }
}
