package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

import java.util.Map;
import java.util.Set;

abstract class AbstractSolicitudReservaState implements SolicitudReservaState {
    private final Map<AccionSolicitud, EstadoSolicitud> transiciones;
    private final Set<AccionSolicitud> accionesSinTransicion;

    protected AbstractSolicitudReservaState(
            Map<AccionSolicitud, EstadoSolicitud> transiciones,
            Set<AccionSolicitud> accionesSinTransicion) {
        this.transiciones = transiciones;
        this.accionesSinTransicion = accionesSinTransicion;
    }

    @Override
    public void validarActualizacion() {
        if (!accionesSinTransicion.contains(AccionSolicitud.ACTUALIZAR)) {
            throw new IllegalStateException("La solicitud no puede actualizarse en su estado actual");
        }
    }

    @Override
    public EstadoSolicitud ponerEnRevision() {
        return transicionar(AccionSolicitud.PONER_EN_REVISION);
    }

    @Override
    public EstadoSolicitud aprobar() {
        return transicionar(AccionSolicitud.APROBAR);
    }

    @Override
    public EstadoSolicitud rechazar() {
        return transicionar(AccionSolicitud.RECHAZAR);
    }

    @Override
    public EstadoSolicitud cancelar() {
        return transicionar(AccionSolicitud.CANCELAR);
    }

    @Override public EstadoSolicitud proponerAlternativa() { return transicionar(AccionSolicitud.PROPONER_ALTERNATIVA); }
    @Override public EstadoSolicitud aceptarPropuesta() { return transicionar(AccionSolicitud.ACEPTAR_PROPUESTA); }
    @Override public EstadoSolicitud rechazarPropuesta() { return transicionar(AccionSolicitud.RECHAZAR_PROPUESTA); }

    private EstadoSolicitud transicionar(AccionSolicitud accion) {
        EstadoSolicitud destino = transiciones.get(accion);
        if (destino == null) {
            throw new IllegalStateException(mensajeInvalido(accion));
        }
        return destino;
    }

    private String mensajeInvalido(AccionSolicitud accion) {
        return switch (accion) {
            case ACTUALIZAR -> "La solicitud no puede actualizarse en su estado actual";
            case PONER_EN_REVISION -> "La solicitud solamente puede ponerse en revisión cuando está pendiente";
            case APROBAR -> "La solicitud solamente puede aprobarse cuando está en revisión";
            case RECHAZAR -> "La solicitud solamente puede rechazarse cuando está en revisión";
            case CANCELAR -> "La solicitud no puede cancelarse en su estado actual";
            case PROPONER_ALTERNATIVA -> "Solo puede proponerse una alternativa a una solicitud en revisión";
            case ACEPTAR_PROPUESTA, RECHAZAR_PROPUESTA -> "La solicitud no tiene una propuesta pendiente";
        };
    }
}
