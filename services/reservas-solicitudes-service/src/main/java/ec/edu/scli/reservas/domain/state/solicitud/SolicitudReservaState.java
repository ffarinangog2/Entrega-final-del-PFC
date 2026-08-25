package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

public interface SolicitudReservaState {
    void validarActualizacion();
    EstadoSolicitud ponerEnRevision();
    EstadoSolicitud aprobar();
    EstadoSolicitud rechazar();
    EstadoSolicitud cancelar();
    EstadoSolicitud proponerAlternativa();
    EstadoSolicitud aceptarPropuesta();
    EstadoSolicitud rechazarPropuesta();
}
