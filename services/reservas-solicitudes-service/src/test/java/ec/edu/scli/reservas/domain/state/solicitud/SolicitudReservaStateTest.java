package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudReservaStateTest {

    @Test
    void pendientePuedePasarARevision() {
        assertEquals(EstadoSolicitud.EN_REVISION,
                SolicitudReservaStates.desde(EstadoSolicitud.PENDIENTE).ponerEnRevision());
    }

    @Test
    void enRevisionPuedeAprobarse() {
        assertEquals(EstadoSolicitud.APROBADA,
                SolicitudReservaStates.desde(EstadoSolicitud.EN_REVISION).aprobar());
    }

    @Test
    void enRevisionPuedeRechazarse() {
        assertEquals(EstadoSolicitud.RECHAZADA,
                SolicitudReservaStates.desde(EstadoSolicitud.EN_REVISION).rechazar());
    }

    @Test
    void aprobadaPuedeCancelarse() {
        assertEquals(EstadoSolicitud.CANCELADA,
                SolicitudReservaStates.desde(EstadoSolicitud.APROBADA).cancelar());
    }

    @Test
    void pendienteYEnRevisionPuedenRetirarse() {
        assertEquals(EstadoSolicitud.CANCELADA,
                SolicitudReservaStates.desde(EstadoSolicitud.PENDIENTE).cancelar());
        assertEquals(EstadoSolicitud.CANCELADA,
                SolicitudReservaStates.desde(EstadoSolicitud.EN_REVISION).cancelar());
    }

    @Test
    void propuestaVuelveARevisionAlAceptarORechazar() {
        assertEquals(EstadoSolicitud.PROPUESTA,
                SolicitudReservaStates.desde(EstadoSolicitud.EN_REVISION).proponerAlternativa());
        assertEquals(EstadoSolicitud.EN_REVISION,
                SolicitudReservaStates.desde(EstadoSolicitud.PROPUESTA).aceptarPropuesta());
        assertEquals(EstadoSolicitud.EN_REVISION,
                SolicitudReservaStates.desde(EstadoSolicitud.PROPUESTA).rechazarPropuesta());
    }

    @Test
    void pendienteNoPuedeAprobarseDirectamente() {
        assertThrows(IllegalStateException.class,
                () -> SolicitudReservaStates.desde(EstadoSolicitud.PENDIENTE).aprobar());
    }

    @Test
    void rechazadaNoPuedeVolverARevision() {
        assertThrows(IllegalStateException.class,
                () -> SolicitudReservaStates.desde(EstadoSolicitud.RECHAZADA).ponerEnRevision());
    }
}
