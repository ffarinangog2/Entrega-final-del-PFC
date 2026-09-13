package ec.edu.scli.reservas.presentation.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HorarioRequestValidationTest {
    private final UUID id = UUID.randomUUID();

    @Test
    void validaHorarioDeCreacion() {
        assertTrue(crear(LocalTime.of(8, 0), LocalTime.of(9, 0)).isHorarioValido());
        assertFalse(crear(LocalTime.of(9, 0), LocalTime.of(8, 0)).isHorarioValido());
    }

    @Test
    void validaHorarioDeActualizacion() {
        assertTrue(actualizar(LocalTime.of(8, 0), LocalTime.of(9, 0)).isHorarioValido());
        assertFalse(actualizar(LocalTime.of(9, 0), LocalTime.of(8, 0)).isHorarioValido());
    }

    @Test
    void validaHorarioDeBloqueo() {
        assertTrue(bloqueo(LocalTime.of(8, 0), LocalTime.of(9, 0)).isHorarioValido());
        assertFalse(bloqueo(LocalTime.of(9, 0), LocalTime.of(8, 0)).isHorarioValido());
    }

    private CrearSolicitudReservaRequest crear(LocalTime inicio, LocalTime fin) {
        return new CrearSolicitudReservaRequest(
                id, id, id, id, id, LocalDate.now(), inicio, fin, 1, "x", null);
    }

    private ActualizarSolicitudReservaRequest actualizar(LocalTime inicio, LocalTime fin) {
        return new ActualizarSolicitudReservaRequest(
                id, id, id, id, LocalDate.now(), inicio, fin, 1, "x", null);
    }

    private CrearBloqueoAgendaRequest bloqueo(LocalTime inicio, LocalTime fin) {
        return new CrearBloqueoAgendaRequest(id, LocalDate.now(), inicio, fin, "x");
    }
}
